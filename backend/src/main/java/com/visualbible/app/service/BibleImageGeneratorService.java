package com.visualbible.app.service;

import com.visualbible.app.model.HighlightRegion;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.*;

@Service
public class BibleImageGeneratorService {
    private static final Logger log = LoggerFactory.getLogger(BibleImageGeneratorService.class);

    private final Path baseImagePath;
    private final List<VerseRecord> verses;
    private final Map<String, Rect> referenceRectangles;
    private final int width;
    private final int height;

    public BibleImageGeneratorService(@Value("${app.bible.base-image:../data/output/base-bible.png}") String baseImagePath) throws IOException {
        this.baseImagePath = Path.of(baseImagePath);
        this.verses = loadVerses();
        LayoutResult layout = buildLayout(verses);
        this.referenceRectangles = layout.referenceRectangles;
        this.width = layout.width;
        this.height = layout.height;
    }

    @PostConstruct
    public void init() {
        log.info("Loaded {} verse rows for visualization", verses.size());
        try {
            generateBaseImageIfMissing();
        } catch (IOException e) {
            log.error("Failed to generate base image on startup", e);
        }
    }

    public GeneratedImage generate(Path outputFile, List<String> highlightedReferences) throws IOException {
        log.info("Generating highlighted Bible image with {} requested references", highlightedReferences.size());
        BufferedImage image = ImageIO.read(baseImagePath.toFile());
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(new Font("Serif", Font.BOLD, 5));

        List<HighlightRegion> regions = new ArrayList<>();
        for (String reference : highlightedReferences) {
            Rect rect = referenceRectangles.get(reference);
            if (rect == null) {
                continue;
            }
            g.setColor(new Color(235, 0, 0));
            g.drawString(reference, rect.x, rect.y + rect.height - 1);
            g.setColor(new Color(255, 0, 0, 70));
            g.fillRect(rect.x - 1, rect.y - rect.height + 2, rect.width + 3, rect.height + 1);
            regions.add(new HighlightRegion(reference, findText(reference), rect.x - 1, rect.y - rect.height + 2, rect.width + 3, rect.height + 1));
        }

        g.dispose();
        Files.createDirectories(outputFile.getParent());
        ImageIO.write(image, "png", outputFile.toFile());
        log.info("Generated highlighted image at {}", outputFile.toAbsolutePath());
        return new GeneratedImage(outputFile, regions);
    }

    private void generateBaseImageIfMissing() throws IOException {
        if (Files.exists(baseImagePath)) {
            log.info("Base Bible image already exists at {}", baseImagePath.toAbsolutePath());
            return;
        }

        log.info("Generating base Bible image at startup: {}", baseImagePath.toAbsolutePath());
        Files.createDirectories(baseImagePath.getParent());

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setColor(new Color(250, 248, 242));
        g.fillRect(0, 0, width, height);

        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        g.setColor(new Color(30, 30, 30));

        String currentBook = "";
        int verseCount = 0;
        for (VerseRecord verse : verses) {
            if (!verse.book.equals(currentBook)) {
                currentBook = verse.book;
                g.drawString(verse.book, verse.x, 14);
                verseCount = 0;
                if ("Matthew".equals(verse.book)) {
                    g.setColor(new Color(120, 120, 120));
                    g.drawLine(verse.x - 8, 18, verse.x - 8, height - 5);
                    g.setColor(new Color(30, 30, 30));
                }
            }

            g.setFont(new Font("Serif", Font.PLAIN, 5));
            g.setColor(new Color(90, 90, 90));
            g.drawString(verse.reference(), verse.x, verse.y);
            if (++verseCount % 5000 == 0) {
                log.info("Rendered {} verse labels into base image...", verseCount);
            }
        }

        g.dispose();
        ImageIO.write(image, "png", baseImagePath.toFile());
        log.info("Base Bible image generated successfully.");
    }

    private String findText(String reference) {
        return verses.stream()
                .filter(v -> v.reference().equals(reference))
                .map(v -> v.text)
                .findFirst()
                .orElse("Reference not found in loaded Bible dataset.");
    }

    private List<VerseRecord> loadVerses() throws IOException {
        log.info("Loading Bible verses from resource bible/kjv_full.csv");
        ClassPathResource resource = new ClassPathResource("bible/kjv_full.csv");
        List<VerseRecord> loaded = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line = reader.readLine();
            while ((line = reader.readLine()) != null) {
                String[] parts = splitCsv(line);
                if (parts.length < 4) {
                    continue;
                }
                String book = parts[0];
                int chapter = Integer.parseInt(parts[1]);
                int verse = Integer.parseInt(parts[2]);
                String text = parts[3];
                loaded.add(new VerseRecord(book, chapter, verse, text));
            }
        }
        return loaded;
    }

    private String[] splitCsv(String row) {
        List<String> out = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < row.length(); i++) {
            char c = row.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
                continue;
            }
            if (c == ',' && !inQuotes) {
                out.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        out.add(sb.toString());
        return out.toArray(String[]::new);
    }

    private LayoutResult buildLayout(List<VerseRecord> rawVerses) {
        int x = 12;
        int y = 28;
        int lineHeight = 6;
        int maxY = 0;
        Map<String, Rect> map = new HashMap<>();
        List<VerseRecord> positioned = new ArrayList<>();

        String previousBook = "";
        for (VerseRecord verse : rawVerses) {
            if (!verse.book.equals(previousBook)) {
                if (!previousBook.isEmpty()) {
                    x += 20;
                }
                if ("Matthew".equals(verse.book)) {
                    x += 30;
                }
                y = 28;
                previousBook = verse.book;
            }

            VerseRecord placed = verse.withPosition(x, y);
            positioned.add(placed);
            map.put(placed.reference(), new Rect(x, y, Math.max(10, placed.reference().length() * 3), lineHeight));

            y += lineHeight;
            maxY = Math.max(maxY, y);
            if (y > 7800) {
                x += 42;
                y = 28;
            }
        }

        this.verses.clear();
        this.verses.addAll(positioned);
        return new LayoutResult(map, x + 80, maxY + 40);
    }

    private static class LayoutResult {
        private final Map<String, Rect> referenceRectangles;
        private final int width;
        private final int height;

        private LayoutResult(Map<String, Rect> referenceRectangles, int width, int height) {
            this.referenceRectangles = referenceRectangles;
            this.width = width;
            this.height = height;
        }
    }

    private static class Rect {
        private final int x;
        private final int y;
        private final int width;
        private final int height;

        private Rect(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    private static class VerseRecord {
        private final String book;
        private final int chapter;
        private final int verse;
        private final String text;
        private final int x;
        private final int y;

        private VerseRecord(String book, int chapter, int verse, String text) {
            this(book, chapter, verse, text, 0, 0);
        }

        private VerseRecord(String book, int chapter, int verse, String text, int x, int y) {
            this.book = book;
            this.chapter = chapter;
            this.verse = verse;
            this.text = text;
            this.x = x;
            this.y = y;
        }

        private VerseRecord withPosition(int x, int y) {
            return new VerseRecord(book, chapter, verse, text, x, y);
        }

        private String reference() {
            return book + " " + chapter + ":" + verse;
        }
    }

    public record GeneratedImage(Path outputFile, List<HighlightRegion> highlights) {
    }
}
