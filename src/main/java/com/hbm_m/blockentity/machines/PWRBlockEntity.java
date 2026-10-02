package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidReceiverMK2;
import com.hbm_m.api.redstoneoverradio.IRORInteractive;
import com.hbm_m.api.redstoneoverradio.IRORValueProvider;
import com.hbm_m.block.machines.PWRBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.tank.FluidTank;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

//? if forge {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
//?}

/**
 * 1:1 {@code BlockPWR.TileEntityBlockPWR}: merkt sich das ersetzte Bauteil ({@link #block}) und den Controller
 * ({@link #core}). Alle zwanzig Ticks prueft er, ob der Controller noch steht und zusammengebaut ist - sonst faellt er in
 * sein Bauteil zurueck. Als Anschluss ({@link PWRBlock#PORT}) reicht er Fluessigkeit, Inventar und Funkwerte durch.
 */
public class PWRBlockEntity extends com.hbm_m.blockentity.BaseHbmBlockEntity
        implements IFluidReceiverMK2, IRORValueProvider, IRORInteractive {

    @Nullable public Block block;
    public BlockPos core = BlockPos.ZERO;

    @Nullable private PWRControllerBlockEntity cachedCore;

    public PWRBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PWR_BLOCK_BE.get(), pos, state);
    }

    public void setup(Block block, BlockPos core) {
        this.block = block;
        this.core = core.immutable();
        setChanged();
    }

    private boolean isPort() {
        return getBlockState().hasProperty(PWRBlock.PORT) && getBlockState().getValue(PWRBlock.PORT);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PWRBlockEntity be) {
        if (level.getGameTime() % 20 == 0 && be.block != null) {
            PWRControllerBlockEntity controller = be.getCore();
            if (controller != null) {
                if (!controller.assembled) be.breakBlock(level, pos);
            } else if (level.hasChunk(be.core.getX() >> 4, be.core.getZ() >> 4)) {
                be.breakBlock(level, pos);
            }
        }
    }

    /** Original: {@code getBlockType().breakBlock(...)} - Bauteil zurueck, Controller zerlegt. */
    public void breakBlock(Level level, BlockPos pos) {
        Block restored = block;
        if (restored == null) return;
        block = null;
        level.setBlock(pos, restored.defaultBlockState(), 3);
        if (level.getBlockEntity(core) instanceof PWRControllerBlockEntity controller) {
            controller.assembled = false;
            controller.setChanged();
        }
    }

    @Nullable
    public PWRControllerBlockEntity getCore() {
        if (cachedCore != null && !cachedCore.isRemoved()) return cachedCore;
        if (level != null && level.hasChunk(core.getX() >> 4, core.getZ() >> 4)) {
            BlockEntity tile = level.getBlockEntity(core);
            if (tile instanceof PWRControllerBlockEntity controller) {
                cachedCore = controller;
                return controller;
            }
        }
        return null;
    }

    // ── Fluessigkeit ────────────────────────────────────────────────────────

    @Override
    public long transferFluid(Fluid type, int pressure, long fluid) {
        if (!isPort() || block == null) return fluid;
        PWRControllerBlockEntity controller = getCore();
        if (controller != null) return controller.transferFluid(type, pressure, fluid);
        return fluid;
    }

    @Override
    public long getDemand(Fluid type, int pressure) {
        if (!isPort() || block == null) return 0;
        PWRControllerBlockEntity controller = getCore();
        if (controller != null) return controller.getDemand(type, pressure);
        return 0;
    }

    @Override
    public FluidTank[] getAllTanks() {
        if (!isPort() || block == null) return new FluidTank[0];
        PWRControllerBlockEntity controller = getCore();
        if (controller != null) return controller.getAllTanks();
        return new FluidTank[0];
    }

    @Override
    public boolean canConnect(Fluid type, Direction dir) {
        return isPort();
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    // ── ROR ─────────────────────────────────────────────────────────────────

    @Override public String[] getFunctionInfo() { return PWRControllerBlockEntity.ROR; }

    @Override
    public String provideRORValue(String name) {
        if (!isPort()) return "";
        PWRControllerBlockEntity controller = getCore();
        if (controller != null) return controller.provideRORValue(name);
        return null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if (!isPort()) return "";
        PWRControllerBlockEntity controller = getCore();
        if (controller != null) return controller.runRORFunction(name, params);
        return null;
    }

    // ── Inventar (Original: ISidedInventory, nur Metadaten 1) ───────────────

    //? if forge {
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            if (!isPort() || block == null) return LazyOptional.empty();
            PWRControllerBlockEntity controller = getCore();
            if (controller != null) return controller.getCapability(cap, side);
            return LazyOptional.empty();
        }
        return super.getCapability(cap, side);
    }
    //?}

    // ── NBT ─────────────────────────────────────────────────────────────────

    @Override
    protected void writeNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        if (block != null) {
            nbt.putString("block", BuiltInRegistries.BLOCK.getKey(block).toString());
            nbt.putInt("cX", core.getX());
            nbt.putInt("cY", core.getY());
            nbt.putInt("cZ", core.getZ());
        }
    }

    @Override
    protected void readNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        block = null;
        if (nbt.contains("block")) {
            Block b = BuiltInRegistries.BLOCK.get(ResourceLocation.tryParse(nbt.getString("block")));
            if (b != null && b != Blocks.AIR) {
                block = b;
                core = new BlockPos(nbt.getInt("cX"), nbt.getInt("cY"), nbt.getInt("cZ"));
            }
        }
        cachedCore = null;
    }
}
