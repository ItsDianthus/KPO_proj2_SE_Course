package com.kpo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FileController.class)
public class FileControllerTest {
    @Autowired
    private MockMvc mockMvc;

    private MockMultipartFile validFile;
    private MockMultipartFile invalidFile;

    @BeforeEach
    void setUp() {
        validFile = new MockMultipartFile("file", "test.txt", MediaType.TEXT_PLAIN_VALUE, "Hello world".getBytes());
        invalidFile = new MockMultipartFile("file", "test.pdf", MediaType.APPLICATION_PDF_VALUE, "PDF content".getBytes());
    }

    @Test
    void uploadTxtFile_success() throws Exception {
        mockMvc.perform(multipart("/files").file(validFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.filename").value("test.txt"));
    }

    @Test
    void uploadNonTxtFile_error() throws Exception {
        mockMvc.perform(multipart("/files").file(invalidFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Only .txt files are allowed"));
    }

    @Test
    void listFiles_emptyInitially() throws Exception {
        mockMvc.perform(get("/files"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void uploadAndDownloadFile_success() throws Exception {
        String response = mockMvc.perform(multipart("/files").file(validFile))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String id = response.replaceAll(".*\\"id\\":\\"([^\\"]+)\\".*", "$1");
        mockMvc.perform(get("/files/" + id))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("test.txt")))
                .andExpect(content().string("Hello world"));
    }

    @Test
    void downloadFile_notFound() throws Exception {
        mockMvc.perform(get("/files/nonexistent-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("File not found"));
    }

    @Test
    void uploadMultipleFiles_andList() throws Exception {
        mockMvc.perform(multipart("/files").file(new MockMultipartFile("file", "a.txt", MediaType.TEXT_PLAIN_VALUE, "A".getBytes())))
                .andExpect(status().isOk());
        mockMvc.perform(multipart("/files").file(new MockMultipartFile("file", "b.txt", MediaType.TEXT_PLAIN_VALUE, "B".getBytes())))
                .andExpect(status().isOk());
        mockMvc.perform(get("/files"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].filename").value("a.txt"))
                .andExpect(jsonPath("$[1].filename").value("b.txt"));
    }
} 