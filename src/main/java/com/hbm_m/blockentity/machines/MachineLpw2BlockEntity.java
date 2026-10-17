package com.hbm_m.blockentity.machines;

import com.hbm_m.api.fluids.IFluidStandardReceiverMK2;
import com.hbm_m.api.space.SpaceApi;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.tank.FluidTank;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code TileEntityMachineLPW2}: ohne Addon ohne Logik, nur die grosse Renderbox fuer den animierten
 * {@code Lpw2Renderer} (Master-Stand).
 *
 * <p>Space-API (W-R6B-1): mit aktiven {@link com.hbm_m.api.space.Lpw2Hooks} (NTM Space: Stationstriebwerk) tickt der
 * Block-Entity, fuehrt die Tanks des Addons ({@link #tanks}, Fluid-Netz ueber die Anschlusszellen), {@link #isOn} und die
 * Animationswerte {@link #time}/{@link #lastTime}/{@link #speed}; freie Zusatzdaten in {@link #addonData}, Laufzeitobjekt
 * des Addons in {@link #addonState}. Gespeichert/synchronisiert werden Tanks, {@code isOn}, {@code addonData}.
 */
public class MachineLpw2BlockEntity extends com.hbm_m.blockentity.BaseHbmBlockEntity implements IFluidStandardReceiverMK2 {

    /** Addon-Tanks (ohne Addon leer -> kein Bedarf im Fluid-Netz). */
    public final FluidTank[] tanks;
    public boolean isOn;
    /** Space-Animation (Client): {@code lastTime = time; time += speed}. */
    public double time, lastTime, speed;
    /** Gespeicherte/synchronisierte Zusatzdaten des Addons. */
    public CompoundTag addonData = new CompoundTag();
    /** Nicht gespeichertes Laufzeitobjekt des Addons (z.B. Triebwerkslogik, Klang). */
    public Object addonState;

    public MachineLpw2BlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LPW2_BE.get(), pos, state);
        FluidTank[] t = SpaceApi.lpw2().active() ? SpaceApi.lpw2().createTanks() : null;
        this.tanks = t != null ? t : new FluidTank[0];
    }

    /** Nur mit aktiven Haken angemeldet ({@code MachineLpw2Block.getTicker}). */
    public static void tick(Level level, BlockPos pos, BlockState state, MachineLpw2BlockEntity be) {
        if (level.isClientSide) SpaceApi.lpw2().clientTick(be);
        else SpaceApi.lpw2().serverTick(be);
    }

    /** W-R9B4-1: Abmelden beim Addon (nur aktiv). */
    @Override
    public void setRemoved() {
        super.setRemoved();
        if (SpaceApi.lpw2().active()) SpaceApi.lpw2().onRemoved(this);
    }

    /** W-R9B4-1: Forge {@code IForgeBlockEntity} / NeoForge {@code IBlockEntityExtension}.onChunkUnloaded. */
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (SpaceApi.lpw2().active()) SpaceApi.lpw2().onRemoved(this);
    }

    /** Speichern + Block-Update an die Clients (Tanks, {@code isOn}, {@code addonData}). */
    public void markForSync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    /** Animationszeit fuer den Renderer: aktiv {@code lastTime..time}, sonst Weltzeit (Master). */
    public double animationTime(long worldTime, float interp) {
        if (!SpaceApi.lpw2().active()) return worldTime + interp;
        return lastTime + (time - lastTime) * interp;
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        if (tanks.length == 0 && !isOn && addonData.isEmpty()) return;
        for (int i = 0; i < tanks.length; i++) tanks[i].writeToNBT(tag, "t" + i);
        tag.putBoolean("isOn", isOn);
        if (!addonData.isEmpty()) tag.put("addon", addonData);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        for (int i = 0; i < tanks.length; i++) if (tag.contains("t" + i)) tanks[i].readFromNBT(tag, "t" + i);
        isOn = tag.getBoolean("isOn");
        addonData = tag.getCompound("addon");
    }

    @Override
    public FluidTank[] getReceivingTanks() {
        return tanks;
    }

    @Override
    public FluidTank[] getAllTanks() {
        return tanks;
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    private AABB bb = null;

    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        if (bb == null) bb = new AABB(worldPosition.getX() - 10, worldPosition.getY(), worldPosition.getZ() - 10,
                worldPosition.getX() + 11, worldPosition.getY() + 7, worldPosition.getZ() + 11);
        return bb;
    }
}
