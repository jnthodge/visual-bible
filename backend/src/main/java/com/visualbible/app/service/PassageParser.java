package com.visualbible.app.service;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class PassageParser {

    public List<String> parsePassages(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            Set<String> unique = new LinkedHashSet<>();
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    unique.add(normalize(trimmed));
                }
            }
            return unique.stream().toList();
        }
    }

    private String normalize(String passage) {
        return passage.replaceAll("\\s+", " ")
                .replace('–', '-')
                .trim();
    }
}
