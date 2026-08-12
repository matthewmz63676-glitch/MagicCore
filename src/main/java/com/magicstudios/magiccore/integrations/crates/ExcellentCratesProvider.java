package com.magicstudios.magiccore.integrations.crates;

import com.magicstudios.magiccore.modules.crates.ExternalCrateProvider;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.UUID;

public final class ExcellentCratesProvider implements ExternalCrateProvider {
    private final Plugin plugin;private final Class<?>api;
    private ExcellentCratesProvider(Plugin plugin,Class<?>api){this.plugin=plugin;this.api=api;}
    public static ExcellentCratesProvider create(Plugin plugin){Class<?>type=null;for(String name:new String[]{"su.nightexpress.excellentcrates.api.CratesAPI","su.nightexpress.excellentcrates.CratesAPI"})try{type=Class.forName(name);break;}catch(ClassNotFoundException ignored){}
        return new ExcellentCratesProvider(plugin,type);}
    @Override public String id(){return "EXCELLENTCRATES";}@Override public boolean available(){return api!=null&&plugin.getServer().getPluginManager().getPlugin("ExcellentCrates")!=null;}
    @Override public boolean hasCrate(String crateId)throws Exception{return invoke(api,"getCrateManager")!=null&&invoke(invoke(api,"getCrateManager"),"getCrateById",crateId)!=null;}
    @Override public long keyBalance(UUID playerId,String keyId)throws Exception{Object key=key(keyId);Object user=user(playerId);Object amount=invoke(user,"countKeys",key);return ((Number)amount).longValue();}
    @Override public void grantKeys(UUID playerId,String keyId,long amount)throws Exception{if(amount>Integer.MAX_VALUE)throw new IllegalArgumentException("ExcellentCrates key grant exceeds integer range");Object key=key(keyId),user=user(playerId),manager=invoke(api,"getKeyManager");invoke(manager,"giveKey",user,key,(int)amount);Object users=invoke(api,"getUserManager");invoke(users,"save",user);}
    @Override public boolean open(UUID playerId,String crateId)throws Exception{Player player=plugin.getServer().getPlayer(playerId);if(player==null)return false;Object manager=invoke(api,"getCrateManager"),crate=invoke(manager,"getCrateById",crateId);if(crate==null)throw new IllegalArgumentException("Unknown ExcellentCrates crate "+crateId);
        Class<?>sourceType=Class.forName("su.nightexpress.excellentcrates.crate.impl.CrateSource");Object source=null;for(var constructor:sourceType.getConstructors())if(constructor.getParameterCount()==3){source=constructor.newInstance(crate,null,null);break;}if(source==null)throw new NoSuchMethodException("ExcellentCrates CrateSource constructor");
        invoke(manager,"preOpenCrate",player,source);return true;}
    private Object key(String keyId)throws Exception{Object key=invoke(invoke(api,"getKeyManager"),"getKeyById",keyId);if(key==null)throw new IllegalArgumentException("Unknown ExcellentCrates key "+keyId);return key;}
    private Object user(UUID playerId)throws Exception{Object user=invoke(invoke(api,"getUserManager"),"getUserData",playerId);if(user==null)throw new IllegalStateException("ExcellentCrates user data is unavailable for "+playerId);return user;}
    private static Object invoke(Object target,String name,Object...args)throws Exception{Class<?>type=target instanceof Class<?>clazz?clazz:target.getClass();for(Method method:type.getMethods())if(method.getName().equals(name)&&method.getParameterCount()==args.length&&compatible(method.getParameterTypes(),args)){return method.invoke(target instanceof Class<?>?null:target,args);}throw new NoSuchMethodException(type.getName()+"#"+name);}
    private static boolean compatible(Class<?>[]types,Object[]args){for(int i=0;i<types.length;i++){if(args[i]==null)continue;Class<?>expected=types[i].isPrimitive()?box(types[i]):types[i];if(!expected.isInstance(args[i]))return false;}return true;}
    private static Class<?>box(Class<?>type){if(type==int.class)return Integer.class;if(type==long.class)return Long.class;if(type==boolean.class)return Boolean.class;if(type==double.class)return Double.class;if(type==float.class)return Float.class;if(type==short.class)return Short.class;if(type==byte.class)return Byte.class;if(type==char.class)return Character.class;return type;}
}
