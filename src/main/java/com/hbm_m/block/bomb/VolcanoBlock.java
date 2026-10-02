package com.hbm_m.block.bomb;

import java.util.List;

import javax.annotation.Nullable;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.bomb.VolcanoCoreBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * 1:1 {@code BlockVolcano} ({@code volcano_core}, {@code volcano_rad_core}): Vulkankern mit fuenf Arten (Meta als
 * {@code MODE}, der Gegenstand traegt sie als {@code BlockStateTag}): 0 aktiv, 1 aktiv/erlischt, 2 waechst, 3 waechst/
 * erlischt, 4 Schildvulkan (schwelend).
 */
public class VolcanoBlock extends BaseEntityBlock {

    public static final int META_STATIC_ACTIVE = 0;
    public static final int META_STATIC_EXTINGUISHING = 1;
    public static final int META_GROWING_ACTIVE = 2;
    public static final int META_GROWING_EXTINGUISHING = 3;
    public static final int META_SMOLDERING = 4;

    public static final IntegerProperty MODE = IntegerProperty.create("mode", 0, 4);

    public final boolean radioactive;

    public VolcanoBlock(Properties properties, boolean radioactive) {
        super(properties);
        this.radioactive = radioactive;
        registerDefaultState(stateDefinition.any().setValue(MODE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MODE);
    }

    public static boolean isGrowing(int meta) { return meta == META_GROWING_ACTIVE || meta == META_GROWING_EXTINGUISHING; }
    public static boolean isExtinguishing(int meta) { return meta == META_STATIC_EXTINGUISHING || meta == META_GROWING_EXTINGUISHING; }

    /** Gegenstand einer Art (Original: Item-Schaden). */
    public static ItemStack stack(Item item, int mode) {
        ItemStack s = new ItemStack(item);
        CompoundTag bst = new CompoundTag();
        bst.putString("mode", Integer.toString(mode));
        s.getOrCreateTag().put("BlockStateTag", bst);
        return s;
    }

    public static int modeOf(ItemStack stack) {
        CompoundTag t = stack.getTag();
        if (t == null || !t.contains("BlockStateTag")) return 0;
        try { return Integer.parseInt(t.getCompound("BlockStateTag").getString("mode")); } catch (NumberFormatException e) { return 0; }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> list, TooltipFlag flag) {
        int meta = modeOf(stack);
        if (meta == META_SMOLDERING) {
            list.add(Component.literal("SHIELD VOLCANO").withStyle(ChatFormatting.GOLD));
            return;
        }
        list.add(isGrowing(meta) ? Component.literal("DOES GROW").withStyle(ChatFormatting.RED) : Component.literal("DOES NOT GROW").withStyle(ChatFormatting.DARK_GRAY));
        list.add(isExtinguishing(meta) ? Component.literal("DOES EXTINGUISH").withStyle(ChatFormatting.RED) : Component.literal("DOES NOT EXTINGUISH").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return stack(asItem(), state.getValue(MODE));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VolcanoCoreBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.VOLCANO_CORE.get(), VolcanoCoreBlockEntity::serverTick);
    }
}
