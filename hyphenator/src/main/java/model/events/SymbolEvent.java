package model.events;
import org.kie.api.definition.type.Role;
import static org.kie.api.definition.type.Role.Type;

@Role(Type.EVENT)
public class SymbolEvent {
    @Override
    public String toString() {
        return "SymbolEvent";
    }
}
