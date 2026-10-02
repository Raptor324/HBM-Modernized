package com.hbm_m.inventory;

import java.util.function.BiPredicate;

import javax.annotation.Nullable;

import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code com.hbm.items.ItemInventory} (+ die eigenstaendigen Inventare von Plastiktuete/Huelsentasche): ein
 * Inventar, das im NBT des getragenen Gegenstands liegt ({@code ItemStackUtil}-Format: Liste "items" mit Byte
 * "slot"). Jede Aenderung schreibt sofort zurueck ({@code markDirty}).
 */
public class HeldItemInventory extends SimpleContainer {

    @Nullable public final Player player;
    public final ItemStack target;
    private final int stackLimit;
    private final BiPredicate<Integer, ItemStack> validator;
    private final boolean crateSounds;
    private final boolean checkSize;

    public HeldItemInventory(@Nullable Player player, ItemStack target, int size, int stackLimit,
                             BiPredicate<Integer, ItemStack> validator, boolean crateSounds, boolean checkSize) {
        super(size);
        this.player = player;
        this.target = target;
        this.stackLimit = stackLimit;
        this.validator = validator;
        this.crateSounds = crateSounds;
        this.checkSize = checkSize;

        if (!target.hasTag()) target.setTag(new CompoundTag());
        ItemStack[] fromNBT = readStacksFromNBT(target, size);
        if (fromNBT != null) {
            for (int i = 0; i < size; i++) {
                if (fromNBT[i] != null) super.setItem(i, fromNBT[i]);
            }
        }
    }

    /** {@code ItemStackUtil.readStacksFromNBT}. */
    @Nullable
    public static ItemStack[] readStacksFromNBT(ItemStack stack, int count) {
        if (!stack.hasTag()) return null;
        ListTag list = stack.getTag().getList("items", 10);
        if (count == 0) count = list.size();
        ItemStack[] stacks = new ItemStack[count];
        for (int i = 0; i < list.size() && i < count; i++) {
            CompoundTag slotNBT = list.getCompound(i);
            byte slot = slotNBT.getByte("slot");
            ItemStack loaded = ItemStack.of(slotNBT);
            if (slot >= 0 && slot < stacks.length && !loaded.isEmpty()) stacks[slot] = loaded;
        }
        return stacks;
    }

    /** {@code ItemStackUtil.addStacksToNBT}. */
    public static void addStacksToNBT(ItemStack stack, ItemStack... stacks) {
        if (!stack.hasTag()) stack.setTag(new CompoundTag());
        ListTag tags = new ListTag();
        for (int i = 0; i < stacks.length; i++) {
            if (stacks[i] != null && !stacks[i].isEmpty()) {
                CompoundTag slotNBT = new CompoundTag();
                slotNBT.putByte("slot", (byte) i);
                stacks[i].save(slotNBT);
                tags.add(slotNBT);
            }
        }
        stack.getTag().put("items", tags);
    }

    @Override
    public int getMaxStackSize() {
        return stackLimit;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return validator.test(slot, stack);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        ItemStack[] slots = new ItemStack[getContainerSize()];
        for (int i = 0; i < slots.length; i++) slots[i] = getItem(i);
        addStacksToNBT(target, slots);
        if (checkSize) checkNBT(target.getTag());
    }

    /** {@code ItemInventory.checkNBT}: ueber 6 kB wird der Inhalt ausgeworfen. */
    private void checkNBT(@Nullable CompoundTag nbt) {
        if (nbt == null || nbt.isEmpty() || player == null) return;
        try {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            NbtIo.writeCompressed(nbt, out);
            if (out.size() > 6000) {
                player.sendSystemMessage(Component.literal("Warning: Container NBT exceeds 6kB, contents will be ejected!").withStyle(ChatFormatting.RED));
                java.util.Random random = new java.util.Random();
                for (int i1 = 0; i1 < getContainerSize(); ++i1) {
                    ItemStack itemstack = removeItemNoUpdate(i1);
                    if (itemstack.isEmpty()) continue;
                    float f = random.nextFloat() * 0.8F + 0.1F;
                    float f1 = random.nextFloat() * 0.8F + 0.1F;
                    float f2 = random.nextFloat() * 0.8F + 0.1F;
                    while (itemstack.getCount() > 0) {
                        int j1 = random.nextInt(21) + 10;
                        if (j1 > itemstack.getCount()) j1 = itemstack.getCount();
                        ItemStack part = itemstack.split(j1);
                        ItemEntity entityitem = new ItemEntity(player.level(), player.getX() + f, player.getY() + f1, player.getZ() + f2, part);
                        float f3 = 0.05F;
                        entityitem.setDeltaMovement(random.nextGaussian() * f3 + player.getDeltaMovement().x,
                                random.nextGaussian() * f3 + 0.2F + player.getDeltaMovement().y,
                                random.nextGaussian() * f3 + player.getDeltaMovement().z);
                        player.level().addFreshEntity(entityitem);
                    }
                }
                addStacksToNBT(target);
            }
        } catch (java.io.IOException ignored) { }
    }

    @Override
    public void startOpen(Player p) {
        if (crateSounds && player != null && !player.level().isClientSide)
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), HbmSoundsNT.get("hbm:block.crateOpen"), SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    @Override
    public void stopOpen(Player p) {
        if (crateSounds && player != null && !player.level().isClientSide)
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), HbmSoundsNT.get("hbm:block.crateClose"), SoundSource.PLAYERS, 1.0F, 0.8F);
    }
}
