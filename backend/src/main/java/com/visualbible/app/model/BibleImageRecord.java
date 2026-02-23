package com.visualbible.app.model;

import java.util.List;

public record BibleImageRecord(
        String id,
        String name,
        String outputPath,
        String imagePath,
        String createdAt,
        List<String> references,
        List<HighlightRegion> highlights
) {
}
