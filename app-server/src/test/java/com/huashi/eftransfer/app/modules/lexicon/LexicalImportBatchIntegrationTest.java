package com.huashi.eftransfer.app.modules.lexicon;

import com.huashi.eftransfer.app.support.AbstractWebIntegrationTest;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LexicalImportBatchIntegrationTest extends AbstractWebIntegrationTest {

    private static final List<String> TEMPLATE_HEADERS = List.of(
            "english_word",
            "french_word",
            "chinese_gloss",
            "lexical_pair_type",
            "semantic_overlap_score",
            "false_friend_risk",
            "default_context_support",
            "difficulty_level",
            "notes",
            "source",
            "active",
            "tags",
            "knowledge_status",
            "embedding_status",
            "sense_english_definition",
            "sense_french_definition",
            "sense_chinese_definition",
            "example_english",
            "example_french",
            "example_chinese",
            "example_context_support"
    );

    @Test
    void shouldCreateDraftFixInvalidRowCommitImportAndDownloadOriginalFile() throws Exception {
        String teacherToken = loginAndGetAccessToken("teacher.zhang", "Teacher@123456");
        byte[] csvContent = """
                english_word,french_word,chinese_gloss,lexical_pair_type,semantic_overlap_score,false_friend_risk,default_context_support,difficulty_level,notes,source,active,tags,knowledge_status,embedding_status,sense_english_definition,sense_french_definition,sense_chinese_definition,example_english,example_french,example_chinese,example_context_support
                lexicalbatchalpha,lexicalbatchalpha,批量导入词对一,cognate,0.88,0.12,medium,3,Valid row,Integration Test,true,batch|alpha,ready,pending,alpha definition,definition alpha,词义 alpha,Alpha example,Exemple alpha,例句 alpha,medium
                lexicalbatchbeta,lexicalbatchbeta,批量导入词对二,not_a_type,0.45,0.66,high,4,Needs fix,Integration Test,true,batch|beta,ready,pending,beta definition,definition beta,词义 beta,Beta example,Exemple beta,例句 beta,high
                """.getBytes(StandardCharsets.UTF_8);

        long batchId = createBatch(
                teacherToken,
                new MockMultipartFile("file", "lexical-import-flow.csv", "text/csv", csvContent)
        );

        JsonNode draftBatch = waitForBatchStatus(teacherToken, batchId, "DRAFT");
        assertThat(draftBatch.path("sourceFormat").asText()).isEqualTo("CSV");
        assertThat(draftBatch.path("readyRows").asInt()).isEqualTo(1);
        assertThat(draftBatch.path("invalidRows").asInt()).isEqualTo(1);

        JsonNode rows = listRows(teacherToken, batchId);
        JsonNode invalidRow = findRowByStatus(rows, "INVALID");
        assertThat(invalidRow.path("validationErrors").size()).isGreaterThan(0);

        ObjectNode updatePayload = ((ObjectNode) invalidRow.path("draft").deepCopy());
        updatePayload.put("lexicalPairType", "false_friend");
        updatePayload.put("skipped", false);

        mockMvc.perform(put("/api/lexical-pairs/import-batches/{batchId}/rows/{rowId}", batchId, invalidRow.path("id").asLong())
                        .with(bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(updatePayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("READY"));

        mockMvc.perform(post("/api/lexical-pairs/import-batches/{batchId}/commit", batchId)
                        .with(bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.batchId").value(batchId))
                .andExpect(jsonPath("$.data.status").value("IMPORTING"));

        JsonNode completedBatch = waitForBatchStatus(teacherToken, batchId, "COMPLETED");
        assertThat(completedBatch.path("readyRows").asInt()).isZero();
        assertThat(completedBatch.path("invalidRows").asInt()).isZero();
        assertThat(completedBatch.path("importedRows").asInt()).isEqualTo(2);
        assertThat(completedBatch.path("pendingEmbeddingCount").asInt()).isEqualTo(2);
        assertThat(completedBatch.path("embeddedCount").asInt()).isZero();
        assertThat(completedBatch.path("failedEmbeddingCount").asInt()).isZero();

        mockMvc.perform(get("/api/lexical-pairs/import-batches/{batchId}/rows", batchId)
                        .with(bearer(teacherToken))
                        .param("pageNo", "1")
                        .param("pageSize", "20")
                        .param("status", "IMPORTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2));

        MvcResult downloadResult = mockMvc.perform(get("/api/lexical-pairs/import-batches/{batchId}/file", batchId)
                        .with(bearer(teacherToken)))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(downloadResult.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION)).contains("lexical-import-flow.csv");
        assertThat(downloadResult.getResponse().getContentAsByteArray()).isEqualTo(csvContent);

        mockMvc.perform(get("/api/lexical-pairs")
                        .with(bearer(teacherToken))
                        .param("pageNo", "1")
                        .param("pageSize", "10")
                        .param("keyword", "lexicalbatchalpha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));

        mockMvc.perform(get("/api/lexical-pairs")
                        .with(bearer(teacherToken))
                        .param("pageNo", "1")
                        .param("pageSize", "10")
                        .param("keyword", "lexicalbatchbeta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void shouldKeepSuccessfulRowsAndMarkDuplicateRowInvalidWithoutOrphanPairs() throws Exception {
        String teacherToken = loginAndGetAccessToken("teacher.zhang", "Teacher@123456");
        byte[] csvContent = """
                english_word,french_word,chinese_gloss,lexical_pair_type,semantic_overlap_score,false_friend_risk,default_context_support,difficulty_level,notes,source,active,tags,knowledge_status,embedding_status,sense_english_definition,sense_french_definition,sense_chinese_definition,example_english,example_french,example_chinese,example_context_support
                lexicaltxnalpha,lexicaltxnalpha,事务导入成功行,cognate,0.88,0.12,medium,3,Valid row,Integration Test,true,batch|txn,ready,pending,alpha definition,definition alpha,词义 alpha,Alpha example,Exemple alpha,例句 alpha,medium
                lexicaltxnbeta,lexicaltxnbeta,事务导入失败行,cognate,0.45,0.66,high,4,Will collide,Integration Test,true,batch|txn,ready,pending,beta definition,definition beta,词义 beta,Beta example,Exemple beta,例句 beta,high
                """.getBytes(StandardCharsets.UTF_8);

        long batchId = createBatch(
                teacherToken,
                new MockMultipartFile("file", "lexical-import-txn.csv", "text/csv", csvContent)
        );
        JsonNode draftBatch = waitForBatchStatus(teacherToken, batchId, "DRAFT");
        assertThat(draftBatch.path("readyRows").asInt()).isEqualTo(2);

        mockMvc.perform(post("/api/lexical-pairs")
                        .with(bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "englishWord": "lexicaltxnbeta",
                                  "frenchWord": "lexicaltxnbeta",
                                  "chineseGloss": "预先存在的冲突词对",
                                  "lexicalPairType": "cognate",
                                  "semanticOverlapScore": 0.10,
                                  "falseFriendRisk": 0.20,
                                  "defaultContextSupport": "medium",
                                  "difficultyLevel": 3,
                                  "active": true,
                                  "knowledgeStatus": "ready",
                                  "embeddingStatus": "pending",
                                  "tags": ["txn-collision"],
                                  "senses": [
                                    {
                                      "sortOrder": 1,
                                      "englishDefinition": "pre-existing",
                                      "frenchDefinition": "pre-existing",
                                      "chineseDefinition": "预先存在",
                                      "examples": []
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/lexical-pairs/import-batches/{batchId}/commit", batchId)
                        .with(bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IMPORTING"));

        JsonNode completedBatch = waitForBatchStatus(teacherToken, batchId, "COMPLETED");
        assertThat(completedBatch.path("importedRows").asInt()).isEqualTo(1);
        assertThat(completedBatch.path("invalidRows").asInt()).isEqualTo(1);

        JsonNode rows = listRows(teacherToken, batchId);
        JsonNode imported = findRowByStatus(rows, "IMPORTED");
        JsonNode invalid = findRowByStatus(rows, "INVALID");
        assertThat(imported.path("draft").path("englishWord").asText()).isEqualTo("lexicaltxnalpha");
        assertThat(invalid.path("draft").path("englishWord").asText()).isEqualTo("lexicaltxnbeta");
        assertThat(invalid.path("importedLexicalPairId").isNull()).isTrue();

        mockMvc.perform(get("/api/lexical-pairs")
                        .with(bearer(teacherToken))
                        .param("pageNo", "1")
                        .param("pageSize", "10")
                        .param("keyword", "lexicaltxnalpha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));

        mockMvc.perform(get("/api/lexical-pairs")
                        .with(bearer(teacherToken))
                        .param("pageNo", "1")
                        .param("pageSize", "10")
                        .param("keyword", "lexicaltxnbeta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void shouldParseXlsxAndPreventTeacherFromReadingAdminBatch() throws Exception {
        String adminToken = loginAndGetAccessToken("admin", "Admin@123456");
        String teacherToken = loginAndGetAccessToken("teacher.zhang", "Teacher@123456");

        byte[] xlsxContent = buildWorkbookBytes(List.of(Map.ofEntries(
                Map.entry("english_word", "lexicalxlsxalpha"),
                Map.entry("french_word", "lexicalxlsxalpha"),
                Map.entry("chinese_gloss", "XLSX 导入词对"),
                Map.entry("lexical_pair_type", "cognate"),
                Map.entry("semantic_overlap_score", "0.91"),
                Map.entry("false_friend_risk", "0.08"),
                Map.entry("default_context_support", "low"),
                Map.entry("difficulty_level", "2"),
                Map.entry("notes", "xlsx row"),
                Map.entry("source", "Integration Test"),
                Map.entry("active", "true"),
                Map.entry("tags", "xlsx|batch"),
                Map.entry("knowledge_status", "ready"),
                Map.entry("embedding_status", "pending"),
                Map.entry("sense_english_definition", "xlsx definition"),
                Map.entry("sense_french_definition", "definition xlsx"),
                Map.entry("sense_chinese_definition", "词义 xlsx"),
                Map.entry("example_english", "xlsx example"),
                Map.entry("example_french", "exemple xlsx"),
                Map.entry("example_chinese", "例句 xlsx"),
                Map.entry("example_context_support", "low")
        )));

        long batchId = createBatch(
                adminToken,
                new MockMultipartFile(
                        "file",
                        "lexical-import-flow.xlsx",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        xlsxContent
                )
        );

        JsonNode draftBatch = waitForBatchStatus(adminToken, batchId, "DRAFT");
        assertThat(draftBatch.path("sourceFormat").asText()).isEqualTo("XLSX");
        assertThat(draftBatch.path("totalRows").asInt()).isEqualTo(1);
        assertThat(draftBatch.path("readyRows").asInt()).isEqualTo(1);

        mockMvc.perform(get("/api/lexical-pairs/import-batches/{batchId}", batchId)
                        .with(bearer(teacherToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/lexical-pairs/import-batches")
                        .with(bearer(adminToken))
                        .param("pageNo", "1")
                        .param("pageSize", "10")
                        .param("ownerUserId", draftBatch.path("ownerUserId").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].id").value(batchId));
    }

    private long createBatch(String accessToken, MockMultipartFile file) throws Exception {
        MvcResult result = mockMvc.perform(multipart("/api/lexical-pairs/import-batches")
                        .file(file)
                        .with(bearer(accessToken))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PARSING"))
                .andReturn();
        return readJson(result).path("data").path("batchId").asLong();
    }

    private JsonNode waitForBatchStatus(String accessToken, long batchId, String expectedStatus) throws Exception {
        for (int attempt = 0; attempt < 100; attempt += 1) {
            MvcResult result = mockMvc.perform(get("/api/lexical-pairs/import-batches/{batchId}", batchId)
                            .with(bearer(accessToken)))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode batch = readJson(result).path("data");
            if (expectedStatus.equals(batch.path("status").asText())) {
                return batch;
            }
            Thread.sleep(100);
        }
        fail("Timed out waiting for batch " + batchId + " to reach status " + expectedStatus);
        return objectMapper.createObjectNode();
    }

    private JsonNode listRows(String accessToken, long batchId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/lexical-pairs/import-batches/{batchId}/rows", batchId)
                        .with(bearer(accessToken))
                        .param("pageNo", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data").path("records");
    }

    private JsonNode findRowByStatus(JsonNode rows, String status) {
        for (JsonNode row : rows) {
            if (status.equals(row.path("status").asText())) {
                return row;
            }
        }
        fail("Could not find row with status " + status);
        return objectMapper.createObjectNode();
    }

    private byte[] buildWorkbookBytes(List<Map<String, String>> rows) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Import");
            var headerRow = sheet.createRow(0);
            for (int columnIndex = 0; columnIndex < TEMPLATE_HEADERS.size(); columnIndex += 1) {
                headerRow.createCell(columnIndex).setCellValue(TEMPLATE_HEADERS.get(columnIndex));
            }
            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex += 1) {
                var row = sheet.createRow(rowIndex + 1);
                Map<String, String> values = rows.get(rowIndex);
                for (int columnIndex = 0; columnIndex < TEMPLATE_HEADERS.size(); columnIndex += 1) {
                    row.createCell(columnIndex).setCellValue(values.getOrDefault(TEMPLATE_HEADERS.get(columnIndex), ""));
                }
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
}
