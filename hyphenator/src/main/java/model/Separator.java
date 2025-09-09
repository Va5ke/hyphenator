package model;

public class Separator {
    private int left;
    private int right;

    public Separator(int left, int right) {
        this.left = left;
        this.right = right;
    }

    public int getLeft() {
        return left;
    }

    public int getRight() {
        return right;
    }

    @Override
    public String toString() {
        return left + "|" + right;
    }
}
