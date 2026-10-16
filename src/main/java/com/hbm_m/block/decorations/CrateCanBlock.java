package com.hbm_m.block.decorations;

import com.hbm_m.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.SimpleContainer;

/**
 * Порт {@code BlockCanCrate} (1.7.10, crate_can): ящик с консервами.
 * Инвентаря не хранит — клик ломом выбивает случайные консервы
 * (лут-таблица {@code hbm_m:crates/crate_can}) и разрушает ящик.
 */
public class CrateCanBlock extends Block {

    public CrateCanBlock(Properties props) {
        super(props);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return hbmOnUse(state, level, pos, player, hand);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return hbmOnUse(state, level, pos, player, InteractionHand.MAIN_HAND);
    }
    *///?}

    private InteractionResult hbmOnUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (!isCrowbar(player.getItemInHand(hand))) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            dropContents(level, pos);
            level.destroyBlock(pos, false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static boolean isCrowbar(ItemStack stack) {
        return stack.getItem() == ModItems.CROWBAR.get();
    }

    /** Аналог {@code BlockCanCrate.dropContents} — выбить лут-таблицу на пол. */
    private void dropContents(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        ResourceLocation tableResource = ResourceLocation.fromNamespaceAndPath(com.hbm_m.lib.RefStrings.MODID, "crates/crate_can");
        LootTable table;
        //? if < 1.21.1 {
        table = serverLevel.getServer().getLootData().getLootTable(tableResource);
        //?} else {
        /*table = serverLevel.getServer().reloadableRegistries().getLootTable(
                net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, tableResource));
        *///?}
        if (table == LootTable.EMPTY) return;

        LootParams params = new LootParams.Builder(serverLevel)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .create(LootContextParamSets.CHEST);
        SimpleContainer temp = new SimpleContainer(27);
        table.fill(temp, params, serverLevel.getRandom().nextLong());
        for (int i = 0; i < temp.getContainerSize(); i++) {
            ItemStack stack = temp.getItem(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
        }
        level.levelEvent(2001, pos, Block.getId(defaultBlockState()));
    }
}
