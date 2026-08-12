package com.magicstudios.magiccore.imports;

import java.util.List;
import java.util.concurrent.CompletionStage;

public interface ImportTarget {
    String targetId();
    List<String> requiredFields();
    List<String> validate(ImportRow row);
    CompletionStage<Void> apply(ImportRow row,String operationKey);
    CompletionStage<Boolean> verify(ImportRow row);
}
