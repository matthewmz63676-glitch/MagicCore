package com.magicstudios.magiccore.config.model;
import java.util.List;
public record OrdersFile(int configVersion,String currency,long minimumUnitPriceMinor,long maximumUnitPriceMinor,
                         long minimumDurationSeconds,long maximumDurationSeconds,List<String> categories){
    public OrdersFile{categories=List.copyOf(categories);}
}
