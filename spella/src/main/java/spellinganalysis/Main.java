/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package spellinganalysis;

import java.io.*;
import java.util.*;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.text.*;
import org.languagetool.*;
import org.languagetool.rules.*;
import org.languagetool.language.*;


public class Main {

    private static class SpellingError {
        private final String error;
        private final int numOfSuggestions;
        private SpellingError(final String error, final int numOfSuggestions) {
            this.error = error;
            this.numOfSuggestions = numOfSuggestions;
        }
        @Override
        public boolean equals(final Object o) {
            if (o instanceof SpellingError) {
                return this.error.equals(((SpellingError)o).error);
            }
            return false;
        }
        @Override
        public int hashCode() {
            return this.error.hashCode();
        }
    }

    public static void main(final String[] args) {
        final File input = new File(args[0]);
        final File output = new File(args[1]);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(output))) {
            final List<String> pages = Main.getPages(input);
            int page = 1;
            for (final String text : pages) {
                final List<SpellingError> errors = Main.getErrors(text, new GermanyGerman());
                errors.retainAll(Main.getErrors(text, new AmericanEnglish()));
                errors.retainAll(Main.getErrors(text, new BritishEnglish()));
                if (!errors.isEmpty()) {
                    writer.write(String.format("//////// Seite %d: ////////", page));
                    writer.newLine();
                }
                for (final SpellingError error : errors) {
                    writer.write(error.error);
//                    writer.write(" (");
//                    writer.write(String.valueOf(error.numOfSuggestions));
//                    writer.write(")");
                    writer.newLine();
                }
                page++;
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    private static List<SpellingError> getErrors(final String text, final Language language) throws IOException {
        final List<SpellingError> errors = new LinkedList<SpellingError>();
        final JLanguageTool langTool = new JLanguageTool(language);
        for (final Rule rule : langTool.getAllRules()) {
            if (!rule.isDictionaryBasedSpellingRule()) {
                langTool.disableRule(rule.getId());
            }
        }
        final List<RuleMatch> matches = langTool.check(text);
        for (final RuleMatch match : matches) {
            errors.add(
                new SpellingError(
                    text.substring(match.getFromPos(), match.getToPos()),
                    match.getSuggestedReplacements().size()
                )
            );
        }
        return errors;
    }

    private static List<String> getPages(final File pdf) throws IOException {
        try (final PDDocument document = PDDocument.load(pdf)) {
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
                result.add(Main.sanitizeText(pdfStripper.getText(document)));
            }
            return result;
        }
    }

    private static String sanitizeText(final String text) {
        return text.replaceAll("-[\\r\\n]+(?=[a-z])", "").replaceAll("-[\\r\\n]+", "-");
    }

}
