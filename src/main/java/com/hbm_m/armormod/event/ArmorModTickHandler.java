package com.hbm_m.armormod.event;

import com.hbm_m.armormod.item.ItemArmorMod;
import com.hbm_m.armormod.util.ArmorModificationHelper;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import dev.architectury.event.events.common.TickEvent;

/**
 * Drives {@link ItemArmorMod#modUpdate}, the port's equivalent of 1.7.10's per-tick armour-mod
 * callback.
 *
 * <p>The original calls {@code modUpdate} from its own armour tick loop for every mod installed in
 * every worn piece. The port's mod system only ever applied attribute modifiers, so mods with
 * active behaviour had no way to run at all.</p>
 *
 * <p>Единый путь для обеих платформ — Architectury {@code TickEvent.PLAYER_POST}. Раньше Forge
 * тикал все LivingEntity через LivingTickEvent, но моды брони носятся только игроками; сужение
 * до игроков соответствует уже принятому поведению NeoForge-ветки и оригиналу.</p>
 */
public class ArmorModTickHandler {

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private static boolean initialized = false;

    public static void init() {
        if (initialized) return;
        initialized = true;
        TickEvent.PLAYER_POST.register(ArmorModTickHandler::tickArmorMods);
    }

    private static void tickArmorMods(LivingEntity entity) {
        if (entity.level().isClientSide) return;

        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack armor = entity.getItemBySlot(slot);
            if (armor.isEmpty() || !ArmorModificationHelper.hasMods(armor)) continue;

            for (ItemStack mod : ArmorModificationHelper.pryMods(armor)) {
                if (mod != null && !mod.isEmpty() && mod.getItem() instanceof ItemArmorMod armorMod) {
                    armorMod.modUpdate(entity, armor);
                }
            }
        }
    }
}
