package com.kpo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
public class GatewayController {
    private final RestTemplate restTemplate = new RestTemplate();
    private final String fileServiceUrl = "http://localhost:8081/files";
    private final String analysisServiceUrl = "http://localhost:8082/analysis";

    // Проксирование загрузки файла
    @PostMapping(value = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        // Проксируем файл в file-storing-service
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            org.springframework.util.LinkedMultiValueMap<String, Object> body = new org.springframework.util.LinkedMultiValueMap<>();
            body.add("file", file.getResource());
            HttpEntity<?> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(fileServiceUrl, requestEntity, Map.class);
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "File upload failed"));
        }
    }

    // Проксирование скачивания файла
    @GetMapping("/files/{id}")
    public ResponseEntity<?> downloadFile(@PathVariable String id) {
        try {
            ResponseEntity<byte[]> response = restTemplate.getForEntity(fileServiceUrl + "/" + id, byte[].class);
            return ResponseEntity.status(response.getStatusCode())
                    .header(HttpHeaders.CONTENT_DISPOSITION, response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("error", "File not found"));
        }
    }

    // Проксирование списка файлов
    @GetMapping("/files")
    public ResponseEntity<?> listFiles() {
        try {
            ResponseEntity<Object> response = restTemplate.getForEntity(fileServiceUrl, Object.class);
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to get file list"));
        }
    }

    // Проксирование анализа файла
    @PostMapping("/analysis/{fileId}")
    public ResponseEntity<?> analyzeFile(@PathVariable String fileId) {
        try {
            ResponseEntity<Object> response = restTemplate.postForEntity(analysisServiceUrl + "/" + fileId, null, Object.class);
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Analysis failed"));
        }
    }

    // Проксирование получения статистики
    @GetMapping("/analysis/{fileId}")
    public ResponseEntity<?> getStats(@PathVariable String fileId) {
        try {
            ResponseEntity<Object> response = restTemplate.getForEntity(analysisServiceUrl + "/" + fileId, Object.class);
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("error", "No analysis found"));
        }
    }

    // Проксирование сравнения файлов
    @PostMapping("/analysis/compare/{fileId1}/{fileId2}")
    public ResponseEntity<?> compareFiles(@PathVariable String fileId1, @PathVariable String fileId2) {
        try {
            ResponseEntity<Object> response = restTemplate.postForEntity(analysisServiceUrl + "/compare/" + fileId1 + "/" + fileId2, null, Object.class);
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Comparison failed"));
        }
    }
} 