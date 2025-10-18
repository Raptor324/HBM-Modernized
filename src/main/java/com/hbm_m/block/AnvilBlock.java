package com.hbm_m.block;

import com.hbm_m.menu.AnvilMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

public class AnvilBlock extends Block {

    public AnvilBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            // Открываем GUI на сервере
            NetworkHooks.openScreen((ServerPlayer) player,
                    new SimpleMenuProvider(
                            (containerId, playerInventory, playerEntity) ->
                                    new AnvilMenu(containerId, playerInventory),
                            Component.translatable("container.hbm_m.anvil")
                    ),
                    pos
            );
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}