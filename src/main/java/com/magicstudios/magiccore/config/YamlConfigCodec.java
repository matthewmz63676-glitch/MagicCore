package com.magicstudios.magiccore.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class YamlConfigCodec<T> implements ConfigCodec<T> {
    private final Class<T> type;
    private final ObjectMapper mapper;
    private final Yaml reader;
    private final Yaml writer;

    public YamlConfigCodec(Class<T> type) {
        this.type = type;
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setAllowDuplicateKeys(false);
        loaderOptions.setMaxAliasesForCollections(50);
        this.reader = new Yaml(new SafeConstructor(loaderOptions));
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumperOptions.setPrettyFlow(true);
        dumperOptions.setIndent(2);
        this.writer = new Yaml(dumperOptions);
    }

    @Override
    public T decode(byte[] bytes) {
        Object loaded = reader.load(new String(bytes, StandardCharsets.UTF_8));
        if (!(loaded instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("Configuration root must be a YAML mapping");
        }
        return mapper.convertValue(map, type);
    }

    @Override
    public byte[] encode(T value) {
        @SuppressWarnings("unchecked")
        Map<String, Object> map = mapper.convertValue(value, Map.class);
        return writer.dump(map).getBytes(StandardCharsets.UTF_8);
    }
}
