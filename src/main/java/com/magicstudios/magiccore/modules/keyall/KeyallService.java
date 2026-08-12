package com.magicstudios.magiccore.modules.keyall;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface KeyallService {
    Map<String,KeyallDefinition> definitions();
    CompletionStage<KeyallRun> preview(String definitionId, KeyallRun.Trigger trigger, Collection<UUID> recipients);
    CompletionStage<KeyallRun> execute(UUID runId, String operationKey);
    CompletionStage<KeyallRun> cancel(UUID runId, String operationKey);
    CompletionStage<Optional<KeyallRun>> find(UUID runId);
    CompletionStage<Optional<KeyallRun>> contribute(String definitionId, long amount, Collection<UUID> recipients, String operationKey);
    CompletionStage<Integer> recover();
}
