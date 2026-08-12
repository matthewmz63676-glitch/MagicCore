package com.magicstudios.magiccore.modules.combat;
import com.magicstudios.magiccore.modules.teams.TeamService;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
public final class TeamRelationCache{
 public enum Relation{UNKNOWN,SAME,DIFFERENT}
 private static final String NONE="";private final TeamService teams;private final ConcurrentHashMap<UUID,String>teamIds=new ConcurrentHashMap<>();
 public TeamRelationCache(TeamService teams){this.teams=teams;}
 public CompletionStage<Void>refresh(UUID id){return teams.teamOf(id).thenAccept(team->teamIds.put(id,team.map(value->value.id().toString()).orElse(NONE)));}
 public Relation relation(UUID first,UUID second){String a=teamIds.get(first),b=teamIds.get(second);if(a==null||b==null)return Relation.UNKNOWN;return !a.isEmpty()&&a.equals(b)?Relation.SAME:Relation.DIFFERENT;}
 public void invalidate(UUID id){teamIds.remove(id);}
 public void invalidateAllAndRefresh(){var players=new ArrayList<>(teamIds.keySet());teamIds.clear();players.forEach(this::refresh);}
}
