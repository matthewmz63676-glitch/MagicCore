package com.magicstudios.magiccore.phasefive;

import com.magicstudios.magiccore.imports.*;
import com.magicstudios.magiccore.platform.BoundedIoExecutor;
import com.magicstudios.magiccore.storage.InMemoryTransactionalDataStore;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

class ImportServiceTest {
    @Test void previewMapsColumnsExecutionCheckpointsAndReconciliationFindsDrift() {
        var store=new InMemoryTransactionalDataStore(new BoundedIoExecutor(2,64,"import-test"));
        try{var service=new PersistentImportService(store, Clock.fixed(Instant.parse("2026-08-10T00:00:00Z"), ZoneOffset.UTC));
            var source=new RowsSource(List.of(new ImportRow("001",Map.of("legacy_id","alice","legacy_value","10")),new ImportRow("002",Map.of("legacy_id","bob","legacy_value","20"))));
            var target=new MapTarget();Map<String,String>mapping=Map.of("id","legacy_id","value","legacy_value");
            var preview=service.preview("players-1",source,target,mapping).toCompletableFuture().join();assertThat(preview.totalRows()).isEqualTo(2);assertThat(preview.validRows()).isEqualTo(2);assertThat(preview.issues()).isEmpty();
            var run=service.execute("players-1",source,target,1).toCompletableFuture().join();assertThat(run.job().status()).isEqualTo(ImportJob.Status.COMPLETE);assertThat(run.job().appliedRows()).isEqualTo(2);assertThat(run.job().verifiedRows()).isEqualTo(2);assertThat(target.values).containsEntry("alice",10).containsEntry("bob",20);
            target.values.put("bob",99);var reconciliation=service.reconcile("players-1",source,target,1).toCompletableFuture().join();assertThat(reconciliation.job().status()).isEqualTo(ImportJob.Status.RECONCILIATION_REQUIRED);assertThat(reconciliation.issues()).singleElement().satisfies(issue->assertThat(issue.rowKey()).isEqualTo("002"));
        }finally{store.close();}
    }
    private static final class RowsSource implements ImportSource{private final List<ImportRow>rows;RowsSource(List<ImportRow>rows){this.rows=rows;}public String sourceId(){return "rows";}public String fingerprint(){return "sha256:test";}
        public java.util.concurrent.CompletionStage<ImportPage>read(String after,int limit){int start=after==null?0:Integer.parseInt(after);int end=Math.min(rows.size(),start+limit);return CompletableFuture.completedFuture(new ImportPage(rows.subList(start,end),Integer.toString(end),end==rows.size()));}}
    private static final class MapTarget implements ImportTarget{private final Map<String,Integer>values=new ConcurrentHashMap<>();public String targetId(){return "map";}public List<String>requiredFields(){return List.of("id","value");}
        public List<String>validate(ImportRow row){try{Integer.parseInt(row.values().get("value"));return row.values().get("id").isBlank()?List.of("blank id"):List.of();}catch(Exception failure){return List.of("invalid value");}}
        public java.util.concurrent.CompletionStage<Void>apply(ImportRow row,String operation){values.put(row.values().get("id"),Integer.parseInt(row.values().get("value")));return CompletableFuture.completedFuture(null);}
        public java.util.concurrent.CompletionStage<Boolean>verify(ImportRow row){return CompletableFuture.completedFuture(values.getOrDefault(row.values().get("id"),-1)==Integer.parseInt(row.values().get("value")));}}
}
