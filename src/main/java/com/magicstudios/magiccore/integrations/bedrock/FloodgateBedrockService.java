package com.magicstudios.magiccore.integrations.bedrock;

import java.lang.reflect.Method;
import java.util.UUID;

public final class FloodgateBedrockService implements BedrockService {
    private final Object api;private final Method check;private final boolean safe;
    private FloodgateBedrockService(Object api,Method check,boolean safe){this.api=api;this.check=check;this.safe=safe;}
    public static BedrockService detect(boolean detect,boolean safe){if(!detect)return unavailable(safe);try{Class<?>type=Class.forName("org.geysermc.floodgate.api.FloodgateApi");Object api=type.getMethod("getInstance").invoke(null);return new FloodgateBedrockService(api,type.getMethod("isFloodgatePlayer",UUID.class),safe);}catch(ReflectiveOperationException failure){return unavailable(safe);}}
    private static BedrockService unavailable(boolean safe){return new BedrockService(){public boolean available(){return false;}public boolean isBedrockPlayer(UUID id){return false;}public boolean useBedrockSafeInteractions(){return safe;}};}
    @Override public boolean available(){return true;}
    @Override public boolean isBedrockPlayer(UUID playerId){try{return Boolean.TRUE.equals(check.invoke(api,playerId));}catch(ReflectiveOperationException failure){return false;}}
    @Override public boolean useBedrockSafeInteractions(){return safe;}
}
