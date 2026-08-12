package com.magicstudios.magiccore.gui;

import java.util.Map;

public record GuiPage(String title,int rows,int page,int pageCount,Map<Integer,GuiElement>elements){public GuiPage{elements=Map.copyOf(elements);if(rows<1||rows>6||page<0||pageCount<1||page>=pageCount)throw new IllegalArgumentException("Invalid GUI page");}}
