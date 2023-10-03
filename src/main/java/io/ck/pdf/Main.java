package io.ck.pdf;

import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    public static final Pattern PATTERN = Pattern.compile("^(.+) (.+) (-?[0-9]+,[0-9]+)$");
    public static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance(Locale.GERMANY);

    public static final Map<String, Double> CATEGORIES = new HashMap<>();

    static {
        CATEGORIES.put("amazon", 0.0);
        CATEGORIES.put("paypal", 0.0);
        CATEGORIES.put("tank", 0.0);
        CATEGORIES.put("rewe", 0.0);
        CATEGORIES.put("kaufland", 0.0);
        CATEGORIES.put("lidl", 0.0);
    }


    public static void main(String[] args) throws Exception {
        File file = new File("/tmp/bank.pdf");
        PDFParser parser = new PDFParser(new RandomAccessReadBufferedFile(file));

        try (PDDocument document = parser.parse()) {
            String text = new PDFTextStripper().getText(document);
            text.lines().forEach(line -> {

                var matcher = PATTERN.matcher(line);
                if (!matcher.find()) {
                    return; // no amount go to next line
                }

                try {
                    final var value = NUMBER_FORMAT.parse(matcher.group(3));
                    CATEGORIES.keySet().stream()
                            .filter(category -> line.toLowerCase(Locale.GERMANY).contains(category))
                            .findAny().ifPresent(category -> CATEGORIES.computeIfPresent(category, (k, v) -> v + value.doubleValue()));
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        CATEGORIES.keySet().forEach(category -> System.out.printf("%s ", category));
        System.out.println();
        CATEGORIES.values().forEach(category -> System.out.printf("%.2f ", category));
    }
}