package com.kpo;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/analysis")
public class FileAnalysisController {
    private final Map<String, Stats> statsMap = new HashMap<>();
    private final RestTemplate restTemplate = new RestTemplate();
    private final String fileServiceUrl = "http://localhost:8081/files/"; // порт file-storing-service

    @PostMapping("/{fileId}")
    public ResponseEntity<?> analyzeFile(@PathVariable String fileId) {
        String text = fetchFileText(fileId);
        if (text == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "File not found"));
        }
        Stats stats = analyze(text);
        statsMap.put(fileId, stats);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<?> getStats(@PathVariable String fileId) {
        Stats stats = statsMap.get(fileId);
        if (stats == null) {
            return ResponseEntity.status(404).body(Map.of("error", "No analysis found for file"));
        }
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/compare/{fileId1}/{fileId2}")
    public ResponseEntity<?> compareFiles(@PathVariable String fileId1, @PathVariable String fileId2) {
        String text1 = fetchFileText(fileId1);
        String text2 = fetchFileText(fileId2);
        if (text1 == null || text2 == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "One or both files not found"));
        }
        boolean identical = text1.equals(text2);
        return ResponseEntity.ok(Map.of("identical", identical));
    }

    private String fetchFileText(String fileId) {
        try {
            ResponseEntity<byte[]> response = restTemplate.getForEntity(fileServiceUrl + fileId, byte[].class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return new String(response.getBody(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private Stats analyze(String text) {
        int paragraphs = (int) Arrays.stream(text.split("\r?\n")).filter(s -> !s.isBlank()).count();
        int words = (int) Arrays.stream(text.split("\s+")).filter(s -> !s.isBlank()).count();
        int chars = text.length();
        return new Stats(paragraphs, words, chars);
    }

    static class Stats {
        public int paragraphs;
        public int words;
        public int chars;
        public Stats(int paragraphs, int words, int chars) {
            this.paragraphs = paragraphs;
            this.words = words;
            this.chars = chars;
        }
    }
} 