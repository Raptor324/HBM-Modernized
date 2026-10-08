package com.hbm_m.item.tools_and_armor;

import com.hbm_m.blockentity.network.IDroneLinkable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1:1 {@code com.hbm.items.tool.ItemDroneLinker}: erster Klick auf einen {@link IDroneLinkable}-Block merkt die Position
 * (x/y/z im Stack-NBT), jeder weitere Klick verlinkt die gemerkte Position mit dem angeklickten Block und merkt sich diesen.
 * Klick in die Luft loescht die Position. Solange das Werkzeug in der Hand ist, zeigt das HUD "Prev pos: x / y / z".
 */
public class ItemDroneLinker extends Item {

    /** Original {@code MainRegistry.proxy.ID_DRONE}. */
    public static final int ID_DRONE = 2101;

    public ItemDroneLinker(Properties properties) {
        super(properties);
    }

    private MutableComponent prefix() {
        return Component.literal("[").withStyle(ChatFormatting.DARK_AQUA)
                .append(Component.translatable(this.getDescriptionId()).withStyle(ChatFormatting.DARK_AQUA))
                .append(Component.literal("] ").withStyle(ChatFormatting.DARK_AQUA));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        BlockEntity tile = world.getBlockEntity(pos);

        if (tile instanceof IDroneLinkable linkable) {

            if (!world.isClientSide && player != null) {
                ItemStack stack = context.getItemInHand();
                CompoundTag tag = com.hbm_m.platform.PlatformHooks.getItemTag(stack);

                if (tag == null || !tag.contains("x")) {
                    com.hbm_m.platform.PlatformHooks.editItemTag(stack, t -> {
                        t.putInt("x", pos.getX());
                        t.putInt("y", pos.getY());
                        t.putInt("z", pos.getZ());
                    });

                    player.sendSystemMessage(prefix().append(Component.literal("Set initial position!").withStyle(ChatFormatting.AQUA)));

                } else {

                    int tx = tag.getInt("x");
                    int ty = tag.getInt("y");
                    int tz = tag.getInt("z");

                    BlockEntity prev = world.getBlockEntity(new BlockPos(tx, ty, tz));

                    if (prev instanceof IDroneLinkable prevLinkable) {

                        prevLinkable.setNextTarget(linkable.getDronePoint());

                        player.sendSystemMessage(prefix().append(Component.literal("Link set!").withStyle(ChatFormatting.AQUA)));
                    } else {
                        player.sendSystemMessage(prefix().append(Component.literal("Previous link lost!").withStyle(ChatFormatting.RED)));
                    }

                    com.hbm_m.platform.PlatformHooks.editItemTag(stack, t -> {
                        t.putInt("x", pos.getX());
                        t.putInt("y", pos.getY());
                        t.putInt("z", pos.getZ());
                    });
                }
            }

            return InteractionResult.sidedSuccess(world.isClientSide);
        }

        return InteractionResult.PASS;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean inhand) {
        if (world.isClientSide && inhand) {
            CompoundTag tag = com.hbm_m.platform.PlatformHooks.getItemTag(stack);
            if (tag != null && tag.contains("x")) {
                int x = tag.getInt("x");
                int y = tag.getInt("y");
                int z = tag.getInt("z");
                com.hbm_m.client.overlay.OverlayInfoToast.show(Component.literal("Prev pos: " + x + " / " + y + " / " + z), 20, ID_DRONE);
            }
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        CompoundTag tag = com.hbm_m.platform.PlatformHooks.getItemTag(stack);

        if (!world.isClientSide && tag != null && tag.contains("x")) {
            com.hbm_m.platform.PlatformHooks.editItemTag(stack, t -> {
                t.remove("x");
                t.remove("y");
                t.remove("z");
            });

            player.sendSystemMessage(prefix().append(Component.literal("Position cleared!").withStyle(ChatFormatting.GREEN)));
        }

        return InteractionResultHolder.sidedSuccess(stack, world.isClientSide);
    }
}
