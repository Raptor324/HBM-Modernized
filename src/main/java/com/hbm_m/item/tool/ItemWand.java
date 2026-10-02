package com.hbm_m.item.tool;

import java.util.List;

import javax.annotation.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ITooltipProvider;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code com.hbm.items.tool.ItemWand} ({@code wand_k}, Konstruktionsstab, nur Kreativ): Schleichklick merkt den
 * Block (Blockzustand statt ID+Meta), zwei Klicks fuellen den Quader; mit {@code wand_air} nur die leeren Felder.
 */
public class ItemWand extends Item implements ITooltipProvider {

    public ItemWand(Properties properties) {
        super(properties.stacksTo(1));
    }

    private static BlockState saved(CompoundTag tag) {
        if (!tag.contains("state")) return Blocks.AIR.defaultBlockState();
        return NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound("state"));
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        list.add(Component.literal("Creative-only item"));
        list.add(Component.literal("\"Destruction brings creation\""));
        list.add(Component.literal("(Set positions with right click,"));
        list.add(Component.literal("set block with shift-right click!)"));

        CompoundTag tag = stack.getTag();
        if (tag != null && !(tag.getInt("x") == 0 && tag.getInt("y") == 0 && tag.getInt("z") == 0)) {
            list.add(Component.literal("Pos: " + tag.getInt("x") + ", " + tag.getInt("y") + ", " + tag.getInt("z")));
        } else {
            list.add(Component.literal("Positions not set!"));
        }

        if (tag != null)
            list.add(Component.literal("Block saved: ").append(saved(tag).getBlock().getName()));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        ItemStack stack = ctx.getItemInHand();
        Level world = ctx.getLevel();
        Player player = ctx.getPlayer();
        BlockPos pos = ctx.getClickedPos();
        CompoundTag tag = stack.getOrCreateTag();

        if (player != null && player.isShiftKeyDown()) {
            tag.put("state", NbtUtils.writeBlockState(world.getBlockState(pos)));

            if (world.isClientSide)
                player.sendSystemMessage(Component.literal("Set block ").append(saved(tag).getBlock().getName()));
        } else {
            if (tag.getInt("x") == 0 && tag.getInt("y") == 0 && tag.getInt("z") == 0) {
                tag.putInt("x", pos.getX());
                tag.putInt("y", pos.getY());
                tag.putInt("z", pos.getZ());

                if (world.isClientSide && player != null)
                    player.sendSystemMessage(Component.literal("Position set!"));
            } else {
                int ox = tag.getInt("x");
                int oy = tag.getInt("y");
                int oz = tag.getInt("z");

                tag.putInt("x", 0);
                tag.putInt("y", 0);
                tag.putInt("z", 0);

                if (!world.isClientSide) {
                    BlockState block = saved(tag);
                    boolean replaceAir = block.is(ModBlocks.WAND_AIR.get());

                    for (int i = Math.min(ox, pos.getX()); i <= Math.max(ox, pos.getX()); i++) {
                        for (int j = Math.min(oy, pos.getY()); j <= Math.max(oy, pos.getY()); j++) {
                            for (int k = Math.min(oz, pos.getZ()); k <= Math.max(oz, pos.getZ()); k++) {
                                BlockPos p = new BlockPos(i, j, k);
                                if (replaceAir && !world.getBlockState(p).isAir()) continue;
                                world.setBlock(p, block, 3);
                            }
                        }
                    }
                }

                if (world.isClientSide && player != null)
                    player.sendSystemMessage(Component.literal("Selection filled!"));
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        CompoundTag tag = stack.getOrCreateTag();

        if (player.isShiftKeyDown()) {
            tag.remove("state");

            if (world.isClientSide)
                player.sendSystemMessage(Component.literal("Set block ").append(saved(tag).getBlock().getName()));
        }

        return InteractionResultHolder.pass(stack);
    }
}
