package com.ftn.sbnz.model.models;

import java.util.ArrayList;
import java.util.List;

public class Text {
    private String lines = "";
    private String criticalWord = "";
    private String temporaryWord = "";

    public String getCriticalWord() {
        return criticalWord;
    }

    public void addSymbol(char symbol) {
        lines += symbol;
    }

    public void addLineBreak() {
        lines += "\n";
    }

    public List<Letter> enterCriticalSection(char symbol) {
        System.out.println(criticalWord);
        criticalWord = getLastWord() + symbol;
        return tokenizeCriticalWord();
    }

    public List<Letter> addToCriticalSection(char symbol) {
        criticalWord += symbol;
        return tokenizeCriticalWord();
    }

    private List<Letter> tokenizeCriticalWord() {
        List<Letter> letters = new ArrayList<>();
        for (int i = 0; i < criticalWord.length(); i++) {
            letters.add(new Letter(i, criticalWord.charAt(i)));
        }
        return letters;
    }

    public void setTemporaryWord(int pos) {
        temporaryWord = new StringBuilder(criticalWord)
        .insert(pos + 1, "-\n")
        .toString();
    }

    public String getRemainingLetters() {
        int lastDash = temporaryWord.lastIndexOf("-");
        return (lastDash == -1) ? "" : temporaryWord.substring(lastDash + 1);
    }

    public int hyphenizePermanently(int maxRowLength) {
        int offset = getRemainingLetters().length() % maxRowLength;
        lines += temporaryWord;
        lines += offset == 0 ? "\n" : " ";
        temporaryWord = "";
        criticalWord = "";
        return offset;
    }

    private String getLastWord() {
        String[] words = lines.split(" ");
        String lastWord = words[words.length - 1].replace("\n", "");

        int lastIndex = lines.lastIndexOf(" ");
        lines = lines.substring(0, lastIndex + 1);

        return lastWord;
    }

    public void print() {
        System.out.println("Text\n" + lines + temporaryWord);
    }

    @Override
    public String toString() {
        return lines + temporaryWord;
    }
}
