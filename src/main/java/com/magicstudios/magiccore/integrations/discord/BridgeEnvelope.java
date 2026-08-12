package com.magicstudios.magiccore.integrations.discord;

import java.time.Instant;
import java.util.UUID;

public record BridgeEnvelope(UUID id,String nonce,String type,String payload,Instant timestamp,String signature) { }
