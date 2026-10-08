package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardReceiverMK2;
import com.hbm_m.block.machines.MachineChimneyBlock;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.handler.pollution.PollutionHandler;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.fluid.trait.PollutionType;

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
 * 1:1 {@code TileEntityChimneyBase}/{@code Brick}/{@code Industrial}: nimmt an den vier Seitenzellen Rauch (normal,
 * verbleit, giftig) in beliebiger Menge an und gibt ihn sofort als Verschmutzung ab - der Ziegelschornstein mit
 * Faktor 0,25, der Industrieschornstein mit 0,1. Ein Aschekasten direkt darunter faengt Flugasche (beide) bzw. Russ
 * (nur Industrie) im Verhaeltnis zur Rauchmenge auf. Solange Rauch kommt, qualmt die Spitze.
 */
public class MachineChimneyBlockEntity extends BaseMachineBlockEntity implements IFluidStandardReceiverMK2 {

    public long ashTick = 0;
    public long sootTick = 0;
    public int onTicks;

    public MachineChimneyBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHIMNEY_BE.get(), pos, state, 0, 0L, 0L, 0L);
    }

    private boolean industrial() {
        return getBlockState().getBlock() instanceof MachineChimneyBlock c && c.isIndustrial();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineChimneyBlockEntity be) {
        if (!level.isClientSide()) be.serverTick(level, pos);
        else if (be.onTicks > 0) be.spawnParticles(level, pos);
    }

    private static boolean isSmoke(Fluid f) {
        return f == ModFluids.SMOKE.getSource() || f == ModFluids.SMOKE_LEADED.getSource() || f == ModFluids.SMOKE_POISON.getSource();
    }

    private void serverTick(Level level, BlockPos pos) {

        if (level.getGameTime() % 20 == 0) {
            Fluid[] types = { ModFluids.SMOKE.getSource(), ModFluids.SMOKE_LEADED.getSource(), ModFluids.SMOKE_POISON.getSource() };
            for (Fluid type : types) {
                this.trySubscribe(type, level, pos.offset(2, 0, 0), Direction.EAST);
                this.trySubscribe(type, level, pos.offset(-2, 0, 0), Direction.WEST);
                this.trySubscribe(type, level, pos.offset(0, 0, 2), Direction.SOUTH);
                this.trySubscribe(type, level, pos.offset(0, 0, -2), Direction.NORTH);
            }
        }

        if (ashTick > 0 || sootTick > 0) {
            if (level.getBlockEntity(pos.below()) instanceof MachineAshpitBlockEntity ashpit) {
                ashpit.addAsh(MachineAshpitBlockEntity.AshType.FLY, (int) ashTick);
                ashpit.addAsh(MachineAshpitBlockEntity.AshType.SOOT, (int) sootTick);
            }
            this.ashTick = 0;
            this.sootTick = 0;
        }

        sendUpdateToClient();

        if (onTicks > 0) onTicks--;
    }

    private void spawnParticles(Level level, BlockPos pos) {
        if (level.getGameTime() % 2 == 0) {
            boolean ind = industrial();
            CompoundTag fx = new CompoundTag();
            fx.putString("type", "tower");
            fx.putFloat("lift", 10F);
            fx.putFloat("base", ind ? 0.75F : 0.5F);
            fx.putFloat("max", 3F);
            fx.putInt("life", 250 + level.random.nextInt(50));
            fx.putInt("color", 0x404040);
            fx.putDouble("posX", pos.getX() + 0.5);
            fx.putDouble("posY", pos.getY() + (ind ? 22 : 12));
            fx.putDouble("posZ", pos.getZ() + 0.5);
            com.hbm_m.particle.helper.ParticleEffectClient.effectNT(fx);
        }
    }

    /** Original: Ziegel 0,25, Industrie 0,1; im Rampant-Modus {@code rampantSmokeStackOverride} (Industrie halbiert). */
    public double getPollutionMod() {
        boolean rampant = com.hbm_m.config.ModClothConfig.get().rampantMode;
        double override = com.hbm_m.config.MobConfig.rampantSmokeStackOverride();
        if (industrial()) return rampant ? override / 2 : 0.1D;
        return rampant ? override : 0.25D;
    }

    // ==================== Fluid ====================

    @Override
    public long getDemand(Fluid fluid, int pressure) {
        return isSmoke(fluid) ? 1_000_000 : 0;
    }

    @Override
    public long transferFluid(Fluid type, int pressure, long fluid) {
        if (!isSmoke(type) || level == null) return fluid;

        onTicks = 20;

        ashTick += fluid;
        if (industrial()) sootTick += fluid;

        double amount = fluid * getPollutionMod();

        if (type == ModFluids.SMOKE.getSource()) PollutionHandler.incrementPollution(level, worldPosition, PollutionType.SOOT, (float) (amount / 100F));
        if (type == ModFluids.SMOKE_LEADED.getSource()) PollutionHandler.incrementPollution(level, worldPosition, PollutionType.HEAVYMETAL, (float) (amount / 100F));
        if (type == ModFluids.SMOKE_POISON.getSource()) PollutionHandler.incrementPollution(level, worldPosition, PollutionType.POISON, (float) (amount / 100F));

        setChanged();
        return 0;
    }

    @Override public FluidTank[] getAllTanks() { return new FluidTank[0]; }
    @Override public FluidTank[] getReceivingTanks() { return new FluidTank[0]; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        return fromDir != null && fromDir.getAxis().isHorizontal() && isSmoke(fluid);
    }

    // ==================== NBT ====================

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putInt("onTicks", onTicks);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        onTicks = tag.getInt("onTicks");
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.chimney");
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

    /** Original: 3x13x3 (Ziegel) bzw. 3x23x3 (Industrie). */
    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        int h = industrial() ? 23 : 13;
        return new AABB(worldPosition.getX() - 1, worldPosition.getY(), worldPosition.getZ() - 1,
                worldPosition.getX() + 2, worldPosition.getY() + h, worldPosition.getZ() + 2);
    }
}
