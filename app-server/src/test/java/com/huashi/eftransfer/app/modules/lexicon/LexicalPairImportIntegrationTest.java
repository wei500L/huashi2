package com.huashi.eftransfer.app.modules.lexicon;

import com.huashi.eftransfer.app.support.AbstractWebIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LexicalPairImportIntegrationTest extends AbstractWebIntegrationTest {

    @Test
    void shouldImportCsvAndReturnFailureReceipt() throws Exception {
        String teacherToken = loginAndGetAccessToken("teacher.zhang", "Teacher@123456");

        mockMvc.perform(get("/api/lexical-pairs/import-template")
                        .with(bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fields.length()").value(21))
                .andExpect(jsonPath("$.data.headerLine").exists());

        String csvContent = """
                english_word,french_word,chinese_gloss,lexical_pair_type,semantic_overlap_score,false_friend_risk,default_context_support,difficulty_level,notes,source,active,tags,knowledge_status,embedding_status,sense_english_definition,sense_french_definition,sense_chinese_definition,example_english,example_french,example_chinese,example_context_support
                coin,coin,硬币；角落,false_friend,0.10,0.92,high,4,High confusion,Teacher Curated,true,false-friend|high-frequency,ready,pending,a piece of money,coin de rue,硬币,He found a coin.,Le chat dort dans le coin.,他找到一枚硬币,high
                table,table,桌子,invalid_type,0.90,0.10,low,1,Basic pair,Teacher Curated,true,basic,ready,pending,a flat surface,table,桌子,This table is new.,Cette table est grande.,这张桌子是新的,medium
                coin,coin,重复词对,false_friend,0.20,0.80,medium,3,Duplicate,Teacher Curated,true,duplicate,ready,pending,money,coin,钱币,A coin shines.,Le coin est sombre.,硬币很亮,medium
                """;
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "lexical-pairs.csv",
                "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/lexical-pairs/import")
                        .file(file)
                        .with(bearer(teacherToken))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.successCount").value(1))
                .andExpect(jsonPath("$.data.failedCount").value(2))
                .andExpect(jsonPath("$.data.failures[0].reason").isNotEmpty());

        mockMvc.perform(get("/api/lexical-pairs")
                        .with(bearer(teacherToken))
                        .param("pageNo", "1")
                        .param("pageSize", "10")
                        .param("keyword", "coin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void shouldExportCsvAndHonorKeywordFilter() throws Exception {
        String teacherToken = loginAndGetAccessToken("teacher.zhang", "Teacher@123456");

        mockMvc.perform(multipart("/api/lexical-pairs/import")
                        .file(new MockMultipartFile(
                                "file",
                                "lexical-pairs.csv",
                                "text/csv",
                                """
                                        english_word,french_word,chinese_gloss,lexical_pair_type,semantic_overlap_score,false_friend_risk,default_context_support,difficulty_level,notes,source,active,tags,knowledge_status,embedding_status,sense_english_definition,sense_french_definition,sense_chinese_definition,example_english,example_french,example_chinese,example_context_support
                                        coin,coin,硬币；角落,false_friend,0.10,0.92,high,4,High confusion,Teacher Curated,true,false-friend|high-frequency,ready,pending,a piece of money,coin de rue,硬币,He found a coin.,Le chat dort dans le coin.,他找到一枚硬币,high
                                        table,table,桌子,cognate,0.90,0.10,low,1,Basic pair,Teacher Curated,true,basic,ready,pending,a flat surface,table,桌子,This table is new.,Cette table est grande.,这张桌子是新的,medium
                                        """.getBytes(StandardCharsets.UTF_8)))
                        .with(bearer(teacherToken))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.successCount").value(2));

        mockMvc.perform(get("/api/lexical-pairs/export.csv")
                        .with(bearer(teacherToken))
                        .param("keyword", "coin"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/csv")))
                .andExpect(header().string("Content-Disposition", containsString("lexical-pairs-")))
                .andExpect(content().string(containsString("english_word,french_word")))
                .andExpect(content().string(containsString("coin,coin")))
                .andExpect(content().string(not(containsString("table,table"))));
    }
}
