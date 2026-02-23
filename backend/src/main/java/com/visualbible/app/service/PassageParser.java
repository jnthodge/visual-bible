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

@Component
public class PassageParser {

    private final ReferenceMatcherService matcherService;

    public PassageParser(ReferenceMatcherService matcherService) {
        this.matcherService = matcherService;
    }

    public List<String> parsePassages(InputStream inputStream, String typedReferences) throws IOException {
        Set<String> unique = new LinkedHashSet<>();

        if (inputStream != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    unique.addAll(matcherService.parseReferences(line));
                }
            }
        }

        unique.addAll(matcherService.parseReferences(typedReferences));
        return unique.stream().toList();
    }
}
