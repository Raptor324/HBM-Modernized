package com.hbm_m.armormod.event;

import com.hbm_m.armormod.item.ItemArmorMod;
import com.hbm_m.armormod.util.ArmorModificationHelper;
import com.hbm_m.lib.RefStrings;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
//? if forge {
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?}

/**
 * Drives {@link ItemArmorMod#modUpdate}, the port's equivalent of 1.7.10's per-tick armour-mod
 * callback.
 *
 * <p>The original calls {@code modUpdate} from its own armour tick loop for every mod installed in
 * every worn piece. The port's mod system only ever applied attribute modifiers, so mods with
 * active behaviour had no way to run at all.</p>
 */
//? if forge {
@Mod.EventBusSubscriber(modid = RefStrings.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
//?} elif neoforge {
/*@net.neoforged.fml.common.EventBusSubscriber(modid = RefStrings.MODID)
*///?}
public class ArmorModTickHandler {

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    /**
     * Auf Forge liefert {@code LivingEvent.LivingTickEvent} alle Lebewesen. Auf 1.21 gibt es das
     * Event nicht; dort deckt Architecturys Player-Tick den realistischen Fall ab (getragene
     * Mod-Ruestung am Spieler). Getrennt gehalten, damit auf Forge nichts doppelt tickt.
     */
    public static void init() {
        //? if fabric {
        /*dev.architectury.event.events.common.TickEvent.PLAYER_POST.register(
                player -> tickArmorMods(player));
        *///?}
    }

    //? if forge {
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        tickArmorMods(event.getEntity());
    }
    //?} elif neoforge {
    /*// NeoForge: Forge LivingTickEvent -> EntityTickEvent.Pre, wie auf Forge fuer alle Lebewesen.
    @net.neoforged.bus.api.SubscribeEvent
    public static void onLivingTick(net.neoforged.neoforge.event.tick.EntityTickEvent.Pre event) {
        if (event.getEntity() instanceof LivingEntity living) tickArmorMods(living);
    }
    *///?}

    private static void tickArmorMods(LivingEntity entity) {
        if (entity.level().isClientSide) return;

        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack armor = entity.getItemBySlot(slot);
            if (armor.isEmpty() || !ArmorModificationHelper.hasMods(armor)) continue;

            for (ItemStack mod : ArmorModificationHelper.pryMods(armor)) {
                if (mod != null && !mod.isEmpty() && mod.getItem() instanceof ItemArmorMod armorMod) {
                    armorMod.modUpdate(entity, armor);
                    // Original ModEventHandler.onLivingUpdate: Mods tragen ihre eigenen Hazards.
                    com.hbm_m.hazard.HazardSystem.applyHazards(mod, entity);
                }
            }
        }
    }
}
