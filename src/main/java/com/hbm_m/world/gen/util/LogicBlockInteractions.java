package com.hbm_m.world.gen.util;

import com.hbm_m.platform.EffectHooks;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Consumer;

import com.hbm_m.api.tile.ILockableTile;
import com.hbm_m.blockentity.generic.LogicBlockEntity;
import com.hbm_m.effect.ModEffects;
import com.hbm_m.interfaces.IEnergyProvider;
import com.hbm_m.interfaces.IEnergyReceiver;
import com.hbm_m.item.ModItems;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** 1:1 {@code com.hbm.world.gen.util.LogicBlockInteractions}: laufen beim Rechtsklick auf den Logikblock. */
public class LogicBlockInteractions {

    /** Welt, Blockentity, x, y, z, Spieler, Seite, hitX, hitY, hitZ - in dieser Reihenfolge. */
    public static LinkedHashMap<String, Consumer<Object[]>> interactions;

    public static Consumer<Object[]> TEST = (array) -> {
        LogicBlockEntity logic = (LogicBlockEntity) array[1];
        Player player = (Player) array[5];

        if (logic.phase > 1) return;

        ItemStack held = player.getMainHandItem();
        if (!held.isEmpty())
            held.shrink(1);

        logic.phase++;
    };

    public static Consumer<Object[]> RAD_CONTAINMENT_SYSTEM = (array) -> {
        LogicBlockEntity logic = (LogicBlockEntity) array[1];
        Player player = (Player) array[5];

        ItemStack held = player.getMainHandItem();
        if (!held.isEmpty() && held.getItem() == ModItems.KEY.get()) {
            held.shrink(1);
            player.sendSystemMessage(Component.literal("[RAD CONTAINMENT SYSTEM]").withStyle(ChatFormatting.LIGHT_PURPLE)
                    .append(Component.literal(" Radiation treatment administered").withStyle(ChatFormatting.WHITE) /* 1.7 §r = Grundfarbe */));
            player.addEffect(new MobEffectInstance(EffectHooks.of(ModEffects.RADAWAY), 3 * 60 * 20, 4));
            player.addEffect(new MobEffectInstance(EffectHooks.of(ModEffects.RADX), 3 * 60 * 20, 4));
            logic.phase = 2;
            logic.timer = 0;
        }
    };

    /** Original {@code IEnergyHandlerMK2.getPower()}: gespeicherte Energie eines Erzeugers oder Verbrauchers. */
    private static long getPower(BlockEntity te) {
        if (te instanceof IEnergyProvider provider) return provider.getEnergyStored();
        if (te instanceof IEnergyReceiver receiver) return receiver.getEnergyStored();
        return 0;
    }

    public static Consumer<Object[]> POWER_LOCK = (array) -> {
        Level world = (Level) array[0];
        Player player = (Player) array[5];
        int x = (int) array[2];
        int y = (int) array[3];
        int z = (int) array[4];
        BlockPos pos = new BlockPos(x, y, z);

        BlockEntity handler = null;

        for (Direction dir : Direction.values()) {
            BlockEntity te = world.getBlockEntity(pos.relative(dir));
            if (te instanceof IEnergyProvider || te instanceof IEnergyReceiver) {
                handler = te;
                break;
            }
        }

        if (handler == null || !(getPower(handler) > 500_000))
            player.sendSystemMessage(Component.literal("[POWER LOCK]").withStyle(ChatFormatting.LIGHT_PURPLE)
                    .append(Component.literal(" Charge adjacent energy storage to at least 500KHE to release emergency lock").withStyle(ChatFormatting.WHITE) /* 1.7 §r = Grundfarbe */));
        else {
            player.sendSystemMessage(Component.literal("[POWER LOCK]").withStyle(ChatFormatting.LIGHT_PURPLE)
                    .append(Component.literal(" Power Restorted! Safe Unlocked!").withStyle(ChatFormatting.WHITE) /* 1.7 §r = Grundfarbe */));

            ILockableTile safe = null;
            for (Direction dir : Direction.values()) {
                if (world.getBlockEntity(pos.relative(dir)) instanceof ILockableTile lockable) {
                    safe = lockable;
                    break;
                }
            }
            if (safe != null) {
                safe.unlock();
                world.playSound(null, player.getX(), player.getY(), player.getZ(), HbmSoundsNT.get("hbm:block.lockOpen"), SoundSource.BLOCKS, 3.0F, 0.8F);
            }
        }
    };

    public static List<String> getInteractionNames() {
        return new ArrayList<>(interactions.keySet());
    }

    // neue Interaktionen hier registrieren
    static {
        initialize();
    }

    public static void initialize() {
        interactions = new LinkedHashMap<>();

        interactions.put("POWER_LOCK", POWER_LOCK);

        // Beispielinteraktionen
        interactions.put("TEST", TEST);
        interactions.put("RADAWAY_INJECTOR", RAD_CONTAINMENT_SYSTEM);
    }
}
