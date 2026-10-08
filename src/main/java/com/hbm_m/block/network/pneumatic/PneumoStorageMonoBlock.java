package com.hbm_m.block.network.pneumatic;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.network.pneumatic.PneumoStorageMonoBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Port von {@code PneumoStorageMono} (1.7.10). Das Massenlager: drei Faecher zu je 100.000 Stueck eines festgelegten Gegenstands.
 */
public class PneumoStorageMonoBlock extends PneumaticStorageBlockBase {

    public PneumoStorageMonoBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PneumoStorageMonoBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.PNEUMO_STORAGE_MONO_BE.get(),
                (lvl, p, st, be) -> PneumoStorageMonoBlockEntity.tick(lvl, p, st, (PneumoStorageMonoBlockEntity) be));
    }

    /** audit10: Original PneumoStorageMono hat kein breakBlock - der Inhalt geht nur ueber das NBT-Item mit. */
    @Override
    protected boolean spillOnRemove(Level level, BlockPos pos) {
        return false;
    }

    /**
     * audit10: 1:1 {@code removedByPlayer} - im Ueberlebensmodus faellt der Block mit Inhalt und Mengen (NBT) heraus;
     * das Original liess danach ueber harvestBlock zusaetzlich einen leeren Block fallen (Doppel-Drop), das entfaellt.
     * {@code onBlockPlacedBy} (Inhalt zurueckladen) uebernimmt BlockItem ueber den BlockEntityTag.
     */
    //? if < 1.21.1 {
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.player.Player player) {
        dropWithContents(level, pos, player);
        super.playerWillDestroy(level, pos, state, player);
    }
    //?} else {
    /*@Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.player.Player player) {
        dropWithContents(level, pos, player);
        return super.playerWillDestroy(level, pos, state, player);
    }
    *///?}

    private void dropWithContents(Level level, BlockPos pos, net.minecraft.world.entity.player.Player player) {
        if (level.isClientSide || player.getAbilities().instabuild) return;
        net.minecraft.world.item.ItemStack drop = new net.minecraft.world.item.ItemStack(this);
        if (level.getBlockEntity(pos) instanceof PneumoStorageMonoBlockEntity inv) {
            boolean any = false;
            for (int i = 0; i < inv.getInventory().getSlots(); i++) if (!inv.getInventory().getStackInSlot(i).isEmpty()) any = true;
            if (any) inv.saveToItem(drop);
        }
        net.minecraft.world.level.block.Block.popResource(level, pos, drop);
    }

    /** Statt des Original-harvestBlock-Drops (siehe oben) nur Statistik/Erschoepfung. */
    @Override
    public void playerDestroy(Level level, net.minecraft.world.entity.player.Player player, BlockPos pos, BlockState state,
                              @Nullable BlockEntity be, net.minecraft.world.item.ItemStack tool) {
        player.awardStat(net.minecraft.stats.Stats.BLOCK_MINED.get(this));
        player.causeFoodExhaustion(0.005F);
    }
}
