package com.magicstudios.magiccore.audit;

import java.util.List;
import java.util.concurrent.CompletionStage;

public interface AuditService {
    CompletionStage<Boolean> record(AuditEvent event);

    CompletionStage<List<AuditEvent>> recent(String afterKey, int limit);
}
