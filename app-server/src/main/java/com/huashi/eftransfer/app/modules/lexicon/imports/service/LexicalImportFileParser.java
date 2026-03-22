package com.huashi.eftransfer.app.modules.lexicon.imports.service;

import com.huashi.eftransfer.app.modules.lexicon.imports.support.LexicalImportParsedRow;
import com.huashi.eftransfer.app.modules.lexicon.imports.support.LexicalImportRowDraft;
import com.huashi.eftransfer.app.modules.lexicon.imports.support.LexicalImportSourceFormat;
import com.huashi.eftransfer.app.modules.lexicon.imports.support.LexicalImportTemplateSupport;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.exception.BusinessException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class LexicalImportFileParser {

    private final LexicalImportTemplateSupport templateSupport;

    public LexicalImportFileParser(LexicalImportTemplateSupport templateSupport) {
        this.templateSupport = templateSupport;
    }

    public List<LexicalImportParsedRow> parse(byte[] content, LexicalImportSourceFormat format) {
        try {
            return switch (format) {
                case CSV -> parseCsv(content);
                case XLSX -> parseXlsx(content);
            };
        } catch (IOException exception) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Failed to read import file", 400);
        }
    }

    private List<LexicalImportParsedRow> parseCsv(byte[] content) throws IOException {
        try (Reader reader = new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreEmptyLines(true)
                     .setTrim(true)
                     .build()
                     .parse(reader)) {
            templateSupport.validateHeaders(parser.getHeaderMap().keySet());
            List<LexicalImportParsedRow> rows = new ArrayList<>();
            for (CSVRecord record : parser) {
                Map<String, String> values = new LinkedHashMap<>();
                for (String header : templateSupport.templateHeaders()) {
                    if (record.isMapped(header)) {
                        values.put(header, record.get(header));
                    }
                }
                LexicalImportRowDraft draft = templateSupport.toDraft(values);
                rows.add(new LexicalImportParsedRow((int) record.getRecordNumber() + 1, draft));
            }
            return rows;
        }
    }

    private List<LexicalImportParsedRow> parseXlsx(byte[] content) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "Failed to read import file", 400);
            }

            DataFormatter formatter = new DataFormatter();
            Row headerRow = findFirstNonEmptyRow(sheet);
            if (headerRow == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "Import file must contain a header row", 400);
            }

            List<String> headers = readHeaders(headerRow, formatter);
            templateSupport.validateHeaders(headers);

            List<LexicalImportParsedRow> rows = new ArrayList<>();
            for (int rowIndex = headerRow.getRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex += 1) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || isEmpty(row, headers.size(), formatter)) {
                    continue;
                }
                Map<String, String> values = new LinkedHashMap<>();
                for (int cellIndex = 0; cellIndex < headers.size(); cellIndex += 1) {
                    String header = headers.get(cellIndex);
                    if (header == null || header.isBlank()) {
                        continue;
                    }
                    Cell cell = row.getCell(cellIndex);
                    values.put(header, cell == null ? null : formatter.formatCellValue(cell));
                }
                rows.add(new LexicalImportParsedRow(rowIndex + 1, templateSupport.toDraft(values)));
            }
            return rows;
        }
    }

    private Row findFirstNonEmptyRow(Sheet sheet) {
        for (int rowIndex = sheet.getFirstRowNum(); rowIndex <= sheet.getLastRowNum(); rowIndex += 1) {
            Row row = sheet.getRow(rowIndex);
            if (row != null && row.getLastCellNum() > 0) {
                return row;
            }
        }
        return null;
    }

    private List<String> readHeaders(Row headerRow, DataFormatter formatter) {
        List<String> headers = new ArrayList<>();
        short lastCellNum = headerRow.getLastCellNum();
        for (int cellIndex = 0; cellIndex < lastCellNum; cellIndex += 1) {
            Cell cell = headerRow.getCell(cellIndex);
            headers.add(cell == null ? "" : formatter.formatCellValue(cell).trim());
        }
        return headers;
    }

    private boolean isEmpty(Row row, int headerCount, DataFormatter formatter) {
        for (int cellIndex = 0; cellIndex < headerCount; cellIndex += 1) {
            Cell cell = row.getCell(cellIndex);
            if (cell != null && !formatter.formatCellValue(cell).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
