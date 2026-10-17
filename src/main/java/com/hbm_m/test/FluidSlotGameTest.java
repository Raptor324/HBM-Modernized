package com.hbm_m.test;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.liquids.ItemFluidTank;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
//? if forge {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} elif neoforge {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
 *///?}

/**
 * Fluid-Lade-/Entladeslots aller HBM-Maschinen (Nutzer-Meldung 2026-10-13 "kann keine Fluessigkeiten einfuellen"):
 * Jede Maschine wird gesetzt, ihr Menue ueber {@code createMenu} erzeugt (wie beim Oeffnen der GUI). Nimmt ein
 * Maschinenslot einen Wassereimer an (= Fluid-Ladeslot), muss er auch die HBM-Behaelter ohne Plattform-Capability
 * annehmen (voller Fluessigkeitstank/-fass); nimmt er einen leeren Eimer an (= Entladeslot), auch die leeren
 * HBM-Behaelter. Frueher pruefte ein Teil der Slots nur die Capability und wies Kanister, Tanks und Faesser ab.
 */
@GameTestHolder("hbm_m")
@PrefixGameTestTemplate(false)
public final class FluidSlotGameTest {

    private FluidSlotGameTest() {}

    private static Player makePlayer(GameTestHelper helper) {
        //? if < 1.21.1 {
        return helper.makeMockPlayer();
        //?} else {
        /*return helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        *///?}
    }

    @GameTest(template = "empty3x3x3", batch = "fluid_slots", timeoutTicks = 400)
    public static void allMachineFluidSlotsAcceptHbmContainers(GameTestHelper helper) {
        Player player = makePlayer(helper);
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        var water = ModFluids.WATER.getSource();
        ItemStack bucket = new ItemStack(Items.WATER_BUCKET);
        ItemStack emptyBucket = new ItemStack(Items.BUCKET);
        ItemStack[] full = { ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), water, 1), ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), water, 1) };
        ItemStack[] empty = { new ItemStack(ModItems.FLUID_TANK_EMPTY.get()), new ItemStack(ModItems.FLUID_BARREL_EMPTY.get()) };

        List<String> failures = new ArrayList<>();
        List<String> found = new ArrayList<>();
        int menus = 0, fluidSlots = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            if (!(block instanceof EntityBlock)) continue;
            var key = BuiltInRegistries.BLOCK.getKey(block);
            if (!"hbm_m".equals(key.getNamespace())) continue;
            AbstractContainerMenu menu;
            try {
                helper.getLevel().setBlock(pos, block.defaultBlockState(), Block.UPDATE_ALL);
                if (!(helper.getLevel().getBlockEntity(pos) instanceof MenuProvider mp)) continue;
                menu = mp.createMenu(0, player.getInventory(), player);
            } catch (Throwable t) {
                continue; // Mehrblock-Teile, Menues mit Zusatzdaten: hier nicht pruefbar
            } finally {
                try {
                    helper.getLevel().removeBlockEntity(pos);
                    helper.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                } catch (Throwable ignored) { }
            }
            if (menu == null) continue;
            menus++;
            for (Slot slot : menu.slots) {
                if (slot.container == player.getInventory()) continue;
                boolean load, unload;
                try {
                    load = slot.mayPlace(bucket);
                    unload = slot.mayPlace(emptyBucket);
                } catch (Throwable t) {
                    continue;
                }
                if (!load && !unload) continue;
                fluidSlots++;
                found.add(key.getPath() + "#" + slot.index + (load ? "L" : "E"));
                for (ItemStack s : load ? full : empty) {
                    boolean ok;
                    try { ok = slot.mayPlace(s); } catch (Throwable t) { ok = false; }
                    if (!ok) failures.add(key.getPath() + "/" + menu.getClass().getSimpleName() + "#" + slot.index + " weist " + BuiltInRegistries.ITEM.getKey(s.getItem()).getPath() + " ab");
                }
            }
        }
        com.hbm_m.main.MainRegistry.LOGGER.info("[FluidSlotGameTest] {} Menues, {} Fluid-Slots: {}", menus, fluidSlots, found);
        // Selbstpruefung: die Faesser mit FluidTankMenu (frueher fehlerhaft) muessen erfasst sein
        if (found.stream().noneMatch(f -> f.startsWith("barrel_plastic#"))) throw new GameTestAssertException("barrel_plastic nicht erfasst: " + found);
        if (menus < 20) throw new GameTestAssertException("nur " + menus + " Menues erzeugt - Test greift nicht");
        if (!failures.isEmpty()) throw new GameTestAssertException(failures.size() + " Fluid-Slots (von " + fluidSlots + ") verweigern HBM-Behaelter: " + String.join("; ", failures));
        helper.succeed();
    }
}
