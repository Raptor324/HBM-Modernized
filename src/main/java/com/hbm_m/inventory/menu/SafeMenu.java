package com.hbm_m.inventory.menu;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.crates.CrateType;
import com.hbm_m.blockentity.crates.SafeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

/** 1:1 {@code ContainerSafe}. */
public class SafeMenu extends BaseCrateMenu {

    public SafeMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, extraData.readBlockPos());
    }

    private SafeMenu(int containerId, Inventory inv, BlockPos pos) {
        this(containerId, inv, inv.player.level().getBlockEntity(pos));
    }

    public SafeMenu(int containerId, Inventory inv, BlockEntity entity) {
        super(ModMenuTypes.SAFE_MENU.get(), containerId, inv,
                entity instanceof SafeBlockEntity be ? be
                        : new SafeBlockEntity(BlockPos.ZERO, ModBlocks.SAFE.get().defaultBlockState()),
                CrateType.SAFE);
    }

    @Override
    protected Block getBlock() {
        return ModBlocks.SAFE.get();
    }
}
