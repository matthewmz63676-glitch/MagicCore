package com.magicstudios.magiccore.modules.securestorage;

public record SecureStorageSession(StorageLease lease, VirtualContainer container, long revision) { }
