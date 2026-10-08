package com.hbm_m.item.special;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.inventory.HeldItemInventory;
import com.hbm_m.item.ITooltipProvider;
import com.hbm_m.item.ModItems;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code ItemKitNBT}: ein Paket, dessen Inhalt als Stapelliste im NBT steht. Rechtsklick legt alles ins Inventar
 * (Ueberlauf faellt wie im Original ins Leere - {@code addItemStackToInventory} ohne Rueckgabe), Tooltip listet den Inhalt.
 */
public class ItemKitNBT extends Item implements ITooltipProvider {

    public ItemKitNBT(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {

        ItemStack stack = player.getItemInHand(hand);
        ItemStack[] stacks = HeldItemInventory.readStacksFromNBT(stack, 0);

        if (!world.isClientSide && stacks != null) {
            for (ItemStack item : stacks) {
                if (item != null) {
                    player.getInventory().add(item.copy());
                }
            }
        }

        ItemStack container = stack.getItem().hasCraftingRemainingItem() ? new ItemStack(stack.getItem().getCraftingRemainingItem()) : ItemStack.EMPTY;

        stack.shrink(1);

        if (!container.isEmpty()) {
            if (stack.getCount() > 0) {
                player.getInventory().add(container.copy());
            } else {
                stack = container.copy();
            }
        }

        world.playSound(null, player.getX(), player.getY(), player.getZ(), HbmSoundsNT.get("hbm:item.unpack"), SoundSource.PLAYERS, 1.0F, 1.0F);

        return InteractionResultHolder.sidedSuccess(stack, world.isClientSide);
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {

        ItemStack[] stacks = HeldItemInventory.readStacksFromNBT(stack, 0);

        if (stacks != null) {

            list.add(Component.literal("Contains:"));

            for (ItemStack item : stacks) {
                if (item == null) continue;
                list.add(Component.literal("-").append(item.getHoverName()).append(item.getCount() > 1 ? (" x" + item.getCount()) : ""));
            }
        }
    }

    public static ItemStack create(ItemStack... contents) {
        ItemStack stack = new ItemStack(ModItems.KIT_CUSTOM.get());
        stack.getOrCreateTag();
        HeldItemInventory.addStacksToNBT(stack, contents);
        return stack;
    }
}
