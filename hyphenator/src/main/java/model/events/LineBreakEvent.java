package model.events;
import org.kie.api.definition.type.Role;
import static org.kie.api.definition.type.Role.Type;

@Role(Type.EVENT)
public class LineBreakEvent {
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
