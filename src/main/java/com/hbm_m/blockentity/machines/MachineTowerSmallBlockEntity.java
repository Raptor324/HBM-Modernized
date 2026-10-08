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
 * 1:1 {@code TileEntityTowerSmall} (ein {@code TileEntityCondenser}): verdichtet Abdampf (Tank 0, 1000 mB) ohne
 * Strom zu Wasser (Tank 1, 1000 mB) - pro Tick so viel, wie im Wassertank Platz ist. Nach jeder Umsetzung
 * steigt 20 Ticks lang Dampf aus der Spitze (alle 2 Ticks, 18 Bloecke hoch, Lebensdauer 250-499).
 * Anschluesse: vier feste Rohrzellen drei Felder neben dem Kern (+-X, +-Z, unabhaengig von der Drehung).
 */
public class MachineTowerSmallBlockEntity extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2 {

    /** Tank 0: Abdampf. */
    public static final int TANK_HOT_IN = 0;
    /** Tank 1: Wasser. */
    public static final int TANK_COOLANT_OUT = 1;

    /** Original (konfigurierbar): {@code inputTankSizeTS = outputTankSizeTS = 1_000}. */
    public static int inputTankSizeTS = 1_000;
    public static int outputTankSizeTS = 1_000;

    private final FluidTank[] tanks = new FluidTank[] {
        new FluidTank(ModFluids.SPENTSTEAM.getSource(), inputTankSizeTS),
        new FluidTank(net.minecraft.world.level.material.Fluids.WATER, outputTankSizeTS)
    };

    public int age = 0;
    public int waterTimer = 0;
    protected int throughput;

    /** Zuletzt an den Client geschickter Stand (Original: {@code networkPackNT(150)} jeden Tick). */
    private int sentIn = -1, sentOut = -1, sentTimer = -1;

    public MachineTowerSmallBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TOWER_SMALL_BE.get(), pos, state, 0, 0L, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineTowerSmallBlockEntity be) {
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

        // subscribeToAllAround / sendFluidToAll des TileEntityTowerSmall
        for (Direction dir : CON_DIRS) be.trySubscribe(in.getTankType(), level, pos.relative(dir, 3), dir);
        for (Direction dir : CON_DIRS) be.tryProvide(out, level, pos.relative(dir, 3), dir);

        if (convert > 0) be.setChanged();
        be.syncIfChanged();
    }

    /** Library.POS_X, NEG_X, POS_Z, NEG_Z in Original-Reihenfolge. */
    private static final Direction[] CON_DIRS = { Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH };

    private void syncIfChanged() {
        int i = tanks[0].getFill(), o = tanks[1].getFill();
        if (i != sentIn || o != sentOut || waterTimer != sentTimer) {
            sentIn = i; sentOut = o; sentTimer = waterTimer;
            sendUpdateToClient();
        }
    }

    /** Original {@code TileEntityTowerSmall}: Dampfschwaden aus der Spitze, solange {@code waterTimer > 0}. */
    private void clientTick(Level level, BlockPos pos) {
        if (com.hbm_m.config.ClientConfig.coolingTowerParticles && (this.waterTimer > 0 && level.getGameTime() % 2 == 0)) {
            CompoundTag data = new CompoundTag();
            data.putString("type", "tower");
            data.putFloat("lift", 1F);
            data.putFloat("base", 0.5F);
            data.putFloat("max", 4F);
            data.putInt("life", 250 + level.random.nextInt(250));
            data.putDouble("posX", pos.getX() + 0.5);
            data.putDouble("posZ", pos.getZ() + 0.5);
            data.putDouble("posY", pos.getY() + 18);
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
        setFluidHandler(new TowerSmallFluidHandler(this));
    }
    //?}

    private AABB bb = null;

    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        if (bb == null) bb = new AABB(worldPosition.getX() - 2, worldPosition.getY(), worldPosition.getZ() - 2,
                worldPosition.getX() + 3, worldPosition.getY() + 20, worldPosition.getZ() + 3);
        return bb;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hbm_m.tower_small");
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
