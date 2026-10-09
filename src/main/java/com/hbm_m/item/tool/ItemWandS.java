package com.hbm_m.item.tool;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.generic.WandStructureBlockEntity;
import com.hbm_m.platform.PlatformHooks;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
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
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code ItemWandS} (wand_s, Strukturstab, nur Kreativ): zwei Klicks spannen einen Bereich auf und setzen darunter
 * einen fertig eingestellten Struktur-Speicherblock; Schleichklick auf einen Block fuegt ihn der Ausschlussliste hinzu
 * (Blockzustand statt ID+Meta), Schleich-Rechtsklick in die Luft leert sie.
 */
public class ItemWandS extends Item {

    public ItemWandS(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    //? if < 1.21.1 {
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
    //?} else {
    /*public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext hbmTooltipCtx, List<Component> list, TooltipFlag flag) {
    *///?}
        list.add(Component.literal("Creative-only item"));
        list.add(Component.literal("\"Replication breeds decadence\""));
        list.add(Component.literal("(Saves an area defined by two right-clicks,"));
        list.add(Component.literal("adds a block to the blacklist by crouch right-clicking!)"));

        if (PlatformHooks.hasItemTag(stack)) {
            int px = PlatformHooks.getInt(stack, "x");
            int py = PlatformHooks.getInt(stack, "y");
            int pz = PlatformHooks.getInt(stack, "z");

            if (px != 0 || py != 0 || pz != 0) {
                list.add(Component.literal("From: " + px + ", " + py + ", " + pz).withStyle(ChatFormatting.AQUA));
            } else {
                list.add(Component.literal("No start position set").withStyle(ChatFormatting.AQUA));
            }

            Set<BlockState> blocks = getBlocks(stack);

            if (blocks.size() > 0) {
                list.add(Component.literal("Blacklist:"));
                for (BlockState block : blocks) {
                    list.add(Component.literal("- " + block.getBlock().getDescriptionId()).withStyle(ChatFormatting.RED));
                }
            }
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        ItemStack stack = ctx.getItemInHand();
        Level world = ctx.getLevel();
        Player player = ctx.getPlayer();
        BlockPos clicked = ctx.getClickedPos();
        int x = clicked.getX(), y = clicked.getY(), z = clicked.getZ();

        if (player != null && player.isShiftKeyDown()) {
            BlockState target = world.getBlockState(clicked);
            Set<BlockState> blocks = getBlocks(stack);

            if (blocks.contains(target)) {
                blocks.remove(target);
                if (world.isClientSide) player.sendSystemMessage(Component.literal("Removed from blacklist " + target.getBlock().getDescriptionId()));
            } else {
                blocks.add(target);
                if (world.isClientSide) player.sendSystemMessage(Component.literal("Added to blacklist " + target.getBlock().getDescriptionId()));
            }

            setBlocks(stack, blocks);

        } else {
            int px = PlatformHooks.getInt(stack, "x");
            int py = PlatformHooks.getInt(stack, "y");
            int pz = PlatformHooks.getInt(stack, "z");

            if (px == 0 && py == 0 && pz == 0) {
                setPosition(stack, x, y, z);

                if (world.isClientSide && player != null) player.sendSystemMessage(Component.literal("First position set!"));
            } else {
                setPosition(stack, 0, 0, 0);

                int minX = Math.min(x, px);
                int minY = Math.min(y, py) - 1;
                int minZ = Math.min(z, pz);

                int sizeX = Math.abs(x - px) + 1;
                int sizeY = Math.abs(y - py) + 1;
                int sizeZ = Math.abs(z - pz) + 1;

                BlockPos pos = new BlockPos(minX, minY, minZ);
                world.setBlock(pos, ModBlocks.WAND_STRUCTURE_SAVE.get().defaultBlockState(), 3);

                if (world.getBlockEntity(pos) instanceof WandStructureBlockEntity structure) {
                    structure.sizeX = sizeX;
                    structure.sizeY = sizeY;
                    structure.sizeZ = sizeZ;

                    structure.blacklist = getBlocks(stack);
                    structure.sync();
                } else {
                    if (world.isClientSide && player != null)
                        player.sendSystemMessage(Component.literal("Could not add a structure block!"));
                    return InteractionResult.sidedSuccess(world.isClientSide);
                }

                if (world.isClientSide && player != null)
                    player.sendSystemMessage(Component.literal("Structure block configured and added at: " + minX + ", " + minY + ", " + minZ));
            }
        }

        return InteractionResult.sidedSuccess(world.isClientSide);
    }

    private void setPosition(ItemStack stack, int x, int y, int z) {
        PlatformHooks.putInt(stack, "x", x);
        PlatformHooks.putInt(stack, "y", y);
        PlatformHooks.putInt(stack, "z", z);
    }

    private Set<BlockState> getBlocks(ItemStack stack) {
        Set<BlockState> blocks = new HashSet<>();
        if (!PlatformHooks.hasItemTag(stack)) {
            return blocks;
        }

        ListTag list = PlatformHooks.getItemTag(stack).getList("blocks", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            blocks.add(NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), list.getCompound(i)));
        }

        return blocks;
    }

    private void setBlocks(ItemStack stack, Set<BlockState> blocks) {
        ListTag list = new ListTag();
        for (BlockState state : blocks) list.add(NbtUtils.writeBlockState(state));
        PlatformHooks.editItemTag(stack, t -> t.put("blocks", list));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            PlatformHooks.editItemTag(stack, t -> t.put("blocks", new ListTag()));

            if (world.isClientSide) {
                player.sendSystemMessage(Component.literal("Cleared blacklist"));
            }
        }

        return InteractionResultHolder.success(stack);
    }
}
