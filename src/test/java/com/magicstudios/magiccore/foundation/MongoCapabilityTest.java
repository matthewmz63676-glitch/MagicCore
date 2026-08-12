package com.magicstudios.magiccore.foundation;

import com.magicstudios.magiccore.storage.MongoTransactionCapability;
import com.magicstudios.magiccore.storage.StorageCapabilities;
import com.magicstudios.magiccore.storage.StorageCapabilityException;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MongoCapabilityTest {
    @Test
    void replicaSetAndShardedDeploymentsSupportTransactions() {
        assertThat(MongoTransactionCapability.supportsTransactions(new Document("logicalSessionTimeoutMinutes", 30)
                .append("setName", "rs0"))).isTrue();
        assertThat(MongoTransactionCapability.supportsTransactions(new Document("logicalSessionTimeoutMinutes", 30)
                .append("msg", "isdbgrid"))).isTrue();
    }

    @Test
    void standaloneMongoFailsClosedForCriticalModules() {
        assertThat(MongoTransactionCapability.supportsTransactions(new Document("logicalSessionTimeoutMinutes", 30)))
                .isFalse();
        assertThatThrownBy(() -> new StorageCapabilities(false, false, true, true)
                .requireCriticalTransactions("MONGODB"))
                .isInstanceOf(StorageCapabilityException.class)
                .hasMessageContaining("transaction-capable deployment");
    }
}
