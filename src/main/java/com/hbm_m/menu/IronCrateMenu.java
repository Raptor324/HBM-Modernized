package com.hbm_m.menu;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.custom.machines.crates.CrateType;
import com.hbm_m.block.entity.custom.crates.IronCrateBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

public class IronCrateMenu extends BaseCrateMenu {

    public IronCrateMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, extraData.readBlockPos());
    }

    private IronCrateMenu(int containerId, Inventory inv, BlockPos pos) {
        this(containerId, inv, inv.player.level().getBlockEntity(pos));
    }

    public IronCrateMenu(int containerId, Inventory inv, BlockEntity entity) {
        super(ModMenuTypes.IRON_CRATE_MENU.get(), containerId, inv,
                entity instanceof IronCrateBlockEntity be ? be
                        : new IronCrateBlockEntity(BlockPos.ZERO, ModBlocks.CRATE_IRON.get().defaultBlockState()),
                CrateType.IRON);
    }

    @Override
    protected Block getBlock() {
        return ModBlocks.CRATE_IRON.get();
    }
}
