package com.hbm_m.blockentity.generic;

import com.hbm_m.api.tile.IControlReceiver;
import com.hbm_m.block.generic.BlockWandJigsaw;
import com.hbm_m.blockentity.BaseHbmBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.util.ForgeDirection;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code BlockWandJigsaw.TileEntityWandJigsaw} (tileentity_wand_jigsaw): Anschlusspunkt einer .nbt-Struktur mit
 * Zielpool, eigenem Namen, Zielname, Ersatzblock, Auswahl-/Platzierungsprioritaet und Gelenkart.
 */
public class WandJigsawBlockEntity extends BaseHbmBlockEntity implements IControlReceiver {

    public int selectionPriority = 0; // hoeher = dieser Jigsaw-Block wird zuerst ausgewertet
    public int placementPriority = 0; // hoeher = Kinder dieses Blocks werden zuerst weiter verfolgt
    public String pool = "default";
    public String name = "default";
    public String target = "default";
    public BlockState replaceBlock = Blocks.AIR.defaultBlockState();
    public boolean isRollable = true; // Gelenkart: rollbare Gelenke duerfen bei senkrechten Anschluessen beliebig gedreht werden

    public WandJigsawBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WAND_JIGSAW.get(), pos, state);
    }

    /** Original {@code networkPackNT}. */
    public void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public int getDirection() {
        BlockState state = getBlockState();
        if (state.hasProperty(BlockWandJigsaw.FACING)) return ForgeDirection.of(state.getValue(BlockWandJigsaw.FACING)).ordinal();
        return 2;
    }

    @Override
    protected void writeNbtData(CompoundTag nbt, HolderLookup.Provider registries) {
        nbt.putInt("direction", getDirection());

        nbt.putInt("selection", selectionPriority);
        nbt.putInt("placement", placementPriority);
        nbt.putString("pool", pool);
        nbt.putString("name", name);
        nbt.putString("target", target);
        nbt.put("block", NbtUtils.writeBlockState(replaceBlock));
        nbt.putBoolean("roll", isRollable);
    }

    @Override
    protected void readNbtData(CompoundTag nbt, HolderLookup.Provider registries) {
        selectionPriority = nbt.getInt("selection");
        placementPriority = nbt.getInt("placement");
        pool = nbt.getString("pool");
        name = nbt.getString("name");
        target = nbt.getString("target");
        replaceBlock = nbt.contains("block") ? NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), nbt.getCompound("block")) : Blocks.AIR.defaultBlockState();
        isRollable = nbt.getBoolean("roll");
    }

    @Override
    public boolean hasPermission(Player player) {
        return true;
    }

    @Override
    public void receiveControl(CompoundTag nbt) {
        readNbtData(nbt, null);
        sync();
    }
}
