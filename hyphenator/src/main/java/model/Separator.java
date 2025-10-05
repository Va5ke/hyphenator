package model;

public class Separator extends Token{
    private boolean valid = true;
    private boolean chosen = false;

    public Separator(int position) {
        super(position);
    }

    public boolean isValid() {
        return valid;
    }

    public void invalidate() {
        this.valid = false;
    }

    public boolean isChosen() {
        return chosen;
    }

    public void choose() {
        this.chosen = true;
    }
}
