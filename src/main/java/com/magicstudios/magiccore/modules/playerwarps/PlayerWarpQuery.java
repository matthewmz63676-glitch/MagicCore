package com.magicstudios.magiccore.modules.playerwarps;

import java.util.UUID;

public record PlayerWarpQuery(String text,String category,UUID ownerId,UUID viewerId,Sort sort,int offset,int limit){
 public enum Sort{SPONSORED,VISITS,NEWEST,NAME,FAVORITES}
 public PlayerWarpQuery{if(offset<0||limit<1||limit>100)throw new IllegalArgumentException("PlayerWarp page bounds are invalid");text=text==null?"":text;category=category==null?"":category;}
}
