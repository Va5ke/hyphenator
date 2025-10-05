package model.events;

public class LineBreakEvent extends TextEvent {
    private int offset;

    public LineBreakEvent(int offset) {
        this.offset = offset;
    }

    public int getOffset() {
        return offset;
    }

    @Override
    public String toString() {
        return "LineBreakEvent(offset=" + offset + ")";
    }
}
