package com.kpo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FileAnalysisController.class)
public class FileAnalysisControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RestTemplate restTemplate;

    private final String fileContent = "Hello\nWorld!";
    private final byte[] fileBytes = fileContent.getBytes();

    @BeforeEach
    void setUp() {
        // По умолчанию файл найден
        when(restTemplate.getForEntity(Mockito.contains("/files/"), Mockito.eq(byte[].class)))
                .thenReturn(ResponseEntity.ok(fileBytes));
    }

    @Test
    void analyzeFile_success() throws Exception {
        mockMvc.perform(post("/analysis/123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paragraphs").value(2))
                .andExpect(jsonPath("$.words").value(2))
                .andExpect(jsonPath("$.chars").value(fileContent.length()));
    }

    @Test
    void analyzeFile_notFound() throws Exception {
        when(restTemplate.getForEntity(Mockito.contains("/files/"), Mockito.eq(byte[].class)))
                .thenThrow(new RuntimeException("Not found"));
        mockMvc.perform(post("/analysis/404"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("File not found"));
    }

    @Test
    void compareFiles_identical() throws Exception {
        mockMvc.perform(post("/analysis/compare/1/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identical").value(true));
    }

    @Test
    void compareFiles_different() throws Exception {
        when(restTemplate.getForEntity(Mockito.contains("/files/2"), Mockito.eq(byte[].class)))
                .thenReturn(ResponseEntity.ok("Another text".getBytes()));
        mockMvc.perform(post("/analysis/compare/1/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identical").value(false));
    }

    @Test
    void getStats_notAnalyzed() throws Exception {
        mockMvc.perform(get("/analysis/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No analysis found for file"));
    }

    @Test
    void analyzeFile_twice_updatesStats() throws Exception {
        mockMvc.perform(post("/analysis/123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paragraphs").value(2));
        mockMvc.perform(post("/analysis/123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paragraphs").value(2));
        mockMvc.perform(get("/analysis/123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paragraphs").value(2));
    }

    @Test
    void compareFiles_oneNotFound() throws Exception {
        when(restTemplate.getForEntity(Mockito.contains("/files/2"), Mockito.eq(byte[].class)))
                .thenThrow(new RuntimeException("Not found"));
        mockMvc.perform(post("/analysis/compare/1/2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("One or both files not found"));
    }
} 