package com.magicstudios.magiccore.admin;

import com.magicstudios.magiccore.config.ConfigCommit;

public record AdminCommitResult<T>(ConfigCommit<T> commit, boolean audited) {
}
