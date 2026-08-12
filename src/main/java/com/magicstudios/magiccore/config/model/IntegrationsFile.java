package com.magicstudios.magiccore.config.model;

public record IntegrationsFile(int configVersion, LuckPerms luckperms, Vault vault,
                               Toggle placeholderapi, Toggle worldguard, Claims claims,
                               ProviderToggle discord, ProviderToggle lunarClient,
                               ProviderOnly display, ProviderOnly customItems, Bedrock bedrock,
                               ProviderOnly holograms, ProviderOnly npcs, ProviderOnly spawners, ProviderOnly crates,
                               Vulcan vulcan) {
    public IntegrationsFile { if(holograms==null)holograms=new ProviderOnly("NONE");if(npcs==null)npcs=new ProviderOnly("NONE");
        if(spawners==null)spawners=new ProviderOnly("AUTO");if(crates==null)crates=new ProviderOnly("INTERNAL");if(vulcan==null)vulcan=new Vulcan(true,true,3600,128); }
    public record LuckPerms(boolean enabled, String mode, boolean syncToLuckperms) { }
    public record Vault(boolean enabled, boolean registerInternalEconomy) { }
    public record Toggle(boolean enabled) { }
    public record Claims(String provider) { }
    public record ProviderToggle(String provider, boolean enabled) { }
    public record ProviderOnly(String provider) { }
    public record Bedrock(boolean detectGeyserFloodgate, boolean useBedrockSafeInteractions) { }
    public record Vulcan(boolean enabled,boolean captureFlags,long retentionSeconds,int maximumFlagsPerPlayer) { }
}
