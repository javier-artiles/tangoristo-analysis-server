package com.tangoristo.server.model;

import com.mariten.kanatools.KanaAppraiser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

@Builder
@AllArgsConstructor
@Data
public class Token {
    private String surfaceForm;
    private String surfaceReading;
    private String baseForm;
    private String partOfSpeech;
    private int startOffset;
    @Builder.Default private boolean isInflected = false;

    private String getKanaPrefix(char[] input) {
        StringBuilder prefix = new StringBuilder();
        boolean isPreviousKana = false;
        for (char c : input) {
            if (isKana(c) || (c == 'ー' && isPreviousKana)) {
                prefix.append(c);
                isPreviousKana = true;
            } else {
                break;
            }
        }
        return prefix.toString();
    }

    public static String getKanaSuffix(char[] input) {
        StringBuilder suffix = new StringBuilder();
        for (int c = input.length - 1; c >= 0; c--) {
            char ch = input[c];
            if (isKana(ch)) {
                suffix.insert(0, ch);
            } else {
                break;
            }
        }
        return suffix.toString();
    }

    public static boolean isKana(String str) {
        for (char ch : str.toCharArray()) {
            if (!isKana(ch)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isKana(char c) {
        return KanaAppraiser.isZenkakuHiragana(c) || KanaAppraiser.isHankakuKatakana(c) || KanaAppraiser.isZenkakuKatakana(c) ||
                KanaAppraiser.isZenkakuLetter(c) || KanaAppraiser.isZenkakuNumber(c) || KanaAppraiser.isZenkakuAscii(c);
    }

    public static boolean isKatakana(String str) {
        for (char ch : str.toCharArray()) {
            if (!isKatakana(ch)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isKatakana(char c) {
        return KanaAppraiser.isHankakuKatakana(c) || KanaAppraiser.isZenkakuKatakana(c);
    }

    public List<ReadingToSurfaceFormPair> getAlignedReadingsToForms() {
        char[] surfaceFormChars = surfaceForm.toCharArray();
        String surfaceKanaPrefix = getKanaPrefix(surfaceFormChars);

        // Exception, we want to keep together days of the week and months to show furigana over the entire sequence
        if (surfaceForm.matches("[０-９][０-９]?(月|日)")) {
            surfaceKanaPrefix = "";
        }

        if (surfaceKanaPrefix.equals(surfaceForm)) {
            // By convention we don't need to show furigana reading for words made of kana
            return Collections.singletonList(new ReadingToSurfaceFormPair("", surfaceForm));
        } else {
            String surfaceKanaSuffix = getKanaSuffix(surfaceFormChars);

            List<ReadingToSurfaceFormPair> pairList = new ArrayList<>();
            if (surfaceKanaPrefix.length() > 0) {
                pairList.add(new ReadingToSurfaceFormPair("", surfaceKanaPrefix));
            }

            String surfaceMiddle = surfaceForm.substring(surfaceKanaPrefix.length(), surfaceForm.length() - surfaceKanaSuffix.length());
            String readingMiddle = surfaceReading
                    .replaceAll("^" + Pattern.quote(surfaceKanaPrefix), "")
                    .replaceAll(Pattern.quote(surfaceKanaSuffix) + "$", "");
            if (surfaceMiddle.equals(readingMiddle) || surfaceMiddle.equals("ー")) {
                pairList.add(new ReadingToSurfaceFormPair("", surfaceMiddle));
            } else {
                pairList.add(new ReadingToSurfaceFormPair(readingMiddle, surfaceMiddle));
            }

            if (surfaceKanaSuffix.length() > 0) {
                pairList.add(new ReadingToSurfaceFormPair("", surfaceKanaSuffix));
            }

            return pairList;
        }
    }
}
