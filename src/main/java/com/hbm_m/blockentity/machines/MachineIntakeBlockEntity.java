package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardSenderMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code TileEntityMachineIntake}: Lufteinlass, 2x2. Hat er mindestens ein Zwanzigstel seines Puffers (100 von
 * 2000 HE), zieht er das ab und fuellt den Drucklufttank (1000 mB) ganz auf; die Luft geht an alle Anschluesse.
 * Kein GUI. Auf dem Client dreht sich der Luefter ({@link #fan}) und der Elektromotor laeuft (Lautstaerke 0,25).
 */
public class MachineIntakeBlockEntity extends BaseMachineBlockEntity implements IFluidStandardSenderMK2 {

    public static final long MAX_POWER = 2_000L;

    public final FluidTank compair = new FluidTank(ModFluids.AIR.getSource(), 1_000);
    public float fan = 0;
    public float prevFan = 0;

    public MachineIntakeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INTAKE_BE.get(), pos, state, 0, MAX_POWER, MAX_POWER);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineIntakeBlockEntity be) {
        if (!level.isClientSide) {
            be.ensureNetworkInitialized();

            if (be.getEnergyStored() >= MAX_POWER / 20) {
                be.compair.setFill(be.compair.getMaxFill());
                be.setEnergyStored(be.getEnergyStored() - MAX_POWER / 20);
            }

            // Original networkPackNT(50)
            be.setChanged();
            be.sendUpdateToClient();
        } else {
            be.prevFan = be.fan;

            boolean running = be.getEnergyStored() >= MAX_POWER / 20;
            if (running) {
                be.fan += 45;

                if (be.fan >= 360) {
                    be.fan -= 360;
                    be.prevFan -= 360;
                }
            }
            be.clientSound(running);
        }
    }

    //? if forge {
    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    //?}
    private void clientSound(boolean running) {
        com.hbm_m.sound.ClientSoundBootstrap.updateSound(this, running, this::createAudioLoop);
    }

    private Object createAudioLoop() {
        try {
            return Class.forName("com.hbm_m.client.sound.IntakeLoopSoundFactory").getMethod("create", MachineIntakeBlockEntity.class).invoke(null, this);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && level.isClientSide) com.hbm_m.sound.ClientSoundBootstrap.updateSound(this, false, null);
    }

    // ==================== Anschluesse (Original: nicht oben/unten) ====================

    @Override
    public boolean canConnectEnergy(Direction side) {
        return side != Direction.UP && side != Direction.DOWN;
    }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        return fluid == ModFluids.AIR.getSource() && fromDir != Direction.UP && fromDir != Direction.DOWN;
    }

    @Override public FluidTank[] getAllTanks() { return new FluidTank[] { compair }; }
    @Override public FluidTank[] getSendingTanks() { return new FluidTank[] { compair }; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    // ==================== NBT ====================

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        compair.writeToNBT(tag, "compair");
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        compair.readFromNBT(tag, "compair");
    }

    // ==================== sonstiges ====================

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hbm_m.intake");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return null;
    }

    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX() - 1, worldPosition.getY(), worldPosition.getZ() - 1,
                worldPosition.getX() + 2, worldPosition.getY() + 1, worldPosition.getZ() + 2);
    }
}
