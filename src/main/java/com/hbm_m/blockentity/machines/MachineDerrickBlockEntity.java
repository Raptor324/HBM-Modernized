package com.hbm_m.blockentity.machines;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.OilDrillBaseBlockEntity;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.menu.MachineDerrickMenu;

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

public class MachineDerrickBlockEntity extends OilDrillBaseBlockEntity {

    private static final long MAX_POWER     = 100_000L;
    private static final int  CONSUMPTION   = 100;
    private static final int  DELAY         = 50;

    /** Original {@code TileEntityMachineOilWell}: 500 mB Oel, 100-500 mB Gas, 5 % Chance, das Vorkommen zu leeren. */
    private static final int OIL_PER_DEPOSIT     = 500;
    private static final int GAS_PER_DEPOSIT_MIN = 100;
    private static final int GAS_PER_DEPOSIT_MAX = 500;
    private static final double DRAIN_CHANCE     = 0.05D;

    public MachineDerrickBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DERRICK_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineDerrickBlockEntity blockEntity) {
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

    /**
     * Original {@code onDrill}: Uran- bzw. Asbesterz im Bohrloch setzt dichtes Radon bzw. Asbestgas 10 Bloecke ueber dem
     * Bohrturm frei (die Diagonal-Eigenheit des Originals - geprueft bei (j,j), gesetzt bei (k,k) - bleibt erhalten).
     */
    @Override
    public void onDrill(BlockPos pos) {
        ItemStack stack = new ItemStack(level.getBlockState(pos).getBlock());
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();

        if (stack.is(net.minecraft.tags.ItemTags.create(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("forge", "ores/uranium")))) {
            for (int j = -1; j <= 1; j++) {
                for (int k = -1; k <= 1; k++) {
                    if (level.getBlockState(new BlockPos(x + j, y + 10, z + j)).canBeReplaced()) {
                        level.setBlockAndUpdate(new BlockPos(x + k, y + 10, z + k), com.hbm_m.block.ModBlocks.GAS_RADON_DENSE.get().defaultBlockState());
                    }
                }
            }
        }

        if (stack.is(net.minecraft.tags.ItemTags.create(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("forge", "ores/asbestos")))) {
            for (int j = -1; j <= 1; j++) {
                for (int k = -1; k <= 1; k++) {
                    if (level.getBlockState(new BlockPos(x + j, y + 10, z + j)).canBeReplaced()) {
                        level.setBlockAndUpdate(new BlockPos(x + k, y + 10, z + k), com.hbm_m.block.ModBlocks.GAS_ASBESTOS.get().defaultBlockState());
                    }
                }
            }
        }
    }

    @Override
    public void onSuck(BlockPos pos) {
        // Original: "game.neutral.swim.splash", 2.0 / 0.5
        level.playSound(null, worldPosition, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 2.0F, 0.5F);

        tanks[0].setFill(tanks[0].getFill() + OIL_PER_DEPOSIT);
        if (tanks[0].getFill() > tanks[0].getMaxFill()) tanks[0].setFill(tanks[0].getMaxFill());
        tanks[1].setFill(tanks[1].getFill() + (GAS_PER_DEPOSIT_MIN + level.getRandom().nextInt(GAS_PER_DEPOSIT_MAX - GAS_PER_DEPOSIT_MIN + 1)));
        if (tanks[1].getFill() > tanks[1].getMaxFill()) tanks[1].setFill(tanks[1].getMaxFill());

        if (level.getRandom().nextDouble() < DRAIN_CHANCE) {
            level.setBlockAndUpdate(pos, com.hbm_m.block.ModBlocks.ORE_OIL_EMPTY.get().defaultBlockState());
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
        return Component.translatable("container.oilWell");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineDerrickMenu.create(id, inventory, this);
    }
}
