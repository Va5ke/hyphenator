package model;

public class Separator extends Token{
    public boolean valid = true;

    public Separator(int position) {
        super(position);
    }

    public boolean isValid() {
        return valid;
    }

    public void invalidate() {
        this.valid = false;
    }
}
