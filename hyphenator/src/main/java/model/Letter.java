package model;

public class Letter extends Token {
    private char symbol;
    
    private PhoneticTraits traits = new PhoneticTraits(null, 0);

    private LetterRole role = LetterRole.NONE;

    public Letter(int pos, char value) {
        super(pos);
        this.symbol = value;
    }

    public char getSymbol() {
        return symbol;
    }

    public PhoneticTraits getTraits() {
        return traits;
    }
    
    public LetterRole getRole() {
        return role;
    }

    public void setRole(LetterRole role) {
        this.role = role;
    }

    @Override
    public String toString() {
        return "Letter(symbol: " + symbol + ", pos: " + getPosition() + ", type: " + traits.getType() + ", sonority: " + traits.getSonority() + ", role: " + role + ")";
    }
}
