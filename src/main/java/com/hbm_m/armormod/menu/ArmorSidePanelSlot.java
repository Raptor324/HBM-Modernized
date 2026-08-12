package com.hbm_m.armormod.menu;

// Этот класс отвечает за боковую панель со слотами брони на игроке в GUI стола модификации брони.
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.EquipmentSlot;

public class ArmorSidePanelSlot extends Slot {

    private final Player player;
    private final EquipmentSlot slotType;

    public ArmorSidePanelSlot(Inventory pContainer, int pIndex, int pX, int pY, Player player, EquipmentSlot slotType) {
        super(pContainer, pIndex, pX, pY);
        this.player = player;
        this.slotType = slotType;
    }

    /**
     * Вызывается, когда игрок забирает предмет из слота.
     * Это наше событие "снятия" брони.
     */
    @Override
    public void onTake(Player pPlayer, ItemStack pStack) {
        if (pStack.getItem() instanceof ArmorItem armorItem) {
            //? if < 1.21.1 {
            playSound(armorItem.getEquipSound());
            //?} else {
            /*playSound(armorItem.getEquipSound().value());
            *///?}
        }
        super.onTake(pPlayer, pStack);
    }

    /**
     * Переопределяем метод set, чтобы отловить момент установки предмета.
     * Это наше событие "надевания" брони.
     */
    @Override
    public void set(ItemStack pStack) {
        if (!ItemStack.isSameItem(this.getItem(), pStack) && pStack.getItem() instanceof ArmorItem armorItem) {
            //? if < 1.21.1 {
            playSound(armorItem.getEquipSound());
            //?} else {
            /*playSound(armorItem.getEquipSound().value());
            *///?}
        }
        super.set(pStack);
    }

    private void playSound(SoundEvent sound) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        //? if < 1.21.1 {
        return Mob.getEquipmentSlotForItem(stack) == this.slotType;
        //?} else {
        /*return this.player.getEquipmentSlotForItem(stack) == this.slotType;
        *///?}
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }
}