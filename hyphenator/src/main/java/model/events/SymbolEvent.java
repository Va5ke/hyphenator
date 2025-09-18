package model.events;

public class SymbolEvent extends TextEvent {
    private char symbol;

    public SymbolEvent(char symbol) {
        this.symbol = symbol;
    }

    public char getSymbol() {
        return symbol;
    }

    @Override
    public String toString() {
        return "SymbolEvent";
    }
}
