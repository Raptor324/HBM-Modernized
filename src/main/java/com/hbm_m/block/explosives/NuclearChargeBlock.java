package com.hbm_m.block.explosives;

import com.hbm_m.explosion.command.ExplosionCommandOptions;
import com.hbm_m.explosion.command.NuclearScenarioLaunchers;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import javax.annotation.Nullable;
import java.util.List;

/**
 *  ЯДЕРНЫЙ БЛОК v6 - АНИМИРОВАННЫЙ РОСТ
 * Управляет таймингами появления каждой части гриба.
 */
public class NuclearChargeBlock extends Block implements IDetonatable {

    public NuclearChargeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable net.minecraft.world.level.BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.hbm_m.nuclear_charge.line1").withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.translatable("tooltip.hbm_m.nuclear_charge.line2").withStyle(ChatFormatting.RED));
    }

    @Override
    public boolean onDetonate(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            ServerLevel serverLevel = (ServerLevel) level;
            level.removeBlock(pos, false);
            NuclearScenarioLaunchers.launchNuclearCharge(serverLevel, pos, ExplosionCommandOptions.DEFAULT);
            return true;
        }
        return false;
    }
}
