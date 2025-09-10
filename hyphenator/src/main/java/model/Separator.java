package model;

public class Separator {
    private int position;
    public boolean valid = true;

    public Separator(int position) {
        this.position = position;
    }

    public int getPosition() {
        return position;
    }

    public boolean isValid() {
        return valid;
    }

    public void invalidate() {
        this.valid = false;
    }
}
