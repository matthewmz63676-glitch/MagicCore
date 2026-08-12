package com.magicstudios.magiccore.config;

import com.magicstudios.magiccore.platform.BoundedIoExecutor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;

public final class AtomicConfigStore<T> {
    private static final DateTimeFormatter BACKUP_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS")
            .withZone(ZoneOffset.UTC);

    private final Path file;
    private final Path backupDirectory;
    private final ConfigCodec<T> codec;
    private final ConfigValidator<T> validator;
    private final BoundedIoExecutor executor;
    private final Clock clock;
    private final AtomicReference<ConfigSnapshot<T>> active = new AtomicReference<>();

    public AtomicConfigStore(Path file, Path backupDirectory, ConfigCodec<T> codec,
                             ConfigValidator<T> validator, BoundedIoExecutor executor) {
        this(file, backupDirectory, codec, validator, executor, Clock.systemUTC());
    }

    AtomicConfigStore(Path file, Path backupDirectory, ConfigCodec<T> codec,
                      ConfigValidator<T> validator, BoundedIoExecutor executor, Clock clock) {
        this.file = Objects.requireNonNull(file, "file");
        this.backupDirectory = Objects.requireNonNull(backupDirectory, "backupDirectory");
        this.codec = Objects.requireNonNull(codec, "codec");
        this.validator = Objects.requireNonNull(validator, "validator");
        this.executor = Objects.requireNonNull(executor, "executor");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public CompletionStage<ConfigSnapshot<T>> load() {
        return executor.submit(() -> {
            byte[] bytes = Files.readAllBytes(file);
            T candidate = codec.decode(bytes);
            validate(candidate);
            ConfigSnapshot<T> previous = active.get();
            long revision = previous == null ? 1 : previous.revision() + 1;
            ConfigSnapshot<T> snapshot = new ConfigSnapshot<>(revision, candidate, clock.instant(), hash(bytes));
            active.set(snapshot);
            return snapshot;
        });
    }

    public CompletionStage<ConfigSnapshot<T>> loadOrCreate(T defaults) {
        return executor.submit(() -> {
            if (!Files.exists(file)) {
                validate(defaults);
                Files.createDirectories(file.getParent());
                writeAtomically(codec.encode(defaults));
            }
            byte[] bytes = Files.readAllBytes(file);
            T candidate = codec.decode(bytes);
            validate(candidate);
            ConfigSnapshot<T> snapshot = new ConfigSnapshot<>(1, candidate, clock.instant(), hash(bytes));
            active.set(snapshot);
            return snapshot;
        });
    }

    public CompletionStage<ConfigCommit<T>> commit(ConfigChange<T> change) {
        return executor.submit(() -> {
            synchronized (this) {
                ConfigSnapshot<T> current = snapshot();
                if (current.revision() != change.expectedRevision()) {
                    throw new ConfigRevisionConflictException(change.expectedRevision(), current.revision());
                }
                T candidate = Objects.requireNonNull(change.mutation().apply(current.value()), "mutation result");
                List<ValidationIssue> issues = validator.validate(candidate);
                List<ValidationIssue> errors = issues.stream()
                        .filter(issue -> issue.severity() == ValidationSeverity.ERROR).toList();
                if (!errors.isEmpty()) {
                    throw new ConfigValidationException(errors);
                }
                byte[] bytes = codec.encode(candidate);
                Path backup = backupCurrent();
                writeAtomically(bytes);
                ConfigSnapshot<T> updated = new ConfigSnapshot<>(current.revision() + 1, candidate,
                        clock.instant(), hash(bytes));
                active.set(updated);
                return new ConfigCommit<>(updated, backup, change.restartRequired(), issues.stream()
                        .filter(issue -> issue.severity() == ValidationSeverity.WARNING).toList());
            }
        });
    }

    public ConfigSnapshot<T> snapshot() {
        ConfigSnapshot<T> snapshot = active.get();
        if (snapshot == null) {
            throw new IllegalStateException("Configuration has not been loaded: " + file);
        }
        return snapshot;
    }

    private void validate(T candidate) {
        List<ValidationIssue> errors = validator.validate(candidate).stream()
                .filter(issue -> issue.severity() == ValidationSeverity.ERROR).toList();
        if (!errors.isEmpty()) {
            throw new ConfigValidationException(errors);
        }
    }

    private Path backupCurrent() throws IOException {
        if (!Files.exists(file)) {
            return null;
        }
        Files.createDirectories(backupDirectory);
        Path backup = backupDirectory.resolve(file.getFileName() + "." + BACKUP_TIME.format(clock.instant()) + ".bak");
        Files.copy(file, backup, StandardCopyOption.COPY_ATTRIBUTES);
        return backup;
    }

    private void writeAtomically(byte[] bytes) throws IOException {
        Files.createDirectories(file.getParent());
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            channel.write(ByteBuffer.wrap(bytes));
            channel.force(true);
        }
        try {
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String hash(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}
