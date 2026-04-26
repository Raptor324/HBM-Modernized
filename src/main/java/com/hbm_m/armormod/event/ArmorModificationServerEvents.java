package com.hbm_m.armormod.event;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

// Этот класс отвечает за корректировку здоровья игрока при смене брони с модификациями.
import com.hbm_m.lib.RefStrings;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
//? if forge {
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RefStrings.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ArmorModificationServerEvents {

    private static final Set<UUID> playersToUpdate = new HashSet<>();

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getSlot().getType() == EquipmentSlot.Type.ARMOR && event.getEntity() instanceof Player) {
            playersToUpdate.add(event.getEntity().getUUID());
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            if (!playersToUpdate.isEmpty()) {
                for (UUID playerUUID : playersToUpdate.toArray(new UUID[0])) {
                    ServerPlayer player = event.getServer().getPlayerList().getPlayer(playerUUID);
                    if (player != null && player.getHealth() > player.getMaxHealth()) {
                        player.setHealth(player.getMaxHealth());
                    }
                }
                playersToUpdate.clear();
            }
        }
    }
}
//?}

//? if fabric {
/*import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;*///?}

//? if fabric {
/*public class ArmorModificationServerEvents {

    private static final Set<UUID> playersToUpdate = ConcurrentHashMap.newKeySet();
    private static final ConcurrentMap<UUID, ArmorSnapshot> lastArmor = new ConcurrentHashMap<>();

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            // 1) Детектим изменение брони (Fabric не даёт удобного LivingEquipmentChangeEvent)
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                ArmorSnapshot now = ArmorSnapshot.capture(player);
                ArmorSnapshot prev = lastArmor.put(player.getUUID(), now);
                if (prev != null && !prev.equals(now)) {
                    playersToUpdate.add(player.getUUID());
                }
            }

            // 2) В конце тика корректируем здоровье, если максимум уменьшился
            if (!playersToUpdate.isEmpty()) {
                for (UUID uuid : playersToUpdate.toArray(new UUID[0])) {
                    ServerPlayer player = server.getPlayerList().getPlayer(uuid);
                    if (player != null && player.getHealth() > player.getMaxHealth()) {
                        player.setHealth(player.getMaxHealth());
                    }
                }
                playersToUpdate.clear();
            }
        });
    }

    private record ArmorSnapshot(Object head, Object chest, Object legs, Object feet) {
        static ArmorSnapshot capture(LivingEntity e) {
            return new ArmorSnapshot(
                    key(e.getItemBySlot(EquipmentSlot.HEAD)),
                    key(e.getItemBySlot(EquipmentSlot.CHEST)),
                    key(e.getItemBySlot(EquipmentSlot.LEGS)),
                    key(e.getItemBySlot(EquipmentSlot.FEET))
            );
        }

        private static Object key(net.minecraft.world.item.ItemStack stack) {
            if (stack.isEmpty()) return "";
            return stack.getItem().builtInRegistryHolder().key().location().toString() + "#" + stack.getTag();
        }
    }
}
*///?}