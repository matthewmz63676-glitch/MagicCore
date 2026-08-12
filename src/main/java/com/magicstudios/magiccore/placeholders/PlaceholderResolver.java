package com.magicstudios.magiccore.placeholders;

@FunctionalInterface
public interface PlaceholderResolver {
    String resolve(PlaceholderContext context) throws Exception;
}
