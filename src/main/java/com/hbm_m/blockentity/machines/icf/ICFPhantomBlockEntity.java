package com.hbm_m.blockentity.machines.icf;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.icf.ICFLaserPart;
import com.hbm_m.blockentity.LoadedMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

/**
 * 1:1-Port von {@code BlockICF.TileEntityBlockICF} (1.7.10): der Platzhalter, in den sich jedes
 * Laserbauteil beim Zusammenbau verwandelt.
 *
 * <p>Er merkt sich, welches Bauteil er ersetzt hat und wo das Steuerpult steht. Zerschlaegt man
 * ihn, kommt das ursprueliche Bauteil zurueck <b>und</b> der ganze Laser gilt als zerlegt. Umgekehrt
 * loest er sich selbst auf, sobald das Steuerpult den Zusammenbau verliert - so raeumt sich der
 * Aufbau vollstaendig ab, egal an welcher Stelle man ihn oeffnet.</p>
 *
 * <p>Ein Platzhalter mit der Rolle {@link ICFLaserPart#PORT} ist zugleich der Energieeingang: er
 * reicht Leistung, Stand und Aufnahme unveraendert an das Steuerpult durch.</p>
 */
public class ICFPhantomBlockEntity extends LoadedMachineBlockEntity {

    @Nullable
    private ICFLaserPart part;
    @Nullable
    private BlockPos corePos;

    @Nullable
    private ICFControllerBlockEntity cachedCore;

    public ICFPhantomBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ICF_PHANTOM_BE.get(), pos, state);
    }

    public void setup(ICFLaserPart part, BlockPos corePos) {
        this.part = part;
        this.corePos = corePos.immutable();
        setChanged();
    }

    @Nullable public ICFLaserPart getPart() { return part; }
    @Nullable public BlockPos getCorePos()  { return corePos; }

    public boolean isPort() {
        return part == ICFLaserPart.PORT;
    }

    /**
     * Original: alle zwanzig Ticks nachsehen, ob das Steuerpult noch steht und noch zusammengebaut
     * ist - sonst faellt dieser Platzhalter in sein Bauteil zurueck.
     */
    public static void tick(Level level, BlockPos pos, BlockState state, ICFPhantomBlockEntity be) {
        if (level.isClientSide() || be.part == null || be.corePos == null) return;
        if (level.getGameTime() % 20 != 0) return;

        ICFControllerBlockEntity controller = be.getCore(level);

        if (controller != null) {
            if (!controller.isAssembled()) be.revert(level, pos);
        } else if (level.isLoaded(be.corePos)) {
            be.revert(level, pos);
        }
    }

    /** Original: {@code breakBlock} - das gemerkte Bauteil zurueckstellen und den Laser zerlegen. */
    public void revert(Level level, BlockPos pos) {
        if (part == null) return;

        ICFLaserPart restored = part;
        BlockPos core = corePos;
        part = null;

        level.setBlock(pos, ModBlocks.icfLaserPart(restored).defaultBlockState(), 3);

        if (core != null && level.getBlockEntity(core) instanceof ICFControllerBlockEntity controller) {
            controller.setAssembled(false);
        }
    }

    @Nullable
    public ICFControllerBlockEntity getCore(Level level) {
        if (cachedCore != null && !cachedCore.isRemoved()) return cachedCore;
        if (corePos == null || !level.isLoaded(corePos)) return null;

        BlockEntity be = level.getBlockEntity(corePos);
        if (be instanceof ICFControllerBlockEntity controller) {
            cachedCore = controller;
            return controller;
        }
        return null;
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putByte("part", (byte) (part == null ? -1 : part.ordinal()));
        if (corePos != null) {
            tag.putInt("cX", corePos.getX());
            tag.putInt("cY", corePos.getY());
            tag.putInt("cZ", corePos.getZ());
        }
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        int ordinal = tag.getByte("part");
        ICFLaserPart[] values = ICFLaserPart.values();
        part = ordinal >= 0 && ordinal < values.length ? values[ordinal] : null;
        corePos = tag.contains("cY") ? new BlockPos(tag.getInt("cX"), tag.getInt("cY"), tag.getInt("cZ")) : null;
        cachedCore = null;
    }
}
