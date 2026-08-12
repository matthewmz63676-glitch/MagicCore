package com.magicstudios.magiccore.modules.settings;

import com.magicstudios.magiccore.storage.IdempotencyKeys;
import com.magicstudios.magiccore.storage.RecordRepository;
import com.magicstudios.magiccore.storage.TransactionalDataStore;

import java.time.Clock;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public final class PersistentPlayerSettingsService implements PlayerSettingsService {
    private final TransactionalDataStore store;
    private final Clock clock;
    private final Map<PlayerSetting,Boolean> configuredDefaults;
    private final RecordRepository<PlayerSettings> settings = new RecordRepository<>("settings.player", PlayerSettings.class);

    public PersistentPlayerSettingsService(TransactionalDataStore store, Clock clock) {
        this(store,clock,Map.of());
    }

    public PersistentPlayerSettingsService(TransactionalDataStore store, Clock clock,Map<PlayerSetting,Boolean>configuredDefaults) {
        this.store = store;
        this.clock = clock;
        this.configuredDefaults=Map.copyOf(configuredDefaults);
    }

    @Override
    public CompletionStage<PlayerSettings> get(UUID playerId) {
        return store.read(reader -> settings.get(reader, playerId.toString())
                .map(RecordRepository.VersionedValue::value).orElse(defaults(playerId)));
    }

    @Override
    public CompletionStage<PlayerSettings> set(UUID playerId, PlayerSetting setting, boolean value, String operationKey) {
        return store.transact("setting-set:" + operationKey, tx -> {
            var current = settings.get(tx, playerId.toString());
            PlayerSettings before = current.map(RecordRepository.VersionedValue::value).orElse(defaults(playerId));
            if (!IdempotencyKeys.reserve(tx, "player-setting", operationKey)) return before;
            Map<PlayerSetting, Boolean> values = new EnumMap<>(PlayerSetting.class);
            values.putAll(before.values());
            values.put(setting, value);
            PlayerSettings updated = new PlayerSettings(playerId, values, clock.instant());
            settings.put(tx, playerId.toString(), updated,
                    current.map(RecordRepository.VersionedValue::revision).orElse(0L));
            return updated;
        });
    }

    private PlayerSettings defaults(UUID playerId) {
        Map<PlayerSetting, Boolean> values = new EnumMap<>(PlayerSetting.class);
        for (PlayerSetting setting : PlayerSetting.values()) values.put(setting, configuredDefaults.getOrDefault(setting,setting.defaultValue()));
        return new PlayerSettings(playerId, values, clock.instant());
    }
}
