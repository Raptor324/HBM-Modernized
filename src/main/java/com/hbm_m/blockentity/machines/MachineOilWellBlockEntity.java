package com.hbm_m.blockentity.machines;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.OilDrillBaseBlockEntity;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.menu.MachineOilWellMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Oil Well - Port von {@code TileEntityMachineOilWell} (1.7.10 Original). Erbt die komplette
 * Bohr-/Saug-Logik von {@link OilDrillBaseBlockEntity} (bereits fuer {@link MachineDerrickBlockEntity}
 * und {@link MachinePumpjackBlockEntity} etabliert) - wie beim Pumpjack wird das Oelvorkommen nur
 * mit {@link #DRAIN_CHANCE} Wahrscheinlichkeit geleert statt garantiert (Direktport von {@code onSuck}).
 * <p>
 * SCOPE-Entscheidung: Das Original spawnt beim Durchbohren von Uran-/Asbest-Erz zusaetzlich dichte
 * Radon-/Asbest-Gaswolken ({@code onDrill}, OreDictionary-Check auf "oreUranium"/"oreAsbestos") -
 * entfaellt hier (keine 1:1 entsprechenden Erzblock-Namen/Tags in diesem Port vorhanden, rein
 * kosmetischer Nebeneffekt ohne Einfluss auf die eigentliche Foerdermechanik).
 */
public class MachineOilWellBlockEntity extends OilDrillBaseBlockEntity {

    private static final long MAX_POWER     = 100_000L;
    private static final int  CONSUMPTION   = 100;
    private static final int  DELAY         = 50;

    private static final int OIL_PER_DEPOSIT     = 500;
    private static final int GAS_PER_DEPOSIT_MIN = 100;
    private static final int GAS_PER_DEPOSIT_MAX = 500;
    private static final double DRAIN_CHANCE     = 0.05D;

    public MachineOilWellBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_WELL_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineOilWellBlockEntity blockEntity) {
        OilDrillBaseBlockEntity.tick(level, pos, state, blockEntity);
    }

    @Override
    public int getPowerReq() {
        return CONSUMPTION;
    }

    @Override
    public int getDelay() {
        return DELAY;
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    public void onSuck(BlockPos pos) {
        level.playSound(null, worldPosition, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 2.0F, 0.5F);

        tanks[0].fillMb(ModFluids.CRUDE_OIL.getSource(), OIL_PER_DEPOSIT);
        int gas = GAS_PER_DEPOSIT_MIN + level.getRandom().nextInt(GAS_PER_DEPOSIT_MAX - GAS_PER_DEPOSIT_MIN + 1);
        tanks[1].fillMb(ModFluids.GAS.getSource(), gas);

        if (level.getRandom().nextDouble() < DRAIN_CHANCE) {
            level.setBlockAndUpdate(pos, ModBlocks.ORE_OIL_EMPTY.get().defaultBlockState());
        }
    }

    @Override
    public Direction[] getConPos() {
        return new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return true;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hbm_m.machine_well");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineOilWellMenu.create(id, inventory, this);
    }
}
