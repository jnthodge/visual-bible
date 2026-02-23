package com.visualbible.app.controller;

import com.visualbible.app.model.BibleImageRecord;
import com.visualbible.app.model.CreateRecordResponse;
import com.visualbible.app.service.BibleImageGeneratorService;
import com.visualbible.app.service.PassageParser;
import com.visualbible.app.service.RecordStorageService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/projects")
@Validated
public class BibleController {

    private final PassageParser parser;
    private final BibleImageGeneratorService generator;
    private final RecordStorageService storage;

    public BibleController(PassageParser parser, BibleImageGeneratorService generator, RecordStorageService storage) {
        this.parser = parser;
        this.generator = generator;
        this.storage = storage;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CreateRecordResponse> create(
            @RequestPart(value = "passagesFile", required = false) MultipartFile passagesFile,
            @RequestPart("name") @NotBlank String name,
            @RequestPart("outputPath") @NotBlank String outputPath,
            @RequestPart(value = "textReferences", required = false) String textReferences
    ) throws IOException {
        InputStream inputStream = passagesFile != null && !passagesFile.isEmpty() ? passagesFile.getInputStream() : null;
        List<String> references = parser.parsePassages(inputStream, textReferences);
        Path outputDir = Path.of(outputPath);
        String filename = name.replaceAll("[^a-zA-Z0-9-_]", "_") + ".png";
        Path outputImage = outputDir.resolve(filename);

        BibleImageGeneratorService.GeneratedImage generatedImage = generator.generate(outputImage, references);

        BibleImageRecord record = storage.save(
                name,
                outputPath,
                outputImage.toAbsolutePath().toString(),
                references,
                generatedImage.highlights()
        );

        return ResponseEntity.ok(new CreateRecordResponse(record.id(), "Saved image and passage map."));
    }

    @GetMapping
    public List<BibleImageRecord> list() throws IOException {
        return storage.list();
    }

    @GetMapping("/{id}")
    public ResponseEntity<BibleImageRecord> getById(@PathVariable String id) throws IOException {
        return storage.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<Resource> getImage(@PathVariable String id) throws IOException {
        BibleImageRecord record = storage.findById(id).orElse(null);
        if (record == null) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new FileSystemResource(record.imagePath());
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .body(resource);
    }
}
