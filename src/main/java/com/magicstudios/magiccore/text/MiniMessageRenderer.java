package com.magicstudios.magiccore.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.ParsingException;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MiniMessageRenderer {
    private static final Pattern NAMED_TAG = Pattern.compile("<(/?)([A-Za-z][A-Za-z0-9_-]*)(?::[^>]*)?>");
    private final MiniMessage miniMessage;

    public MiniMessageRenderer() {
        this.miniMessage = MiniMessage.builder().strict(true).build();
    }

    public Component render(String template, TagResolver... resolvers) {
        return miniMessage.deserialize(validate(template), resolvers);
    }

    public String validate(String template) {
        Objects.requireNonNull(template, "template");
        if (template.matches("(?s).*&[0-9A-FK-ORXa-fk-orx].*")) {
            throw new IllegalArgumentException("Legacy color codes are not allowed; convert the template to MiniMessage tags");
        }
        rejectOrphanClosingTags(template);
        try {
            miniMessage.deserialize(template);
            return template;
        } catch (ParsingException failure) {
            throw new IllegalArgumentException("Invalid MiniMessage template: " + failure.getMessage(), failure);
        }
    }

    private static void rejectOrphanClosingTags(String template) {
        Map<String, Integer> openTags = new HashMap<>();
        Matcher matcher = NAMED_TAG.matcher(template);
        while (matcher.find()) {
            String tag = matcher.group(2).toLowerCase(java.util.Locale.ROOT);
            if (matcher.group(1).isEmpty()) {
                openTags.merge(tag, 1, Integer::sum);
            } else {
                int count = openTags.getOrDefault(tag, 0);
                if (count == 0) {
                    throw new IllegalArgumentException("Closing tag </" + tag + "> has no matching opening tag");
                }
                openTags.put(tag, count - 1);
            }
        }
    }

    public Map<String, String> validateCatalog(Map<String, String> templates) {
        Map<String, String> valid = new LinkedHashMap<>();
        templates.forEach((path, template) -> {
            try {
                valid.put(path, validate(template));
            } catch (IllegalArgumentException failure) {
                throw new IllegalArgumentException("messages.yml key '" + path + "': " + failure.getMessage(), failure);
            }
        });
        return Map.copyOf(valid);
    }
}
