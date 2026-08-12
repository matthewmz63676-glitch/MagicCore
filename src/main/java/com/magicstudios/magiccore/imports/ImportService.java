package com.magicstudios.magiccore.imports;

import java.util.Map;
import java.util.concurrent.CompletionStage;

public interface ImportService {
 CompletionStage<ImportPreview>preview(String importId,ImportSource source,ImportTarget target,Map<String,String>mapping);
 CompletionStage<ImportRun>execute(String importId,ImportSource source,ImportTarget target,int batchSize);
 CompletionStage<ImportRun>reconcile(String importId,ImportSource source,ImportTarget target,int batchSize);
 CompletionStage<java.util.Optional<ImportJob>>job(String importId);
}
