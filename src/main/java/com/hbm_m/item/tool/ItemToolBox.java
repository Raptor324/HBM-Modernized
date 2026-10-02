package com.hbm_m.item.tool;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

import com.hbm_m.inventory.HeldItemInventory;
import com.hbm_m.inventory.menu.HeldItemMenu;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code com.hbm.items.tool.ItemToolBox} ({@code toolbox}): 24 Plaetze in drei Achterreihen. Rechtsklick tauscht
 * die Hotbar (ohne die Kiste selbst) reihum mit den Reihen der Kiste, Schleich-Rechtsklick oeffnet sie
 * ({@code isOpen} zeigt dabei die offene Kiste). Werkzeugkisten in der Hotbar fallen beim Tausch heraus.
 */
public class ItemToolBox extends Item {

    public static final int SIZE = 24;

    public ItemToolBox(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 1;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        list.add(Component.literal("Click with the toolbox to swap hotbars in/out of the toolbox."));
        list.add(Component.literal("Shift-click with the toolbox to open the toolbox."));
    }

    private static ItemStack[] read(ItemStack box) {
        return HeldItemInventory.readStacksFromNBT(box, SIZE);
    }

    // Finds active rows in the toolbox (rows with items inside them).
    public List<Integer> getActiveRows(ItemStack box) {
        ItemStack[] stacks = read(box);
        if (stacks == null)
            return new ArrayList<>();
        List<Integer> activeRows = new ArrayList<>();
        for (int row = 0; row < 3; row++) {
            for (int slot = 0; slot < 8; slot++) {
                if (stacks[row * 8 + slot] != null) {
                    activeRows.add(row);
                    break;
                }
            }
        }
        return activeRows;
    }

    // This function genuinely hurts my soul, but it works...
    public void moveRows(ItemStack box, Player player) {

        // Move from hotbar into array in preparation for boxing.
        ItemStack[] endingHotBar = new ItemStack[9];
        ItemStack[] stacksToTransferToBox = new ItemStack[8];

        boolean hasToolbox = false;
        int extraToolboxes = 0;
        int current = player.getInventory().selected;
        for (int i = 0; i < 9; i++) { // Maximum allowed HotBar size is 9.

            ItemStack slot = player.getInventory().getItem(i);

            if (!slot.isEmpty() && slot.getItem() == this && i != current) {

                extraToolboxes++;
                player.drop(slot, true);
                player.getInventory().setItem(i, ItemStack.EMPTY);

            } else if (i == current) {
                hasToolbox = true;
                endingHotBar[i] = slot;
            } else {
                stacksToTransferToBox[i - (hasToolbox ? 1 : 0)] = slot.isEmpty() ? null : slot;
            }
        }

        if (extraToolboxes > 0) {
            if (extraToolboxes == 1)
                player.sendSystemMessage(Component.literal("You can't toolbox a toolbox... ").withStyle(ChatFormatting.RED));
            else
                player.sendSystemMessage(Component.literal("You can't toolbox a toolbox... (x" + extraToolboxes + ")").withStyle(ChatFormatting.RED));
        }

        // Move stacks around inside the box, mostly shifts rows to other rows and shifts the top row to the hotbar.
        ItemStack[] stacks = read(box);
        ItemStack[] endingStacks = new ItemStack[SIZE];

        int lowestActiveIndex = Integer.MAX_VALUE; // Lowest active index to find which row to move *to* the hotbar.
        int lowestInactiveIndex = Integer.MAX_VALUE; // Lowest *inactive* index to find which row to move the hotbar to.

        if (stacks != null) {
            List<Integer> activeRows = getActiveRows(box);

            { // despair
                for (int i = 0; i < 3; i++) {
                    if (activeRows.contains(i))
                        lowestActiveIndex = Math.min(i, lowestActiveIndex);
                    else
                        lowestInactiveIndex = Math.min(i, lowestInactiveIndex);
                }

                if (lowestInactiveIndex > 2) // No inactive rows...
                    lowestInactiveIndex = 2; // Set to the last possible row; the items will be moved out of the way in time.
                else
                    lowestInactiveIndex = Math.max(0, lowestInactiveIndex - 1); // A little shittery to make items pop into the row that's *going* to be empty.
            }

            for (Integer activeRowIndex : activeRows) {

                int activeIndex = 8 * activeRowIndex;

                if (activeRowIndex == lowestActiveIndex) { // Items to "flow" to the hotbar.
                    hasToolbox = false;
                    for (int i = 0; i < 9; i++) {
                        if (i == current) {
                            hasToolbox = true;
                            continue;
                        }
                        endingHotBar[i] = stacks[activeIndex + i - (hasToolbox ? 1 : 0)];
                    }
                    continue;
                }

                int targetIndex = 8 * (activeRowIndex - 1);

                System.arraycopy(stacks, activeIndex, endingStacks, targetIndex, 8);
            }
        }

        if (stacks == null)
            lowestInactiveIndex = 0; // Fix crash relating to a null NBT causing this value to be Integer.MAX_VALUE.

        // Finally, move all temporary arrays into their respective locations.
        System.arraycopy(stacksToTransferToBox, 0, endingStacks, lowestInactiveIndex * 8, 8);

        for (int i = 0; i < endingHotBar.length; i++) {
            player.getInventory().setItem(i, endingHotBar[i] == null ? ItemStack.EMPTY : endingHotBar[i]);
        }

        box.setTag(new CompoundTag());
        HeldItemInventory.addStacksToNBT(box, endingStacks);

        CompoundTag nbt = box.getTag();

        if (!nbt.isEmpty()) {
            Random random = new Random();

            try {
                java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                NbtIo.writeCompressed(nbt, out);

                if (out.size() > 6000) {
                    player.sendSystemMessage(Component.literal("Warning: Container NBT exceeds 6kB, contents will be ejected!").withStyle(ChatFormatting.RED));
                    ItemStack[] stacks1 = read(box);
                    if (stacks1 == null)
                        return;
                    for (ItemStack itemstack : stacks1) {

                        if (itemstack != null) {
                            float f = random.nextFloat() * 0.8F + 0.1F;
                            float f1 = random.nextFloat() * 0.8F + 0.1F;
                            float f2 = random.nextFloat() * 0.8F + 0.1F;

                            while (itemstack.getCount() > 0) {
                                int j1 = random.nextInt(21) + 10;

                                if (j1 > itemstack.getCount()) {
                                    j1 = itemstack.getCount();
                                }

                                ItemEntity entityitem = new ItemEntity(player.level(), player.getX() + f, player.getY() + f1, player.getZ() + f2, itemstack.split(j1));

                                float f3 = 0.05F;
                                entityitem.setDeltaMovement((float) random.nextGaussian() * f3 + player.getDeltaMovement().x,
                                        (float) random.nextGaussian() * f3 + 0.2F + player.getDeltaMovement().y,
                                        (float) random.nextGaussian() * f3 + player.getDeltaMovement().z);
                                player.level().addFreshEntity(entityitem);
                            }
                        }
                    }

                    box.setTag(new CompoundTag()); // Reset.
                }
            } catch (java.io.IOException ignored) { }
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!world.isClientSide) {
            if (!player.isShiftKeyDown()) {
                moveRows(stack, player);
                player.inventoryMenu.broadcastChanges();
            } else if (player instanceof ServerPlayer sp) {
                stack.getOrCreateTag().putBoolean("isOpen", true);
                MenuRegistry.openExtendedMenu(sp, new MenuProvider() {
                    @Override public Component getDisplayName() { return stack.getHoverName(); }
                    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) { return new HeldItemMenu(id, inv, HeldItemMenu.Layout.TOOLBOX, hand); }
                }, buf -> {
                    buf.writeEnum(HeldItemMenu.Layout.TOOLBOX);
                    buf.writeEnum(hand);
                });
            }
        }
        return InteractionResultHolder.success(stack);
    }

    /** InventoryToolBox.closeInventory: Kiste wieder zu, Zufallswert erzwingt die Synchronisierung. */
    public static void onClose(Player player, ItemStack box) {
        if (box.hasTag()) {
            box.getTag().remove("isOpen");
            box.getTag().putInt("rand", player.level().random.nextInt());
        }
        player.inventoryMenu.broadcastChanges();
    }
}
