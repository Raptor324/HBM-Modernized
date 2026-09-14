package com.hbm_m.blockentity.machines.fusion;

import java.util.Map.Entry;

import com.hbm_m.api.fluids.IFluidStandardReceiverMK2;
import com.hbm_m.api.fusion.KlystronNetwork;
import com.hbm_m.api.fusion.KlystronNetworkProvider;
import com.hbm_m.api.network.GenNode;
import com.hbm_m.api.network.NodeDirPos;
import com.hbm_m.api.network.UniNodespace;
import com.hbm_m.block.machines.fusion.FusionMultiblockBlock;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.MachineFusionKlystronMenu;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1-Port von {@code TileEntityFusionKlystron} (1.7.10).
 *
 * <p>Das Klystron wandelt Strom und Druckluft in "Klystronenergie" (KyU) um und speist sie ueber
 * das Klystronnetz in den Torus. Der Spieler stellt die gewuenschte Ausgangsleistung ein
 * ({@link #outputTarget}); der Energiepuffer wird daraus abgeleitet
 * ({@code max(1.000.000, outputTarget * 100)}).</p>
 */
public class FusionKlystronBlockEntity extends BaseMachineBlockEntity implements IFluidStandardReceiverMK2 {

    public static final int SLOT_BATTERY = 0;
    public static final int INVENTORY_SIZE = 1;

    public static final long MAX_OUTPUT = 1_000_000L;
    public static final int AIR_CONSUMPTION = 2_500;

    private GenNode<KlystronNetwork> klystronNode;

    public long outputTarget;
    public long output;

    // Rendering (Client)
    public float fan;
    public float prevFan;
    public float fanSpeed;
    public static final float FAN_ACCELERATION = 0.125F;

    public final FluidTank compair;

    public FusionKlystronBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUSION_KLYSTRON_BE.get(), pos, state, INVENTORY_SIZE, 1_000_000L, Long.MAX_VALUE, 0L);
        this.compair = new FluidTank(ModFluids.AIR.getSource(), AIR_CONSUMPTION * 60);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FusionKlystronBlockEntity be) {
        if (level instanceof ServerLevel serverLevel) {
            be.serverTick(serverLevel, pos, state);
        } else {
            be.clientTick();
        }
    }

    private void serverTick(ServerLevel level, BlockPos pos, BlockState state) {

        ensureNetworkInitialized();

        setEnergyCapacity(Math.max(1_000_000L, this.outputTarget * 100L));

        chargeFromBatterySlot(SLOT_BATTERY);

        for (NodeDirPos con : getConPos(pos, state)) {
            trySubscribe(level, con.getX(), con.getY(), con.getZ(), con.getDir());
            trySubscribe(compair.getTankType(), level, con.getPos(), con.getDir());
        }

        this.output = 0;

        double powerFactor = FusionTorusBlockEntity.getSpeedScaled(getMaxEnergyStored(), getEnergyStored());
        double airFactor = FusionTorusBlockEntity.getSpeedScaled(compair.getMaxFill(), compair.getFill());
        double factor = Math.min(powerFactor, airFactor);

        long powerReq = (long) Math.ceil(outputTarget * factor);
        int airReq = (int) Math.ceil(AIR_CONSUMPTION * factor);

        if (outputTarget > 0 && getEnergyStored() >= powerReq && compair.getFill() >= airReq) {
            this.output = powerReq;
            setEnergyStored(getEnergyStored() - powerReq);
            this.compair.setFill(this.compair.getFill() - airReq);
        }

        if (output < outputTarget / 50) output = 0;

        this.klystronNode = handleKlystronNode(this.klystronNode, this, level, pos, state);
        provideKyU(this.klystronNode, this.output);

        setChanged();
        sendUpdateToClient();
    }

    private void clientTick() {
        double mult = FusionTorusBlockEntity.getSpeedScaled(outputTarget, output);
        if (this.output > 0) this.fanSpeed += FAN_ACCELERATION * mult;
        else this.fanSpeed -= FAN_ACCELERATION;

        this.fanSpeed = Mth.clamp(this.fanSpeed, 0F, 5F * (float) mult);

        this.prevFan = this.fan;
        this.fan += this.fanSpeed;

        if (this.fan >= 360F) {
            this.fan -= 360F;
            this.prevFan -= 360F;
        }
    }

    /**
     * 1:1-Port von {@code TileEntityFusionKlystron.handleKNode}: sorgt dafuer, dass der K-Knoten
     * existiert und der Anbieter darin eingetragen ist. Wird auch vom Kreativ-Klystron benutzt.
     */
    public static GenNode<KlystronNetwork> handleKlystronNode(GenNode<KlystronNetwork> node, BlockEntity that,
                                                             ServerLevel level, BlockPos pos, BlockState state) {

        if (node == null || node.expired) {
            Direction dir = state.getValue(FusionMultiblockBlock.FACING).getOpposite();
            BlockPos nodePos = pos.offset(dir.getStepX() * 4, 2, dir.getStepZ() * 4);
            node = UniNodespace.getNode(level, nodePos, KlystronNetworkProvider.THE_PROVIDER);

            if (node == null) {
                node = new GenNode<>(KlystronNetworkProvider.THE_PROVIDER, nodePos)
                        .setConnections(new NodeDirPos(pos.offset(dir.getStepX() * 5, 2, dir.getStepZ() * 5), dir));
                UniNodespace.createNode(level, node);
            }
        }

        if (node.net != null) node.net.addProvider(that);
        return node;
    }

    /**
     * 1:1-Port von {@code TileEntityFusionKlystron.provideKyU}: schiebt die Energie in den Torus,
     * der als Empfaenger im K-Netz haengt. Gibt true zurueck, wenn eine Verbindung besteht.
     */
    public static boolean provideKyU(GenNode<KlystronNetwork> node, long output) {
        boolean connected = false;

        if (node != null && node.net != null) {
            KlystronNetwork net = node.net;

            for (Entry<BlockEntity, Long> e : net.receiverEntries.entrySet()) {
                // Original: hier steht ein harter instanceof-Check auf den Torus mit dem Kommentar,
                // das durch ein Interface zu ersetzen, sollte es je mehr Abnehmer geben.
                if (e.getKey() instanceof FusionTorusBlockEntity torus) {
                    if (torus.isLoaded() && !torus.isRemoved()) {
                        torus.klystronEnergy += output;
                        connected = true;
                        break; // wir bedienen ohnehin nur einen
                    }
                }
            }
        }

        return connected;
    }

    /** Original: {@code getConPos()} - Plasmaseite plus zwei seitliche Anschluesse. */
    public NodeDirPos[] getConPos(BlockPos pos, BlockState state) {
        Direction dir = state.getValue(FusionMultiblockBlock.FACING);
        Direction rot = dir.getClockWise();

        return new NodeDirPos[] {
                new NodeDirPos(pos.offset(dir.getStepX() * 4, 2, dir.getStepZ() * 4), dir),
                new NodeDirPos(pos.offset(rot.getStepX() * 3, 0, rot.getStepZ() * 3), rot),
                new NodeDirPos(pos.offset(-rot.getStepX() * 3, 0, -rot.getStepZ() * 3), rot.getOpposite())
        };
    }

    /** Original: {@code receiveControl} - der Zielwert wird im GUI eingestellt und geklemmt. */
    public void setOutputTarget(long amount) {
        this.outputTarget = Math.max(0L, Math.min(amount, MAX_OUTPUT));
        setChanged();
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == SLOT_BATTERY; // Original: nur der Batterieslot
    }

    @Override
    public FluidTank[] getAllTanks() { return new FluidTank[] { compair }; }

    @Override
    public FluidTank[] getReceivingTanks() { return new FluidTank[] { compair }; }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level instanceof ServerLevel serverLevel && klystronNode != null) {
            UniNodespace.destroyNode(serverLevel, klystronNode);
        }
    }

    @Override
    protected void writeNbtData(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putLong("outputTarget", outputTarget);
        tag.putLong("output", output);
        tag.put("compair", compair.writeNBT(new CompoundTag()));
    }

    @Override
    protected void readNbtData(CompoundTag tag, HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        this.outputTarget = tag.getLong("outputTarget");
        this.output = tag.getLong("output");
        if (tag.contains("compair")) compair.readNBT(tag.getCompound("compair"));
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.fusion_klystron");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineFusionKlystronMenu.create(id, inventory, this);
    }

    private AABB renderBounds = null;

    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        if (renderBounds == null) {
            renderBounds = new AABB(
                    worldPosition.getX() - 4, worldPosition.getY(), worldPosition.getZ() - 4,
                    worldPosition.getX() + 5, worldPosition.getY() + 5, worldPosition.getZ() + 5);
        }
        return renderBounds;
    }
}
