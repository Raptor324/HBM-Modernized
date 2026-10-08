package com.hbm_m.blockentity.machines;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
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
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code TileEntityTowerLarge} (ein {@code TileEntityCondenser}): verdichtet Abdampf (Tank 0, 10000 mB) ohne
 * Strom zu Wasser (Tank 1, 10000 mB) - pro Tick so viel, wie im Wassertank Platz ist. Nach jeder Umsetzung
 * steigt 20 Ticks lang Dampf auf (alle 4 Ticks, Lebensdauer 750-999).
 * Anschluesse: zwoelf feste Rohrzellen fuenf Felder neben dem Kern, je Seite mittig und +-3 quer.
 */
public class MachineCoolingTowerBlockEntity extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2 {

    /** Tank 0: Abdampf. */
    public static final int TANK_HOT_IN = 0;
    /** Tank 1: Wasser. */
    public static final int TANK_COOLANT_OUT = 1;

    /** Original (konfigurierbar): {@code inputTankSizeTL = outputTankSizeTL = 10_000}. */
    public static int inputTankSizeTL = 10_000;
    public static int outputTankSizeTL = 10_000;

    private final FluidTank[] tanks = new FluidTank[] {
        new FluidTank(ModFluids.SPENTSTEAM.getSource(), inputTankSizeTL),
        new FluidTank(net.minecraft.world.level.material.Fluids.WATER, outputTankSizeTL)
    };

    public int age = 0;
    public int waterTimer = 0;
    protected int throughput;

    /** Zuletzt an den Client geschickter Stand (Original: {@code networkPackNT(150)} jeden Tick). */
    private int sentIn = -1, sentOut = -1, sentTimer = -1;

    public MachineCoolingTowerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COOLING_TOWER_BE.get(), pos, state, 0, 0L, 0L);
    }

    /** ForgeDirection 2..5: NORTH, SOUTH, WEST, EAST. */
    private static final Direction[] DIRS = { Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST };

    public static void tick(Level level, BlockPos pos, BlockState state, MachineCoolingTowerBlockEntity be) {
        if (level.isClientSide()) {
            be.clientTick(level, pos);
            return;
        }

        // 1:1 TileEntityCondenser.updateEntity
        be.age++;
        if (be.age >= 2) be.age = 0;

        if (be.waterTimer > 0) be.waterTimer--;

        FluidTank in = be.tanks[TANK_HOT_IN];
        FluidTank out = be.tanks[TANK_COOLANT_OUT];

        int convert = Math.min(in.getFill(), out.getMaxFill() - out.getFill());
        be.throughput = convert;

        in.setFill(in.getFill() - convert);
        if (convert > 0) be.waterTimer = 20;
        out.setFill(out.getFill() + convert);

        // subscribeToAllAround des TileEntityTowerLarge
        for (Direction dir : DIRS) {
            Direction rot = dir.getClockWise(); // dir.getRotation(UP); +-3 ist symmetrisch
            BlockPos p = pos.relative(dir, 5);
            be.trySubscribe(in.getTankType(), level, p, dir);
            be.trySubscribe(in.getTankType(), level, p.relative(rot, 3), dir);
            be.trySubscribe(in.getTankType(), level, p.relative(rot, -3), dir);
        }
        // sendFluidToAll des TileEntityTowerLarge
        for (Direction dir : DIRS) {
            Direction rot = dir.getClockWise();
            BlockPos p = pos.relative(dir, 5);
            be.tryProvide(out, level, p, dir);
            be.tryProvide(out, level, p.relative(rot, 3), dir);
            be.tryProvide(out, level, p.relative(rot, -3), dir);
        }

        if (convert > 0) be.setChanged();
        be.syncIfChanged();
    }

    private void syncIfChanged() {
        int i = tanks[0].getFill(), o = tanks[1].getFill();
        if (i != sentIn || o != sentOut || waterTimer != sentTimer) {
            sentIn = i; sentOut = o; sentTimer = waterTimer;
            sendUpdateToClient();
        }
    }

    /** Original {@code TileEntityTowerLarge}: Dampfschwaden, solange {@code waterTimer > 0}. */
    private void clientTick(Level level, BlockPos pos) {
        if (com.hbm_m.config.ClientConfig.coolingTowerParticles && (this.waterTimer > 0 && level.getGameTime() % 4 == 0)) {
            CompoundTag data = new CompoundTag();
            data.putString("type", "tower");
            data.putFloat("lift", 0.5F);
            data.putFloat("base", 1F);
            data.putFloat("max", 10F);
            data.putInt("life", 750 + level.random.nextInt(250));
            data.putDouble("posX", pos.getX() + 0.5 + level.random.nextDouble() * 3 - 1.5);
            data.putDouble("posZ", pos.getZ() + 0.5 + level.random.nextDouble() * 3 - 1.5);
            data.putDouble("posY", pos.getY() + 1);
            com.hbm_m.particle.helper.ParticleEffectClient.effectNT(data);
        }
    }

    public FluidTank[] getTanks() {
        return tanks;
    }

    public FluidTank getTank(int index) {
        return (index >= 0 && index < tanks.length) ? tanks[index] : tanks[0];
    }

    /** Arbeitet gerade (Original: {@code waterTimer > 0}). */
    public boolean isCooling() {
        return waterTimer > 0;
    }

    public int getThroughput() {
        return throughput;
    }

    // ═══════════════════════════ IFluidStandardTransceiverMK2 ════════════════════════════════

    @Override
    public FluidTank[] getReceivingTanks() {
        return new FluidTank[] { tanks[TANK_HOT_IN] };
    }

    @Override
    public FluidTank[] getSendingTanks() {
        return new FluidTank[] { tanks[TANK_COOLANT_OUT] };
    }

    @Override
    public FluidTank[] getAllTanks() {
        return tanks;
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        // Original-Schluessel: "water" = Eingang, "steam" = Ausgang
        tanks[0].writeToNBT(tag, "water");
        tanks[1].writeToNBT(tag, "steam");
        tag.putByte("waterTimer", (byte) waterTimer);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        tanks[0].readFromNBT(tag, "water");
        tanks[1].readFromNBT(tag, "steam");
        waterTimer = tag.getByte("waterTimer");
    }

    //? if forge {
    @Override
    protected void setupFluidCapability() {
        setFluidHandler(new CoolingTowerFluidHandler(this));
    }
    //?}

    private AABB bb = null;

    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        if (bb == null) bb = new AABB(worldPosition.getX() - 4, worldPosition.getY(), worldPosition.getZ() - 4,
                worldPosition.getX() + 5, worldPosition.getY() + 13, worldPosition.getZ() + 5);
        return bb;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hbm_m.cooling_tower");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        // Original: kein GUI
        return null;
    }
}
