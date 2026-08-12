package com.magicstudios.magiccore.bootstrap;

import com.magicstudios.magiccore.modules.playerwarps.PlayerWarpService;
import com.magicstudios.magiccore.modules.securestorage.SecureStorageService;
import com.magicstudios.magiccore.platform.SchedulerFacade;
import com.magicstudios.magiccore.platform.TaskHandle;
import org.bukkit.plugin.Plugin;
import java.time.Duration;
import java.util.concurrent.CopyOnWriteArrayList;

public final class EventMaintenanceController implements AutoCloseable {
    private final Plugin plugin;private final SchedulerFacade scheduler;private final PlayerWarpService warps;private final SecureStorageService storage;private final Duration sponsorshipInterval;private final Duration storageInterval;private final java.util.List<TaskHandle>tasks=new CopyOnWriteArrayList<>();private volatile boolean closed;
    public EventMaintenanceController(Plugin plugin,SchedulerFacade scheduler,PlayerWarpService warps,SecureStorageService storage,Duration sponsorshipInterval,Duration storageInterval){this.plugin=plugin;this.scheduler=scheduler;this.warps=warps;this.storage=storage;this.sponsorshipInterval=sponsorshipInterval;this.storageInterval=storageInterval;}
    public void start(){scheduleSponsorships(sponsorshipInterval);scheduleStorage(storageInterval);}
    private void scheduleSponsorships(Duration delay){tasks.add(scheduler.executeGlobalLater(delay,()->{if(closed)return;warps.expireSponsorships().whenComplete((count,failure)->{if(failure!=null)plugin.getLogger().warning("PlayerWarp sponsorship maintenance failed: "+root(failure));});scheduleSponsorships(sponsorshipInterval);}));}
    private void scheduleStorage(Duration delay){tasks.add(scheduler.executeGlobalLater(delay,()->{if(closed)return;storage.recoverExpired().whenComplete((count,failure)->{if(failure!=null)plugin.getLogger().warning("Secure-storage lease maintenance failed: "+root(failure));});scheduleStorage(storageInterval);}));}
    @Override public void close(){closed=true;tasks.forEach(TaskHandle::cancel);tasks.clear();}
    private static String root(Throwable failure){Throwable value=failure;while(value.getCause()!=null)value=value.getCause();return value.getMessage();}
}
