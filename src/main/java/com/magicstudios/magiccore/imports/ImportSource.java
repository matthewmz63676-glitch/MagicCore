package com.magicstudios.magiccore.imports;

import java.util.concurrent.CompletionStage;

public interface ImportSource { String sourceId();String fingerprint();CompletionStage<ImportPage>read(String afterCursor,int limit); }
