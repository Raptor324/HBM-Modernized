package com.hbm_m.item.tools_and_armor;

import com.hbm_m.block.machines.pile.BlockGraphiteDrilledBase;
import com.hbm_m.item.LoreTooltipItem;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Порт дефузера — NEXT {@code com.hbm.items.tool.ItemDefuser} (extends ItemTooling): меняет
 * порог автосрабатывания нейтронного детектора CP-1 через IToolable.onScrew. В Modernized нет
 * интерфейса IToolable, поэтому диспатч — прямой instanceof, как в {@link ScrewdriverItem}.
 * Блок-хуки пропускаются движком, когда игрок крадётся с непустой рукой, так что sneak-ветки
 * детектора (уменьшение лимита) достижимы только отсюда.
 */
public class ItemDefuser extends LoreTooltipItem {

    public ItemDefuser(List<Component> lines, Properties properties) {
        super(lines, properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof BlockGraphiteDrilledBase pile)
            return pile.onScrewForTool(state, level, pos, player, context.getClickedFace(), context.getHand());

        return InteractionResult.PASS;
    }
}
