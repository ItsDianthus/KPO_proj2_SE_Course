package com.kpo;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@RestController
@RequestMapping("/files")
public class FileController {
    private static final String UPLOAD_DIR = "uploads";
    private final Map<String, FileMeta> files = new HashMap<>();

    @PostConstruct
    public void init() {
        File uploadDir = new File(UPLOAD_DIR);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty() || !file.getOriginalFilename().endsWith(".txt")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Only .txt files are allowed"));
        }
        String id = UUID.randomUUID().toString();
        String filename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        Path filePath = Paths.get(UPLOAD_DIR, id + "_" + filename);
        Files.copy(file.getInputStream(), filePath);
        files.put(id, new FileMeta(id, filename, filePath.toString()));
        return ResponseEntity.ok(Map.of("id", id, "filename", filename));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> downloadFile(@PathVariable String id) throws IOException {
        FileMeta meta = files.get(id);
        if (meta == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "File not found"));
        }
        File file = new File(meta.path);
        if (!file.exists()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "File not found on disk"));
        }
        byte[] content = Files.readAllBytes(file.toPath());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.setContentDispositionFormData("attachment", meta.filename);
        return new ResponseEntity<>(content, headers, HttpStatus.OK);
    }

    @GetMapping
    public List<Map<String, String>> listFiles() {
        List<Map<String, String>> result = new ArrayList<>();
        for (FileMeta meta : files.values()) {
            result.add(Map.of("id", meta.id, "filename", meta.filename));
        }
        return result;
    }

    static class FileMeta {
        String id;
        String filename;
        String path;
        FileMeta(String id, String filename, String path) {
            this.id = id;
            this.filename = filename;
            this.path = path;
        }
    }
} 