package com.magicstudios.magiccore.gui;

import java.util.Locale;

final class GuiText {
    private GuiText() { }

    static String safe(Object value) {
        return String.valueOf(value).replace('<', '‹').replace('>', '›');
    }

    static String label(String enumName) {
        String[] words = enumName.toLowerCase(Locale.ROOT).split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!result.isEmpty()) result.append(' ');
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }
}
