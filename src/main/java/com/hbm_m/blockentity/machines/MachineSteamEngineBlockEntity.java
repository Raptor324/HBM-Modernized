package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.fluid.trait.FT_Coolable;
import com.hbm_m.inventory.fluid.trait.FT_Coolable.CoolingType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code TileEntitySteamEngine}: setzt Dampf ueber {@code FT_Coolable} (TURBINE, Wirkungsgrad 85 %) in Altdampf um
 * und gibt die Energie im selben Tick ab - der Puffer wird jeden Tick geleert, die Maschine speichert nichts. Das
 * Schwungrad beschleunigt bei Betrieb um 0,1 je Tick (bis 40) und laeuft sonst aus; jede volle Umdrehung schlaegt
 * hoerbar, die Tonhoehe steigt mit dem Tempo. Drei Anschluesse auf der rechten Seite in Hoehe +1; kein GUI, dafuer
 * das Blick-Overlay.
 */
public class MachineSteamEngineBlockEntity extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2 {

    private static final int steamCap = 2_000;
    private static final int ldsCap = 20;
    private static final double efficiency = 0.85D;

    /** Grosser Rahmen, der Puffer selbst wird jeden Tick auf die Tagesproduktion gesetzt. */
    private static final long BUFFER_FRAME = 1_000_000_000_000L;

    public final FluidTank[] tanks = new FluidTank[] {
            new FluidTank(ModFluids.STEAM.getSource(), steamCap),
            new FluidTank(ModFluids.SPENTSTEAM.getSource(), ldsCap)
    };

    public float rotor;
    public float lastRotor;
    private float syncRotor;
    private int turnProgress;
    private float acceleration = 0F;

    public MachineSteamEngineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STEAM_ENGINE_BE.get(), pos, state, 0, BUFFER_FRAME, 0L, BUFFER_FRAME);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineSteamEngineBlockEntity be) {
        if (level instanceof ServerLevel world) be.serverTick(world, pos);
        else be.clientTick();
    }

    private record DirPos(BlockPos pos, Direction dir) { }

    private DirPos[] getConPos() {
        Direction dir = getBlockState().getValue(DummyableMachineBlock.FACING);
        Direction rot = dir.getClockWise();
        BlockPos base = worldPosition.relative(rot, 2).above();
        return new DirPos[] {
                new DirPos(base, rot),
                new DirPos(base.relative(dir), rot),
                new DirPos(base.relative(dir, -1), rot)
        };
    }

    private void serverTick(ServerLevel world, BlockPos pos) {

        long powerBuffer = 0;

        tanks[0].setTankType(ModFluids.STEAM.getSource());
        tanks[1].setTankType(ModFluids.SPENTSTEAM.getSource());

        FT_Coolable trait = com.hbm_m.inventory.fluid.FluidType.getTrait(tanks[0].getTankType(), FT_Coolable.class);
        int ops = 0;
        if (trait != null) {
            double eff = trait.getEfficiency(CoolingType.TURBINE) * efficiency;

            int inputOps = tanks[0].getFill() / trait.amountReq;
            int outputOps = (tanks[1].getMaxFill() - tanks[1].getFill()) / trait.amountProduced;
            ops = Math.min(inputOps, outputOps);
            tanks[0].setFill(tanks[0].getFill() - ops * trait.amountReq);
            tanks[1].setFill(tanks[1].getFill() + ops * trait.amountProduced);
            powerBuffer += (long) (ops * trait.heatEnergy * eff);
        }

        this.energy = powerBuffer;

        if (ops > 0) {
            this.acceleration += 0.1F;
        } else {
            this.acceleration -= 0.1F;
        }

        this.acceleration = Mth.clamp(this.acceleration, 0F, 40F);
        this.rotor += this.acceleration;

        if (this.rotor >= 360D) {
            this.rotor -= 360D;
            world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), com.hbm_m.sound.HbmSoundsNT.get("hbm:block.steamEngineOperate"),
                    SoundSource.BLOCKS, 1.0F, 0.5F + (acceleration / 80F));
        }

        for (DirPos con : getConPos()) {
            if (this.energy > 0) this.tryProvide(world, con.pos.getX(), con.pos.getY(), con.pos.getZ(), con.dir);
            this.trySubscribe(tanks[0].getTankType(), world, con.pos, con.dir);
            this.tryProvide(tanks[1], world, con.pos, con.dir);
        }

        sendUpdateToClient();
    }

    private void clientTick() {
        this.lastRotor = this.rotor;

        if (this.turnProgress > 0) {
            double d = Mth.wrapDegrees(this.syncRotor - (double) this.rotor);
            this.rotor = (float) ((double) this.rotor + d / (double) this.turnProgress);
            --this.turnProgress;
        } else {
            this.rotor = this.syncRotor;
        }
    }

    @Override
    public boolean canConnectEnergy(Direction side) {
        return side != null && side != Direction.UP && side != Direction.DOWN;
    }

    // ==================== Fluid ====================

    @Override public FluidTank[] getAllTanks() { return tanks; }
    @Override public FluidTank[] getSendingTanks() { return new FluidTank[] { tanks[1] }; }
    @Override public FluidTank[] getReceivingTanks() { return new FluidTank[] { tanks[0] }; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        return fromDir != null;
    }

    // ==================== NBT ====================

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putLong("powerBuffer", energy);
        tag.putFloat("acceleration", acceleration);
        tag.putFloat("rotor", rotor);
        tanks[0].writeToNBT(tag, "s");
        tanks[1].writeToNBT(tag, "w");
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        energy = tag.getLong("powerBuffer");
        acceleration = tag.getFloat("acceleration");
        tanks[0].readFromNBT(tag, "s");
        tanks[1].readFromNBT(tag, "w");
    }

    @Override
    protected void applyClientUpdate(CompoundTag tag) {
        super.applyClientUpdate(tag);
        this.syncRotor = tag.getFloat("rotor");
        this.turnProgress = 3; // Original: 3 Zwischenschritte fuer weicheres Drehen
    }

    // ==================== Sonstiges ====================

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.steam_engine");
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

    public FluidTank getSteamTank() { return tanks[0]; }
    public FluidTank getSpentSteamTank() { return tanks[1]; }

    /** Original: {@code INFINITE_EXTENT_AABB}. */
    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return INFINITE_EXTENT_AABB;
    }
}
