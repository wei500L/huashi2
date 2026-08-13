package com.huashi.eftransfer.app.modules.assessment.service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

final class ResearchExportWorkbook {

    private static final ZoneId BEIJING = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MAX_CELL = 32_000;

    private ResearchExportWorkbook() {
    }

    record AttemptRow(
            String participantCode,
            String participantType,
            String status,
            String submitReason,
            Integer answeredCount,
            Integer questionCount,
            Double percentageScore,
            Long durationMs,
            List<String> qualityFlags,
            Integer attachmentCount,
            String aiStatus,
            LocalDateTime startedAt,
            LocalDateTime lastSavedAt,
            LocalDateTime submittedAt
    ) {
    }

    record AnswerRow(
            String participantCode,
            Integer questionOrder,
            String questionType,
            String sectionCode,
            boolean formalSection,
            String stemText,
            String options,
            String response,
            String justification,
            Boolean correct,
            Integer scoreAwarded,
            Integer questionScore,
            Long durationMs,
            Integer changeCount
    ) {
    }

    record QuestionRow(
            Integer questionOrder,
            String questionType,
            String sectionCode,
            boolean formalSection,
            String stemText,
            String options,
            String correctAnswers
    ) {
    }

    record AttachmentRow(
            String participantCode,
            Integer questionOrder,
            Long fileId,
            String fileName,
            String mimeType,
            Long sizeBytes,
            String scanStatus
    ) {
    }

    record KvRow(String group, String item, String value, String note) {
    }

    record QuestionStatRow(
            Integer questionOrder,
            String questionCode,
            String section,
            String questionType,
            String stemText,
            long answeredCount,
            long skippedCount,
            Double correctRate,
            Double skipRate,
            Long medianReactionMs,
            boolean qualityWarning
    ) {
    }

    record OptionStatRow(
            Integer questionOrder,
            String questionCode,
            String stemText,
            String optionKey,
            String optionLabel,
            boolean correctOption,
            long count,
            Double answeredShare,
            Double submittedShare
    ) {
    }

    record DimensionStatRow(String dimension, long answeredCount, long correctCount, Double correctRate) {
    }

    record ReactionStatRow(
            Integer questionOrder,
            String questionCode,
            long sampleCount,
            Long medianMs,
            Long q1Ms,
            Long q3Ms,
            Long p90Ms
    ) {
    }

    record QualityFlagRow(String flag, long count, Double share) {
    }

    record GroupAiFindingRow(String section, Integer order, String text) {
    }

    record AttemptAiRow(
            String participantCode,
            String status,
            String source,
            String modelName,
            LocalDateTime completedAt,
            String overview,
            String strengths,
            String risks,
            String contextInterpretation,
            String reactionTimeInterpretation,
            String recommendations,
            Double confidence,
            String qualityNotice,
            String fallbackReason
    ) {
    }

    static final class WorkbookData {
        final String paperTitle;
        final String releaseCode;
        final String generatedAt;
        final String filterSummary;
        final List<AttemptRow> attempts;
        final List<AnswerRow> answers;
        final List<QuestionRow> questions;
        final List<AttachmentRow> attachments;
        List<KvRow> summary = List.of();
        List<QuestionStatRow> questionStats = List.of();
        List<QuestionStatRow> hardQuestions = List.of();
        List<OptionStatRow> optionStats = List.of();
        List<DimensionStatRow> dimensions = List.of();
        List<ReactionStatRow> reactionTimes = List.of();
        List<QualityFlagRow> qualityFlags = List.of();
        List<KvRow> groupAiMeta = List.of();
        List<GroupAiFindingRow> groupAiFindings = List.of();
        List<AttemptAiRow> attemptAi = List.of();

        WorkbookData(
                String paperTitle,
                String releaseCode,
                String generatedAt,
                String filterSummary,
                List<AttemptRow> attempts,
                List<AnswerRow> answers,
                List<QuestionRow> questions,
                List<AttachmentRow> attachments
        ) {
            this.paperTitle = paperTitle;
            this.releaseCode = releaseCode;
            this.generatedAt = generatedAt;
            this.filterSummary = filterSummary;
            this.attempts = attempts == null ? List.of() : attempts;
            this.answers = answers == null ? List.of() : answers;
            this.questions = questions == null ? List.of() : questions;
            this.attachments = attachments == null ? List.of() : attachments;
        }

        String paperTitle() { return paperTitle; }
        String releaseCode() { return releaseCode; }
        String generatedAt() { return generatedAt; }
        String filterSummary() { return filterSummary; }
        List<AttemptRow> attempts() { return attempts; }
        List<AnswerRow> answers() { return answers; }
        List<QuestionRow> questions() { return questions; }
        List<AttachmentRow> attachments() { return attachments; }
    }

    static byte[] write(WorkbookData data) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            CellStyle headerStyle = headerStyle(workbook);
            writeReadme(workbook, headerStyle, data);
            writeSummary(workbook, headerStyle, data.summary);
            writeQuestionStats(workbook, headerStyle, data.questionStats);
            writeHardQuestions(workbook, headerStyle, data.hardQuestions);
            writeOptionStats(workbook, headerStyle, data.optionStats);
            writeDimensions(workbook, headerStyle, data.dimensions);
            writeReactionTimes(workbook, headerStyle, data.reactionTimes);
            writeQualityFlags(workbook, headerStyle, data.qualityFlags);
            writeProfileSummary(workbook, headerStyle, data);
            writeGroupAi(workbook, headerStyle, data);
            writeAttemptAi(workbook, headerStyle, data.attemptAi);
            writeAttempts(workbook, headerStyle, data.attempts());
            writeWideAnswers(workbook, headerStyle, data);
            writeAnswerDetails(workbook, headerStyle, data.answers());
            writeCodebook(workbook, headerStyle, data.questions());
            writeAttachments(workbook, headerStyle, data.attachments());
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private static void writeReadme(XSSFWorkbook workbook, CellStyle headerStyle, WorkbookData data) {
        Sheet sheet = workbook.createSheet("导出说明");
        String[][] rows = {
                {"问卷标题", nullToEmpty(data.paperTitle())},
                {"发布编号", nullToEmpty(data.releaseCode())},
                {"导出时间（北京时间）", nullToEmpty(data.generatedAt())},
                {"当前筛选", nullToEmpty(data.filterSummary())},
                {"时区", "Asia/Shanghai（北京时间）。库内 UTC 已换算。"},
                {"总体统计", "漏斗、完成率、分数/用时分布、质量与 AI 状态。完成率=已提交/已开始。"},
                {"题目统计", "每题作答数、跳过数、正确率、中位用时。正确率分母是有效作答，不含未答。"},
                {"错题排行", "正确率从低到高，至少 3 次有效作答才进入排名。"},
                {"选项分布", "选择题各选项被选次数和占比，用于干扰项分析。"},
                {"维度统计", "按构念/迁移类别汇总正确率。"},
                {"作答用时", "每题反应时的中位数和四分位。"},
                {"质量标记", "过快作答、总时长过短等标记的人数。"},
                {"资料汇总", "资料题（姓名、联系方式、学业背景）按人展开。"},
                {"群体AI报告", "当前已生成的群体解读；未生成时表内会说明。"},
                {"单份AI摘要", "每位参与者的单份解读或规则摘要。"},
                {"答卷总览", "一行一位参与者：进度、用时、质量标记、提交时间。"},
                {"作答宽表", "一行一位参与者，每题一列，便于 SPSS / Excel 透视。列名是 Q题号。"},
                {"逐题明细", "一行一题：题干、作答、对错、用时，适合逐项核对。"},
                {"题目说明", "题号、部分、题型、选项和参考答案。"},
                {"附件清单", "附件元数据。文件需在系统内单独下载。"},
                {"匿名编号", "P-xxxxxx。姓名、联系方式见资料汇总或资料题作答列。"},
        };
        writeHeader(sheet, headerStyle, "项目", "说明");
        int rowIndex = 1;
        for (String[] row : rows) {
            writeRow(sheet, rowIndex++, row[0], row[1]);
        }
        autosize(sheet, 2);
        sheet.createFreezePane(0, 1);
    }

    private static void writeSummary(XSSFWorkbook workbook, CellStyle headerStyle, List<KvRow> rows) {
        Sheet sheet = workbook.createSheet("总体统计");
        writeHeader(sheet, headerStyle, "分组", "指标", "数值", "口径");
        int rowIndex = 1;
        for (KvRow row : rows) {
            writeRow(sheet, rowIndex++, row.group(), row.item(), row.value(), row.note());
        }
        if (rows.isEmpty()) {
            writeRow(sheet, rowIndex, "总体", "暂无统计", "", "这份发布还没有可汇总的答卷。");
        }
        sheet.createFreezePane(0, 1);
        autosize(sheet, 4);
    }

    private static void writeQuestionStats(XSSFWorkbook workbook, CellStyle headerStyle, List<QuestionStatRow> rows) {
        Sheet sheet = workbook.createSheet("题目统计");
        writeHeader(sheet, headerStyle, "题号", "题码", "部分", "题型", "题干", "作答数", "跳过数", "正确率", "跳过率", "中位用时秒", "质量提醒");
        int rowIndex = 1;
        for (QuestionStatRow row : rows) {
            writeRow(sheet, rowIndex++,
                    row.questionOrder(),
                    row.questionCode(),
                    row.section(),
                    questionTypeLabel(row.questionType()),
                    row.stemText(),
                    row.answeredCount(),
                    row.skippedCount(),
                    percent(row.correctRate()),
                    percent(row.skipRate()),
                    toSeconds(row.medianReactionMs()),
                    row.qualityWarning() ? "有过快作答" : "");
        }
        sheet.createFreezePane(0, 1);
        if (!rows.isEmpty()) {
            sheet.setAutoFilter(new CellRangeAddress(0, rows.size(), 0, 10));
        }
        sheet.setColumnWidth(4, 40 * 256);
    }

    private static void writeHardQuestions(XSSFWorkbook workbook, CellStyle headerStyle, List<QuestionStatRow> rows) {
        Sheet sheet = workbook.createSheet("错题排行");
        writeHeader(sheet, headerStyle, "排名", "题号", "题码", "部分", "题干", "正确率", "作答数", "跳过数");
        int rowIndex = 1;
        int rank = 1;
        for (QuestionStatRow row : rows) {
            writeRow(sheet, rowIndex++,
                    rank++,
                    row.questionOrder(),
                    row.questionCode(),
                    row.section(),
                    row.stemText(),
                    percent(row.correctRate()),
                    row.answeredCount(),
                    row.skippedCount());
        }
        if (rows.isEmpty()) {
            writeRow(sheet, 1, "", "", "", "", "有效作答不足，暂不排名", "", "", "");
        }
        sheet.createFreezePane(0, 1);
        sheet.setColumnWidth(4, 40 * 256);
    }

    private static void writeOptionStats(XSSFWorkbook workbook, CellStyle headerStyle, List<OptionStatRow> rows) {
        Sheet sheet = workbook.createSheet("选项分布");
        writeHeader(sheet, headerStyle, "题号", "题码", "题干", "选项", "选项内容", "是否正确答案", "选择人数", "占作答", "占总提交");
        int rowIndex = 1;
        for (OptionStatRow row : rows) {
            writeRow(sheet, rowIndex++,
                    row.questionOrder(),
                    row.questionCode(),
                    row.stemText(),
                    row.optionKey(),
                    row.optionLabel(),
                    row.correctOption() ? "是" : "否",
                    row.count(),
                    percent(row.answeredShare()),
                    percent(row.submittedShare()));
        }
        sheet.createFreezePane(0, 1);
        if (!rows.isEmpty()) {
            sheet.setAutoFilter(new CellRangeAddress(0, rows.size(), 0, 8));
        }
        sheet.setColumnWidth(2, 36 * 256);
        sheet.setColumnWidth(4, 24 * 256);
    }

    private static void writeDimensions(XSSFWorkbook workbook, CellStyle headerStyle, List<DimensionStatRow> rows) {
        Sheet sheet = workbook.createSheet("维度统计");
        writeHeader(sheet, headerStyle, "维度", "有效作答", "答对", "正确率");
        int rowIndex = 1;
        for (DimensionStatRow row : rows) {
            writeRow(sheet, rowIndex++, row.dimension(), row.answeredCount(), row.correctCount(), percent(row.correctRate()));
        }
        sheet.createFreezePane(0, 1);
        autosize(sheet, 4);
    }

    private static void writeReactionTimes(XSSFWorkbook workbook, CellStyle headerStyle, List<ReactionStatRow> rows) {
        Sheet sheet = workbook.createSheet("作答用时");
        writeHeader(sheet, headerStyle, "题号", "题码", "样本", "中位秒", "P25秒", "P75秒", "P90秒");
        int rowIndex = 1;
        for (ReactionStatRow row : rows) {
            writeRow(sheet, rowIndex++,
                    row.questionOrder(),
                    row.questionCode(),
                    row.sampleCount(),
                    toSeconds(row.medianMs()),
                    toSeconds(row.q1Ms()),
                    toSeconds(row.q3Ms()),
                    toSeconds(row.p90Ms()));
        }
        sheet.createFreezePane(0, 1);
        if (!rows.isEmpty()) {
            sheet.setAutoFilter(new CellRangeAddress(0, rows.size(), 0, 6));
        }
    }

    private static void writeQualityFlags(XSSFWorkbook workbook, CellStyle headerStyle, List<QualityFlagRow> rows) {
        Sheet sheet = workbook.createSheet("质量标记");
        writeHeader(sheet, headerStyle, "标记", "人数", "占已提交");
        int rowIndex = 1;
        for (QualityFlagRow row : rows) {
            writeRow(sheet, rowIndex++, qualityFlagText(row.flag()), row.count(), percent(row.share()));
        }
        if (rows.isEmpty()) {
            writeRow(sheet, 1, "无质量标记", 0, "");
        }
        sheet.createFreezePane(0, 1);
        autosize(sheet, 3);
    }

    private static void writeProfileSummary(XSSFWorkbook workbook, CellStyle headerStyle, WorkbookData data) {
        Sheet sheet = workbook.createSheet("资料汇总");
        List<AnswerRow> profileAnswers = data.answers().stream()
                .filter(answer -> !answer.formalSection() && !"INSTRUCTION".equalsIgnoreCase(answer.questionType()))
                .sorted(Comparator.comparing((AnswerRow row) -> row.questionOrder() == null ? 0 : row.questionOrder()))
                .toList();
        List<Integer> orders = profileAnswers.stream().map(AnswerRow::questionOrder).filter(Objects::nonNull).distinct().sorted().toList();
        Map<Integer, String> headersByOrder = new LinkedHashMap<>();
        for (AnswerRow answer : profileAnswers) {
            if (answer.questionOrder() == null || headersByOrder.containsKey(answer.questionOrder())) {
                continue;
            }
            String stem = answer.stemText() == null ? "" : answer.stemText().replaceAll("\\s+", " ").trim();
            if (stem.length() > 18) {
                stem = stem.substring(0, 18) + "…";
            }
            headersByOrder.put(answer.questionOrder(), stem.isBlank() ? "Q" + answer.questionOrder() : stem);
        }
        List<String> headers = new ArrayList<>();
        headers.add("匿名编号");
        for (Integer order : orders) {
            headers.add(headersByOrder.getOrDefault(order, "Q" + order));
        }
        writeHeader(sheet, headerStyle, headers.toArray(String[]::new));
        Map<String, Map<Integer, String>> byParticipant = new LinkedHashMap<>();
        for (AnswerRow answer : profileAnswers) {
            byParticipant.computeIfAbsent(answer.participantCode(), key -> new LinkedHashMap<>())
                    .put(answer.questionOrder(), joinNonBlank(answer.response(), answer.justification()));
        }
        for (AttemptRow attempt : data.attempts()) {
            byParticipant.computeIfAbsent(attempt.participantCode(), key -> new LinkedHashMap<>());
        }
        int rowIndex = 1;
        for (Map.Entry<String, Map<Integer, String>> entry : byParticipant.entrySet()) {
            Object[] values = new Object[headers.size()];
            values[0] = entry.getKey();
            for (int i = 0; i < orders.size(); i++) {
                values[i + 1] = entry.getValue().getOrDefault(orders.get(i), "");
            }
            writeRow(sheet, rowIndex++, values);
        }
        if (orders.isEmpty()) {
            writeRow(sheet, 1, "本问卷没有资料题，或当前筛选下没有作答。");
        }
        sheet.createFreezePane(1, 1);
    }

    private static void writeGroupAi(XSSFWorkbook workbook, CellStyle headerStyle, WorkbookData data) {
        Sheet sheet = workbook.createSheet("群体AI报告");
        writeHeader(sheet, headerStyle, "区块", "序号", "内容");
        int rowIndex = 1;
        if (data.groupAiMeta.isEmpty() && data.groupAiFindings.isEmpty()) {
            writeRow(sheet, rowIndex, "说明", "", "尚未生成群体报告。可在数据页点击「生成 / 刷新报告」，样本不足时只会保留规则统计。");
            autosize(sheet, 3);
            return;
        }
        for (KvRow row : data.groupAiMeta) {
            writeRow(sheet, rowIndex++, "报告信息", row.item(), joinNonBlank(row.value(), row.note()));
        }
        for (GroupAiFindingRow row : data.groupAiFindings) {
            writeRow(sheet, rowIndex++, row.section(), row.order(), row.text());
        }
        sheet.createFreezePane(0, 1);
        sheet.setColumnWidth(0, 16 * 256);
        sheet.setColumnWidth(2, 64 * 256);
    }

    private static void writeAttemptAi(XSSFWorkbook workbook, CellStyle headerStyle, List<AttemptAiRow> rows) {
        Sheet sheet = workbook.createSheet("单份AI摘要");
        writeHeader(sheet, headerStyle,
                "匿名编号", "状态", "来源", "模型", "完成时间", "总览", "优势", "风险",
                "语境解读", "反应时解读", "建议", "置信度", "质量提醒", "降级原因");
        int rowIndex = 1;
        for (AttemptAiRow row : rows) {
            writeRow(sheet, rowIndex++,
                    row.participantCode(),
                    aiStatusLabel(row.status()),
                    row.source(),
                    row.modelName(),
                    formatDateTime(row.completedAt()),
                    row.overview(),
                    row.strengths(),
                    row.risks(),
                    row.contextInterpretation(),
                    row.reactionTimeInterpretation(),
                    row.recommendations(),
                    row.confidence() == null ? "" : percent(row.confidence()),
                    row.qualityNotice(),
                    row.fallbackReason());
        }
        if (rows.isEmpty()) {
            writeRow(sheet, 1, "", "未生成", "", "", "", "当前筛选下还没有单份解读。", "", "", "", "", "", "", "");
        }
        sheet.createFreezePane(1, 1);
        if (!rows.isEmpty()) {
            sheet.setAutoFilter(new CellRangeAddress(0, rows.size(), 0, 13));
        }
        sheet.setColumnWidth(5, 40 * 256);
    }

    private static void writeAttempts(XSSFWorkbook workbook, CellStyle headerStyle, List<AttemptRow> attempts) {
        Sheet sheet = workbook.createSheet("答卷总览");
        writeHeader(sheet, headerStyle,
                "匿名编号", "进入方式", "状态", "提交方式", "已答", "总题数", "参考分",
                "用时秒", "质量标记", "附件数", "解读状态", "开始时间", "最后保存", "提交时间");
        int rowIndex = 1;
        for (AttemptRow row : attempts) {
            writeRow(sheet, rowIndex++,
                    row.participantCode(),
                    participantTypeLabel(row.participantType()),
                    statusLabel(row.status()),
                    submitReasonLabel(row.submitReason()),
                    row.answeredCount(),
                    row.questionCount(),
                    row.percentageScore(),
                    toSeconds(row.durationMs()),
                    joinFlags(row.qualityFlags()),
                    row.attachmentCount(),
                    aiStatusLabel(row.aiStatus()),
                    formatDateTime(row.startedAt()),
                    formatDateTime(row.lastSavedAt()),
                    formatDateTime(row.submittedAt()));
        }
        autosize(sheet, 14);
        sheet.createFreezePane(1, 1);
        if (!attempts.isEmpty()) {
            sheet.setAutoFilter(new CellRangeAddress(0, attempts.size(), 0, 13));
        }
    }

    private static void writeWideAnswers(XSSFWorkbook workbook, CellStyle headerStyle, WorkbookData data) {
        Sheet sheet = workbook.createSheet("作答宽表");
        List<Integer> orders = data.questions().stream()
                .map(QuestionRow::questionOrder)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
        if (orders.isEmpty()) {
            orders = data.answers().stream()
                    .map(AnswerRow::questionOrder)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .toList();
        }
        List<String> headers = new ArrayList<>();
        headers.add("匿名编号");
        for (Integer order : orders) {
            headers.add("Q" + String.format(Locale.ROOT, "%02d", order));
        }
        writeHeader(sheet, headerStyle, headers.toArray(String[]::new));

        Map<String, Map<Integer, String>> byParticipant = new LinkedHashMap<>();
        for (AnswerRow answer : data.answers()) {
            byParticipant.computeIfAbsent(answer.participantCode(), key -> new LinkedHashMap<>())
                    .put(answer.questionOrder(), joinNonBlank(answer.response(), answer.justification()));
        }
        for (AttemptRow attempt : data.attempts()) {
            byParticipant.computeIfAbsent(attempt.participantCode(), key -> new LinkedHashMap<>());
        }

        int rowIndex = 1;
        for (Map.Entry<String, Map<Integer, String>> entry : byParticipant.entrySet()) {
            Object[] values = new Object[headers.size()];
            values[0] = entry.getKey();
            for (int i = 0; i < orders.size(); i++) {
                values[i + 1] = entry.getValue().getOrDefault(orders.get(i), "");
            }
            writeRow(sheet, rowIndex++, values);
        }
        sheet.createFreezePane(1, 1);
        if (rowIndex > 1) {
            sheet.setAutoFilter(new CellRangeAddress(0, rowIndex - 1, 0, headers.size() - 1));
        }
        sheet.setColumnWidth(0, 14 * 256);
        for (int i = 1; i < Math.min(headers.size(), 40); i++) {
            sheet.setColumnWidth(i, 18 * 256);
        }
    }

    private static void writeAnswerDetails(XSSFWorkbook workbook, CellStyle headerStyle, List<AnswerRow> answers) {
        Sheet sheet = workbook.createSheet("逐题明细");
        writeHeader(sheet, headerStyle,
                "匿名编号", "题号", "部分", "题型", "题干", "选项", "作答", "补充说明",
                "是否正确", "得分", "满分", "用时秒", "修改次数");
        List<AnswerRow> sorted = new ArrayList<>(answers);
        sorted.sort(Comparator.comparing((AnswerRow row) -> nullToEmpty(row.participantCode()))
                .thenComparing(row -> row.questionOrder() == null ? 0 : row.questionOrder()));
        int rowIndex = 1;
        for (AnswerRow row : sorted) {
            writeRow(sheet, rowIndex++,
                    row.participantCode(),
                    row.questionOrder(),
                    sectionLabel(row.sectionCode(), row.formalSection()),
                    questionTypeLabel(row.questionType()),
                    row.stemText(),
                    row.options(),
                    row.response(),
                    row.justification(),
                    correctLabel(row.correct()),
                    row.scoreAwarded(),
                    row.questionScore(),
                    toSeconds(row.durationMs()),
                    row.changeCount());
        }
        sheet.createFreezePane(2, 1);
        sheet.setColumnWidth(0, 14 * 256);
        sheet.setColumnWidth(4, 40 * 256);
        sheet.setColumnWidth(5, 28 * 256);
        sheet.setColumnWidth(6, 24 * 256);
        if (!sorted.isEmpty()) {
            sheet.setAutoFilter(new CellRangeAddress(0, sorted.size(), 0, 12));
        }
    }

    private static void writeCodebook(XSSFWorkbook workbook, CellStyle headerStyle, List<QuestionRow> questions) {
        Sheet sheet = workbook.createSheet("题目说明");
        writeHeader(sheet, headerStyle, "题号", "宽表列", "部分", "题型", "题干", "选项", "参考答案");
        int rowIndex = 1;
        for (QuestionRow row : questions) {
            writeRow(sheet, rowIndex++,
                    row.questionOrder(),
                    row.questionOrder() == null ? "" : "Q" + String.format(Locale.ROOT, "%02d", row.questionOrder()),
                    sectionLabel(row.sectionCode(), row.formalSection()),
                    questionTypeLabel(row.questionType()),
                    row.stemText(),
                    row.options(),
                    row.correctAnswers());
        }
        sheet.createFreezePane(0, 1);
        autosize(sheet, 7);
        if (!questions.isEmpty()) {
            sheet.setAutoFilter(new CellRangeAddress(0, questions.size(), 0, 6));
        }
    }

    private static void writeAttachments(XSSFWorkbook workbook, CellStyle headerStyle, List<AttachmentRow> attachments) {
        Sheet sheet = workbook.createSheet("附件清单");
        writeHeader(sheet, headerStyle, "匿名编号", "题号", "文件ID", "文件名", "类型", "大小字节", "扫描状态");
        int rowIndex = 1;
        for (AttachmentRow row : attachments) {
            writeRow(sheet, rowIndex++,
                    row.participantCode(),
                    row.questionOrder(),
                    row.fileId(),
                    row.fileName(),
                    row.mimeType(),
                    row.sizeBytes(),
                    scanStatusLabel(row.scanStatus()));
        }
        sheet.createFreezePane(0, 1);
        autosize(sheet, 7);
    }

    private static CellStyle headerStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private static void writeHeader(Sheet sheet, CellStyle headerStyle, String... headers) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private static void writeRow(Sheet sheet, int rowIndex, Object... values) {
        Row row = sheet.createRow(rowIndex);
        for (int i = 0; i < values.length; i++) {
            setCell(row.createCell(i), values[i]);
        }
    }

    private static void setCell(Cell cell, Object value) {
        if (value == null) {
            cell.setBlank();
            return;
        }
        if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
            return;
        }
        if (value instanceof Boolean bool) {
            cell.setCellValue(bool);
            return;
        }
        cell.setCellValue(truncate(String.valueOf(value)));
    }

    private static void autosize(Sheet sheet, int columns) {
        int[] widths = {18, 24, 28, 36, 40, 20, 18};
        for (int i = 0; i < columns; i++) {
            int width = widths[Math.min(i, widths.length - 1)];
            sheet.setColumnWidth(i, Math.min(Math.max(width * 256, 12 * 256), 48 * 256));
        }
    }

    static String exportFileName(String paperTitle, String releaseCode, String extension) {
        String base = (paperTitle == null || paperTitle.isBlank() ? "研究问卷" : paperTitle)
                .replaceAll("[\\\\/:*?\"<>|\\r\\n]", "-")
                .replaceAll("\\s+", "-")
                .replaceAll("-{2,}", "-");
        if (base.length() > 36) {
            base = base.substring(0, 36);
        }
        String code = releaseCode == null || releaseCode.isBlank() ? "" : "-" + releaseCode.replaceAll("[\\\\/:*?\"<>|\\s]", "");
        String date = java.time.LocalDate.now(BEIJING).format(DateTimeFormatter.BASIC_ISO_DATE);
        String suffix = extension == null || extension.isBlank() ? "xlsx" : extension.replace(".", "");
        return "研究数据-" + base + code + "-" + date + "." + suffix;
    }

    static String contentDisposition(String fileName) {
        String safe = fileName == null || fileName.isBlank() ? "research-export.xlsx" : fileName;
        String ascii = safe.replaceAll("[^\\x20-\\x7E]", "_");
        String encoded = java.net.URLEncoder.encode(safe, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20");
        return "attachment; filename=\"" + ascii + "\"; filename*=UTF-8''" + encoded;
    }

    static String formatDateTime(LocalDateTime utcNaive) {
        if (utcNaive == null) {
            return "";
        }
        return utcNaive.atZone(ZoneOffset.UTC).withZoneSameInstant(BEIJING).format(DATE_TIME);
    }

    static String questionTypeLabel(String type) {
        if (type == null) {
            return "";
        }
        return switch (type.toUpperCase(Locale.ROOT)) {
            case "INSTRUCTION" -> "说明文字";
            case "INFORMED_CONSENT" -> "知情同意";
            case "SHORT_TEXT" -> "短文本";
            case "NUMBER" -> "数字填写";
            case "SINGLE_CHOICE" -> "单选题";
            case "MULTIPLE_CHOICE" -> "多选题";
            case "FILL_BLANK" -> "填空题";
            case "TRUE_FALSE_WITH_JUSTIFICATION" -> "判断并说明";
            case "TRUE_FALSE" -> "判断题";
            case "SPELLING" -> "拼写";
            case "FILE_UPLOAD" -> "文件上传";
            default -> type;
        };
    }

    static String sectionLabel(String sectionCode, boolean formalSection) {
        if (sectionCode != null && sectionCode.toUpperCase(Locale.ROOT).startsWith("BASIC")) {
            return "资料";
        }
        return formalSection ? "正式题" : "资料";
    }

    static boolean resolveFormalSection(String sectionCode, String questionType) {
        if (sectionCode != null && sectionCode.toUpperCase(Locale.ROOT).startsWith("BASIC")) {
            return false;
        }
        return !"INSTRUCTION".equalsIgnoreCase(questionType)
                || (sectionCode != null && !sectionCode.toUpperCase(Locale.ROOT).contains("BASIC"));
    }

    private static String participantTypeLabel(String type) {
        if (type == null) {
            return "";
        }
        return switch (type.toUpperCase(Locale.ROOT)) {
            case "PUBLIC_CODE" -> "参与码";
            case "PUBLIC_QR" -> "二维码";
            default -> type;
        };
    }

    private static String statusLabel(String status) {
        if (status == null) {
            return "";
        }
        return switch (status.toUpperCase(Locale.ROOT)) {
            case "SUBMITTED" -> "已提交";
            case "IN_PROGRESS" -> "作答中";
            default -> status;
        };
    }

    private static String submitReasonLabel(String reason) {
        if (reason == null || reason.isBlank()) {
            return "";
        }
        return switch (reason.toUpperCase(Locale.ROOT)) {
            case "MANUAL" -> "主动提交";
            case "TIMEOUT" -> "超时提交";
            case "SCHEDULER" -> "系统提交";
            default -> reason;
        };
    }

    private static String aiStatusLabel(String status) {
        if (status == null || status.isBlank()) {
            return "未生成";
        }
        return switch (status.toUpperCase(Locale.ROOT)) {
            case "PENDING" -> "排队中";
            case "PROCESSING" -> "生成中";
            case "COMPLETED" -> "已完成";
            case "FAILED" -> "失败";
            case "FALLBACK" -> "规则摘要";
            default -> status;
        };
    }

    private static String scanStatusLabel(String status) {
        if (status == null) {
            return "";
        }
        return switch (status.toUpperCase(Locale.ROOT)) {
            case "CLEAN" -> "扫描通过";
            case "PENDING" -> "扫描中";
            case "INFECTED" -> "未通过扫描";
            case "FAILED" -> "扫描失败";
            default -> status;
        };
    }

    static String percent(Double value) {
        if (value == null) {
            return "";
        }
        return (Math.round(value * 1000d) / 10d) + "%";
    }

    static String qualityFlagText(String flag) {
        if (flag == null || flag.isBlank()) {
            return "";
        }
        return switch (flag.toUpperCase(Locale.ROOT)) {
            case "FAST_ITEM" -> "过快作答";
            case "SHORT_TOTAL_DURATION" -> "总时长过短";
            case "TIMING_GAP" -> "计时缺失";
            default -> flag;
        };
    }

    static String joinList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.stream().filter(Objects::nonNull).map(String::trim).filter(value -> !value.isEmpty()).collect(java.util.stream.Collectors.joining("；"));
    }

    private static String joinFlags(List<String> flags) {
        if (flags == null || flags.isEmpty()) {
            return "";
        }
        List<String> labels = new ArrayList<>();
        for (String flag : flags) {
            labels.add(qualityFlagText(flag));
        }
        return String.join("、", labels);
    }

    private static String correctLabel(Boolean correct) {
        if (correct == null) {
            return "";
        }
        return correct ? "对" : "错";
    }

    private static Double toSeconds(Long durationMs) {
        if (durationMs == null) {
            return null;
        }
        return Math.round(durationMs / 100.0) / 10.0;
    }

    private static String joinNonBlank(String response, String justification) {
        if (response == null || response.isBlank()) {
            return justification == null ? "" : justification;
        }
        if (justification == null || justification.isBlank()) {
            return response;
        }
        return response + "；" + justification;
    }

    private static String truncate(String value) {
        if (value.length() <= MAX_CELL) {
            return value;
        }
        return value.substring(0, MAX_CELL);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
