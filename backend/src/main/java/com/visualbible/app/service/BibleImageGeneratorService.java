package com.visualbible.app.service;

import com.visualbible.app.model.HighlightRegion;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class BibleImageGeneratorService {

    private static final List<BookInfo> BOOKS = List.of(
            new BookInfo("Genesis", 50), new BookInfo("Exodus", 40), new BookInfo("Leviticus", 27), new BookInfo("Numbers", 36), new BookInfo("Deuteronomy", 34),
            new BookInfo("Joshua", 24), new BookInfo("Judges", 21), new BookInfo("Ruth", 4), new BookInfo("1 Samuel", 31), new BookInfo("2 Samuel", 24),
            new BookInfo("1 Kings", 22), new BookInfo("2 Kings", 25), new BookInfo("1 Chronicles", 29), new BookInfo("2 Chronicles", 36), new BookInfo("Ezra", 10),
            new BookInfo("Nehemiah", 13), new BookInfo("Esther", 10), new BookInfo("Job", 42), new BookInfo("Psalms", 150), new BookInfo("Proverbs", 31),
            new BookInfo("Ecclesiastes", 12), new BookInfo("Song of Solomon", 8), new BookInfo("Isaiah", 66), new BookInfo("Jeremiah", 52), new BookInfo("Lamentations", 5),
            new BookInfo("Ezekiel", 48), new BookInfo("Daniel", 12), new BookInfo("Hosea", 14), new BookInfo("Joel", 3), new BookInfo("Amos", 9),
            new BookInfo("Obadiah", 1), new BookInfo("Jonah", 4), new BookInfo("Micah", 7), new BookInfo("Nahum", 3), new BookInfo("Habakkuk", 3),
            new BookInfo("Zephaniah", 3), new BookInfo("Haggai", 2), new BookInfo("Zechariah", 14), new BookInfo("Malachi", 4), new BookInfo("Matthew", 28),
            new BookInfo("Mark", 16), new BookInfo("Luke", 24), new BookInfo("John", 21), new BookInfo("Acts", 28), new BookInfo("Romans", 16),
            new BookInfo("1 Corinthians", 16), new BookInfo("2 Corinthians", 13), new BookInfo("Galatians", 6), new BookInfo("Ephesians", 6), new BookInfo("Philippians", 4),
            new BookInfo("Colossians", 4), new BookInfo("1 Thessalonians", 5), new BookInfo("2 Thessalonians", 3), new BookInfo("1 Timothy", 6), new BookInfo("2 Timothy", 4),
            new BookInfo("Titus", 3), new BookInfo("Philemon", 1), new BookInfo("Hebrews", 13), new BookInfo("James", 5), new BookInfo("1 Peter", 5),
            new BookInfo("2 Peter", 3), new BookInfo("1 John", 5), new BookInfo("2 John", 1), new BookInfo("3 John", 1), new BookInfo("Jude", 1),
            new BookInfo("Revelation", 22)
    );

    public GeneratedImage generate(Path outputFile, List<String> highlightedReferences) throws IOException {
        Set<String> highlights = new HashSet<>(highlightedReferences);
        int width = 2400;
        int lineHeight = 26;
        int totalLines = BOOKS.stream().mapToInt(book -> book.chapters + 1).sum() + 20;
        int height = Math.max(2200, totalLines * lineHeight);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        g.setColor(new Color(248, 245, 238));
        g.fillRect(0, 0, width, height);
        g.setColor(new Color(224, 216, 204));
        for (int x = 0; x < width; x += 400) {
            g.fillRect(x, 0, 8, height);
        }

        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(new Font("Serif", Font.PLAIN, 18));

        List<HighlightRegion> regions = new ArrayList<>();
        int y = 40;

        g.setColor(new Color(58, 43, 27));
        g.setFont(new Font("Serif", Font.BOLD, 36));
        g.drawString("Visual Bible (Generated Overview)", 40, y);
        y += lineHeight * 2;
        g.setFont(new Font("Serif", Font.PLAIN, 18));

        for (BookInfo book : BOOKS) {
            g.setColor(new Color(40, 30, 20));
            g.setFont(new Font("Serif", Font.BOLD, 24));
            g.drawString(book.name, 40, y);
            y += lineHeight;
            g.setFont(new Font("Serif", Font.PLAIN, 18));

            for (int chapter = 1; chapter <= book.chapters; chapter++) {
                String verse = book.name + " " + chapter + ":1";
                String verseText = "In chapter " + chapter + " of " + book.name + ", this generated line represents the chapter text for visualization.";
                String line = verse + "  " + verseText;

                if (highlights.contains(verse)) {
                    int boxY = y - 19;
                    g.setColor(new Color(255, 205, 205));
                    g.fillRect(32, boxY, width - 70, 24);
                    g.setColor(new Color(190, 0, 0));
                    g.drawRect(32, boxY, width - 70, 24);
                    regions.add(new HighlightRegion(verse, verseText, 32, boxY, width - 70, 24));
                    g.setColor(new Color(150, 0, 0));
                } else {
                    g.setColor(new Color(52, 52, 52));
                }

                g.drawString(line, 40, y);
                y += lineHeight;
            }

            y += 8;
        }

        g.dispose();
        Files.createDirectories(outputFile.getParent());
        ImageIO.write(image, "png", outputFile.toFile());

        return new GeneratedImage(outputFile, regions);
    }

    private record BookInfo(String name, int chapters) {}

    public record GeneratedImage(Path outputFile, List<HighlightRegion> highlights) {}
}
