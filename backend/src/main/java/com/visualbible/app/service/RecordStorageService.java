package com.visualbible.app.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.visualbible.app.model.BibleImageRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RecordStorageService {

    private final ObjectMapper mapper;
    private final Path dataFile;

    public RecordStorageService(ObjectMapper mapper, @Value("${app.storage.data-file}") String dataFilePath) {
        this.mapper = mapper;
        this.dataFile = Path.of(dataFilePath);
    }

    public synchronized BibleImageRecord save(String name, String outputPath, String imagePath, List<String> references, List<com.visualbible.app.model.HighlightRegion> highlights) throws IOException {
        List<BibleImageRecord> records = loadAll();
        BibleImageRecord record = new BibleImageRecord(UUID.randomUUID().toString(), name, outputPath, imagePath, references, highlights);
        records.add(record);
        persist(records);
        return record;
    }

    public synchronized List<BibleImageRecord> list() throws IOException {
        return loadAll();
    }

    public synchronized Optional<BibleImageRecord> findById(String id) throws IOException {
        return loadAll().stream().filter(record -> record.id().equals(id)).findFirst();
    }

    private List<BibleImageRecord> loadAll() throws IOException {
        if (!Files.exists(dataFile)) {
            return new ArrayList<>();
        }
        String content = Files.readString(dataFile);
        if (content.isBlank()) {
            return new ArrayList<>();
        }
        return mapper.readValue(content, new TypeReference<>() {});
    }

    private void persist(List<BibleImageRecord> records) throws IOException {
        Files.createDirectories(dataFile.getParent());
        mapper.writerWithDefaultPrettyPrinter().writeValue(dataFile.toFile(), records);
    }
}
