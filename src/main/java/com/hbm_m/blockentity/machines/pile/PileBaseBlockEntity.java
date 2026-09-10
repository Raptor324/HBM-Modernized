package com.hbm_m.blockentity.machines.pile;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.LoadedMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

/**
 * 1:1-Port von {@code TileEntityPileBaseMK2} (1.7.10): jeder Block des Meilers ausser dem Kern.
 *
 * <p>Er merkt sich nur, wo sein Kern steht, und faellt in einen gewoehnlichen Graphitziegel
 * zurueck, sobald dieser verschwunden ist - so zerfaellt der ganze Meiler von selbst, wenn man ihm
 * den Kern herausschlaegt.</p>
 */
public class PileBaseBlockEntity extends LoadedMachineBlockEntity {

    /** Original: {@code coreY = -999} als "noch kein Kern gesetzt". */
    private static final int NO_CORE = -999;

    private BlockPos corePos = null;

    @Nullable
    private PileCoreBlockEntity cachedCore;

    public PileBaseBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.PILE_BASE_BE.get(), pos, state);
    }

    protected PileBaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public PileBaseBlockEntity setCore(BlockPos pos) {
        this.corePos = pos.immutable();
        setChanged();
        return this;
    }

    public @Nullable BlockPos getCorePos() {
        return corePos;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PileBaseBlockEntity be) {
        if (level.isClientSide() || be.corePos == null) return;

        // Original: ist der Kern weg, aber sein Bereich geladen, faellt dieser Block zurueck.
        PileCoreBlockEntity core = be.getCore(level);
        if ((core == null || core.isRemoved()) && level.isLoaded(be.corePos)) {
            level.setBlock(pos, ModBlocks.PILE_BRICK.get().defaultBlockState(), 3);
        }
    }

    /** Original: {@code getCore} samt Zwischenspeicher. */
    @Nullable
    public PileCoreBlockEntity getCore(Level level) {
        if (cachedCore != null && !cachedCore.isRemoved()) return cachedCore;
        if (corePos == null || !level.isLoaded(corePos)) return null;

        BlockEntity be = level.getBlockEntity(corePos);
        if (be instanceof PileCoreBlockEntity core) {
            cachedCore = core;
            return core;
        }
        return null;
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putInt("cX", corePos != null ? corePos.getX() : 0);
        tag.putInt("cY", corePos != null ? corePos.getY() : NO_CORE);
        tag.putInt("cZ", corePos != null ? corePos.getZ() : 0);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        int cy = tag.contains("cY") ? tag.getInt("cY") : NO_CORE;
        corePos = cy == NO_CORE ? null : new BlockPos(tag.getInt("cX"), cy, tag.getInt("cZ"));
        cachedCore = null;
    }
}
