package com.magicstudios.magiccore.bootstrap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.magicstudios.magiccore.delivery.DeliveryMailbox;
import com.magicstudios.magiccore.delivery.MailboxDelivery;
import com.magicstudios.magiccore.modules.essentials.BackService;
import com.magicstudios.magiccore.modules.essentials.TeleportService;
import com.magicstudios.magiccore.modules.kits.KitDefinition;
import com.magicstudios.magiccore.modules.shop.InternalShopService;
import com.magicstudios.magiccore.modules.lifesteal.HeartItemPayload;
import com.magicstudios.magiccore.modules.crates.CrateItemPayload;
import com.magicstudios.magiccore.modules.store.StoreItemPayload;
import com.magicstudios.magiccore.modules.securestorage.PersistentSecureStorageService;
import com.magicstudios.magiccore.text.MiniMessageRenderer;
import com.magicstudios.magiccore.platform.BukkitWorldPositions;
import com.magicstudios.magiccore.platform.SchedulerFacade;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

public final class PhaseTwoPlayerListener implements Listener {
    private static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules();
    private final Plugin plugin;
    private final SchedulerFacade scheduler;
    private final TeleportService teleports;
    private final BackService back;
    private final DeliveryMailbox mailbox;
    private final NamespacedKey deliveryMarker;
    private final NamespacedKey heartItemMarker;

    public PhaseTwoPlayerListener(Plugin plugin, SchedulerFacade scheduler, TeleportService teleports,
                                  BackService back, DeliveryMailbox mailbox) {
        this.plugin = plugin; this.scheduler = scheduler; this.teleports = teleports; this.back = back; this.mailbox = mailbox;
        this.deliveryMarker = new NamespacedKey(plugin, "delivery_id");
        this.heartItemMarker = new NamespacedKey(plugin, "heart_item");
    }

    @EventHandler public void onMove(PlayerMoveEvent event) {
        if (event.getTo() != null) teleports.observeMovement(event.getPlayer(), event.getTo());
    }

    @EventHandler public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        back.recordDeath(player.getUniqueId(), BukkitWorldPositions.from(player.getLocation()),
                "death:" + player.getUniqueId() + ":" + System.nanoTime());
    }

    @EventHandler public void onJoin(PlayerJoinEvent event) {
        deliverPending(event.getPlayer());
    }

    public void deliverPending(Player player) {
        mailbox.pending(player.getUniqueId(), 32).whenComplete((deliveries, failure) -> {
            if (failure != null || deliveries.isEmpty()) return;
            scheduler.executeEntity(player, () -> deliverPending(player, deliveries), () -> { });
        });
    }

    private void deliverPending(Player player, List<MailboxDelivery> deliveries) {
        for (MailboxDelivery delivery : deliveries) {
            if (alreadyPresent(player, delivery.id())) { mark(player, delivery); continue; }
            List<ItemStack> items;
            try { items = decode(delivery); }
            catch (Exception failure) {
                plugin.getLogger().warning("Delivery " + delivery.id() + " has invalid payload: " + failure.getMessage());
                continue;
            }
            items.forEach(item -> item.editPersistentDataContainer(pdc ->
                    pdc.set(deliveryMarker, PersistentDataType.STRING, delivery.id().toString())));
            if (!fits(player, items)) {
                player.sendMessage(Component.text("MagicCore has a pending delivery. Free inventory space and reconnect."));
                continue;
            }
            var leftovers = player.getInventory().addItem(items.toArray(ItemStack[]::new));
            if (!leftovers.isEmpty()) {
                plugin.getLogger().severe("Inventory preflight mismatch for delivery " + delivery.id() + "; keeping journal pending");
                continue;
            }
            mark(player, delivery);
        }
    }

    private void mark(Player player, MailboxDelivery delivery) {
        mailbox.markDelivered(delivery.id(), player.getUniqueId(), "inventory-deliver:" + delivery.id())
                .whenComplete((marked, failure) -> { if (failure != null) plugin.getLogger().warning("Could not finalize delivery " + delivery.id()); });
    }

    private boolean alreadyPresent(Player player, UUID deliveryId) {
        String expected = deliveryId.toString();
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item != null && expected.equals(item.getPersistentDataContainer().get(deliveryMarker, PersistentDataType.STRING))) return true;
        }
        return false;
    }

    private List<ItemStack> decode(MailboxDelivery delivery) throws Exception {
        if (delivery.payloadType().equals("magiccore/kit-v1")) {
            List<KitDefinition.KitItem> entries = JSON.readValue(delivery.payload(), new TypeReference<>() { });
            List<ItemStack> items = new ArrayList<>();
            for (var entry : entries) items.add(item(entry.material(), entry.amount(), entry.itemDataBase64()));
            return items;
        }
        if (delivery.payloadType().equals("magiccore/shop-purchase-v1")) {
            var entry = JSON.readValue(delivery.payload(), InternalShopService.PurchasePayload.class);
            return List.of(item(entry.material(), entry.amount(), entry.itemDataBase64()));
        }
        if (delivery.payloadType().equals("magiccore/auction-item-v1")
                || delivery.payloadType().equals("magiccore/order-item-v1")) {
            List<ItemStack> items = new ArrayList<>();
            try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(delivery.payload()))) {
                int count = input.readInt();
                if (count < 1 || count > 54) throw new IllegalArgumentException("Invalid auction item count");
                for (int i = 0; i < count; i++) {
                    int length = input.readInt();
                    if (length < 1 || length > 1_048_576) throw new IllegalArgumentException("Invalid auction item payload length");
                    items.add(ItemStack.deserializeBytes(input.readNBytes(length)));
                }
                if (input.available() != 0) throw new IllegalArgumentException("Trailing auction item payload data");
            }
            return items;
        }
        if(delivery.payloadType().equals("magiccore/heart-v1")){
            HeartItemPayload payload=JSON.readValue(delivery.payload(),HeartItemPayload.class);
            ItemStack heart=new ItemStack(Material.valueOf(payload.material().toUpperCase()),payload.amount());
            heart.editMeta(meta->{meta.displayName(new MiniMessageRenderer().render(payload.displayName()));
                meta.getPersistentDataContainer().set(heartItemMarker,PersistentDataType.STRING,payload.kind());});
            return List.of(heart);
        }
        if(delivery.payloadType().equals("magiccore/crate-items-v1")){
            List<CrateItemPayload> entries=JSON.readValue(delivery.payload(),new TypeReference<>(){});
            List<ItemStack> items=new ArrayList<>();for(var entry:entries)items.add(item(entry.material(),entry.amount(),entry.itemDataBase64()));return items;
        }
        if(delivery.payloadType().equals("magiccore/store-items-v1")){
            List<StoreItemPayload> entries=JSON.readValue(delivery.payload(),new TypeReference<>(){});List<ItemStack> items=new ArrayList<>();
            for(var entry:entries)items.add(item(entry.material(),entry.amount(),entry.itemDataBase64()));return items;
        }
        if(delivery.payloadType().equals("magiccore/secure-storage-recovery-v1")){
            var payload=JSON.readValue(delivery.payload(),PersistentSecureStorageService.RecoveryPayload.class);List<ItemStack>items=new ArrayList<>();for(var stored:payload.items())items.add(ItemStack.deserializeBytes(Base64.getDecoder().decode(stored.payloadBase64())));return items;
        }
        throw new IllegalArgumentException("Unsupported payload type " + delivery.payloadType());
    }

    private static ItemStack item(String material, int amount, String encoded) {
        ItemStack item = encoded == null || encoded.isBlank()
                ? new ItemStack(Material.valueOf(material.toUpperCase()))
                : ItemStack.deserializeBytes(Base64.getDecoder().decode(encoded));
        item.setAmount(amount);
        return item;
    }

    private static boolean fits(Player player, List<ItemStack> additions) {
        ItemStack[] simulated = player.getInventory().getStorageContents();
        for (ItemStack addition : additions) {
            int remaining = addition.getAmount();
            for (int i = 0; i < simulated.length && remaining > 0; i++) {
                ItemStack present = simulated[i];
                if (present != null && present.isSimilar(addition)) {
                    int placed = Math.min(remaining, present.getMaxStackSize() - present.getAmount());
                    ItemStack grown = present.clone(); grown.setAmount(present.getAmount() + placed); simulated[i] = grown;
                    remaining -= placed;
                }
            }
            int empty = 0; for (ItemStack present : simulated) if (present == null || present.getType().isAir()) empty++;
            int needed = (remaining + addition.getMaxStackSize() - 1) / addition.getMaxStackSize();
            if (needed > empty) return false;
            for (int i = 0; i < simulated.length && remaining > 0; i++) if (simulated[i] == null || simulated[i].getType().isAir()) {
                int placed = Math.min(remaining, addition.getMaxStackSize());
                simulated[i] = addition.clone(); simulated[i].setAmount(placed); remaining -= placed;
            }
        }
        return true;
    }
}
