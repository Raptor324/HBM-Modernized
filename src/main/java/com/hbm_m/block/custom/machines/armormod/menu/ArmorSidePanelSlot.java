package com.hbm_m.block.custom.machines.armormod.menu;

// Этот класс отвечает за боковую панель со слотами брони на игроке в GUI стола модификации брони.
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.EquipmentSlot;

import javax.annotation.Nonnull;

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
    public void onTake(@Nonnull Player pPlayer, @Nonnull ItemStack pStack) {
        if (pStack.getItem() instanceof ArmorItem armorItem) {
            playSound(armorItem.getEquipSound());
        }
        super.onTake(pPlayer, pStack);
    }

    /**
     * Переопределяем метод set, чтобы отловить момент установки предмета.
     * Это наше событие "надевания" брони.
     */
    @Override
    public void set(@Nonnull ItemStack pStack) {
        // Мы хотим проиграть звук, только если предмет действительно изменился
        if (!ItemStack.isSameItem(this.getItem(), pStack) && pStack.getItem() instanceof ArmorItem armorItem) {
            playSound(armorItem.getEquipSound());
        }
        super.set(pStack);
    }

    private void playSound(SoundEvent sound) {
        // Воспроизводим звук на позиции игрока
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    // Валидация и прочие методы, как в ванильном слоте брони

    @Override
    public boolean mayPlace(@Nonnull ItemStack stack) {
        return stack.canEquip(this.slotType, this.player);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }
}