package com.visualbible.app.model;

public record HighlightRegion(
        String verse,
        String text,
        int x,
        int y,
        int width,
        int height
) {
}
