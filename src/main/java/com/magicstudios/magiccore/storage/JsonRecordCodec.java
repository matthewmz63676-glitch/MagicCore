package com.magicstudios.magiccore.storage;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class JsonRecordCodec<T> {
    private final ObjectMapper mapper;
    private final Class<T> type;

    public JsonRecordCodec(Class<T> type) {
        this.type = type;
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    }

    public byte[] encode(T value) throws Exception {
        return mapper.writeValueAsBytes(value);
    }

    public T decode(byte[] bytes) throws Exception {
        return mapper.readValue(bytes, type);
    }
}
