package com.hbm_m.blockentity.machines;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.fluid.trait.FT_Heatable;
import com.hbm_m.inventory.fluid.trait.FT_Heatable.HeatingStep;
import com.hbm_m.inventory.fluid.trait.FT_Heatable.HeatingType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code TileEntityMachineHephaestus}: Erdwaermetauscher. Jede Sekunde wird reihum eine der zehn Schichten unter
 * dem Kern in 15x15 abgetastet (Lava 5, Vulkanlava 150, Vulkanerz 300 - letzteres verdreifacht die Summe fuer 20
 * Ticks); mit der Summe erhitzt er das Eingangsfluid nach dessen erster Waermetauscherstufe. Acht Anschluesse unten
 * und oben. Im Betrieb drehen sich die Rotoren, der Kern glueht und Dampfwolken steigen auf.
 */
public class MachineHephaestusBlockEntity extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2 {

    public final FluidTank input = new FluidTank(ModFluids.CRUDE_OIL.getSource(), 24_000);
    public final FluidTank output = new FluidTank(ModFluids.HOTOIL.getSource(), 24_000);

    public int bufferedHeat;
    public float rot;
    public float prevRot;

    private final int[] heat = new int[10];
    private long fissureScanTime;

    public MachineHephaestusBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HEPHAESTUS_BE.get(), pos, state, 0, 0L, 0L, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineHephaestusBlockEntity be) {
        if (!level.isClientSide) be.serverTick(level, pos);
        else be.clientTick(level, pos);
    }

    private void serverTick(Level world, BlockPos pos) {

        setupTanks();

        if (world.getGameTime() % 20 == 0) {
            this.updateConnections(world);
        }

        int height = (int) (world.getGameTime() % 10);
        int range = 7;
        int y = pos.getY() - 1 - height;

        heat[height] = 0;

        if (y >= world.getMinBuildHeight()) {
            for (int x = -range; x <= range; x++) {
                for (int z = -range; z <= range; z++) {
                    heat[height] += heatFromBlock(world, pos.getX() + x, y, pos.getZ() + z);
                }
            }
        }

        heatFluid();

        if (output.getFill() > 0) {
            for (DirPos con : getConPos()) {
                this.tryProvide(output, world, con.pos, con.dir);
            }
        }
        this.bufferedHeat = this.getTotalHeat();

        setChanged();
        sendUpdateToClient();
    }

    private void clientTick(Level world, BlockPos pos) {

        this.prevRot = this.rot;

        if (this.bufferedHeat > 0) {
            this.rot += 0.5F;

            if (world.random.nextInt(7) == 0) {
                double x = world.random.nextGaussian() * 2;
                double y = world.random.nextGaussian() * 3;
                double z = world.random.nextGaussian() * 2;
                world.addParticle(ParticleTypes.CLOUD, pos.getX() + 0.5 + x, pos.getY() + 6 + y, pos.getZ() + 0.5 + z, 0, 0, 0);
            }
        }

        com.hbm_m.sound.ClientSoundBootstrap.updateSound(this, this.bufferedHeat > 0, this::createAudioLoop);

        if (this.rot >= 360F) {
            this.prevRot -= 360F;
            this.rot -= 360F;
        }
    }

    private Object createAudioLoop() {
        try {
            return Class.forName("com.hbm_m.client.sound.HephaestusLoopSoundFactory").getMethod("create", MachineHephaestusBlockEntity.class).invoke(null, this);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    protected void heatFluid() {

        FT_Heatable trait = FluidType.getTrait(input.getTankType(), FT_Heatable.class);

        if (trait != null) {
            int heat = this.getTotalHeat();
            HeatingStep step = trait.getFirstStep();

            int inputOps = input.getFill() / step.amountReq;
            int outputOps = (output.getMaxFill() - output.getFill()) / step.amountProduced;
            int heatOps = heat / step.heatReq;
            int ops = Math.min(Math.min(inputOps, outputOps), heatOps);

            input.setFill(input.getFill() - step.amountReq * ops);
            output.setFill(output.getFill() + step.amountProduced * ops);
            setChanged();
        }
    }

    protected void setupTanks() {

        FT_Heatable trait = FluidType.getTrait(input.getTankType(), FT_Heatable.class);

        if (trait != null && trait.getEfficiency(HeatingType.HEATEXCHANGER) > 0) {
            Fluid outType = trait.getFirstStep().typeProduced;
            output.setTankType(outType);
            return;
        }

        input.setTankType(ModFluids.NONE.getSource());
        output.setTankType(ModFluids.NONE.getSource());
    }

    protected int heatFromBlock(Level world, int x, int y, int z) {
        BlockState state = world.getBlockState(new BlockPos(x, y, z));
        Block b = state.getBlock();

        if (b == Blocks.LAVA) return 5;
        if (b == com.hbm_m.block.ModBlocks.VOLCANIC_LAVA_BLOCK.get()) return 150;

        if (b == com.hbm_m.block.ModBlocks.ORE_VOLCANO.get()) {
            this.fissureScanTime = world.getGameTime();
            return 300;
        }

        return 0;
    }

    public int getTotalHeat() {
        boolean fissure = level != null && level.getGameTime() - this.fissureScanTime < 20;
        int heat = 0;

        for (int h : this.heat) {
            heat += h;
        }

        if (fissure) {
            heat *= 3;
        }

        return heat;
    }

    private void updateConnections(Level world) {

        if (input.getTankType() == ModFluids.NONE.getSource()) return;

        for (DirPos con : getConPos()) {
            this.trySubscribe(input.getTankType(), world, con.pos, con.dir);
        }
    }

    /** Anschluss samt Richtung (Original {@code DirPos}). */
    private record DirPos(BlockPos pos, Direction dir) { }

    private DirPos[] getConPos() {
        BlockPos p = worldPosition;
        return new DirPos[] {
                new DirPos(p.offset(2, 0, 0), Direction.EAST),
                new DirPos(p.offset(-2, 0, 0), Direction.WEST),
                new DirPos(p.offset(0, 0, 2), Direction.SOUTH),
                new DirPos(p.offset(0, 0, -2), Direction.NORTH),
                new DirPos(p.offset(2, 11, 0), Direction.EAST),
                new DirPos(p.offset(-2, 11, 0), Direction.WEST),
                new DirPos(p.offset(0, 11, 2), Direction.SOUTH),
                new DirPos(p.offset(0, 11, -2), Direction.NORTH)
        };
    }

    // ── Fluid ──────────────────────────────────────────────────────────────

    @Override public FluidTank[] getAllTanks() { return new FluidTank[] { input, output }; }
    @Override public FluidTank[] getReceivingTanks() { return new FluidTank[] { input }; }
    @Override public FluidTank[] getSendingTanks() { return new FluidTank[] { output }; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    /** Original: nur seitlich. */
    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        return fromDir != null && fromDir != Direction.UP && fromDir != Direction.DOWN;
    }

    public FluidTank getInputTank()  { return input; }
    public FluidTank getOutputTank() { return output; }

    // ── NBT ────────────────────────────────────────────────────────────────

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        input.writeToNBT(tag, "0");
        output.writeToNBT(tag, "1");
        tag.putInt("bufferedHeat", bufferedHeat);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        input.readFromNBT(tag, "0");
        output.readFromNBT(tag, "1");
        bufferedHeat = tag.getInt("bufferedHeat");
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hbm_m.hephaestus");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return null; // Kein GUI im Original.
    }

    private AABB bb = null;

    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        if (bb == null) bb = new AABB(worldPosition.getX() - 3, worldPosition.getY(), worldPosition.getZ() - 3,
                worldPosition.getX() + 4, worldPosition.getY() + 12, worldPosition.getZ() + 4);
        return bb;
    }
}
