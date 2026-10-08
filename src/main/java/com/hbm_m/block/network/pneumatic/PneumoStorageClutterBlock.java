package com.hbm_m.block.network.pneumatic;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.network.pneumatic.PneumoStorageClutterBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Port von {@code PneumoStorageClutter} (1.7.10). Das Ramschlager: 54 gewoehnliche Plaetze, nimmt alles an.
 */
public class PneumoStorageClutterBlock extends PneumaticStorageBlockBase {

    public PneumoStorageClutterBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PneumoStorageClutterBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.PNEUMO_STORAGE_CLUTTER_BE.get(),
                (lvl, p, st, be) -> PneumoStorageClutterBlockEntity.tick(lvl, p, st, (PneumoStorageClutterBlockEntity) be));
    }

    /** Wird in playerWillDestroy gesetzt, wenn der Inhalt im NBT-Item mitgeht (Original {@code dropInv = false}). */
    private static final ThreadLocal<BlockPos> KEEP = new ThreadLocal<>();

    @Override
    protected boolean spillOnRemove(Level level, BlockPos pos) {
        return !pos.equals(KEEP.get());
    }

    /**
     * audit10: 1:1 {@code removedByPlayer} - ohne CRATE_KEEP_CONTENTS faellt der leere Block (nicht Kreativ) und der Inhalt
     * heraus; mit: im Ueberlebensmodus der Block mit allen Slots (NBT), ueber 6 kB NBT stattdessen Warnung, leerer Block
     * und verstreuter Inhalt; im Kreativmodus verschwindet der Inhalt. Keine eigenen Drops ({@code getItemDropped} null).
     */
    //? if < 1.21.1 {
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.player.Player player) {
        handleRemoval(level, pos, player);
        super.playerWillDestroy(level, pos, state, player);
    }
    //?} else {
    /*@Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.player.Player player) {
        handleRemoval(level, pos, player);
        return super.playerWillDestroy(level, pos, state, player);
    }
    *///?}

    private void handleRemoval(Level level, BlockPos pos, net.minecraft.world.entity.player.Player player) {
        KEEP.remove();
        if (level.isClientSide) return;
        boolean creative = player.getAbilities().instabuild;

        if (!com.hbm_m.config.ModClothConfig.get().crateKeepContents) {
            if (!creative) popResource(level, pos, new net.minecraft.world.item.ItemStack(this));
            return; // Inhalt verstreut onRemove
        }

        if (!creative && level.getBlockEntity(pos) instanceof PneumoStorageClutterBlockEntity inv) {
            net.minecraft.world.item.ItemStack drop = new net.minecraft.world.item.ItemStack(this);
            boolean any = false;
            for (int i = 0; i < inv.getInventory().getSlots(); i++) if (!inv.getInventory().getStackInSlot(i).isEmpty()) any = true;
            if (any) {
                inv.saveToItem(drop);
                net.minecraft.nbt.CompoundTag tag = com.hbm_m.platform.PlatformHooks.getItemTag(drop);
                if (tag != null && sizeOf(tag) > 6000) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Warning: Container NBT exceeds 6kB, contents will be ejected!")
                            .withStyle(net.minecraft.ChatFormatting.RED));
                    popResource(level, pos, new net.minecraft.world.item.ItemStack(this));
                    return; // Inhalt verstreut onRemove
                }
            }
            popResource(level, pos, drop);
        }
        KEEP.set(pos.immutable()); // Kreativ oder mitgenommen: nichts verstreuen
    }

    private static int sizeOf(net.minecraft.nbt.CompoundTag tag) {
        try {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            net.minecraft.nbt.NbtIo.writeCompressed(tag, out);
            return out.size();
        } catch (java.io.IOException e) {
            return 0;
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        super.onRemove(state, level, pos, newState, isMoving);
        if (pos.equals(KEEP.get())) KEEP.remove();
    }
}
