package com.visualbible.app.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ReferenceMatcherService {

    private static final Pattern SPACED = Pattern.compile("^((?:[1-3]\\s*)?[A-Za-z .]+?)\\s*(\\d+):(\\d+)(?:-(\\d+))?$");
    private static final Pattern COMPACT = Pattern.compile("^([1-3]?[A-Za-z]{2,})\\s*(\\d+):(\\d+)(?:-(\\d+))?$");
    private final Map<String, String> aliases = buildAliases();

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
        Matcher spaced = SPACED.matcher(candidate);
        Matcher compact = COMPACT.matcher(candidate);
        Matcher m = spaced.matches() ? spaced : (compact.matches() ? compact : null);
        if (m == null) {
            return Optional.empty();
        }

        String rawBook = m.group(1).trim();
        String canonicalBook = aliases.get(normalizeBook(rawBook));
        if (canonicalBook == null) {
            return Optional.empty();
        }

        int chapter = Integer.parseInt(m.group(2));
        int verseStart = Integer.parseInt(m.group(3));
        int verseEnd = m.group(4) != null ? Integer.parseInt(m.group(4)) : verseStart;
        if (verseEnd < verseStart) {
            int t = verseStart;
            verseStart = verseEnd;
            verseEnd = t;
        }

        List<String> refs = new ArrayList<>();
        for (int verse = verseStart; verse <= verseEnd; verse++) {
            refs.add(canonicalBook + " " + chapter + ":" + verse);
        }
        return Optional.of(refs);
    }

    private String normalizeBook(String raw) {
        return raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
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
