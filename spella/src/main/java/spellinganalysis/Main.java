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

    public static void main(final String[] args) {
        final File input = new File(args[0]);
        final File output = new File(args[1]);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(output))) {
            final String text = Main.getText(input);
            final List<String> errors = Main.getErrors(text, new GermanyGerman());
            errors.retainAll(Main.getErrors(text, new AmericanEnglish()));
            errors.retainAll(Main.getErrors(text, new BritishEnglish()));
            for (final String error : errors) {
                writer.write(error);
                writer.newLine();
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    private static List<String> getErrors(final String text, final Language language) throws IOException {
        final List<String> errors = new ArrayList<String>();
        final JLanguageTool langTool = new JLanguageTool(language);
        for (final Rule rule : langTool.getAllRules()) {
          if (!rule.isDictionaryBasedSpellingRule()) {
            langTool.disableRule(rule.getId());
          }
        }
        final List<RuleMatch> matches = langTool.check(text);
        for (final RuleMatch match : matches) {
          errors.add(text.substring(match.getFromPos(), match.getToPos()));
        }
        return errors;
    }

    private static String getText(final File pdf) throws IOException {
        try (final PDDocument document = PDDocument.load(pdf)) {
            final PDFTextStripper pdfStripper = new PDFTextStripper();
            return pdfStripper.getText(document);
        }
    }

}
