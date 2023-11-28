package spellinganalysis;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.text.*;
import org.languagetool.*;
import org.languagetool.rules.*;
import org.languagetool.language.*;


public class Main {

    public static void main(final String[] args) {
        final File input = new File(args[0]);
        final File output = new File(args[1]);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(output))) {
            final List<String> personalDictionary = Main.readPersonalDictionary(args);
            final List<String> pages = Main.getPages(input);
            int page = 1;
            for (final String text : pages) {
                final List<String> errors =
                    new ArrayList<String>(
                        Main.getErrors(text, new GermanyGerman()).stream().filter(s -> s.length() > 2).toList()
                    );
                errors.retainAll(Main.getErrors(text, new AmericanEnglish()));
                errors.retainAll(Main.getErrors(text, new BritishEnglish()));
                errors.removeAll(personalDictionary);
                if (!errors.isEmpty()) {
                    writer.write(String.format("//////// Seite %d: ////////", page));
                    writer.newLine();
                }
                for (final String error : errors) {
                    writer.write(error);
                    writer.newLine();
                }
                page++;
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    private static List<String> getErrors(final String text, final Language language) throws IOException {
        final JLanguageTool langTool = new JLanguageTool(language);
        for (final Rule rule : langTool.getAllRules()) {
            if (!rule.isDictionaryBasedSpellingRule()) {
                langTool.disableRule(rule.getId());
            }
        }
        return
            langTool.check(text).stream().map(match -> text.substring(match.getFromPos(), match.getToPos())).toList();
    }

    private static List<String> getPages(final File input) throws IOException {
        if (input.getName().toLowerCase().endsWith(".pdf")) {
            try (final PDDocument document = PDDocument.load(input)) {
                final List<String> result = new LinkedList<String>();
                final int pages = document.getNumberOfPages();
                final PDFTextStripper pdfStripper = new PDFTextStripper() {
                    @Override
                    protected void writePage() throws IOException {
                        super.writePage();
                    }
                };
                for (int i = 1; i <= pages; i++) {
                    pdfStripper.setStartPage(i);
                    pdfStripper.setEndPage(i);
                    final String originalText = pdfStripper.getText(document);
                    final String sanitizedText = Main.sanitizeText(originalText);
//                    if (!originalText.equals(sanitizedText)) {
//                        System.out.println("foo");
//                    }
                    result.add(sanitizedText);
                }
                return result;
            }
        }
        return List.of(
            Files
            .readString(input.toPath())
            .replaceAll("\uFB03", "ffi")
            .replaceAll("\uFB04", "ffl")
            .replaceAll("\uFB00", "ff")
            .replaceAll("\uFB01", "fi")
            .replaceAll("\uFB02", "fl")
            .replaceAll("\uFB05", "ft")
            .split("\f")
        );
    }

    private static List<String> readPersonalDictionary(final String[] args) throws IOException {
        if (args.length == 3) {
            return Files.newBufferedReader(Paths.get(args[2])).lines().toList();
        }
        return Collections.emptyList();
    }

    private static String sanitizeText(final String text) {
        return text.replaceAll("-[\\r\\n]+(?=[a-z])", "").replaceAll("-[\\r\\n]+", "-");
    }

}
