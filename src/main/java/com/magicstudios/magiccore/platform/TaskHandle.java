package com.magicstudios.magiccore.platform;

public interface TaskHandle {
    void cancel();

    boolean cancelled();
}
