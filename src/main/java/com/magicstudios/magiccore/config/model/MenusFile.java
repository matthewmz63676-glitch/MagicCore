package com.magicstudios.magiccore.config.model;

import java.util.List;
import java.util.Map;

public record MenusFile(int configVersion,Theme theme,Map<String,Layout>layouts,List<RootEntry>rootEntries){public MenusFile{layouts=Map.copyOf(layouts);rootEntries=List.copyOf(rootEntries);}
 public record Theme(String fillMaterial,String accentMaterial,String positiveMaterial,String negativeMaterial,
                     String previousMaterial,String closeMaterial,String nextMaterial){}
 public record Layout(String title,int rows){}
 public record RootEntry(String id,int slot,String material,String name,List<String>lore,String menuId,String requiredCapability){public RootEntry{lore=List.copyOf(lore);}}
}
