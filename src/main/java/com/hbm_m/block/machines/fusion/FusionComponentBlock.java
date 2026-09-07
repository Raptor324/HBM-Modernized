package com.hbm_m.block.machines.fusion;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1-Port des Werkzeugverhaltens von {@code BlockFusionComponent} / {@code BlockToolConversion}
 * (1.7.10): Ein Schneidbrenner verschweisst die rohe BSCCO-Spule zum fertigen Bauteil, wenn der
 * Spieler eine gegossene Stahlplatte dabei hat.
 *
 * <p>Im Original sind das vier Metadaten eines Blocks; in diesem Port existieren die Varianten seit
 * jeher als eigene Bloecke, deshalb setzt die Umwandlung hier den Nachbarblock statt der Metadaten.
 * Die Umwandlungstabelle steht 1:1 in {@code BlockToolConversion.registerRecipes()}:
 * {@code ToolType.TORCH + fusion_component:0 + STEEL.plateCast() -> fusion_component:1}.</p>
 */
public class FusionComponentBlock extends Block {

    public FusionComponentBlock(Properties properties) {
        super(properties);
    }

    private static boolean isTorch(ItemStack stack) {
        return stack.is(ModItems.BLOWTORCH.get()) || stack.is(ModItems.ACETYLENE_TORCH.get());
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        return convert(level, pos, player, hand);
    }
    //?} else {
    /*@Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        return convert(level, pos, player, hand);
    }
    *///?}

    private InteractionResult convert(Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (!isTorch(player.getItemInHand(hand))) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        // Original: InventoryUtil.doesPlayerHaveAStacks(player, list, true) - Material wird verbraucht.
        if (!player.isCreative() && !consumePlate(player)) return InteractionResult.CONSUME;

        level.setBlock(pos, ModBlocks.FUSION_COMPONENT_BSCCO_WELDED.get().defaultBlockState(), 3);
        level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.5F, 1.5F);
        return InteractionResult.CONSUME;
    }

    private static boolean consumePlate(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ModItems.PLATE_CAST_STEEL.get())) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }
}
