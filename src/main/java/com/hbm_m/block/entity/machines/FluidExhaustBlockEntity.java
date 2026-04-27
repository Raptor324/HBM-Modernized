package com.hbm_m.block.entity.machines;

import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import com.hbm_m.api.fluids.FluidNetProvider;
import com.hbm_m.api.fluids.FluidNode;
import com.hbm_m.api.fluids.IFluidPipeMK2;
import com.hbm_m.api.fluids.ModFluids;
import com.hbm_m.api.network.UniNodespace;
import com.hbm_m.block.entity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

/**
 * BlockEntity выхлопной трубы (порт TileEntityPipeExhaust из 1.7.10).
 *
 * Особенность: создаёт три отдельных FluidNode на одной позиции —
 * для SMOKE, SMOKE_LEADED и SMOKE_POISON.
 * canConnect разрешает подключение только к этим трём типам.
 *
 * Типы дыма задаются через ModFluids (аналог Fluids.SMOKE_* из 1.7.10).
 */
public class FluidExhaustBlockEntity extends BlockEntity implements IFluidPipeMK2 {

    /**
     * Типы дыма, через которые проходит выхлоп. Инициализируются при первом onLoad.
     * Используем ленивую ссылку, чтобы ModFluids успел зарегистрироваться.
     */
    private static volatile Fluid[] SMOKE_TYPES = null;

    private static Fluid[] smokeTypes() {
        if (SMOKE_TYPES == null) {
            SMOKE_TYPES = new Fluid[]{
                    ModFluids.SMOKE.getSource(),
                    ModFluids.SMOKE_LEADED.getSource(),
                    ModFluids.SMOKE_POISON.getSource()
            };
        }
        return SMOKE_TYPES;
    }

    /** По одному узлу на тип дыма. */
    private final FluidNode[] nodes = new FluidNode[3];

    public FluidExhaustBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_EXHAUST_BE.get(), pos, state);
    }

    // =====================================================================================
    // IFluidPipeMK2
    // =====================================================================================

    @Override
    public Fluid getFluidType() {
        return null; // выхлоп поддерживает несколько типов
    }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        if (fromDir == null) return false;
        for (Fluid smoke : smokeTypes()) {
            if (fluid == smoke) return true;
        }
        return false;
    }

    // =====================================================================================
    // Node lifecycle
    // =====================================================================================

    private void initNodes(ServerLevel serverLevel) {
        Fluid[] smokes = smokeTypes();
        for (int i = 0; i < smokes.length; i++) {
            ensureNode(serverLevel, i, smokes[i]);
        }
    }

    //? if forge {
    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel) {
            initNodes(serverLevel);
        }
    }
    //?}

    //? if fabric {
    /*@Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (level instanceof ServerLevel serverLevel) {
            initNodes(serverLevel);
        }
    }
    *///?}

    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel serverLevel) {
            for (int i = 0; i < nodes.length; i++) {
                if (nodes[i] != null && !nodes[i].isExpired()) {
                    UniNodespace.destroyNode(serverLevel, nodes[i]);
                }
                nodes[i] = null;
            }
        }
        super.setRemoved();
    }

    //? if forge {
    @Override
    public void onChunkUnloaded() {
        for (FluidNode n : nodes) {
            if (n != null) n.expired = true;
        }
        super.onChunkUnloaded();
    }
    //?}

    private void ensureNode(ServerLevel serverLevel, int index, Fluid fluid) {
        if (nodes[index] == null || nodes[index].isExpired()) {
            var existing = UniNodespace.getNode(serverLevel, worldPosition, FluidNetProvider.forFluid(fluid));
            if (existing instanceof FluidNode fn && !fn.isExpired()) {
                nodes[index] = fn;
            } else {
                nodes[index] = createNode(fluid, worldPosition);
                UniNodespace.createNode(serverLevel, nodes[index]);
            }
        }
    }

    // =====================================================================================
    // Minimal tick — восстановление узлов
    // =====================================================================================

    public static void tick(net.minecraft.world.level.Level level, BlockPos pos,
                            BlockState state, FluidExhaustBlockEntity entity) {
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) return;
        Fluid[] smokes = smokeTypes();
        for (int i = 0; i < smokes.length; i++) {
            if (entity.nodes[i] == null || entity.nodes[i].isExpired()) {
                entity.ensureNode(serverLevel, i, smokes[i]);
            }
        }
    }

    // =====================================================================================
    // NBT (нет собственных данных, только тип блока)
    // =====================================================================================

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return super.getUpdateTag();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
