package com.huashi.eftransfer.app.modules.assessment.service;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResearchExportWorkbookTest {

    @Test
    void writesResearchSheetsWithBeijingTimeAndAnswers() throws Exception {
        ResearchExportWorkbook.WorkbookData data = new ResearchExportWorkbook.WorkbookData(
                "Lexi-Bridge V1",
                "RES-TEST",
                "2026-08-13 00:00:00",
                "全部答卷",
                List.of(new ResearchExportWorkbook.AttemptRow(
                        "P-000018",
                        1,
                        "PUBLIC_CODE",
                        "SUBMITTED",
                        "MANUAL",
                        1,
                        1,
                        80.0,
                        1_358_000L,
                        List.of("FAST_ITEM"),
                        0,
                        "FALLBACK",
                        LocalDateTime.of(2026, 8, 12, 5, 17, 30),
                        LocalDateTime.of(2026, 8, 12, 5, 40, 8),
                        LocalDateTime.of(2026, 8, 12, 5, 40, 8)
                )),
                List.of(new ResearchExportWorkbook.AnswerRow(
                        "P-000018",
                        1,
                        "SINGLE_CHOICE",
                        "P1",
                        true,
                        "actual 的意思是？",
                        "A. 实际的 | B. 当前的",
                        "B",
                        null,
                        false,
                        0,
                        1,
                        4000L,
                        1
                )),
                List.of(new ResearchExportWorkbook.QuestionRow(
                        1,
                        "SINGLE_CHOICE",
                        "P1",
                        true,
                        "actual 的意思是？",
                        "A. 实际的 | B. 当前的",
                        "A"
                )),
                List.of(),
                true
        );
        data.summary = List.of(new ResearchExportWorkbook.KvRow("完成率", "完成率", "50%（1/2）", "已提交 / 已开始"));
        data.questionStats = List.of(new ResearchExportWorkbook.QuestionStatRow(
                1, "Q1", "正式题", "SINGLE_CHOICE", "actual 的意思是？", 2, 0, 0.5, 0d, 1200L, false));
        data.hardQuestions = data.questionStats;
        data.groupAiMeta = List.of(new ResearchExportWorkbook.KvRow("报告信息", "状态", "COMPLETED", "AI"));
        data.groupAiFindings = List.of(new ResearchExportWorkbook.GroupAiFindingRow("执行摘要", 1, "假朋友题正确率偏低"));
        data.attemptAi = List.of(new ResearchExportWorkbook.AttemptAiRow(
                "P-000018", "FALLBACK", "规则摘要", "rules",
                LocalDateTime.of(2026, 8, 12, 5, 41, 0),
                "整体表现中等", "同源词较稳", "假朋友易混", "", "", "建议复练", 0.6, "", ""));
        byte[] bytes = ResearchExportWorkbook.write(data);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertThat(workbook.getSheet("导出说明")).isNotNull();
            assertThat(workbook.getSheet("总体统计")).isNotNull();
            assertThat(workbook.getSheet("题目统计")).isNotNull();
            assertThat(workbook.getSheet("错题排行")).isNotNull();
            assertThat(workbook.getSheet("群体AI报告")).isNotNull();
            assertThat(workbook.getSheet("单份AI摘要")).isNotNull();
            assertThat(workbook.getSheet("答卷总览")).isNotNull();
            assertThat(workbook.getSheet("作答宽表")).isNotNull();
            assertThat(workbook.getSheet("逐题明细")).isNotNull();
            assertThat(workbook.getSheet("题目说明")).isNotNull();
            assertThat(workbook.getSheet("总体统计").getRow(1).getCell(2).getStringCellValue()).contains("50%");
            assertThat(workbook.getSheet("群体AI报告").getRow(2).getCell(2).getStringCellValue()).contains("假朋友");
            assertThat(workbook.getSheet("单份AI摘要").getRow(1).getCell(5).getStringCellValue()).contains("整体表现");

            Sheet overview = workbook.getSheet("答卷总览");
            assertThat(overview.getRow(1).getCell(0).getStringCellValue()).isEqualTo("P-000018");
            assertThat(overview.getRow(1).getCell(1).getNumericCellValue()).isEqualTo(1d);
            assertThat(overview.getRow(1).getCell(2).getStringCellValue()).isEqualTo("参与码");
            assertThat(overview.getRow(1).getCell(9).getStringCellValue()).isEqualTo("过快作答");
            assertThat(overview.getRow(1).getCell(14).getStringCellValue()).isEqualTo("2026-08-12 13:40:08");

            Sheet wide = workbook.getSheet("作答宽表");
            assertThat(wide.getRow(0).getCell(1).getStringCellValue()).isEqualTo("Q01");
            assertThat(wide.getRow(1).getCell(1).getStringCellValue()).isEqualTo("B");

            Sheet details = workbook.getSheet("逐题明细");
            assertThat(details.getRow(1).getCell(4).getStringCellValue()).contains("actual");
            assertThat(details.getRow(1).getCell(6).getStringCellValue()).isEqualTo("B");
            assertThat(details.getRow(1).getCell(8).getStringCellValue()).isEqualTo("错");
        }
    }

    @Test
    void omitsProfileAnswersWhenIncludeSensitiveFieldsIsFalse() throws Exception {
        ResearchExportWorkbook.AttemptRow attempt = new ResearchExportWorkbook.AttemptRow(
                "P-000018",
                1,
                "PUBLIC_CODE",
                "SUBMITTED",
                "MANUAL",
                2,
                2,
                80.0,
                1_358_000L,
                List.of(),
                0,
                "FALLBACK",
                LocalDateTime.of(2026, 8, 12, 5, 17, 30),
                LocalDateTime.of(2026, 8, 12, 5, 40, 8),
                LocalDateTime.of(2026, 8, 12, 5, 40, 8)
        );
        ResearchExportWorkbook.WorkbookData data = new ResearchExportWorkbook.WorkbookData(
                "Lexi-Bridge V1",
                "RES-TEST",
                "2026-08-13 00:00:00",
                "全部答卷",
                List.of(attempt),
                List.of(
                        new ResearchExportWorkbook.AnswerRow(
                                "P-000018",
                                1,
                                "SHORT_TEXT",
                                "BASIC_INFO",
                                false,
                                "姓名",
                                "",
                                "张三",
                                "13800138000",
                                null,
                                0,
                                0,
                                1000L,
                                0
                        ),
                        new ResearchExportWorkbook.AnswerRow(
                                "P-000018",
                                2,
                                "SINGLE_CHOICE",
                                "P1",
                                true,
                                "actual 的意思是？",
                                "A. 实际的 | B. 当前的",
                                "B",
                                null,
                                false,
                                0,
                                1,
                                4000L,
                                1
                        )
                ),
                List.of(
                        new ResearchExportWorkbook.QuestionRow(1, "SHORT_TEXT", "BASIC_INFO", false, "姓名", "", ""),
                        new ResearchExportWorkbook.QuestionRow(2, "SINGLE_CHOICE", "P1", true, "actual 的意思是？", "A. 实际的 | B. 当前的", "A")
                ),
                List.of(),
                false
        );
        byte[] bytes = ResearchExportWorkbook.write(data);
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet profile = workbook.getSheet("资料汇总");
            assertThat(profile.getRow(1).getCell(0).getStringCellValue()).contains("非敏感导出");
            assertThat(profile.getRow(1).getCell(0).getStringCellValue()).doesNotContain("张三");

            Sheet wide = workbook.getSheet("作答宽表");
            assertThat(wide.getRow(0).getCell(1).getStringCellValue()).isEqualTo("Q02");
            assertThat(wide.getRow(1).getCell(1).getStringCellValue()).isEqualTo("B");

            Sheet details = workbook.getSheet("逐题明细");
            assertThat(details.getRow(1).getCell(4).getStringCellValue()).contains("actual");
            assertThat(details.getPhysicalNumberOfRows()).isEqualTo(2);

            Sheet codebook = workbook.getSheet("题目说明");
            assertThat(codebook.getRow(1).getCell(4).getStringCellValue()).contains("actual");
            assertThat(codebook.getPhysicalNumberOfRows()).isEqualTo(2);
        }
    }

    @Test
    void buildsDownloadableChineseFileName() {
        String name = ResearchExportWorkbook.exportFileName("Lexi-Bridge V1", "RES-TEST", "xlsx");
        assertThat(name).startsWith("研究数据-Lexi-Bridge-V1-RES-TEST-");
        assertThat(name).endsWith(".xlsx");
        assertThat(ResearchExportWorkbook.contentDisposition(name)).contains("filename*=UTF-8''");
    }
}
