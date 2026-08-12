package com.magicstudios.magiccore.modules.events;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface KothService {
    List<KothDefinition> definitions();
    CompletionStage<KothRun> start(String definitionId,String operationKey);
    CompletionStage<KothRun> sample(UUID runId,Collection<KothContender>contenders,Duration elapsed,String operationKey);
    CompletionStage<KothRun> cancel(UUID runId,String operationKey);
    CompletionStage<Optional<KothRun>> active(String definitionId);
    CompletionStage<List<KothRun>> recent(int limit);
}
