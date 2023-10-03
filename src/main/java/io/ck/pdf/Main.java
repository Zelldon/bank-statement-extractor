package io.ck.pdf;

import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Pattern;

public class Main {

    public static final Pattern PATTERN = Pattern.compile("^(.+) (.+) (-?[0-9.]*[0-9]+,[0-9]+)$");
    public static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance(Locale.GERMANY);

    /**
     * Categories used to extract the data from the bank statement
     */
    public static final Map<String, Double> CATEGORIES = new HashMap<>();

    /**
     * Groups used to define group of categories which should be calculated and grouped together on printing.
     */
    public static final Map<String, List<String>> GROUPS = new HashMap<>();

    public static final String OUT_KEY = "out";
    public static final String IN_KEY = "in";

    static {
        CATEGORIES.put("amazon", 0.0);
        CATEGORIES.put("paypal", 0.0);
        CATEGORIES.put("tank", 0.0);
        CATEGORIES.put("rewe", 0.0);
        CATEGORIES.put("kaufland", 0.0);
        CATEGORIES.put("edeka", 0.0);
        CATEGORIES.put("lidl", 0.0);
        CATEGORIES.put("star", 0.0);

        GROUPS.put("Grocery", List.of("rewe", "lidl", "edeka", "kaufland"));
        GROUPS.put("Shopping", List.of("amazon", "paypal"));
        GROUPS.put("Fuel", List.of("tank", "star"));
    }


    public static void main(String[] args) throws Exception {
        File file = new File("/tmp/bank.pdf");
        PDFParser parser = new PDFParser(new RandomAccessReadBufferedFile(file));

        try (PDDocument document = parser.parse()) {
            String text = new PDFTextStripper().getText(document);
            final var bankData = convertBankStatement(text);
            printData(file, bankData);
        }
    }

    private static void printData(File file, Map<String, Double> bankData) {
        // header
        System.out.printf("title %s %s ", IN_KEY, OUT_KEY);
        final var groups = GROUPS.keySet();
        groups.forEach(category -> System.out.printf("%s ", category));

        System.out.println();

        // values
        System.out.printf("%s ", file.getName());
        System.out.printf("%.2f ", bankData.get(IN_KEY));
        System.out.printf("%.2f ", bankData.get(OUT_KEY));

        groups.forEach(group -> {
            final var categories = GROUPS.get(group);
            final var amount =
                    categories.stream().map(bankData::get).mapToDouble(Double::doubleValue).sum();

            System.out.printf("%.2f ", amount);
        });


//        CATEGORIES.keySet().forEach(category -> System.out.printf("%.2f ", bankData.get(category)));
    }


    /**
     * Converts a bank statement, to a map with specified categories.
     * @param text the text which contains all the bank statement details
     * @return a map which contains the categorized data
     */
    private static Map<String, Double> convertBankStatement(String text) {
        // init
        final var categories = new HashMap<>(CATEGORIES);
        categories.put(OUT_KEY, 0.0);
        categories.put(IN_KEY, 0.0);

        text.lines().forEach(line -> {

            var matcher = PATTERN.matcher(line);
            if (!matcher.find() || line.toLowerCase(Locale.GERMANY).contains("saldo")) {
                return; // no amount go to next line
            }

            try {
                final var value = NUMBER_FORMAT.parse(matcher.group(3)).doubleValue();
                if (value > 0) {
                    categories.computeIfPresent(IN_KEY, (k, v) -> v + value);
                } else {
                    categories.computeIfPresent(OUT_KEY, (k, v) -> v + value);
                }

                CATEGORIES.keySet().stream()
                        .filter(category -> line.toLowerCase(Locale.GERMANY).contains(category))
                        .findAny().ifPresent(category -> categories.computeIfPresent(category, (k, v) -> v + value));
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        });
        return categories;
    }
}
