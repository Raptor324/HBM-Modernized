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
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.fluid.trait.FT_Heatable;
import com.hbm_m.sound.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;

/**
 * 1:1-Port von {@code TileEntityFusionBoiler} (1.7.10).
 *
 * <p>Der Boiler nimmt Plasmaleistung ab und wandelt damit Wasser in ueberhitzten Dampf um. Die
 * Umwandlungsrate kommt aus der {@link FT_Heatable}-Eigenschaft des Eingangsfluids
 * ({@code getFirstStep().heatReq} Waermeeinheiten pro mB) - genau wie im Original.</p>
 */
public class FusionBoilerBlockEntity extends FusionSyncedBlockEntity implements IFluidStandardTransceiverMK2, IFusionPowerReceiver {

    private GenNode<PlasmaNetwork> plasmaNode;

    /** Im aktuellen Tick empfangene Plasmaleistung (wird jeden Tick zurueckgesetzt). */
    public long plasmaEnergy;
    /** Wert des Vortticks - nur der wird zum Client gesendet (Original: Batch-Pakete). */
    public long plasmaEnergySync;

    public final FluidTank[] tanks;

    public FusionBoilerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUSION_BOILER_BE.get(), pos, state);
        this.tanks = new FluidTank[] {
                new FluidTank(ModFluids.WATER.getSource(), 32_000),
                new FluidTank(ModFluids.SUPERHOTSTEAM.getSource(), 32_000)
        };
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FusionBoilerBlockEntity be) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        be.serverTick(serverLevel, pos, state);
    }

    private void serverTick(ServerLevel level, BlockPos pos, BlockState state) {

        this.plasmaEnergySync = this.plasmaEnergy;
        this.plasmaEnergy = 0;

        for (NodeDirPos con : getConPos(pos, state)) {
            if (tanks[0].getTankType() != Fluids.EMPTY) {
                trySubscribe(tanks[0].getTankType(), level, con.getPos(), con.getDir());
            }
            if (tanks[1].getFill() > 0) tryProvide(tanks[1], level, con.getPos(), con.getDir());
        }

        if (plasmaNode == null || plasmaNode.expired) {
            Direction dir = state.getValue(FusionMultiblockBlock.FACING).getOpposite();
            BlockPos nodePos = pos.offset(dir.getStepX() * 4, 2, dir.getStepZ() * 4);
            plasmaNode = UniNodespace.getNode(level, nodePos, PlasmaNetworkProvider.THE_PROVIDER);

            if (plasmaNode == null) {
                plasmaNode = new GenNode<>(PlasmaNetworkProvider.THE_PROVIDER, nodePos)
                        .setConnections(new NodeDirPos(pos.offset(dir.getStepX() * 5, 2, dir.getStepZ() * 5), dir));
                UniNodespace.createNode(level, plasmaNode);
            }
        }

        if (plasmaNode != null && plasmaNode.hasValidNet()) plasmaNode.net.addReceiver(this);

        setChanged();
        sendUpdateToClient();
    }

    /** Original: {@code getConPos()} - vier Rohranschluesse an den Ecken des Kessels. */
    public NodeDirPos[] getConPos(BlockPos pos, BlockState state) {
        Direction dir = state.getValue(FusionMultiblockBlock.FACING);
        Direction rot = dir.getClockWise();

        return new NodeDirPos[] {
                new NodeDirPos(pos.offset(-dir.getStepX() + rot.getStepX() * 2, 0, -dir.getStepZ() + rot.getStepZ() * 2), rot),
                new NodeDirPos(pos.offset(-dir.getStepX() - rot.getStepX() * 2, 0, -dir.getStepZ() - rot.getStepZ() * 2), rot.getOpposite()),
                new NodeDirPos(pos.offset(dir.getStepX() * 2 + rot.getStepX() * 2, 0, dir.getStepZ() * 2 + rot.getStepZ() * 2), rot),
                new NodeDirPos(pos.offset(dir.getStepX() * 2 - rot.getStepX() * 2, 0, dir.getStepZ() * 2 - rot.getStepZ() * 2), rot.getOpposite())
        };
    }

    @Override
    public boolean receivesFusionPower() {
        return true;
    }

    @Override
    public void receiveFusionPower(long fusionPower, double neutronPower, float r, float g, float b) {
        this.plasmaEnergy = fusionPower;

        FT_Heatable trait = FluidType.getTrait(tanks[0].getStoredFluid(), FT_Heatable.class);
        if (trait == null) return;
        FT_Heatable.HeatingStep step = trait.getFirstStep();
        if (step == null || step.heatReq <= 0) return;

        int waterCycles = Math.min(tanks[0].getFill(), tanks[1].getMaxFill() - tanks[1].getFill());
        // Original: das Math.min steckt bewusst schon im Cast, damit bei absurd hoher
        // Fusionsleistung kein long->int-Ueberlauf entsteht.
        int steamCycles = (int) Math.min(fusionPower / step.heatReq, waterCycles);

        if (steamCycles > 0) {
            tanks[0].setFill(tanks[0].getFill() - steamCycles);
            tanks[1].setFill(tanks[1].getFill() + steamCycles);

            if (level != null && level.random.nextInt(200) == 0) {
                level.playSound(null, worldPosition, ModSounds.BOILER_GROAN.get(), SoundSource.BLOCKS, 2.5F, 1.0F);
            }
        }
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
    }

    @Override
    protected void readNbtData(CompoundTag tag, HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        if (tag.contains("t0")) tanks[0].readNBT(tag.getCompound("t0"));
        if (tag.contains("t1")) tanks[1].readNBT(tag.getCompound("t1"));
        this.plasmaEnergySync = tag.getLong("plasma");
    }

    private AABB renderBounds = null;

    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        if (renderBounds == null) {
            renderBounds = new AABB(
                    worldPosition.getX() - 4, worldPosition.getY(), worldPosition.getZ() - 4,
                    worldPosition.getX() + 5, worldPosition.getY() + 4, worldPosition.getZ() + 5);
        }
        return renderBounds;
    }
}
