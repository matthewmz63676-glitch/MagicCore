package com.magicstudios.magiccore.platform;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;

public interface SchedulerFacade {
    TaskHandle executeGlobal(Runnable task);

    TaskHandle executeGlobalLater(Duration delay, Runnable task);

    TaskHandle executeRegion(Location location, Runnable task);

    TaskHandle executeEntity(Entity entity, Runnable task, Runnable retired);

    <T> CompletionStage<T> supplyAsync(Callable<T> task);

    void close();
}
