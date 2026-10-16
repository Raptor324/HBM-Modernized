package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.trait.FluidTrait;
import com.hbm_m.inventory.menu.FluidTankMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Corroded Barrel BlockEntity - only ever exists as the runtime result of an Iron Barrel decaying
 * (see {@link BarrelIronBlockEntity#handleIncompatibleFluid}), not as something you can craft or
 * place directly (matches the original 1.7.10 {@code BlockFluidBarrel}, whose
 * {@code createNewTileEntity} is hard-coded to return {@code null} specifically for
 * {@code barrel_corroded} - i.e. a freshly-placed corroded barrel there is purely decorative).
 * <p>
 * Reuses the base tank's generic "exploded" leak system ({@link MachineFluidTankBlockEntity#hasExploded})
 * permanently switched on, with the original's (NEXT {@code BlockEntityBarrel#checkFluidInteraction},
 * {@code barrel_corroded} branch) exact rates: 1 mB drained per tick with probability 1/3 (~6.7 mB/s,
 * a full barrel lasts ~15 minutes), each successful roll accompanied by
 * {@code FluidTrait.onRelease(SPILL, 1)} (spill world-effects from the fluid's traits), plus a
 * per-tick {@code nextInt(3 * 60 * 20) == 0} chance of the barrel being destroyed outright
 * (no drops, no explosion effect; expected 180 s).
 * <p>
 * Intentional deviation from NEXT (where barrel_corroded is a 6,000 mB inert block with no BE,
 * placed only by the silo structure processor): here it is the live endpoint of iron decay, so the
 * BE/GUI/decay are kept; only the capacity follows NEXT's 6,000 mB anchor.
 */
public class BarrelCorrodedBlockEntity extends MachineFluidTankBlockEntity {

    /** Паритет NEXT BlockFluidBarrel: barrel_corroded — 6,000 mB. */
    public static final int CAPACITY = 6_000;

    /** Паритет NEXT BlockEntityBarrel: рулон разрушения каждый тик — nextInt(3 * 60 * 20) == 0
     *  (матожидание 180 с, как в оригинале). */
    private static final int DESTROY_CHANCE_ROLL = 3 * 60 * 20;

    public BarrelCorrodedBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BARREL_CORRODED_BE.get(), pos, state, CAPACITY);
        this.hasExploded = true;
    }

    /** Паритет 1.7.10 BlockFluidBarrel.breakBlock: barrel_corroded слоты не дропает. */
    @Override
    protected boolean dropInventoryOnRemove() { return false; }

    @Override
    protected int calculateLeakAmount() {
        // Паритет NEXT (ветка barrel_corroded): 1 mB за тик с вероятностью 1/3 — в среднем
        // ~6.7 mB/с; полная бочка опустошается примерно за 15 минут.
        if (level == null || level.isClientSide) return 0;
        return level.random.nextInt(3) == 0 ? 1 : 0;
    }

    @Override
    protected void updateLeak(int amount) {
        super.updateLeak(amount);
        if (level == null || level.isClientSide) return;
        if (amount > 0) {
            // Паритет NEXT: каждый успешный рулон слива сопровождается FluidTrait.onRelease(SPILL, 1)
            // — мировые эффекты пролива из трейтов жидкости (загрязнение/радиация).
            FluidType ftype = FluidType.forFluid(getFluidTank().getTankType());
            for (FluidTrait trait : ftype.traits.values()) {
                trait.onFluidRelease(level, worldPosition, getFluidTank(), amount, FluidTrait.FluidReleaseType.SPILL);
            }
        }
        // Паритет NEXT BlockEntityBarrel: рулон разрушения идет КАЖДЫЙ тик независимо от
        // рулона слива (nextInt(3*60*20), матожидание 180 с).
        if (level.random.nextInt(DESTROY_CHANCE_ROLL) == 0) {
            level.destroyBlock(worldPosition, false);
        }
    }

    @Override
    public Component getDisplayName() {
        // Паритет NEXT BlockEntityBarrel.getDefaultName: у всех бочек общий заголовок GUI
        // "Barrel" (container.hbm.barrel); имя материала остаётся у блока/предмета.
        return Component.translatable("container.hbm_m.barrel");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new FluidTankMenu(id, inventory, this, this.data);
    }
}
