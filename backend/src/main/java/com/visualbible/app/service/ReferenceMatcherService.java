package com.visualbible.app.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ReferenceMatcherService {

    private static final Pattern CHAPTER_ONLY = Pattern.compile("^((?:[1-3]\\s*)?[A-Za-z .]+?)\\s+(\\d+)$");
    private static final Pattern SINGLE_OR_SAME_CHAPTER_RANGE = Pattern.compile("^((?:[1-3]\\s*)?[A-Za-z .]+?)\\s*(\\d+):(\\d+)(?:-(\\d+))?$");
    private static final Pattern CROSS_CHAPTER_RANGE = Pattern.compile("^((?:[1-3]\\s*)?[A-Za-z .]+?)\\s*(\\d+):(\\d+)\\s*-\\s*(\\d+):(\\d+)$");
    private static final Pattern FULL_CHAPTER_RANGE = Pattern.compile("^((?:[1-3]\\s*)?[A-Za-z .]+?)\\s*(\\d+)\\s*-\\s*(\\d+)$");
    private final Map<String, String> aliases = buildAliases();
    private final Map<String, NavigableMap<Integer, Integer>> chapterVerseCount;

    public ReferenceMatcherService() throws IOException {
        this.chapterVerseCount = loadChapterVerseCount();
    }

    public List<String> parseReferences(String input) {
        if (input == null || input.isBlank()) {
            return List.of();
        }

        Set<String> out = new LinkedHashSet<>();
        String[] chunks = input.split("[\\n,;]");
        for (String chunk : chunks) {
            String candidate = chunk.trim();
            if (candidate.isEmpty()) {
                continue;
            }
            parseCandidate(candidate).ifPresent(out::addAll);
        }
        return out.stream().toList();
    }

    private Optional<List<String>> parseCandidate(String candidate) {
        Matcher crossRange = CROSS_CHAPTER_RANGE.matcher(candidate);
        if (crossRange.matches()) {
            String canonicalBook = canonicalBook(crossRange.group(1));
            if (canonicalBook == null) return Optional.empty();
            return Optional.of(expandCrossChapterRange(
                    canonicalBook,
                    parseInt(crossRange.group(2)),
                    parseInt(crossRange.group(3)),
                    parseInt(crossRange.group(4)),
                    parseInt(crossRange.group(5))
            ));
        }

        Matcher chapterRange = FULL_CHAPTER_RANGE.matcher(candidate);
        if (chapterRange.matches()) {
            String canonicalBook = canonicalBook(chapterRange.group(1));
            if (canonicalBook == null) return Optional.empty();
            int startChapter = parseInt(chapterRange.group(2));
            int endChapter = parseInt(chapterRange.group(3));
            return Optional.of(expandChapterRange(canonicalBook, startChapter, endChapter));
        }

        Matcher single = SINGLE_OR_SAME_CHAPTER_RANGE.matcher(candidate);
        if (single.matches()) {
            String canonicalBook = canonicalBook(single.group(1));
            if (canonicalBook == null) return Optional.empty();

            int chapter = parseInt(single.group(2));
            int startVerse = parseInt(single.group(3));
            int endVerse = single.group(4) != null ? parseInt(single.group(4)) : startVerse;
            return Optional.of(expandSingleChapterRange(canonicalBook, chapter, startVerse, endVerse));
        }

        Matcher chapterOnly = CHAPTER_ONLY.matcher(candidate);
        if (chapterOnly.matches()) {
            String canonicalBook = canonicalBook(chapterOnly.group(1));
            if (canonicalBook == null) return Optional.empty();
            int chapter = parseInt(chapterOnly.group(2));
            return Optional.of(expandChapter(canonicalBook, chapter));
        }

        return tryCompact(candidate);
    }

    private Optional<List<String>> tryCompact(String candidate) {
        String stripped = candidate.replaceAll("\\s+", "");
        Matcher compactCross = Pattern.compile("^([1-3]?[A-Za-z]{2,})(\\d+):(\\d+)-(\\d+):(\\d+)$").matcher(stripped);
        if (compactCross.matches()) {
            String canonicalBook = canonicalBook(compactCross.group(1));
            if (canonicalBook == null) return Optional.empty();
            return Optional.of(expandCrossChapterRange(
                    canonicalBook,
                    parseInt(compactCross.group(2)),
                    parseInt(compactCross.group(3)),
                    parseInt(compactCross.group(4)),
                    parseInt(compactCross.group(5))
            ));
        }

        Matcher compactSingle = Pattern.compile("^([1-3]?[A-Za-z]{2,})(\\d+):(\\d+)(?:-(\\d+))?$").matcher(stripped);
        if (compactSingle.matches()) {
            String canonicalBook = canonicalBook(compactSingle.group(1));
            if (canonicalBook == null) return Optional.empty();
            int chapter = parseInt(compactSingle.group(2));
            int startVerse = parseInt(compactSingle.group(3));
            int endVerse = compactSingle.group(4) != null ? parseInt(compactSingle.group(4)) : startVerse;
            return Optional.of(expandSingleChapterRange(canonicalBook, chapter, startVerse, endVerse));
        }

        Matcher compactChapter = Pattern.compile("^([1-3]?[A-Za-z]{2,})(\\d+)$").matcher(stripped);
        if (compactChapter.matches()) {
            String canonicalBook = canonicalBook(compactChapter.group(1));
            if (canonicalBook == null) return Optional.empty();
            return Optional.of(expandChapter(canonicalBook, parseInt(compactChapter.group(2))));
        }

        return Optional.empty();
    }

    private List<String> expandCrossChapterRange(String book, int startChapter, int startVerse, int endChapter, int endVerse) {
        if (endChapter < startChapter || (endChapter == startChapter && endVerse < startVerse)) {
            int tmpC = startChapter;
            int tmpV = startVerse;
            startChapter = endChapter;
            startVerse = endVerse;
            endChapter = tmpC;
            endVerse = tmpV;
        }

        NavigableMap<Integer, Integer> chapters = chapterVerseCount.getOrDefault(book, new TreeMap<>());
        List<String> out = new ArrayList<>();
        for (int c = startChapter; c <= endChapter; c++) {
            int maxVerse = chapters.getOrDefault(c, 0);
            if (maxVerse <= 0) continue;

            int vStart = c == startChapter ? startVerse : 1;
            int vEnd = c == endChapter ? endVerse : maxVerse;
            vStart = Math.max(1, vStart);
            vEnd = Math.min(maxVerse, vEnd);
            for (int v = vStart; v <= vEnd; v++) {
                out.add(book + " " + c + ":" + v);
            }
        }
        return out;
    }

    private List<String> expandChapterRange(String book, int startChapter, int endChapter) {
        if (endChapter < startChapter) {
            int t = startChapter;
            startChapter = endChapter;
            endChapter = t;
        }
        List<String> out = new ArrayList<>();
        for (int chapter = startChapter; chapter <= endChapter; chapter++) {
            out.addAll(expandChapter(book, chapter));
        }
        return out;
    }

    private List<String> expandSingleChapterRange(String book, int chapter, int startVerse, int endVerse) {
        int maxVerse = chapterVerseCount.getOrDefault(book, new TreeMap<>()).getOrDefault(chapter, 0);
        if (maxVerse <= 0) {
            return List.of();
        }
        if (endVerse < startVerse) {
            int t = startVerse;
            startVerse = endVerse;
            endVerse = t;
        }
        startVerse = Math.max(1, startVerse);
        endVerse = Math.min(maxVerse, endVerse);

        List<String> refs = new ArrayList<>();
        for (int verse = startVerse; verse <= endVerse; verse++) {
            refs.add(book + " " + chapter + ":" + verse);
        }
        return refs;
    }

    private List<String> expandChapter(String book, int chapter) {
        int maxVerse = chapterVerseCount.getOrDefault(book, new TreeMap<>()).getOrDefault(chapter, 0);
        if (maxVerse <= 0) {
            return List.of();
        }
        List<String> refs = new ArrayList<>();
        for (int verse = 1; verse <= maxVerse; verse++) {
            refs.add(book + " " + chapter + ":" + verse);
        }
        return refs;
    }

    private String canonicalBook(String rawBook) {
        return aliases.get(normalizeBook(rawBook));
    }

    private int parseInt(String raw) {
        return Integer.parseInt(raw.trim());
    }

    private String normalizeBook(String raw) {
        return raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private Map<String, NavigableMap<Integer, Integer>> loadChapterVerseCount() throws IOException {
        Map<String, NavigableMap<Integer, Integer>> out = new HashMap<>();
        ClassPathResource resource = new ClassPathResource("bible/kjv_full.csv");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line = reader.readLine();
            while ((line = reader.readLine()) != null) {
                String[] parts = splitCsv(line);
                if (parts.length < 3) continue;
                String book = parts[0];
                int chapter = Integer.parseInt(parts[1]);
                int verse = Integer.parseInt(parts[2]);
                out.computeIfAbsent(book, ignored -> new TreeMap<>())
                        .merge(chapter, verse, Math::max);
            }
        }
        return out;
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

    private Map<String, String> buildAliases() {
        Map<String, String> map = new HashMap<>();
        add(map, "Genesis", "gen", "ge", "gn");
        add(map, "Exodus", "exo", "ex", "exod");
        add(map, "Leviticus", "lev", "le", "lv");
        add(map, "Numbers", "num", "nu", "nm", "nb");
        add(map, "Deuteronomy", "deu", "dt");
        add(map, "Joshua", "jos", "josh");
        add(map, "Judges", "jdg", "judg", "jg");
        add(map, "Ruth", "rth", "ru");
        add(map, "1 Samuel", "1samuel", "1sam", "1sa", "i samuel", "isam");
        add(map, "2 Samuel", "2samuel", "2sam", "2sa", "ii samuel", "iisam");
        add(map, "1 Kings", "1kings", "1ki", "1kgs", "i kings", "ikings");
        add(map, "2 Kings", "2kings", "2ki", "2kgs", "ii kings", "iikings");
        add(map, "1 Chronicles", "1chronicles", "1ch", "1chr", "i chronicles");
        add(map, "2 Chronicles", "2chronicles", "2ch", "2chr", "ii chronicles");
        add(map, "Ezra", "ezr"); add(map, "Nehemiah", "neh"); add(map, "Esther", "est");
        add(map, "Job", "job"); add(map, "Psalms", "ps", "psa", "psalm");
        add(map, "Proverbs", "pro", "prov", "prv"); add(map, "Ecclesiastes", "ecc", "ec");
        add(map, "Song of Solomon", "songofsolomon", "song", "sos", "songofsongs");
        add(map, "Isaiah", "isa", "is"); add(map, "Jeremiah", "jer", "je");
        add(map, "Lamentations", "lam", "la"); add(map, "Ezekiel", "ezk", "eze");
        add(map, "Daniel", "dan", "da"); add(map, "Hosea", "hos", "ho"); add(map, "Joel", "joe", "jl");
        add(map, "Amos", "amo", "am"); add(map, "Obadiah", "oba", "ob"); add(map, "Jonah", "jon", "jnh");
        add(map, "Micah", "mic", "mc"); add(map, "Nahum", "nah", "na"); add(map, "Habakkuk", "hab", "hb");
        add(map, "Zephaniah", "zep", "zp"); add(map, "Haggai", "hag", "hg"); add(map, "Zechariah", "zec", "zc");
        add(map, "Malachi", "mal", "ml"); add(map, "Matthew", "mat", "mt"); add(map, "Mark", "mrk", "mk");
        add(map, "Luke", "luk", "lk"); add(map, "John", "jhn", "jn", "joh"); add(map, "Acts", "act", "ac");
        add(map, "Romans", "rom", "ro", "rm");
        add(map, "1 Corinthians", "1corinthians", "1cor", "1co", "i corinthians", "icor");
        add(map, "2 Corinthians", "2corinthians", "2cor", "2co", "ii corinthians", "iicor");
        add(map, "Galatians", "gal", "ga"); add(map, "Ephesians", "eph", "ep"); add(map, "Philippians", "php", "phil", "ph");
        add(map, "Colossians", "col", "co");
        add(map, "1 Thessalonians", "1thessalonians", "1th", "1thes", "i thessalonians");
        add(map, "2 Thessalonians", "2thessalonians", "2th", "2thes", "ii thessalonians");
        add(map, "1 Timothy", "1timothy", "1tim", "1ti", "i timothy");
        add(map, "2 Timothy", "2timothy", "2tim", "2ti", "ii timothy");
        add(map, "Titus", "tit", "ti"); add(map, "Philemon", "phm", "phile"); add(map, "Hebrews", "heb", "he");
        add(map, "James", "jas", "jm");
        add(map, "1 Peter", "1peter", "1pet", "1pe", "i peter");
        add(map, "2 Peter", "2peter", "2pet", "2pe", "ii peter");
        add(map, "1 John", "1john", "1jn", "1joh", "i john");
        add(map, "2 John", "2john", "2jn", "2joh", "ii john");
        add(map, "3 John", "3john", "3jn", "3joh", "iii john");
        add(map, "Jude", "jud", "jd"); add(map, "Revelation", "rev", "re", "rv");
        return map;
    }

    private void add(Map<String, String> map, String canonical, String... forms) {
        map.put(normalizeBook(canonical), canonical);
        for (String form : forms) {
            map.put(normalizeBook(form), canonical);
        }
    }
}
