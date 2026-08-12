package com.magicstudios.magiccore.platform;

import com.magicstudios.magiccore.modules.shop.InventoryRemovalPort;
import com.magicstudios.magiccore.modules.shop.ItemFingerprint;
import com.magicstudios.magiccore.modules.shop.BatchInventoryRemovalPort;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/** Inventory mutation is confined to the player's entity scheduler and revalidates every removed stack. */
public final class BukkitInventoryRemovalPort implements InventoryRemovalPort, BatchInventoryRemovalPort {
    private final Plugin plugin;
    private final SchedulerFacade scheduler;
    public BukkitInventoryRemovalPort(Plugin plugin, SchedulerFacade scheduler) { this.plugin = plugin; this.scheduler = scheduler; }

    @Override
    public CompletionStage<RemovalReceipt> removeExact(UUID playerId, ItemFingerprint fingerprint,
                                                        int quantity, String operationKey) {
        CompletableFuture<RemovalReceipt> result = new CompletableFuture<>();
        scheduler.executeGlobal(() -> {
            Player player = plugin.getServer().getPlayer(playerId);
            if (player == null) { result.complete(new RemovalReceipt(false, "PLAYER_OFFLINE", "")); return; }
            scheduler.executeEntity(player, () -> remove(player, fingerprint, quantity, result),
                    () -> result.complete(new RemovalReceipt(false, "ENTITY_RETIRED", "")));
        });
        return result;
    }

    @Override public CompletionStage<BatchRemovalReceipt> removeBatchExact(UUID playerId,List<RemovalLine> lines,String operationKey){if(lines.isEmpty())throw new IllegalArgumentException("batch lines must not be empty");CompletableFuture<BatchRemovalReceipt>result=new CompletableFuture<>();scheduler.executeGlobal(()->{Player player=plugin.getServer().getPlayer(playerId);if(player==null){result.complete(new BatchRemovalReceipt(false,"PLAYER_OFFLINE",""));return;}scheduler.executeEntity(player,()->removeBatch(player,lines,result),()->result.complete(new BatchRemovalReceipt(false,"ENTITY_RETIRED","")));});return result;}

    private static void removeBatch(Player player,List<RemovalLine>lines,CompletableFuture<BatchRemovalReceipt>result){java.util.Map<ItemFingerprint,Integer>required=new java.util.LinkedHashMap<>();for(RemovalLine line:lines)required.merge(line.fingerprint(),line.quantity(),Math::addExact);ItemStack[]contents=player.getInventory().getStorageContents();java.util.Map<ItemFingerprint,Integer>available=new java.util.HashMap<>();for(ItemStack item:contents)if(item!=null&&!item.getType().isAir()){ItemFingerprint fingerprint=BukkitItemFingerprint.fingerprint(item);if(required.containsKey(fingerprint))available.merge(fingerprint,item.getAmount(),Math::addExact);}for(var entry:required.entrySet())if(available.getOrDefault(entry.getKey(),0)<entry.getValue()){result.complete(new BatchRemovalReceipt(false,"STALE_FINGERPRINT_OR_QUANTITY",""));return;}
        ItemStack[]updated=java.util.Arrays.stream(contents).map(item->item==null?null:item.clone()).toArray(ItemStack[]::new);List<ItemStack>removed=new ArrayList<>();for(var entry:required.entrySet()){int remaining=entry.getValue();for(int slot=0;slot<updated.length&&remaining>0;slot++){ItemStack item=updated[slot];if(!matches(item,entry.getKey()))continue;int take=Math.min(remaining,item.getAmount());ItemStack recovery=item.clone();recovery.setAmount(take);removed.add(recovery);if(take==item.getAmount())updated[slot]=null;else{item.setAmount(item.getAmount()-take);}remaining-=take;}}
        player.getInventory().setStorageContents(updated);result.complete(new BatchRemovalReceipt(true,"REMOVED",encode(removed)));}

    private static void remove(Player player, ItemFingerprint fingerprint, int quantity,
                               CompletableFuture<RemovalReceipt> result) {
        ItemStack[] contents = player.getInventory().getStorageContents();
        int available = 0;
        for (ItemStack item : contents) if (matches(item, fingerprint)) available = Math.addExact(available, item.getAmount());
        if (available < quantity) { result.complete(new RemovalReceipt(false, "STALE_FINGERPRINT_OR_QUANTITY", "")); return; }
        int remaining = quantity;
        List<ItemStack> removed = new ArrayList<>();
        for (int slot = 0; slot < contents.length && remaining > 0; slot++) {
            ItemStack item = contents[slot];
            if (!matches(item, fingerprint)) continue;
            int take = Math.min(remaining, item.getAmount());
            ItemStack recovery = item.clone(); recovery.setAmount(take); removed.add(recovery);
            if (take == item.getAmount()) contents[slot] = null;
            else { ItemStack retained = item.clone(); retained.setAmount(item.getAmount() - take); contents[slot] = retained; }
            remaining -= take;
        }
        player.getInventory().setStorageContents(contents);
        result.complete(new RemovalReceipt(true, "REMOVED", encode(removed)));
    }

    private static boolean matches(ItemStack item, ItemFingerprint fingerprint) {
        return item != null && !item.getType().isAir() && BukkitItemFingerprint.fingerprint(item).equals(fingerprint);
    }

    private static String encode(List<ItemStack> items) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                output.writeInt(items.size());
                for (ItemStack item : items) { byte[] encoded = item.serializeAsBytes(); output.writeInt(encoded.length); output.write(encoded); }
            }
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch (java.io.IOException impossible) { throw new IllegalStateException(impossible); }
    }
}
