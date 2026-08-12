package com.magicstudios.magiccore.modules.essentials;

public record RtpResult(boolean completed, String code, WorldPosition destination, int attempts) { }
