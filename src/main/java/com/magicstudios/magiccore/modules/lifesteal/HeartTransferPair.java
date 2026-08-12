package com.magicstudios.magiccore.modules.lifesteal;
import java.time.Instant;
import java.util.UUID;
public record HeartTransferPair(UUID first,UUID second,Instant lastTransferAt){}
