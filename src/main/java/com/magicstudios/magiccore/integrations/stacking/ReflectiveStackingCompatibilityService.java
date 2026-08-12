package com.magicstudios.magiccore.integrations.stacking;

import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;
import java.lang.reflect.Method;

public final class ReflectiveStackingCompatibilityService implements StackingCompatibilityService {
    private final String provider;private final Object api;private final Method lookup;
    private ReflectiveStackingCompatibilityService(String provider,Object api,Method lookup){this.provider=provider;this.api=api;this.lookup=lookup;}
    public static StackingCompatibilityService detect(Plugin plugin,String configured){String provider=configured.toUpperCase();if(provider.equals("AUTO")){
        if(plugin.getServer().getPluginManager().getPlugin("RoseStacker")!=null)provider="ROSESTACKER";else if(plugin.getServer().getPluginManager().getPlugin("WildStacker")!=null)provider="WILDSTACKER";else return unavailable("NONE");}
        try{if(provider.equals("ROSESTACKER")){Class<?>type=Class.forName("dev.rosewood.rosestacker.api.RoseStackerAPI");Object api=type.getMethod("getInstance").invoke(null);return new ReflectiveStackingCompatibilityService(provider,api,type.getMethod("getStackedSpawner",Block.class));}
            if(provider.equals("WILDSTACKER")){Class<?>type=Class.forName("com.bgsoftware.wildstacker.api.WildStackerAPI");Object api=type.getMethod("getWildStacker").invoke(null);Object manager=type.getMethod("getSystemManager").invoke(api);return new ReflectiveStackingCompatibilityService(provider,manager,manager.getClass().getMethod("getStackedSpawner",Block.class));}}
        catch(ReflectiveOperationException failure){return unavailable(provider);}return unavailable(provider);}
    private static StackingCompatibilityService unavailable(String provider){return new StackingCompatibilityService(){public String provider(){return provider;}public boolean available(){return false;}public boolean isManagedSpawner(Block block){return false;}};}
    @Override public String provider(){return provider;}@Override public boolean available(){return true;}@Override public boolean isManagedSpawner(Block block){try{return lookup.invoke(api,block)!=null;}catch(ReflectiveOperationException failure){return false;}}
}
