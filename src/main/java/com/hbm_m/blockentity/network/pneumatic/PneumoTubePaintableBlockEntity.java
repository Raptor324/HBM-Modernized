package com.hbm_m.blockentity.network.pneumatic;

import com.hbm_m.blockentity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

/**
 * 1:1-Port von {@code PneumoTubePaintableBlock.TileEntityPneumoTubePaintable} (1.7.10).
 *
 * <p>Ein gewoehnliches Rohr, das sich hinter einem beliebigen Block verstecken laesst. Das ist
 * kein Beiwerk: eine Lageranlage besteht aus Dutzenden Metern Rohr, und die verschwinden damit
 * sauber in Wand und Boden, statt als silbernes Geflecht durch den Raum zu laufen.</p>
 *
 * <p>Ueberstrichen wird mit einem Rechtsklick, der Anstrich geht mit dem Handbohrer wieder ab.</p>
 */
public class PneumoTubePaintableBlockEntity extends PneumoTubeBlockEntity {

    /** Original: {@code Block block} samt {@code meta} - hier der ganze Blockzustand. */
    @Nullable
    private BlockState camo;

    public PneumoTubePaintableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PNEUMO_TUBE_PAINTABLE_BE.get(), pos, state);
    }

    @Nullable
    public BlockState getCamo() {
        return camo;
    }

    /** Original: die Metadatenzahl - 1 blendet die Seitenmarkierung aus. */
    private boolean markingsHidden = false;

    public boolean areMarkingsHidden() { return markingsHidden; }

    public void setMarkingsHidden(boolean hidden) {
        this.markingsHidden = hidden;
        setChanged();
        if (level != null && !level.isClientSide() && !isRemoved()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    public void setCamo(@Nullable BlockState camo) {
        this.camo = camo;
        setChanged();
        if (level != null && !level.isClientSide() && !isRemoved()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void writeNbtData(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        if (camo != null) tag.put("camo", NbtUtils.writeBlockState(camo));
        tag.putBoolean("markingsHidden", markingsHidden);
    }

    @Override
    protected void readNbtData(CompoundTag tag, HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);

        markingsHidden = tag.getBoolean("markingsHidden");

        camo = null;
        if (!tag.contains("camo")) return;

        HolderLookup.Provider access = registries;
        if (access == null && level != null) access = level.registryAccess();
        if (access != null) {
            camo = NbtUtils.readBlockState(access.lookupOrThrow(Registries.BLOCK), tag.getCompound("camo"));
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hbm_m.pneumatic_tube_paintable");
    }
}
