package model;

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

    public int hyphenizePermanently() {
        lines += temporaryWord;
        temporaryWord = "";
        criticalWord = "";
        //TODO razmak ili enter u zavisnosti od duzine, vrati offset prelomljenog dela
        return 0;
    }

    private String getLastWord() {
        String[] words = lines.split(" ");
        String lastWord = words[words.length - 1].replace("\n", "");

        int lastIndex = lines.lastIndexOf(" ");
        lines = lines.substring(0, lastIndex + 1);

        return lastWord;
    }

    @Override
    public String toString() {
        return "Text\n" + lines;
    }
}
