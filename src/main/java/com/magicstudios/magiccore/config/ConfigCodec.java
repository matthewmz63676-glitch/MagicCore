package com.magicstudios.magiccore.config;

public interface ConfigCodec<T> {
    T decode(byte[] bytes) throws Exception;

    byte[] encode(T value) throws Exception;
}
