package com.hbm_m.block.nature;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.Explosion;

//? if neoforge {
/*import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.TooltipFlag;
*///?}

//? if forge {
import net.minecraft.world.item.TooltipFlag;
//?}

//? if fabric {
/*import net.minecraft.world.item.TooltipFlag;
*///?}

import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DepthOreBlock extends Block {

    public DepthOreBlock(Properties properties) {
        // Original BlockDepth: setBlockUnbreakable + setResistance(10) -> Werte kommen aus ModBlocks (-1 / 6)
        super(properties
                .requiresCorrectToolForDrops()
                .pushReaction(PushReaction.BLOCK)
        );
    }

    //? if forge {
    @Override
    public void appendHoverText(ItemStack stack,
                                @Nullable BlockGetter level,
                                List<Component> tooltip,
                                TooltipFlag flag) {
        addDepthOreTooltip(tooltip);
    }
    //?}

    //? if neoforge {
    /*@Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        addDepthOreTooltip(tooltip);
    }
    *///?}

    //? if fabric {
    /*@Override
    public void appendHoverText(ItemStack stack,
                                @Nullable net.minecraft.world.level.BlockGetter level,
                                List<Component> tooltip,
                                TooltipFlag flag) {
        addDepthOreTooltip(tooltip);
    }
    *///?}

    /** 1:1 BlockDepth.addInformation: eine gelbe Zeile "trait.tile.depth". */
    private static void addDepthOreTooltip(List<Component> tooltip) {
        tooltip.add(Component.translatable("trait.tile.depth")
                .withStyle(ChatFormatting.YELLOW));
    }

    // Не ломается поршнями — через Properties.pushReaction (совместимо с 1.20–1.21+)

    // Запретить ломать блок инструментом (игроком)
    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        // Original BlockDepth.getPlayerRelativeBlockHardness: IDepthRockTool bricht Tiefengestein mit 1/50
        net.minecraft.world.item.ItemStack held = player.getMainHandItem();
        if (!held.isEmpty() && held.getItem() instanceof com.hbm_m.api.item.IDepthRockTool tool) {
            if (tool.canBreakRock(level, player, held, state, pos))
                return (float) (1D / 50D);
        }
        return 0.0F;
    }

    // vJ2: Original BlockDepth/BlockDepthOre ueberschreiben das Sprengverhalten nicht: die Explosion laesst die normale
    // Beute fallen (getItemDropped/quantityDropped -> hier die Loot-Tabelle mit survives_explosion/explosion_decay) und
    // entfernt den Block. Frueher fiel hier immer der Block selbst heraus.
}
