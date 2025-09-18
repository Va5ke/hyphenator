package model;

import java.util.ArrayList;
import java.util.List;

public class Text {
    private int maxRowLength;
    private List<String> rows;
    private boolean inCriticalSection = false;

    public Text(int maxRowLength) {
        this.maxRowLength = maxRowLength;
        this.rows = new ArrayList<>();
        rows.add("");
    }

    public int getMaxRowLength() {
        return maxRowLength;
    }

    public void setMaxRowLength(int maxRowLength) {
        this.maxRowLength = maxRowLength;
    }

    public boolean isInCriticalSection() {
        return inCriticalSection;
    }

    public void setInCriticalSection(boolean inCriticalSection) {
        this.inCriticalSection = inCriticalSection;
    }

    public void add(char symbol) {
        rows.set(rows.size() - 1, rows.get(rows.size() - 1) + symbol);
    }

    public void backspace() {
        String row = rows.get(rows.size() - 1);
        if (!row.isEmpty()) {
            rows.set(rows.size() - 1, row.substring(0, row.length() - 1));
        }
    }

    @Override
    public String toString() {
        return "Text(" + maxRowLength + ")\n" + String.join("\n", rows);
    }
}
