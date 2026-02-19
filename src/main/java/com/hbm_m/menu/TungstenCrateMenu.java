package com.hbm_m.menu;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.custom.machines.crates.CrateType;
import com.hbm_m.block.entity.custom.crates.TungstenCrateBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

public class TungstenCrateMenu extends BaseCrateMenu {

    public TungstenCrateMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, extraData.readBlockPos());
    }

    private TungstenCrateMenu(int containerId, Inventory inv, BlockPos pos) {
        this(containerId, inv, inv.player.level().getBlockEntity(pos));
    }

    public TungstenCrateMenu(int containerId, Inventory inv, BlockEntity entity) {
        super(ModMenuTypes.TUNGSTEN_CRATE_MENU.get(), containerId, inv,
                entity instanceof TungstenCrateBlockEntity be ? be
                        : new TungstenCrateBlockEntity(BlockPos.ZERO, ModBlocks.CRATE_TUNGSTEN.get().defaultBlockState()),
                CrateType.TUNGSTEN);
    }

    @Override
    protected Block getBlock() {
        return ModBlocks.CRATE_TUNGSTEN.get();
    }
}
