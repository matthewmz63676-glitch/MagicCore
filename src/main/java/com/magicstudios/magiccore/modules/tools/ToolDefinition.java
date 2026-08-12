package com.magicstudios.magiccore.modules.tools;
import java.time.Duration;import java.util.List;import java.util.Set;
public record ToolDefinition(String id,String material,String displayName,int durability,Duration cooldown,String dropPolicy,Set<String>blockAllowlist,List<ToolUpgrade>upgrades){public ToolDefinition{blockAllowlist=Set.copyOf(blockAllowlist);upgrades=List.copyOf(upgrades);}public ToolUpgrade upgrade(int level){return upgrades.stream().filter(value->value.level()==level).findFirst().orElseThrow(()->new IllegalArgumentException("Unknown tool upgrade level"));}}
