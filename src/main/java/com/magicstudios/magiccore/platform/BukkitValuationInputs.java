package com.magicstudios.magiccore.platform;

import com.magicstudios.magiccore.modules.worth.ValuationInput;
import org.bukkit.block.Container;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.Damageable;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class BukkitValuationInputs {
    private BukkitValuationInputs() { }
    public static ValuationInput from(ItemStack item){return from(item,Optional.empty());}
    public static ValuationInput from(ItemStack item,Optional<String> customItemId){if(item==null||item.getType().isAir())throw new IllegalArgumentException("Cannot value air");var meta=item.getItemMeta();
        String material=item.getType().getKey().asString(),id=customItemId.filter(value->!value.isBlank()).orElse(material);int enchantments=meta==null?0:meta.getEnchants().values().stream().mapToInt(Integer::intValue).sum();
        int damage=meta instanceof Damageable damageable?damageable.getDamage():0,maximum=item.getType().getMaxDurability();int contained=0;String spawner="";
        if(meta instanceof BlockStateMeta blockMeta){if(blockMeta.getBlockState() instanceof Container container)contained=java.util.Arrays.stream(container.getInventory().getContents()).filter(value->value!=null&&!value.getType().isAir()).mapToInt(ItemStack::getAmount).sum();if(blockMeta.getBlockState() instanceof CreatureSpawner creatureSpawner)spawner=creatureSpawner.getSpawnedType().getKey().asString();}
        Set<String>keys=meta==null?Set.of():meta.getPersistentDataContainer().getKeys().stream().map(Object::toString).collect(Collectors.toUnmodifiableSet());boolean nonstandard=meta!=null&&(meta.hasDisplayName()||meta.hasLore()||!keys.isEmpty());
        return new ValuationInput(id,material,BukkitItemFingerprint.fingerprint(item),item.getAmount(),enchantments,damage,maximum,nonstandard,contained,spawner,keys);}
}
