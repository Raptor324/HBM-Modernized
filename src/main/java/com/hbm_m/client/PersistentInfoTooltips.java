package com.hbm_m.client;

import java.util.List;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.util.BobMathUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * Original {@code IPersistentInfoProvider} ({@code ItemBlockBase.addInformation}): zeigt im Tooltip, was ein abgebauter
 * Tank / eine Raffinerie / ein Bohrturm / ein Kondensator mitgenommen hat (gelb: Fuellstand, gruen: Strom).
 */
public final class PersistentInfoTooltips {

    private PersistentInfoTooltips() { }

    public static void append(ItemStack stack, List<Component> list) {
        if (!(stack.getItem() instanceof BlockItem bi)) return;
        CompoundTag data = BlockItem.getBlockEntityData(stack);
        if (data == null) return;
        Block b = bi.getBlock();

        if (b == ModBlocks.FLUID_TANK.get() || b == ModBlocks.BAT9000.get() || b == ModBlocks.ORBUS.get() || b == ModBlocks.MACHINE_BIGASSTANK.get()
                || b == ModBlocks.BARREL_CORRODED.get() || b == ModBlocks.BARREL_IRON.get() || b == ModBlocks.BARREL_PLASTIC.get()
                || b == ModBlocks.BARREL_STEEL.get() || b == ModBlocks.BARREL_TCALLOY.get() || b == ModBlocks.BARREL_ANTIMATTER.get()) {
            tank(list, data, "tank");
        } else if (b == ModBlocks.HYDROTREATER.get() || b == ModBlocks.CATALYTIC_REFORMER.get()) {
            for (int i = 0; i < 4; i++) tank(list, data, "tank" + i);
        } else if (b == ModBlocks.VACUUM_DISTILL.get()) {
            for (String k : new String[] { "input", "heavy", "reformate", "light", "gas" }) tank(list, data, k);
        } else if (b == ModBlocks.REFINERY.get()) {
            for (int i = 0; i < 5; i++) tank(list, data, "tank_" + i);
        } else if (b == ModBlocks.PUMPJACK.get() || b == ModBlocks.DERRICK.get()) {
            list.add(Component.literal(BobMathUtil.getShortNumber(data.getLong("power")) + "HE").withStyle(ChatFormatting.GREEN));
            for (int i = 0; i < 2; i++) tank(list, data, "t" + i);
        } else if (b == ModBlocks.MACHINE_FENSU.get()) {
            // Original MachineFENSU.addInformation
            list.add(Component.literal(BobMathUtil.getShortNumber(data.getLong("energy")) + "/" + BobMathUtil.getShortNumber(Long.MAX_VALUE) + "HE").withStyle(ChatFormatting.YELLOW));
        } else if (b == ModBlocks.FENSU2.get()) {
            byte[] p = data.getByteArray("power");
            if (p.length > 0) list.add(Component.literal(String.format(java.util.Locale.US, "%,d", new java.math.BigInteger(p)) + " HE").withStyle(ChatFormatting.YELLOW));
        } else if (b == ModBlocks.HYDRAULIC_FRACKINING_TOWER.get()) {
            list.add(Component.literal(BobMathUtil.getShortNumber(data.getLong("energy")) + "HE").withStyle(ChatFormatting.GREEN));
            for (String k : new String[] { "oilTank", "gasTank", "fracksolTank" }) {
                if (!data.contains(k)) continue;
                FluidTank tank = new FluidTank(0);
                tank.readNBT(data.getCompound(k));
                list.add(Component.literal(tank.getFill() + "/" + tank.getMaxFill() + "mB ").append(FluidType.forFluid(tank.getTankType()).getLocalizedName()).withStyle(ChatFormatting.YELLOW));
            }
        } else if (b == ModBlocks.CAPACITOR_BUS.get() || b == ModBlocks.CAPACITOR_COPPER.get() || b == ModBlocks.CAPACITOR_GOLD.get()
                || b == ModBlocks.CAPACITOR_NIOBIUM.get() || b == ModBlocks.CAPACITOR_SCHRABIDATE.get() || b == ModBlocks.CAPACITOR_TANTALIUM.get()) {
            // Original MachineCapacitor.addInformation(persistentTag): Kapazitaet und Lade-/Entladetempo in Gold
            if (b != ModBlocks.CAPACITOR_BUS.get() && b instanceof com.hbm_m.block.machines.MachineCapacitorBlock cap) {
                long power = cap.getCapacity();
                list.add(Component.literal("Stores up to " + BobMathUtil.getShortNumber(power) + "HE").withStyle(ChatFormatting.GOLD));
                list.add(Component.literal("Charge speed: " + BobMathUtil.getShortNumber(power / 200) + "HE").withStyle(ChatFormatting.GOLD));
                list.add(Component.literal("Discharge speed: " + BobMathUtil.getShortNumber(power / 600) + "HE").withStyle(ChatFormatting.GOLD));
            }
            list.add(Component.literal(BobMathUtil.getShortNumber(data.getLong("energy")) + "/" + BobMathUtil.getShortNumber(data.getLong("capacity")) + "HE").withStyle(ChatFormatting.YELLOW));
        }
    }

    private static void tank(List<Component> list, CompoundTag data, String key) {
        if (!data.contains(key)) return;
        FluidTank tank = new FluidTank(0);
        tank.readFromNBT(data, key);
        list.add(Component.literal(tank.getFill() + "/" + tank.getMaxFill() + "mB ").append(FluidType.forFluid(tank.getTankType()).getLocalizedName()).withStyle(ChatFormatting.YELLOW));
    }
}
