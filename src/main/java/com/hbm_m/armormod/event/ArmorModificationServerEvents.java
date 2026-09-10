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
/*import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RefStrings.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
@SuppressWarnings("UnstableApiUsage")
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
*///?}


