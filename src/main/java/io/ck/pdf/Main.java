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
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    public static final Pattern PATTERN = Pattern.compile("^(.+) (.+) (-?[0-9]+,[0-9]+)$");
    public static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance(Locale.GERMANY);


    public static void main(String[] args) throws Exception {
        File file = new File("/tmp/bank.pdf");
        PDFParser parser = new PDFParser(new RandomAccessReadBufferedFile(file));

        final var amountRef = new AtomicReference<>(new BigDecimal(0));
        try (PDDocument document = parser.parse()) {
            String text = new PDFTextStripper().getText(document);
            text.lines().forEach(line -> {
                if (line.toLowerCase().contains("amazon")) {
                    var matcher = PATTERN.matcher(line);
                    System.out.print(line);
                    if (matcher.find()) {
                        System.out.print(" Match 2: " + matcher.group(3));
                        Number parse = null;
                        try {
                            parse = NUMBER_FORMAT.parse(matcher.group(3));
                        } catch (ParseException e) {
                            throw new RuntimeException(e);
                        }
//                        var value = Double.parseDouble(matcher.group(3));
                        amountRef.set(amountRef.get().add(BigDecimal.valueOf(parse.doubleValue())));
                    }
                    System.out.println();
                }
            });
        }
        System.out.println("Amount: " + amountRef.get().toString());
    }
}