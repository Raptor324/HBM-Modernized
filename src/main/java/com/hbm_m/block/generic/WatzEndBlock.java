package com.hbm_m.block.generic;

import com.hbm_m.api.block.IToolable;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * {@code watz_end} Meta 0 als {@code BlockToolConversion}: BOLT + 4 {@code DURA.bolt()} -> {@code watz_end} Meta 1
 * (im Port {@code watz_end_bolted}).
 */
public class WatzEndBlock extends Block implements IToolable {

    public WatzEndBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean onScrew(Level world, Player player, BlockPos pos, Direction side, float fX, float fY, float fZ, InteractionHand hand, ToolType tool) {
        if (world.isClientSide) return false;
        if (tool != ToolType.BOLT) return false;

        int have = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack st = player.getInventory().getItem(i);
            if (st.is(ModItems.BOLT_HIGHSPEED_STEEL.get())) have += st.getCount();
        }
        if (have < 4) return false;

        int need = 4;
        for (int i = 0; i < player.getInventory().getContainerSize() && need > 0; i++) {
            ItemStack st = player.getInventory().getItem(i);
            if (!st.is(ModItems.BOLT_HIGHSPEED_STEEL.get())) continue;
            int take = Math.min(need, st.getCount());
            st.shrink(take);
            need -= take;
        }

        world.setBlock(pos, ModBlocks.WATZ_END_BOLTED.get().defaultBlockState(), 3);
        return true;
    }
}
