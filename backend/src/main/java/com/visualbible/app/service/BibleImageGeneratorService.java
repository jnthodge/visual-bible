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

    private static final int PAGE_WIDTH = 64;
    private static final int PAGE_HEIGHT = 112;
    private static final int PAGES_PER_COLUMN = 20;

    private final Path baseImagePath;
    private final List<VerseRecord> verses;
    private final Map<String, Rect> referenceRectangles;
    private final List<PageRect> pages;
    private final int width;
    private final int height;

    public BibleImageGeneratorService(@Value("${app.bible.base-image:../data/output/base-bible.png}") String baseImagePath) throws IOException {
        this.baseImagePath = Path.of(baseImagePath);
        List<VerseRecord> loaded = loadVerses();
        LayoutResult layout = buildLayout(loaded);
        this.verses = layout.verses;
        this.referenceRectangles = layout.referenceRectangles;
        this.pages = layout.pages;
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
            g.setColor(new Color(255, 0, 0, 90));
            g.fillRect(rect.x - 1, rect.y - rect.height + 2, rect.width + 2, rect.height + 1);
            g.setColor(new Color(230, 0, 0));
            g.drawString(reference, rect.x, rect.y);
            regions.add(new HighlightRegion(reference, findText(reference), rect.x - 1, rect.y - rect.height + 2, rect.width + 2, rect.height + 1));
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

        g.setColor(new Color(244, 238, 226));
        g.fillRect(0, 0, width, height);

        for (PageRect page : pages) {
            g.setColor(new Color(252, 250, 244));
            g.fillRect(page.x, page.y, PAGE_WIDTH, PAGE_HEIGHT);
            g.setColor(new Color(212, 203, 189));
            g.drawRect(page.x, page.y, PAGE_WIDTH, PAGE_HEIGHT);
            g.setColor(new Color(235, 228, 214));
            g.drawLine(page.x + (PAGE_WIDTH / 2), page.y + 8, page.x + (PAGE_WIDTH / 2), page.y + PAGE_HEIGHT - 8);
        }

        String currentBook = "";
        int rendered = 0;
        for (VerseRecord verse : verses) {
            if (!verse.book.equals(currentBook)) {
                currentBook = verse.book;
                g.setFont(new Font("SansSerif", Font.BOLD, 6));
                g.setColor(new Color(35, 35, 35));
                g.drawString(currentBook, verse.bookLabelX, verse.bookLabelY);
            }

            g.setFont(new Font("Serif", Font.PLAIN, 3));
            g.setColor(new Color(95, 95, 95));
            g.drawString(verse.renderText, verse.x, verse.y);

            rendered++;
            if (rendered % 5000 == 0) {
                log.info("Rendered {} verse rows into base page image...", rendered);
            }
        }

        g.dispose();
        ImageIO.write(image, "png", baseImagePath.toFile());
        log.info("Base Bible image generated successfully ({}x{}).", width, height);
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
                loaded.add(new VerseRecord(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), parts[3]));
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
        int marginX = 12;
        int marginY = 12;
        int pageGapY = 8;
        int pageGapX = 12;
        int columnGapX = 30;

        int topPad = 12;
        int lineHeight = 3;
        int leftPad = 3;
        int colGap = 4;
        int colWidth = (PAGE_WIDTH - leftPad * 2 - colGap) / 2;
        int linesPerColumn = (PAGE_HEIGHT - topPad - 5) / lineHeight;
        int linesPerPage = linesPerColumn * 2;

        List<VerseRecord> positioned = new ArrayList<>();
        List<PageRect> pageRects = new ArrayList<>();
        Set<Integer> seenPages = new HashSet<>();
        Map<String, Rect> map = new HashMap<>();

        int pageIndex = 0;
        int lineInPage = 0;
        String currentBook = "";
        int maxX = 0;
        int maxY = 0;

        for (VerseRecord raw : rawVerses) {
            boolean bookChanged = !raw.book.equals(currentBook);
            if (bookChanged) {
                if (!currentBook.isEmpty()) {
                    pageIndex++;
                    lineInPage = 0;
                }
                if ("Matthew".equals(raw.book)) {
                    pageIndex += 2;
                }
                currentBook = raw.book;
            }

            if (lineInPage >= linesPerPage) {
                pageIndex++;
                lineInPage = 0;
            }

            int pageColumn = pageIndex / PAGES_PER_COLUMN;
            int pageRow = pageIndex % PAGES_PER_COLUMN;
            int pageX = marginX + pageColumn * (PAGE_WIDTH + pageGapX + columnGapX);
            int pageY = marginY + pageRow * (PAGE_HEIGHT + pageGapY);

            if (seenPages.add(pageIndex)) {
                pageRects.add(new PageRect(pageIndex, pageX, pageY));
            }

            int col = lineInPage / linesPerColumn;
            int row = lineInPage % linesPerColumn;
            int textX = pageX + leftPad + col * (colWidth + colGap);
            int textY = pageY + topPad + row * lineHeight;

            String draw = abbreviate(raw.reference(), colWidth / 2 + 6);
            VerseRecord placed = raw.withPosition(textX, textY, draw, pageX + 2, pageY + 8);
            positioned.add(placed);
            map.put(placed.reference(), new Rect(textX, textY, Math.max(8, draw.length() * 2), lineHeight + 1));

            lineInPage++;
            maxX = Math.max(maxX, pageX + PAGE_WIDTH);
            maxY = Math.max(maxY, pageY + PAGE_HEIGHT);
        }

        return new LayoutResult(positioned, map, pageRects, maxX + 16, maxY + 16);
    }

    private String abbreviate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, Math.max(0, maxLength - 1)) + "…";
    }

    private static class LayoutResult {
        private final List<VerseRecord> verses;
        private final Map<String, Rect> referenceRectangles;
        private final List<PageRect> pages;
        private final int width;
        private final int height;

        private LayoutResult(List<VerseRecord> verses, Map<String, Rect> referenceRectangles, List<PageRect> pages, int width, int height) {
            this.verses = verses;
            this.referenceRectangles = referenceRectangles;
            this.pages = pages;
            this.width = width;
            this.height = height;
        }
    }

    private static class PageRect {
        private final int pageIndex;
        private final int x;
        private final int y;

        private PageRect(int pageIndex, int x, int y) {
            this.pageIndex = pageIndex;
            this.x = x;
            this.y = y;
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
        private final String renderText;
        private final int bookLabelX;
        private final int bookLabelY;

        private VerseRecord(String book, int chapter, int verse, String text) {
            this(book, chapter, verse, text, 0, 0, "", 0, 0);
        }

        private VerseRecord(String book, int chapter, int verse, String text, int x, int y, String renderText, int bookLabelX, int bookLabelY) {
            this.book = book;
            this.chapter = chapter;
            this.verse = verse;
            this.text = text;
            this.x = x;
            this.y = y;
            this.renderText = renderText;
            this.bookLabelX = bookLabelX;
            this.bookLabelY = bookLabelY;
        }

        private VerseRecord withPosition(int x, int y, String renderText, int bookLabelX, int bookLabelY) {
            return new VerseRecord(book, chapter, verse, text, x, y, renderText, bookLabelX, bookLabelY);
        }

        private String reference() {
            return book + " " + chapter + ":" + verse;
        }
    }

    public record GeneratedImage(Path outputFile, List<HighlightRegion> highlights) {
    }
}
