package model.events;
import org.kie.api.definition.type.Role;
import static org.kie.api.definition.type.Role.Type;

@Role(Type.EVENT)
public class SpaceEvent {
    @Override
    public String toString() {
        return "SpaceEvent";
    }
}
