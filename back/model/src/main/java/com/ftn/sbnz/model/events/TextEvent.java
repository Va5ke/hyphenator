package com.ftn.sbnz.model.events;

import org.kie.api.definition.type.Role;
import static org.kie.api.definition.type.Role.Type;

@Role(Type.EVENT)

public class TextEvent {
    private boolean processed = false;

    public boolean isProcessed() {
        return processed;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }
    
}
