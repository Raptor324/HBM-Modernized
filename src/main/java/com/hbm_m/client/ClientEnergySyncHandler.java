package com.hbm_m.client;

import com.hbm_m.api.energy.ILongEnergyMenu;
import com.hbm_m.powerarmor.ModArmorFSBPowered;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientEnergySyncHandler {

    private static final int ARMOR_SLOT_HEAD = ModArmorFSBPowered.ARMOR_SLOT_HEAD;
    private static final int ARMOR_SLOT_CHEST = ModArmorFSBPowered.ARMOR_SLOT_CHEST;
    private static final int ARMOR_SLOT_LEGS = ModArmorFSBPowered.ARMOR_SLOT_LEGS;
    private static final int ARMOR_SLOT_FEET = ModArmorFSBPowered.ARMOR_SLOT_FEET;

    // Добавили аргумент long delta
    public static void handle(int containerId, long energy, long maxEnergy, long delta) {
        if (containerId < 0) {
            handleArmorEnergy(containerId, energy, maxEnergy);
            return;
        }
        Player player = Minecraft.getInstance().player;
        if (player != null && player.containerMenu != null) {
            if (player.containerMenu.containerId == containerId &&
                    player.containerMenu instanceof ILongEnergyMenu menu) {
                // Передаем дельту в меню
                menu.setEnergy(energy, maxEnergy, delta);
            }
        }
    }

    private static void handleArmorEnergy(int containerId, long energy, long maxEnergy) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        
        // Декодируем player ID и слот из отрицательного containerId
        int absId = -containerId;
        int playerId = absId / 4;
        int slotIndex = absId % 4;
        
        // Применяем только к локальному игроку (свой заряд)
        if (playerId != mc.player.getId()) return;
        
        EquipmentSlot slot = switch (slotIndex) {
            case 0 -> EquipmentSlot.HEAD;
            case 1 -> EquipmentSlot.CHEST;
            case 2 -> EquipmentSlot.LEGS;
            case 3 -> EquipmentSlot.FEET;
            default -> null;
        };
        
        if (slot == null) return;
        
        ItemStack armorStack = mc.player.getItemBySlot(slot);
        if (armorStack.getItem() instanceof ModArmorFSBPowered) {
            // Обновляем отображаемый заряд на клиенте
            // (реальный заряд хранится на сервере, клиент только отображает)
            CompoundTag tag = armorStack.getOrCreateTag();
            tag.putLong("charge", energy);
        }
    }
}