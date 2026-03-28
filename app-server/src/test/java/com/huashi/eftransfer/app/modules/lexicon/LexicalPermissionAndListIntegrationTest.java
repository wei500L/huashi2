package com.huashi.eftransfer.app.modules.lexicon;

import com.huashi.eftransfer.app.support.AbstractWebIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LexicalPermissionAndListIntegrationTest extends AbstractWebIntegrationTest {

    @Test
    void shouldRestrictStudentFromLexiconManagementReadsAndManageTeacherOwnedList() throws Exception {
        String teacherToken = loginAndGetAccessToken("teacher.zhang", "Teacher@123456");
        String studentToken = loginAndGetAccessToken("student.li", "Student@123456");
        String adminToken = loginAndGetAccessToken("admin", "Admin@123456");

        mockMvc.perform(post("/api/lexical-pairs")
                        .with(bearer(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "englishWord": "banal",
                                  "frenchWord": "banal",
                                  "chineseGloss": "平庸的",
                                  "lexicalPairType": "cognate",
                                  "semanticOverlapScore": 0.90,
                                  "falseFriendRisk": 0.05,
                                  "defaultContextSupport": "low",
                                  "difficultyLevel": 1,
                                  "active": true,
                                  "tags": ["basic"]
                                }
                                """))
                .andExpect(status().isForbidden());

        MvcResult createPairResult = mockMvc.perform(post("/api/lexical-pairs")
                        .with(bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "englishWord": "librairie",
                                  "frenchWord": "library",
                                  "chineseGloss": "书店 / 图书馆",
                                  "lexicalPairType": "false_friend",
                                  "semanticOverlapScore": 0.20,
                                  "falseFriendRisk": 0.88,
                                  "defaultContextSupport": "medium",
                                  "difficultyLevel": 4,
                                  "active": true,
                                  "tags": ["false-friend"]
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        long lexicalPairId = readJson(createPairResult).path("data").asLong();

        mockMvc.perform(get("/api/lexical-pairs")
                        .with(bearer(studentToken))
                        .param("pageNo", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/lexical-pairs/{lexicalPairId}", lexicalPairId)
                        .with(bearer(studentToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/lexical-pairs/import-template")
                        .with(bearer(studentToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/lexical-pairs/export.csv")
                        .with(bearer(studentToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/lexical-pairs")
                        .with(bearer(teacherToken))
                        .param("pageNo", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));

        mockMvc.perform(get("/api/lexical-pairs/{lexicalPairId}", lexicalPairId)
                        .with(bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value((int) lexicalPairId));

        MvcResult createListResult = mockMvc.perform(post("/api/lexical-lists")
                        .with(bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "listName": "False Friend Drill",
                                  "description": "Week 1 focus",
                                  "active": true
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        long lexicalListId = readJson(createListResult).path("data").asLong();

        mockMvc.perform(get("/api/lexical-lists")
                        .with(bearer(studentToken))
                        .param("pageNo", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/lexical-lists/{listId}", lexicalListId)
                        .with(bearer(studentToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/lexical-lists/{listId}/items", lexicalListId)
                        .with(bearer(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "lexicalPairIds": [%d]
                                }
                                """.formatted(lexicalPairId)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/lexical-lists/{listId}/items", lexicalListId)
                        .with(bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "lexicalPairIds": [%d]
                                }
                                """.formatted(lexicalPairId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.addedCount").value(1));

        mockMvc.perform(post("/api/lexical-lists/{listId}/items", lexicalListId)
                        .with(bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "lexicalPairIds": [%d]
                                }
                                """.formatted(lexicalPairId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.addedCount").value(0))
                .andExpect(jsonPath("$.data.skippedCount").doesNotExist())
                .andExpect(jsonPath("$.data.skippedPairIds[0]").value((int) lexicalPairId));

        MvcResult detailResult = mockMvc.perform(get("/api/lexical-lists/{listId}", lexicalListId)
                        .with(bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itemCount").value(1))
                .andReturn();

        long itemId = readJson(detailResult).path("data").path("items").get(0).path("itemId").asLong();

        mockMvc.perform(get("/api/lexical-lists")
                        .with(bearer(teacherToken))
                        .param("pageNo", "1")
                        .param("pageSize", "10")
                        .param("mineOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));

        mockMvc.perform(delete("/api/lexical-lists/{listId}/items/{itemId}", lexicalListId, itemId)
                        .with(bearer(teacherToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/lexical-lists/{listId}", lexicalListId)
                        .with(bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itemCount").value(0));
    }

    @Test
    void shouldRejectDuplicateItemIdsDuringLexicalListReorder() throws Exception {
        String teacherToken = loginAndGetAccessToken("teacher.zhang", "Teacher@123456");
        long firstPairId = createLexicalPair(teacherToken, "coin", "coin", "硬币；角落");
        long secondPairId = createLexicalPair(teacherToken, "table", "table", "桌子");
        long thirdPairId = createLexicalPair(teacherToken, "actually", "actuellement", "实际上；目前");

        long lexicalListId = createLexicalList(teacherToken, "Reorder Guard");
        mockMvc.perform(post("/api/lexical-lists/{listId}/items", lexicalListId)
                        .with(bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "lexicalPairIds": [%d, %d, %d]
                                }
                                """.formatted(firstPairId, secondPairId, thirdPairId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.addedCount").value(3));

        MvcResult detailResult = mockMvc.perform(get("/api/lexical-lists/{listId}", lexicalListId)
                        .with(bearer(teacherToken)))
                .andExpect(status().isOk())
                .andReturn();

        long firstItemId = readJson(detailResult).path("data").path("items").get(0).path("itemId").asLong();
        long secondItemId = readJson(detailResult).path("data").path("items").get(1).path("itemId").asLong();
        long thirdItemId = readJson(detailResult).path("data").path("items").get(2).path("itemId").asLong();

        mockMvc.perform(put("/api/lexical-lists/{listId}/items/reorder", lexicalListId)
                        .with(bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderedItemIds": [%d, %d, %d]
                                }
                                """.formatted(firstItemId, secondItemId, secondItemId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("orderedItemIds must not contain duplicate lexical list item ids"));

        mockMvc.perform(get("/api/lexical-lists/{listId}", lexicalListId)
                        .with(bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itemCount").value(3))
                .andExpect(jsonPath("$.data.items[0].itemId").value((int) firstItemId))
                .andExpect(jsonPath("$.data.items[0].sortOrder").value(1))
                .andExpect(jsonPath("$.data.items[1].itemId").value((int) secondItemId))
                .andExpect(jsonPath("$.data.items[1].sortOrder").value(2))
                .andExpect(jsonPath("$.data.items[2].itemId").value((int) thirdItemId))
                .andExpect(jsonPath("$.data.items[2].sortOrder").value(3));
    }

    private long createLexicalPair(String teacherToken, String englishWord, String frenchWord, String chineseGloss) throws Exception {
        MvcResult createPairResult = mockMvc.perform(post("/api/lexical-pairs")
                        .with(bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "englishWord": "%s",
                                  "frenchWord": "%s",
                                  "chineseGloss": "%s",
                                  "lexicalPairType": "false_friend",
                                  "semanticOverlapScore": 0.20,
                                  "falseFriendRisk": 0.88,
                                  "defaultContextSupport": "medium",
                                  "difficultyLevel": 4,
                                  "active": true,
                                  "tags": ["false-friend"]
                                }
                                """.formatted(englishWord, frenchWord, chineseGloss)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(createPairResult).path("data").asLong();
    }

    private long createLexicalList(String teacherToken, String listName) throws Exception {
        MvcResult createListResult = mockMvc.perform(post("/api/lexical-lists")
                        .with(bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "listName": "%s",
                                  "description": "Week 1 focus",
                                  "active": true
                                }
                                """.formatted(listName)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(createListResult).path("data").asLong();
    }
}
