package com.hbm_m.menu;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.custom.machines.crates.CrateType;
import com.hbm_m.block.entity.custom.crates.TemplateCrateBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

public class TemplateCrateMenu extends BaseCrateMenu {

    public TemplateCrateMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, extraData.readBlockPos());
    }

    private TemplateCrateMenu(int containerId, Inventory inv, BlockPos pos) {
        this(containerId, inv, inv.player.level().getBlockEntity(pos));
    }

    public TemplateCrateMenu(int containerId, Inventory inv, BlockEntity entity) {
        super(ModMenuTypes.TEMPLATE_CRATE_MENU.get(), containerId, inv,
                entity instanceof TemplateCrateBlockEntity be ? be
                        : new TemplateCrateBlockEntity(BlockPos.ZERO, ModBlocks.CRATE_TEMPLATE.get().defaultBlockState()),
                CrateType.TEMPLATE);
    }

    @Override
    protected Block getBlock() {
        return ModBlocks.CRATE_TEMPLATE.get();
    }
}
