package com.hbm_m.block.entity.machines;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.api.fluids.FluidNetProvider;
import com.hbm_m.api.fluids.FluidNode;
import com.hbm_m.api.fluids.IFluidPipeMK2;
import com.hbm_m.api.fluids.VanillaFluidEquivalence;
import com.hbm_m.api.network.UniNodespace;
import com.hbm_m.block.entity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * BlockEntity клапана (порт TileEntityFluidValve из 1.7.10).
 *
 * При закрытом клапане узел НЕ создаётся → разрыв графа.
 * При открытом — ведёт себя как обычная труба.
 * Управляется сигналом редстоуна: по умолчанию открыт (нет сигнала).
 */
public class FluidValveBlockEntity extends BlockEntity implements IFluidPipeMK2 {

    private static final String NBT_FLUID_TYPE = "FluidType";
    private static final String NBT_OPEN       = "ValveOpen";

    private Fluid fluidType = Fluids.EMPTY;
    private boolean open = true; // true = открыт (нет сигнала ред.камня)

    private FluidNode node;

    public FluidValveBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_VALVE_BE.get(), pos, state);
    }

    // =====================================================================================
    // IFluidPipeMK2
    // =====================================================================================

    @Override
    public Fluid getFluidType() { return fluidType; }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        return fromDir != null && open && VanillaFluidEquivalence.sameSubstance(fluid, this.fluidType);
    }

    // =====================================================================================
    // Fluid type management (вызывается FluidDuctBlock.use)
    // =====================================================================================

    public void setFluidType(Fluid fluid) {
        Fluid prev = this.fluidType;
        this.fluidType = fluid != null ? fluid : Fluids.EMPTY;
        setChanged();
        if (level instanceof ServerLevel serverLevel) {
            rebuildNode(serverLevel, prev);
        }
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // =====================================================================================
    // Open / close logic
    // =====================================================================================

    public boolean isOpen() { return open; }

    /**
     * Обновить состояние клапана по сигналу редстоуна.
     * Вызывается из block.neighborChanged.
     */
    public void updateRedstone(Level level, BlockPos pos) {
        boolean powered = level.hasNeighborSignal(pos);
        boolean newOpen = !powered; // нет сигнала = открыт
        if (newOpen == open) return;
        open = newOpen;
        setChanged();
        if (level instanceof ServerLevel serverLevel) {
            if (!open) {
                // Закрылся — уничтожить узел (разрыв графа)
                destroyCurrentNode(serverLevel);
            } else {
                // Открылся — создать узел
                ensureNode(serverLevel);
            }
            level.sendBlockUpdated(pos, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // =====================================================================================
    // Node lifecycle
    // =====================================================================================

    private void rebuildNode(ServerLevel serverLevel, Fluid prev) {
        destroyCurrentNode(serverLevel);
        if (prev != null && prev != Fluids.EMPTY && prev != fluidType) {
            UniNodespace.destroyNode(serverLevel, worldPosition, FluidNetProvider.forFluid(prev));
        }
        if (open) ensureNode(serverLevel);
    }

    private void destroyCurrentNode(ServerLevel serverLevel) {
        if (node != null && !node.isExpired()) {
            UniNodespace.destroyNode(serverLevel, node);
        }
        node = null;
    }

    private void ensureNode(ServerLevel serverLevel) {
        if (fluidType == Fluids.EMPTY || !open) return;
        if (node == null || node.isExpired()) {
            var existing = UniNodespace.getNode(serverLevel, worldPosition, FluidNetProvider.forFluid(fluidType));
            if (existing instanceof FluidNode fn && !fn.isExpired()) {
                node = fn;
            } else {
                node = createNode(fluidType, worldPosition);
                UniNodespace.createNode(serverLevel, node);
            }
        }
    }

    private void initFromLevel(Level level) {
        if (level.isClientSide) return;
        boolean powered = level.hasNeighborSignal(worldPosition);
        boolean newOpen = !powered;
        if (newOpen != open) {
            updateRedstone(level, worldPosition);
        }
        if (level instanceof ServerLevel sl && open) {
            ensureNode(sl);
        }
    }


    //? if forge {
    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null) initFromLevel(level);
    }
    //?}

    //? if fabric {
    /*@Override
    public void setLevel(Level level) {
        super.setLevel(level);
        initFromLevel(level);
    }
    *///?}

    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel sl) destroyCurrentNode(sl);
        node = null;
        super.setRemoved();
    }

    //? if forge {
    @Override
    public void onChunkUnloaded() {
        if (node != null) node.expired = true;
        super.onChunkUnloaded();
    }
    //?}

    // =====================================================================================
    // NBT
    // =====================================================================================

    @Override
    protected void saveAdditional( @NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        ResourceLocation loc = BuiltInRegistries.FLUID.getKey(fluidType);
        if (loc != null) tag.putString(NBT_FLUID_TYPE, loc.toString());
        tag.putBoolean(NBT_OPEN, open);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        if (tag.contains(NBT_FLUID_TYPE)) {
            Fluid f = BuiltInRegistries.FLUID.get(ResourceLocation.tryParse(tag.getString(NBT_FLUID_TYPE)));
            this.fluidType = f != null ? f : Fluids.EMPTY;
        }
        open = !tag.contains(NBT_OPEN) || tag.getBoolean(NBT_OPEN);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
