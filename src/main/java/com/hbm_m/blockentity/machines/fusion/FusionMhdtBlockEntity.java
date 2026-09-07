package com.hbm_m.blockentity.machines.fusion;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.api.fusion.IFusionPowerReceiver;
import com.hbm_m.api.fusion.PlasmaNetwork;
import com.hbm_m.api.fusion.PlasmaNetworkProvider;
import com.hbm_m.api.network.GenNode;
import com.hbm_m.api.network.NodeDirPos;
import com.hbm_m.api.network.UniNodespace;
import com.hbm_m.block.machines.fusion.FusionMultiblockBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.interfaces.IEnergyProvider;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;

/**
 * 1:1-Port von {@code TileEntityFusionMHDT} (1.7.10) - die magnetohydrodynamische Turbine.
 *
 * <p>Sie wandelt Plasmaleistung direkt in Strom um ({@link #PLASMA_EFFICIENCY}), braucht dafuer
 * aber staendig Kuehlmittel. Unterhalb von {@link #MINIMUM_PLASMA} halbiert sich der Ertrag;
 * ohne Kuehlung faellt er komplett aus.</p>
 */
public class FusionMhdtBlockEntity extends FusionSyncedBlockEntity
        implements IEnergyProvider, IFluidStandardTransceiverMK2, IFusionPowerReceiver {

    private GenNode<PlasmaNetwork> plasmaNode;

    public long plasmaEnergy;
    public long plasmaEnergySync;
    public long power;

    // Rendering (Client)
    public float rotor;
    public float prevRotor;
    public float rotorSpeed;
    public static final float ROTOR_ACCELERATION = 0.125F;

    public static final double PLASMA_EFFICIENCY = 1.35D;
    public static final int COOLANT_USE = 50;
    /** Original: ueber {@code IConfigurableMachine} konfigurierbar ({@code mhd-turbine.json}). */
    public static long MINIMUM_PLASMA = 5_000_000L;

    public final FluidTank[] tanks;

    public FusionMhdtBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUSION_MHDT_BE.get(), pos, state);
        this.tanks = new FluidTank[] {
                new FluidTank(ModFluids.PERFLUOROMETHYL_COLD.getSource(), 4_000),
                new FluidTank(ModFluids.PERFLUOROMETHYL.getSource(), 4_000)
        };
    }

    public boolean hasMinimumPlasma() {
        return this.plasmaEnergy >= MINIMUM_PLASMA;
    }

    public boolean isCool() {
        return tanks[0].getFill() >= COOLANT_USE && tanks[1].getFill() + COOLANT_USE <= tanks[1].getMaxFill();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FusionMhdtBlockEntity be) {
        if (level instanceof ServerLevel serverLevel) {
            be.serverTick(serverLevel, pos, state);
        } else {
            be.clientTick();
        }
    }

    private void serverTick(ServerLevel level, BlockPos pos, BlockState state) {

        this.plasmaEnergySync = this.plasmaEnergy;

        if (isCool()) {
            this.power = (long) Math.floor(this.plasmaEnergy * PLASMA_EFFICIENCY);
            if (!this.hasMinimumPlasma()) this.power /= 2;
            tanks[0].setFill(tanks[0].getFill() - COOLANT_USE);
            tanks[1].setFill(tanks[1].getFill() + COOLANT_USE);
        }

        for (NodeDirPos con : getConPos(pos, state)) {
            tryProvide(level, con.getX(), con.getY(), con.getZ(), con.getDir());
            if (tanks[0].getTankType() != Fluids.EMPTY) {
                trySubscribe(tanks[0].getTankType(), level, con.getPos(), con.getDir());
            }
            if (tanks[1].getFill() > 0) tryProvide(tanks[1], level, con.getPos(), con.getDir());
        }

        if (plasmaNode == null || plasmaNode.expired) {
            Direction dir = state.getValue(FusionMultiblockBlock.FACING).getOpposite();
            BlockPos nodePos = pos.offset(dir.getStepX() * 6, 2, dir.getStepZ() * 6);
            plasmaNode = UniNodespace.getNode(level, nodePos, PlasmaNetworkProvider.THE_PROVIDER);

            if (plasmaNode == null) {
                plasmaNode = new GenNode<>(PlasmaNetworkProvider.THE_PROVIDER, nodePos)
                        .setConnections(new NodeDirPos(pos.offset(dir.getStepX() * 7, 2, dir.getStepZ() * 7), dir));
                UniNodespace.createNode(level, plasmaNode);
            }
        }

        if (plasmaNode != null && plasmaNode.hasValidNet()) plasmaNode.net.addReceiver(this);

        setChanged();
        sendUpdateToClient();
        this.plasmaEnergy = 0;
    }

    private void clientTick() {
        if (this.plasmaEnergy > 0 && isCool()) this.rotorSpeed += ROTOR_ACCELERATION;
        else this.rotorSpeed -= ROTOR_ACCELERATION;

        this.rotorSpeed = Mth.clamp(this.rotorSpeed, 0F, hasMinimumPlasma() ? 15F : 10F);

        this.prevRotor = this.rotor;
        this.rotor += this.rotorSpeed;

        if (this.rotor >= 360F) {
            this.rotor -= 360F;
            this.prevRotor -= 360F;
        }
    }

    /** Original: {@code getConPos()} - zwei seitliche Kuehlmittelanschluesse und der Stromausgang. */
    public NodeDirPos[] getConPos(BlockPos pos, BlockState state) {
        Direction dir = state.getValue(FusionMultiblockBlock.FACING);
        Direction rot = dir.getClockWise();

        return new NodeDirPos[] {
                new NodeDirPos(pos.offset(dir.getStepX() * 4 + rot.getStepX() * 4, 0, dir.getStepZ() * 4 + rot.getStepZ() * 4), rot),
                new NodeDirPos(pos.offset(dir.getStepX() * 4 - rot.getStepX() * 4, 0, dir.getStepZ() * 4 - rot.getStepZ() * 4), rot.getOpposite()),
                new NodeDirPos(pos.offset(dir.getStepX() * 8, 1, dir.getStepZ() * 8), dir)
        };
    }

    @Override
    public boolean receivesFusionPower() {
        return true;
    }

    @Override
    public void receiveFusionPower(long fusionPower, double neutronPower, float r, float g, float b) {
        this.plasmaEnergy = fusionPower;
    }

    // --- Energie (Original: IEnergyProviderMK2, Puffer == Erzeugung dieses Ticks) ---

    @Override public long getEnergyStored() { return power; }
    @Override public void setEnergyStored(long energy) { this.power = energy; }
    @Override public long getMaxEnergyStored() { return power; }
    @Override public long getProvideSpeed() { return power; }
    @Override public boolean canExtract() { return true; }
    @Override public boolean canConnectEnergy(Direction side) { return true; }

    @Override
    public long extractEnergy(long maxExtract, boolean simulate) {
        long extracted = Math.min(maxExtract, power);
        if (!simulate) power -= extracted;
        return extracted;
    }

    @Override
    public FluidTank[] getAllTanks() { return tanks; }

    @Override
    public FluidTank[] getReceivingTanks() { return new FluidTank[] { tanks[0] }; }

    @Override
    public FluidTank[] getSendingTanks() { return new FluidTank[] { tanks[1] }; }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level instanceof ServerLevel serverLevel && plasmaNode != null) {
            UniNodespace.destroyNode(serverLevel, plasmaNode);
        }
    }

    @Override
    protected void writeNbtData(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.put("t0", tanks[0].writeNBT(new CompoundTag()));
        tag.put("t1", tanks[1].writeNBT(new CompoundTag()));
        tag.putLong("plasma", plasmaEnergySync);
        tag.putLong("power", power);
    }

    @Override
    protected void readNbtData(CompoundTag tag, HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        if (tag.contains("t0")) tanks[0].readNBT(tag.getCompound("t0"));
        if (tag.contains("t1")) tanks[1].readNBT(tag.getCompound("t1"));
        this.plasmaEnergy = tag.getLong("plasma");
        this.plasmaEnergySync = this.plasmaEnergy;
        this.power = tag.getLong("power");
    }

    private AABB renderBounds = null;

    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        if (renderBounds == null) {
            renderBounds = new AABB(
                    worldPosition.getX() - 7, worldPosition.getY(), worldPosition.getZ() - 7,
                    worldPosition.getX() + 8, worldPosition.getY() + 4, worldPosition.getZ() + 8);
        }
        return renderBounds;
    }
}
