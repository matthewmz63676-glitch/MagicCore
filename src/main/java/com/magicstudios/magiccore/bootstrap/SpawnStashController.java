package com.magicstudios.magiccore.bootstrap;

import com.magicstudios.magiccore.api.DomainEventBus;
import com.magicstudios.magiccore.capabilities.CapabilityService;
import com.magicstudios.magiccore.config.model.SpawnStashFile;
import com.magicstudios.magiccore.integrations.vulcan.VulcanFlagObserved;
import com.magicstudios.magiccore.integrations.vulcan.VulcanService;
import com.magicstudios.magiccore.modules.essentials.WorldPosition;
import com.magicstudios.magiccore.modules.spawnstash.*;
import com.magicstudios.magiccore.platform.SchedulerFacade;
import com.magicstudios.magiccore.platform.TaskHandle;
import com.magicstudios.magiccore.protection.ProtectionAction;
import com.magicstudios.magiccore.protection.ProtectionService;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/** Folia-safe world adapter for the persistent, observe-only SpawnStash case service. */
public final class SpawnStashController implements Listener, AutoCloseable {
    private static final String OWNER = "spawnstash-controller";
    private final Plugin plugin; private final SchedulerFacade scheduler; private final SpawnStashService service;
    private final ProtectionService protection; private final CapabilityService capabilities; private final VulcanService vulcan;
    private final DomainEventBus events; private final SpawnStashFile config; private final Clock clock;
    private final Map<UUID, SpawnStashCase> cases = new ConcurrentHashMap<>();
    private final Map<StashPosition, UUID> protectedBlocks = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> targetCases = new ConcurrentHashMap<>();
    private final Map<String, Instant> signalCooldowns = new ConcurrentHashMap<>();
    private final Map<String, ArrayDeque<Double>> pathDistances = new ConcurrentHashMap<>();
    private volatile TaskHandle expiryTask;

    public SpawnStashController(Plugin plugin, SchedulerFacade scheduler, SpawnStashService service,
                                ProtectionService protection, CapabilityService capabilities, VulcanService vulcan,
                                DomainEventBus events, SpawnStashFile config, Clock clock) {
        this.plugin=plugin;this.scheduler=scheduler;this.service=service;this.protection=protection;this.capabilities=capabilities;
        this.vulcan=vulcan;this.events=events;this.config=config;this.clock=clock;
        events.subscribe(OWNER,VulcanFlagObserved.class,this::onVulcanFlag);
        events.subscribe(OWNER,SpawnStashSignalRecorded.class,this::alertStaff);
        recover(); scheduleExpirySweep();
    }

    public CompletionStage<List<StashPosition>> preview(Player actor, Player target) {
        CompletableFuture<List<StashPosition>> result=new CompletableFuture<>();
        scheduler.executeEntity(target,()->{List<Location> locations=candidateLocations(randomOrigin(target.getLocation()),config.placement().blockCount());
            scheduler.executeEntity(actor,()->{locations.forEach(location->actor.spawnParticle(Particle.END_ROD,location.clone().add(.5,.5,.5),2,0,0,0,0));result.complete(locations.stream().map(SpawnStashController::position).toList());},()->result.completeExceptionally(new IllegalStateException("Staff viewer is no longer available")));
        },()->result.completeExceptionally(new IllegalStateException("Target is no longer available")));
        return result;
    }

    public CompletionStage<SpawnStashCase> create(Player actor, Player target) {
        UUID caseId=UUID.randomUUID();
        return service.activeForTarget(target.getUniqueId()).thenCompose(existing->{if(!existing.isEmpty())throw new IllegalStateException("Target already has an open SpawnStash case");
            CompletableFuture<Location> originFuture=new CompletableFuture<>();scheduler.executeEntity(target,()->originFuture.complete(randomOrigin(target.getLocation())),()->originFuture.completeExceptionally(new IllegalStateException("Target retired before case creation")));
            return originFuture;}).thenCompose(origin->collectBlocks(actor.getUniqueId(),origin)).thenCompose(blocks->{if(blocks.size()<config.placement().blockCount())throw new IllegalStateException("Could not find enough safe protected positions for SpawnStash");
            StashPosition origin=blocks.getFirst().position();return service.prepare(caseId,target.getUniqueId(),actor.getUniqueId(),actor.getName(),origin,blocks,
            clock.instant().plusSeconds(config.expirySeconds()),operation("prepare",caseId));}).thenCompose(this::placePrepared);
    }

    public CompletionStage<SpawnStashCase> addNote(UUID caseId, Player actor, String note) {
        return service.addNote(caseId,actor.getUniqueId(),actor.getName(),note,operation("note",caseId)).thenApply(this::cache);
    }

    public CompletionStage<SpawnStashCase> cleanup(UUID caseId, SpawnStashCase.Outcome outcome, Player actor, String note) {
        return service.beginCleanup(caseId,outcome,actor.getUniqueId(),actor.getName(),note,operation("cleanup",caseId))
                .thenApply(this::cache).thenCompose(this::restoreRemaining);
    }

    public Optional<SpawnStashCase> cached(UUID caseId){return Optional.ofNullable(cases.get(caseId));}
    public CompletionStage<Optional<SpawnStashCase>> find(UUID caseId){return service.find(caseId);}
    public List<SpawnStashCase> openCases(){return cases.values().stream().sorted(Comparator.comparing(SpawnStashCase::createdAt)).toList();}

    private CompletionStage<List<SpawnStashBlock>> collectBlocks(UUID actorId,Location origin){CompletableFuture<List<SpawnStashBlock>> result=new CompletableFuture<>();
        List<Location> candidates=candidateLocations(origin,config.placement().maximumCandidateAttempts());collectCandidate(actorId,candidates,0,new ArrayList<>(),result);return result;}
    private void collectCandidate(UUID actorId,List<Location> candidates,int index,List<SpawnStashBlock> accepted,CompletableFuture<List<SpawnStashBlock>> result){
        if(accepted.size()>=config.placement().blockCount()||index>=candidates.size()){result.complete(List.copyOf(accepted));return;}Location location=candidates.get(index);
        scheduler.executeRegion(location,()->{Block block=location.getBlock();StashPosition pos=position(location);if(protectedBlocks.containsKey(pos)||!replaceable(block)){collectCandidate(actorId,candidates,index+1,accepted,result);return;}
            String original=block.getBlockData().getAsString(true);WorldPosition worldPosition=new WorldPosition(location.getWorld().getUID(),location.getWorld().getName(),location.getBlockX(),location.getBlockY(),location.getBlockZ(),0,0);
            protection.check(actorId,worldPosition,ProtectionAction.BLOCK_PLACE).whenComplete((decision,failure)->scheduler.executeRegion(location,()->{if(failure==null&&decision.allowed()&&block.getBlockData().getAsString(true).equals(original))accepted.add(decoy(pos,original));collectCandidate(actorId,candidates,index+1,accepted,result);}));});}

    private CompletionStage<SpawnStashCase> placeAll(SpawnStashCase stashCase,int index){if(index>=stashCase.blocks().size())return service.activate(stashCase.id(),operation("activate",stashCase.id())).thenApply(this::cache);
        SpawnStashBlock planned=stashCase.blocks().get(index);CompletableFuture<Void> placed=new CompletableFuture<>();Location location=location(planned.position());if(location==null){placed.completeExceptionally(new IllegalStateException("SpawnStash world unavailable"));}
        else scheduler.executeRegion(location,()->{try{Block block=location.getBlock();block.setBlockData(Bukkit.createBlockData(planned.decoyBlockData()),false);populateAppearance(block,planned);placed.complete(null);}catch(Throwable failure){placed.completeExceptionally(failure);}});
        return placed.thenCompose(ignored->service.markPlaced(stashCase.id(),planned.position(),operation("placed-"+index,stashCase.id()))).thenApply(this::cache).thenCompose(updated->placeAll(updated,index+1));}

    private CompletionStage<SpawnStashCase> placePrepared(SpawnStashCase prepared){cache(prepared);CompletableFuture<SpawnStashCase> result=new CompletableFuture<>();placeAll(prepared,0).whenComplete((active,failure)->{if(failure==null){result.complete(active);return;}
        service.beginCleanup(prepared.id(),SpawnStashCase.Outcome.CANCELLED,prepared.actorId(),prepared.actorName(),"Placement failed: "+rootMessage(failure),operation("placement-failed",prepared.id())).thenApply(this::cache).thenCompose(this::restoreRemaining).whenComplete((ignored,cleanupFailure)->{if(cleanupFailure!=null)failure.addSuppressed(cleanupFailure);result.completeExceptionally(failure);});});return result;}

    private CompletionStage<SpawnStashCase> restoreRemaining(SpawnStashCase stashCase){Optional<SpawnStashBlock> remaining=stashCase.blocks().stream().filter(block->block.state()!=SpawnStashBlock.State.RESTORED).findFirst();
        if(remaining.isEmpty())return service.completeCleanup(stashCase.id(),operation("complete",stashCase.id())).thenApply(this::cache);
        SpawnStashBlock block=remaining.orElseThrow();Location location=location(block.position());if(location==null)return CompletableFuture.failedFuture(new IllegalStateException("SpawnStash world unavailable during restoration"));CompletableFuture<Void> restored=new CompletableFuture<>();
        scheduler.executeRegion(location,()->{try{location.getBlock().setBlockData(Bukkit.createBlockData(block.originalBlockData()),false);restored.complete(null);}catch(Throwable failure){restored.completeExceptionally(failure);}});
        return restored.thenCompose(ignored->service.markRestored(stashCase.id(),block.position(),operation("restored",stashCase.id()))).thenApply(this::cache).thenCompose(this::restoreRemaining);}

    private void recover(){service.openCases().whenComplete((open,failure)->{if(failure!=null){plugin.getLogger().severe("SpawnStash recovery failed: "+failure.getMessage());return;}open.forEach(stashCase->{cache(stashCase);if(stashCase.status()==SpawnStashCase.Status.PREPARED){service.beginCleanup(stashCase.id(),SpawnStashCase.Outcome.CANCELLED,stashCase.actorId(),stashCase.actorName(),"Recovered incomplete placement after restart",operation("recover",stashCase.id())).thenApply(this::cache).thenCompose(this::restoreRemaining);}
            else if(stashCase.status()==SpawnStashCase.Status.CLEANING)restoreRemaining(stashCase);else if(!stashCase.expiresAt().isAfter(clock.instant()))service.beginCleanup(stashCase.id(),SpawnStashCase.Outcome.EXPIRED,stashCase.actorId(),stashCase.actorName(),"Expired during restart",operation("expired",stashCase.id())).thenApply(this::cache).thenCompose(this::restoreRemaining);});});}

    private void scheduleExpirySweep(){expiryTask=scheduler.executeGlobalLater(Duration.ofSeconds(30),()->{Instant now=clock.instant();for(SpawnStashCase stashCase:cases.values())if(stashCase.status()==SpawnStashCase.Status.ACTIVE&&!stashCase.expiresAt().isAfter(now))
            service.beginCleanup(stashCase.id(),SpawnStashCase.Outcome.EXPIRED,stashCase.actorId(),stashCase.actorName(),"Automatic expiry",operation("expired",stashCase.id())).thenApply(this::cache).thenCompose(this::restoreRemaining);scheduleExpirySweep();});}

    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void onBreak(BlockBreakEvent event){UUID caseId=protectedBlocks.get(position(event.getBlock().getLocation()));if(caseId==null)return;event.setCancelled(true);recordPlayerSignal(caseId,event.getPlayer(),SpawnStashSignal.Type.BREAK,event.getBlock().getLocation(),Map.of("action","block-break"));}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void onPlace(BlockPlaceEvent event){if(protectedBlocks.containsKey(position(event.getBlock().getLocation())))event.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void onInteract(PlayerInteractEvent event){if(event.getClickedBlock()==null)return;UUID caseId=protectedBlocks.get(position(event.getClickedBlock().getLocation()));if(caseId==null)return;event.setCancelled(true);recordPlayerSignal(caseId,event.getPlayer(),SpawnStashSignal.Type.INTERACT,event.getClickedBlock().getLocation(),Map.of("action",event.getAction().name()));}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void onBlockExplode(BlockExplodeEvent event){event.blockList().removeIf(block->protectedBlocks.containsKey(position(block.getLocation())));}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void onEntityExplode(EntityExplodeEvent event){event.blockList().removeIf(block->protectedBlocks.containsKey(position(block.getLocation())));}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void onPistonExtend(BlockPistonExtendEvent event){if(event.getBlocks().stream().anyMatch(block->protectedBlocks.containsKey(position(block.getLocation()))||protectedBlocks.containsKey(position(block.getRelative(event.getDirection()).getLocation()))))event.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void onPistonRetract(BlockPistonRetractEvent event){if(event.getBlocks().stream().anyMatch(block->protectedBlocks.containsKey(position(block.getLocation()))))event.setCancelled(true);}

    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void onMove(PlayerMoveEvent event){if(event.getTo()==null||sameBlock(event.getFrom(),event.getTo()))return;Set<UUID> ids=targetCases.get(event.getPlayer().getUniqueId());if(ids==null)return;for(UUID id:List.copyOf(ids)){SpawnStashCase stashCase=cases.get(id);if(stashCase==null||stashCase.status()!=SpawnStashCase.Status.ACTIVE)continue;evaluateMovement(stashCase,event.getPlayer(),event.getTo());}}
    private void evaluateMovement(SpawnStashCase stashCase,Player player,Location current){SpawnStashBlock nearest=stashCase.blocks().stream().filter(block->block.state()==SpawnStashBlock.State.PLACED).min(Comparator.comparingDouble(block->distanceSquared(current,block.position()))).orElse(null);if(nearest==null)return;double distance=Math.sqrt(distanceSquared(current,nearest.position()));
        if(distance<=config.signals().approachDistance())recordPlayerSignal(stashCase.id(),player,SpawnStashSignal.Type.APPROACH,current,Map.of("distance",String.format(Locale.ROOT,"%.2f",distance)));
        if(distance<=config.signals().revealDistance()&&visible(player,nearest.position(),distance))recordPlayerSignal(stashCase.id(),player,SpawnStashSignal.Type.REVEAL,current,Map.of("distance",String.format(Locale.ROOT,"%.2f",distance)));
        String pathKey=stashCase.id()+":"+player.getUniqueId();ArrayDeque<Double> samples=pathDistances.computeIfAbsent(pathKey,ignored->new ArrayDeque<>());samples.addLast(distance);while(samples.size()>config.signals().suspiciousPathSamples())samples.removeFirst();
        if(samples.size()==config.signals().suspiciousPathSamples()&&samples.getFirst()-samples.getLast()>=config.signals().suspiciousPathImprovement()){recordPlayerSignal(stashCase.id(),player,SpawnStashSignal.Type.SUSPICIOUS_PATH,current,Map.of("improvement",String.format(Locale.ROOT,"%.2f",samples.getFirst()-samples.getLast()),"samples",Integer.toString(samples.size())));samples.clear();}}

    private void onVulcanFlag(VulcanFlagObserved event){Set<UUID> ids=targetCases.get(event.playerId());if(ids==null)return;for(UUID id:List.copyOf(ids)){SpawnStashCase stashCase=cases.get(id);if(stashCase!=null&&stashCase.status()==SpawnStashCase.Status.ACTIVE)recordSignal(id,event.playerId(),SpawnStashSignal.Type.VULCAN_FLAG,stashCase.origin(),Map.of("check",safe(event.flag().check()),"violationLevel",Double.toString(event.flag().violationLevel()),"detail",safe(event.flag().detail())));}}
    private void recordPlayerSignal(UUID caseId,Player player,SpawnStashSignal.Type type,Location location,Map<String,String> details){SpawnStashCase stashCase=cases.get(caseId);if(stashCase==null||!stashCase.targetId().equals(player.getUniqueId()))return;recordSignal(caseId,player.getUniqueId(),type,position(location),details);}
    private void recordSignal(UUID caseId,UUID playerId,SpawnStashSignal.Type type,StashPosition position,Map<String,String> details){String cooldownKey=caseId+":"+type;Instant now=clock.instant(),eligible=signalCooldowns.get(cooldownKey);if(eligible!=null&&eligible.isAfter(now))return;signalCooldowns.put(cooldownKey,now.plusSeconds(config.signals().perSignalCooldownSeconds()));
        Map<String,String> enriched=new LinkedHashMap<>(details);var flags=vulcan.recentFlags(playerId,now.minusSeconds(300));enriched.put("recentVulcanFlags",Integer.toString(flags.size()));if(!flags.isEmpty())enriched.put("recentVulcanChecks",flags.stream().map(flag->flag.check()).distinct().limit(8).toList().toString());
        service.recordSignal(caseId,type,playerId,position,enriched,operation("signal-"+type,caseId)).thenApply(this::cache).exceptionally(failure->{plugin.getLogger().warning("SpawnStash signal persistence failed: "+failure.getMessage());return null;});}

    private void alertStaff(SpawnStashSignalRecorded event){if(!config.alerts().enabled())return;for(Player player:plugin.getServer().getOnlinePlayers())capabilities.has(player.getUniqueId(),config.alerts().capability()).thenAccept(allowed->{if(allowed)scheduler.executeEntity(player,()->player.sendMessage(Component.text("[SpawnStash] "+event.signal().type()+" in case "+event.caseId()+" for "+event.targetId())),()->{});});}

    private SpawnStashCase cache(SpawnStashCase stashCase){SpawnStashCase previous=cases.put(stashCase.id(),stashCase);if(previous!=null)unindex(previous);if(stashCase.status()==SpawnStashCase.Status.CLOSED){cases.remove(stashCase.id());return stashCase;}targetCases.computeIfAbsent(stashCase.targetId(),ignored->ConcurrentHashMap.newKeySet()).add(stashCase.id());stashCase.blocks().stream().filter(block->block.state()!=SpawnStashBlock.State.RESTORED).forEach(block->protectedBlocks.put(block.position(),stashCase.id()));return stashCase;}
    private void unindex(SpawnStashCase stashCase){Set<UUID> ids=targetCases.get(stashCase.targetId());if(ids!=null){ids.remove(stashCase.id());if(ids.isEmpty())targetCases.remove(stashCase.targetId());}stashCase.blocks().forEach(block->protectedBlocks.remove(block.position(),stashCase.id()));}
    private SpawnStashBlock decoy(StashPosition position,String original){SpawnStashFile.DecoyBlock definition=weighted(config.decoyBlocks(),SpawnStashFile.DecoyBlock::weight);List<SpawnStashBlock.LootAppearance> loot=new ArrayList<>();for(var item:definition.lootAppearance()){int amount=ThreadLocalRandom.current().nextInt(item.minimumAmount(),item.maximumAmount()+1);loot.add(new SpawnStashBlock.LootAppearance(item.material(),amount));}return new SpawnStashBlock(position,original,definition.blockData(),loot,SpawnStashBlock.State.PLANNED);}
    private void populateAppearance(Block block,SpawnStashBlock planned){if(!(block.getState() instanceof Container container))return;container.getInventory().clear();for(var appearance:planned.lootAppearance()){Material material=Material.matchMaterial(appearance.material());if(material!=null)container.getInventory().addItem(new ItemStack(material,appearance.amount()));}container.update(true,false);}
    private List<Location> candidateLocations(Location origin,int count){List<Location> result=new ArrayList<>(count);int radius=config.placement().clusterRadius();for(int i=0;i<count;i++){int x=ThreadLocalRandom.current().nextInt(-radius,radius+1),y=ThreadLocalRandom.current().nextInt(-radius,radius+1),z=ThreadLocalRandom.current().nextInt(-radius,radius+1);Location candidate=origin.clone().add(x,y,z);candidate.setY(Math.max(origin.getWorld().getMinHeight()+1,Math.min(origin.getWorld().getMaxHeight()-2,candidate.getY())));result.add(candidate.getBlock().getLocation());}Collections.shuffle(result);return result;}
    private Location randomOrigin(Location target){double angle=ThreadLocalRandom.current().nextDouble(Math.PI*2),radius=ThreadLocalRandom.current().nextDouble(config.placement().minimumHorizontalRadius(),config.placement().maximumHorizontalRadius()+1);int y=ThreadLocalRandom.current().nextInt(config.placement().minimumVerticalOffset(),config.placement().maximumVerticalOffset()+1);return target.clone().add(Math.cos(angle)*radius,y,Math.sin(angle)*radius).getBlock().getLocation();}
    private boolean visible(Player player,StashPosition position,double distance){Location eye=player.getEyeLocation(),target=location(position);if(target==null)return false;Vector direction=target.clone().add(.5,.5,.5).toVector().subtract(eye.toVector());if(direction.lengthSquared()==0)return true;double dot=eye.getDirection().normalize().dot(direction.clone().normalize());if(dot<config.signals().revealDotProduct())return false;RayTraceResult hit=player.getWorld().rayTraceBlocks(eye,direction.normalize(),distance+.75,FluidCollisionMode.NEVER,true);return hit!=null&&hit.getHitBlock()!=null&&position(hit.getHitBlock().getLocation()).equals(position);}
    private static boolean replaceable(Block block){return block.getType().isSolid()&&!(block.getState() instanceof TileState)&&!Set.of(Material.BEDROCK,Material.BARRIER,Material.END_PORTAL_FRAME,Material.REINFORCED_DEEPSLATE).contains(block.getType());}
    private static double distanceSquared(Location location,StashPosition position){if(location.getWorld()==null||!location.getWorld().getUID().equals(position.worldId()))return Double.POSITIVE_INFINITY;double dx=location.getX()-(position.x()+.5),dy=location.getY()-(position.y()+.5),dz=location.getZ()-(position.z()+.5);return dx*dx+dy*dy+dz*dz;}
    private static boolean sameBlock(Location a,Location b){return a.getWorld()==b.getWorld()&&a.getBlockX()==b.getBlockX()&&a.getBlockY()==b.getBlockY()&&a.getBlockZ()==b.getBlockZ();}
    private static StashPosition position(Location location){return new StashPosition(Objects.requireNonNull(location.getWorld()).getUID(),location.getBlockX(),location.getBlockY(),location.getBlockZ());}
    private Location location(StashPosition position){World world=plugin.getServer().getWorld(position.worldId());return world==null?null:new Location(world,position.x(),position.y(),position.z());}
    private static <T> T weighted(List<T> values,java.util.function.ToIntFunction<T> weight){int total=values.stream().mapToInt(weight).sum(),choice=ThreadLocalRandom.current().nextInt(total);for(T value:values){choice-=weight.applyAsInt(value);if(choice<0)return value;}return values.getLast();}
    private static String operation(String prefix,UUID caseId){return "spawnstash:"+prefix+":"+caseId+":"+UUID.randomUUID();}
    private static String safe(String value){return value==null?"":value;}
    private static String rootMessage(Throwable failure){Throwable current=failure;while(current.getCause()!=null)current=current.getCause();return current.getMessage()==null?current.getClass().getSimpleName():current.getMessage();}

    @Override public void close(){TaskHandle task=expiryTask;if(task!=null)task.cancel();events.unsubscribeOwner(OWNER);HandlerList.unregisterAll(this);cases.clear();protectedBlocks.clear();targetCases.clear();signalCooldowns.clear();pathDistances.clear();}
}
