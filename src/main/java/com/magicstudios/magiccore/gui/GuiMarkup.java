package com.magicstudios.magiccore.gui;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Completes style-guide shorthand into strict MiniMessage before parsing. */
public final class GuiMarkup {
    private static final Pattern TAG=Pattern.compile("<(/?)([A-Za-z][A-Za-z0-9_-]*|#[A-Fa-f0-9]{6})(?::[^>]*)?>");
    private static final Set<String>SELF_CLOSING=Set.of("br","newline","reset");
    private GuiMarkup(){}
    public static String complete(String template){Deque<String>open=new ArrayDeque<>();Matcher matcher=TAG.matcher(template);while(matcher.find()){String name=matcher.group(2).toLowerCase(java.util.Locale.ROOT);if(SELF_CLOSING.contains(name))continue;if(matcher.group(1).isEmpty())open.push(name);else if(!open.isEmpty()&&open.peek().equals(name))open.pop();else open.removeFirstOccurrence(name);}StringBuilder result=new StringBuilder(template);while(!open.isEmpty())result.append("</").append(open.pop()).append('>');return result.toString();}
}
