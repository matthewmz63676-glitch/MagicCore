package com.magicstudios.magiccore.modules.tools;
import java.time.Instant;import java.util.List;import java.util.UUID;
public interface CustomToolService{List<ToolDefinition>definitions();ToolDefinition require(String id);boolean claimCooldown(UUID playerId,String toolId,Instant now);}
