package com.hbm_m.item.weapon.sedna.impl;

import com.hbm_m.api.fluids.IFillableItem;
import com.hbm_m.api.item.IBatteryItem;
import com.hbm_m.item.weapon.sedna.GunConfig;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.item.weapon.sedna.factory.XFactoryDrill;
import com.hbm_m.item.weapon.sedna.mags.IMagazine;
import com.hbm_m.item.weapon.sedna.mags.MagazineElectricEngine;
import com.hbm_m.item.weapon.sedna.mags.MagazineLiquidEngine;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * 1:1 {@code ItemGunDrill}: Bohrer mit Fluessigmotor (Betankung ueber {@link IFillableItem}) oder Elektromotor
 * (Laden ueber {@link IBatteryItem}). Abbaustufe aus {@link XFactoryDrill#getModdableHarvestLevel}.
 */
@SuppressWarnings("rawtypes")
public class ItemGunDrill extends ItemGunBaseNT implements IFillableItem, IBatteryItem {

    public ItemGunDrill(WeaponQuality quality, GunConfig... cfg) {
        super(quality, cfg);
    }

    /** Original {@code getHarvestLevel(stack, toolClass)}: Eisen-Stufe, durch Bohrkopf-Mods erhoeht. */
    public int getHarvestLevel(ItemStack stack) {
        return XFactoryDrill.getModdableHarvestLevel(stack, Tiers.IRON.getLevel());
    }

    /**
     * Original {@code canHarvestBlock} = true ("this lets us break things that have no set harvest level (i.e. most NTM shit)");
     * nur Bloecke mit hoeherer Werkzeugstufe als {@link #getHarvestLevel} lassen sich nicht ernten (Forge 1.7 verglich die Stufe).
     */
    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        int level = getHarvestLevel(stack);
        if (level < 3 && state.is(BlockTags.NEEDS_DIAMOND_TOOL)) return false;
        if (level < 2 && state.is(BlockTags.NEEDS_IRON_TOOL)) return false;
        if (level < 1 && state.is(BlockTags.NEEDS_STONE_TOOL)) return false;
        return true;
    }

    /** Portergaenzung: Forge bricht {@code destroyBlock} ab, wenn der Gegenstand keine Bloecke angreifen darf (siehe {@link XFactoryDrill#harvesting}). */
    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return XFactoryDrill.harvesting || super.canAttackBlock(state, level, pos, player);
    }

    @Override
    public boolean acceptsFluid(Fluid type, ItemStack stack) {
        IMagazine mag = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);

        if (mag instanceof MagazineLiquidEngine engine) {
            for (Fluid acc : XFactoryDrill.acceptedTypes(engine)) if (type == acc) return true;
        }

        return false;
    }

    @Override
    public int tryFill(Fluid type, int amount, ItemStack stack) {
        IMagazine mag = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);

        if (mag instanceof MagazineLiquidEngine engine) {
            int toFill = Math.min(amount, 50);
            toFill = Math.min(toFill, engine.getCapacity(stack) - this.getFill(stack));
            engine.setAmount(stack, this.getFill(stack) + toFill);
            return amount - toFill;
        }

        return 0;
    }

    @Override public boolean providesFluid(Fluid type, ItemStack stack) { return false; }
    @Override public int tryEmpty(Fluid type, int amount, ItemStack stack) { return amount; }

    @Override
    public Fluid getFirstFluidType(ItemStack stack) {
        IMagazine mag = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);
        if (mag instanceof MagazineLiquidEngine engine) return engine.getType(stack, null);
        return Fluids.EMPTY;
    }

    @Override
    public int getFill(ItemStack stack) {
        IMagazine mag = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);

        if (mag instanceof MagazineLiquidEngine engine) {
            return engine.getAmount(stack, null);
        }

        return 0;
    }

    @Override
    public void chargeBattery(ItemStack stack, long i) {
        IMagazine mag = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);

        if (mag instanceof MagazineElectricEngine engine) {
            engine.setAmount(stack, Math.min(engine.capacity, engine.getAmount(stack, null) + (int) i));
        }
    }

    @Override
    public void setCharge(ItemStack stack, long i) {
        IMagazine mag = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);

        if (mag instanceof MagazineElectricEngine engine) {
            engine.setAmount(stack, (int) i);
        }
    }

    @Override
    public void dischargeBattery(ItemStack stack, long i) {
        IMagazine mag = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);

        if (mag instanceof MagazineElectricEngine engine) {
            engine.setAmount(stack, Math.max(0, engine.getAmount(stack, null) - (int) i));
        }
    }

    @Override
    public long getCharge(ItemStack stack) {
        IMagazine mag = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);

        if (mag instanceof MagazineElectricEngine engine) {
            return engine.getAmount(stack, null);
        }

        return 0;
    }

    @Override
    public long getMaxCharge(ItemStack stack) {
        IMagazine mag = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);

        if (mag instanceof MagazineElectricEngine engine) {
            return engine.getCapacity(stack);
        }

        return 0;
    }

    @Override public long getChargeRate(ItemStack stack) { return 50_000; }
    @Override public long getDischargeRate(ItemStack stack) { return 0; }
}
