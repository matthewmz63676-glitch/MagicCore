package com.magicstudios.magiccore.modules.worth;

import com.magicstudios.magiccore.config.model.ItemWorthFile;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class ConfiguredItemValuationService implements ItemValuationService {
    private final ItemWorthFile config;
    private final Map<String,ItemWorthFile.WorthEntry> entries;
    private final Set<String> protectedIds,protectedKeys;
    public ConfiguredItemValuationService(ItemWorthFile config){this.config=config;entries=config.entries().stream().collect(Collectors.toUnmodifiableMap(entry->entry.itemId().toLowerCase(Locale.ROOT),Function.identity()));
        protectedIds=config.policies().protectedItemIds().stream().map(value->value.toLowerCase(Locale.ROOT)).collect(Collectors.toUnmodifiableSet());
        protectedKeys=config.policies().protectedMetadataKeys().stream().map(value->value.toLowerCase(Locale.ROOT)).collect(Collectors.toUnmodifiableSet());}
    @Override public String currency(){return config.currency();}
    @Override public ItemValuation value(ValuationInput input){String id=input.itemId().toLowerCase(Locale.ROOT);if(protectedIds.contains(id)||input.metadataKeys().stream().map(value->value.toLowerCase(Locale.ROOT)).anyMatch(protectedKeys::contains))return rejected(input,"PROTECTED_ITEM");ItemWorthFile.WorthEntry entry=entries.get(id);if(entry==null)return rejected(input,"UNVALUED_ITEM");
        String container=config.policies().containers().toUpperCase(Locale.ROOT);if(container.equals("REJECT_ALL")||container.equals("REJECT_NONEMPTY")&&input.containedItemCount()>0)return rejected(input,"CONTAINER_POLICY");
        if(config.policies().spawners().equalsIgnoreCase("REJECT_ALL")&&(!input.spawnerEntityId().isBlank()||id.equals("minecraft:spawner")))return rejected(input,"SPAWNER_POLICY");
        if(config.policies().metadata().equalsIgnoreCase("REJECT_NONSTANDARD")&&input.nonstandardMetadata())return rejected(input,"METADATA_POLICY");
        if(config.policies().enchantments().equalsIgnoreCase("REJECT")&&input.enchantmentLevels()>0)return rejected(input,"ENCHANTMENT_POLICY");
        if(config.policies().durability().equalsIgnoreCase("REJECT_DAMAGED")&&input.damage()>0)return rejected(input,"DURABILITY_POLICY");
        long basisPoints=10_000;if(config.policies().durability().equalsIgnoreCase("LINEAR")&&input.maximumDamage()>0){long remaining=Math.max(0,input.maximumDamage()-input.damage());basisPoints=Math.max(config.policies().minimumDurabilityBasisPoints(),remaining*10_000L/input.maximumDamage());}
        if(config.policies().enchantments().equalsIgnoreCase("ADDITIVE"))basisPoints=Math.addExact(basisPoints,Math.multiplyExact(config.policies().enchantmentBasisPointsPerLevel(),input.enchantmentLevels()));
        long unit=Math.multiplyExact(entry.unitWorthMinor(),basisPoints)/10_000L;long total=Math.multiplyExact(unit,input.amount());return new ItemValuation(true,"VALUED",entry.id(),entry.itemId(),entry.category(),input.fingerprint(),input.amount(),unit,total);}
    @Override public Optional<ItemValuation> unitValue(String itemId){var entry=entries.get(itemId.toLowerCase(Locale.ROOT));return entry==null?Optional.empty():Optional.of(new ItemValuation(true,"VALUED",entry.id(),entry.itemId(),entry.category(),null,1,entry.unitWorthMinor(),entry.unitWorthMinor()));}
    @Override public List<String> categories(){return entries.values().stream().map(ItemWorthFile.WorthEntry::category).distinct().sorted().toList();}
    @Override public String render(ItemValuation value){if(!value.sellable())return config.presentation().unavailableText();return config.presentation().worthTemplate().replace("{amount}",Long.toString(value.totalWorthMinor())).replace("{currency}",config.currency()).replace("{quantity}",Integer.toString(value.quantity()));}
    private static ItemValuation rejected(ValuationInput input,String code){return new ItemValuation(false,code,"",input.itemId(),"",input.fingerprint(),input.amount(),0,0);}
}
