package com.huashi.eftransfer.app.modules.practice.service;

import com.huashi.eftransfer.app.modules.practice.support.PracticeSectionCatalog;
import com.huashi.eftransfer.app.modules.practice.vo.PracticeBankVO;
import com.huashi.eftransfer.app.modules.practice.vo.PracticeSectionVO;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.exception.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads the student self-practice question bank (the seeded FF4 V2 bank) and
 * exposes its four practice sections. Question content itself is snapshotted
 * by {@link PracticeSessionService} at session start.
 */
@Service
public class PracticeBankService {

    private static final List<String> ALL_CONSTRUCT_CODES = PracticeSectionCatalog.SECTIONS.stream()
            .flatMap(section -> section.constructCodes().stream())
            .toList();

    private final JdbcTemplate jdbcTemplate;

    public PracticeBankService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Loads bank metadata plus per-section question counts for the practice
     * bank. Returns an empty list when the bank has not been seeded.
     */
    public List<PracticeBankVO> listPracticeBanks() {
        List<BankRow> banks = queryBanks();
        if (banks.isEmpty()) {
            return List.of();
        }
        List<PracticeBankVO> result = new ArrayList<>();
        for (BankRow bank : banks) {
            Map<String, Integer> countsByConstruct = countQuestionsByConstruct(bank.id());
            List<PracticeSectionVO> sections = PracticeSectionCatalog.SECTIONS.stream()
                    .map(section -> new PracticeSectionVO(
                            section.code(),
                            section.title(),
                            section.description(),
                            sumCounts(countsByConstruct, section.constructCodes()),
                            section.constructCodes()
                    ))
                    .toList();
            int total = sections.stream().mapToInt(PracticeSectionVO::questionCount).sum();
            result.add(new PracticeBankVO(bank.bankCode(), bank.name(), bank.description(), total, sections));
        }
        return result;
    }

    /**
     * Loads the latest question version of every practice item of a bank,
     * optionally restricted to one section. Questions are returned in a
     * stable order; the session service shuffles them when a session starts.
     */
    public List<BankQuestion> loadBankQuestions(String bankCode, String sectionCode) {
        List<BankRow> banks = queryBanks();
        BankRow bank = banks.stream()
                .filter(row -> row.bankCode().equals(bankCode))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "Practice bank was not found", 404));
        List<String> constructCodes = ALL_CONSTRUCT_CODES;
        if (sectionCode != null) {
            if (!PracticeSectionCatalog.isPracticeSection(sectionCode)) {
                throw new BusinessException(ResultCode.VALIDATION_ERROR, "Unknown practice section: " + sectionCode, 400);
            }
            constructCodes = PracticeSectionCatalog.metaOf(sectionCode).constructCodes();
        }
        String constructPlaceholder = String.join(",", constructCodes.stream().map(code -> "?").toList());
        return jdbcTemplate.query("""
                        SELECT v.id, v.question_code, v.question_type, v.stem_text, v.prompt_text,
                               v.options_json, v.correct_answer_json, v.explanation_text,
                               v.option_explanations_json, v.construct_code, v.transfer_category, v.target_word
                        FROM assessment_question_version v
                        JOIN assessment_question_bank b ON b.id = v.question_bank_id
                        WHERE b.bank_code = ?
                          AND v.deleted = FALSE
                          AND v.construct_code IN (%s)
                          AND v.version_no = (
                              SELECT MAX(latest.version_no)
                              FROM assessment_question_version latest
                              WHERE latest.question_bank_id = v.question_bank_id
                                AND latest.question_code = v.question_code
                                AND latest.deleted = FALSE
                          )
                        ORDER BY v.question_code ASC
                        """.formatted(constructPlaceholder),
                (resultSet, rowNumber) -> new BankQuestion(
                        resultSet.getLong(1),
                        resultSet.getString(2),
                        resultSet.getString(3),
                        resultSet.getString(4),
                        resultSet.getString(5),
                        resultSet.getString(6),
                        resultSet.getString(7),
                        resultSet.getString(8),
                        resultSet.getString(9),
                        resultSet.getString(10),
                        resultSet.getString(11),
                        resultSet.getString(12)
                ),
                concat(bankCode, constructCodes)
        );
    }

    public record BankQuestion(
            Long questionVersionId,
            String questionCode,
            String questionType,
            String stemText,
            String promptText,
            String optionsJson,
            String correctAnswerJson,
            String explanationText,
            String optionExplanationsJson,
            String constructCode,
            String transferCategory,
            String targetWord
    ) {
    }

    private record BankRow(Long id, String bankCode, String name, String description) {
    }

    private List<BankRow> queryBanks() {
        return jdbcTemplate.query("""
                        SELECT id, bank_code, name, description
                        FROM assessment_question_bank
                        WHERE bank_code = ?
                          AND deleted = FALSE
                          AND status = 'ACTIVE'
                        """, (resultSet, rowNumber) -> new BankRow(
                resultSet.getLong(1),
                resultSet.getString(2),
                resultSet.getString(3),
                resultSet.getString(4)
        ), PracticeSectionCatalog.BANK_CODE);
    }

    private Map<String, Integer> countQuestionsByConstruct(Long bankId) {
        List<Map.Entry<String, Integer>> rows = jdbcTemplate.query("""
                        SELECT v.construct_code, COUNT(*) AS question_count
                        FROM assessment_question_version v
                        WHERE v.question_bank_id = ?
                          AND v.deleted = FALSE
                          AND v.construct_code IN (%s)
                          AND v.version_no = (
                              SELECT MAX(latest.version_no)
                              FROM assessment_question_version latest
                              WHERE latest.question_bank_id = v.question_bank_id
                                AND latest.question_code = v.question_code
                                AND latest.deleted = FALSE
                          )
                        GROUP BY v.construct_code
                        """.formatted(String.join(",", ALL_CONSTRUCT_CODES.stream().map(code -> "?").toList())),
                (resultSet, rowNumber) -> Map.entry(resultSet.getString(1), resultSet.getInt(2)),
                concat(bankId, ALL_CONSTRUCT_CODES)
        );
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> row : rows) {
            counts.put(row.getKey(), row.getValue());
        }
        return counts;
    }

    private int sumCounts(Map<String, Integer> countsByConstruct, List<String> constructCodes) {
        return constructCodes.stream().mapToInt(code -> countsByConstruct.getOrDefault(code, 0)).sum();
    }

    private Object[] concat(Object first, List<String> rest) {
        Object[] args = new Object[1 + rest.size()];
        args[0] = first;
        for (int index = 0; index < rest.size(); index++) {
            args[index + 1] = rest.get(index);
        }
        return args;
    }
}
