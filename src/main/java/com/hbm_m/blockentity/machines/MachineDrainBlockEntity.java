package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardReceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.fluid.trait.FluidTraitSimple.FT_Amat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of {@code TileEntityMachineDrain} (1.7.10 Original) - a small fluid-network sink that
 * continuously disposes of whatever fluid is piped into it (used to dump excess/waste fluid
 * instead of storing it). Antimatter still triggers a real explosion, matching the original.
 * <p>
 * <p><b>Entsorgen hat Folgen.</b> Was hier hineingeht, verschwindet nicht einfach: die Haelfte
 * des Tankinhalts geht jeden Tick als Verschmutzung in die Umgebung
 * ({@link com.hbm_m.inventory.fluid.trait.FT_Polluting}). Bei zaehen, brennbaren Fluessigkeiten -
 * also Oel - bildet sich ausserdem hin und wieder eine <b>Pfuetze</b> ein Stueck vor dem Ablauf.
 * Wer sein Altoel hier loswird, versaut sich die Gegend.</p>
 *
 * <p>Antimaterie ist die Ausnahme: die sprengt den Ablauf, statt sich entsorgen zu lassen.</p>
 *
 * <p>Er belegt wie im Original drei Felder in einer Reihe
 * ({@code getDimensions {0,0,2,0,0,0}}).</p>
 */
public class MachineDrainBlockEntity extends BaseMachineBlockEntity implements IFluidStandardReceiverMK2 {

    private static final int TANK_CAPACITY = 2_000;

    private final FluidTank tank = new FluidTank(ModFluids.NONE.getSource(), TANK_CAPACITY);

    public MachineDrainBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_DRAIN_BE.get(), pos, state, 0, 0L, 0L, 0L);
    }

    //? if forge {
    @Override
    public @org.jetbrains.annotations.NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
            net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER) {
            return tank.getForgeFluidCapability().cast();
        }
        return super.getCapability(cap, side);
    }
    //?}

    @Override
    public FluidTank[] getAllTanks() { return new FluidTank[] { tank }; }

    @Override
    public FluidTank[] getReceivingTanks() { return new FluidTank[] { tank }; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    public FluidTank getTank() { return tank; }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineDrainBlockEntity be) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return;
        be.serverTick(serverLevel, pos);
    }

    private void serverTick(ServerLevel level, BlockPos pos) {
        if (level.getGameTime() % 20 == 0) {
            for (Direction dir : Direction.values()) {
                if (dir == Direction.UP || dir == Direction.DOWN) continue;
                trySubscribe(tank.getTankType(), level, pos.relative(dir), dir);
            }
        }

        if (tank.getFluidAmountMb() > 0) {
            if (FluidType.getTrait(tank.getStoredFluid(), FT_Amat.class) != null) {
                level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        10.0F, Level.ExplosionInteraction.BLOCK);
                tank.drainMb(tank.getFluidAmountMb());
                return;
            }

            // 1:1: die Haelfte je Tick, mindestens ein Millibucket.
            int toSpill = Math.max(tank.getFluidAmountMb() / 2, 1);
            tank.drainMb(toSpill);

            com.hbm_m.inventory.fluid.trait.FT_Polluting.pollute(level, pos, tank.getStoredFluid(),
                    com.hbm_m.inventory.fluid.trait.FluidTrait.FluidReleaseType.SPILL, toSpill);

            spillPuddle(level, pos, toSpill);
        }

        setChanged();
    }

    /**
     * 1:1-Port des Pfuetzenteils: bei groesseren Mengen zaeher, brennbarer Fluessigkeit sucht ein
     * Strahl schraeg nach unten einen Boden und legt dort eine Oelpfuetze an.
     *
     * <p>Das trifft nur Oel und Verwandtes - Wasser hinterlaesst nichts.</p>
     */
    private void spillPuddle(ServerLevel level, BlockPos pos, int toSpill) {
        if (toSpill < 100 || level.getRandom().nextInt(20) != 0) return;

        var fluid = tank.getStoredFluid();
        if (FluidType.getTrait(fluid, com.hbm_m.inventory.fluid.trait.FluidTraitSimple.FT_Liquid.class) == null) return;
        if (FluidType.getTrait(fluid, com.hbm_m.inventory.fluid.trait.FluidTraitSimple.FT_Viscous.class) == null) return;
        if (FluidType.getTrait(fluid, com.hbm_m.inventory.fluid.trait.FT_Flammable.class) == null) return;

        Direction facing = getBlockState().hasProperty(
                net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)
                ? getBlockState().getValue(
                        net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)
                : Direction.NORTH;

        // Original: der Strahl startet drei Felder hinter dem Ablauf und geht 25 nach unten.
        net.minecraft.world.phys.Vec3 start = new net.minecraft.world.phys.Vec3(
                pos.getX() + 0.5 - facing.getStepX() * 3,
                pos.getY() + 0.5,
                pos.getZ() + 0.5 - facing.getStepZ() * 3);
        net.minecraft.world.phys.Vec3 end = start.add(
                level.getRandom().nextGaussian() * 5, -25, level.getRandom().nextGaussian() * 5);

        var hit = level.clip(new net.minecraft.world.level.ClipContext(start, end,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, null));

        // Nur auf einer Oberseite bildet sich eine Pfuetze.
        if (hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) return;
        if (hit.getDirection() != Direction.UP) return;

        BlockPos above = hit.getBlockPos().above();
        var state = level.getBlockState(above);
        if (!state.getFluidState().isEmpty() || !state.canBeReplaced()) return;

        level.setBlockAndUpdate(above, com.hbm_m.block.ModBlocks.OIL_SPILL.get().defaultBlockState());
    }

    public void retype(net.minecraft.world.level.material.Fluid fluid) {
        tank.setTankType(fluid);
        setChanged();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.machine_drain");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return null;
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.put("tank", tank.writeNBT(new CompoundTag()));
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        if (tag.contains("tank")) tank.readNBT(tag.getCompound("tank"));
    }
}
