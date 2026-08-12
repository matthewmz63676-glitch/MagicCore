package com.magicstudios.magiccore.storage;

import org.bson.Document;

public final class MongoTransactionCapability {
    private MongoTransactionCapability() {
    }

    public static boolean supportsTransactions(Document helloResponse) {
        boolean sessions = helloResponse.get("logicalSessionTimeoutMinutes") instanceof Number;
        boolean replicaSet = helloResponse.getString("setName") != null;
        boolean sharded = "isdbgrid".equals(helloResponse.getString("msg"));
        return sessions && (replicaSet || sharded);
    }
}
