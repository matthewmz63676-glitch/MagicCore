package com.magicstudios.magiccore.imports;

import java.time.Instant;
import java.util.Map;

public record ImportJob(String importId,String sourceId,String targetId,String fingerprint,Map<String,String>mapping,
                        String checkpoint,long appliedRows,long verifiedRows,Status status,Instant updatedAt){
 public enum Status{PREVIEWED,RUNNING,COMPLETE,RECONCILIATION_REQUIRED}
 public ImportJob{mapping=Map.copyOf(mapping);}
}
