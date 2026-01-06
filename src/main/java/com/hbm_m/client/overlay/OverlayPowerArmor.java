package com.hbm_m.client.overlay;

import com.hbm_m.item.armor.ModPowerArmorItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11;

/**
 * Оверлей для силовой брони.
 * Показывает индикатор заряда брони над health баром.
 * В оригинале рендерится с помощью Tessellator без текстур.
 */
public class OverlayPowerArmor {

    public static void onRenderOverlay(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player == null || mc.options.hideGui) {
            return;
        }

        // Проверяем, носит ли игрок силовую броню
        if (!ModPowerArmorItem.hasFSBArmor(player)) {
            return;
        }

        // Получаем нагрудник
        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!(chestplate.getItem() instanceof ModPowerArmorItem armorItem)) {
            return;
        }

        // Получаем все части брони для отображения
        ItemStack[] armorPieces = new ItemStack[] {
            player.getItemBySlot(EquipmentSlot.HEAD),
            player.getItemBySlot(EquipmentSlot.CHEST),
            player.getItemBySlot(EquipmentSlot.LEGS),
            player.getItemBySlot(EquipmentSlot.FEET)
        };

        boolean noHelmet = armorItem.getSpecs().noHelmetRequired;
        int piecesToShow = noHelmet ? 3 : 4; // Если шлем не требуется, показываем только 3 части

        // Позиция для полосок - слева от health бара
        int left = screenWidth / 2 - 91;
        int height = screenHeight - 39; // Над health баром

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();

        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();

        buffer.begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (int i = 0; i < piecesToShow; i++) {
            ItemStack stack = armorPieces[i];
            if (!(stack.getItem() instanceof ModPowerArmorItem pieceArmor)) {
                continue;
            }

            // Рассчитываем уровень заряда для этой части брони
            long currentEnergy = pieceArmor.getEnergy(stack);
            long maxEnergy = pieceArmor.getModifiedCapacity(stack); // Учитываем модификаторы батареи

            if (maxEnergy <= 0) continue;

            float energyPercent = (float) currentEnergy / maxEnergy;

            int top = height - (piecesToShow - 1 - i) * 3; // Располагаем полоски друг над другом

            // Фон полоски (серый)
            buffer.vertex(left - 0.5, top - 0.5, 0).color(0.25f, 0.25f, 0.25f, 1.0f).endVertex();
            buffer.vertex(left - 0.5, top + 1.5, 0).color(0.25f, 0.25f, 0.25f, 1.0f).endVertex();
            buffer.vertex(left + 81.5, top + 1.5, 0).color(0.25f, 0.25f, 0.25f, 1.0f).endVertex();
            buffer.vertex(left + 81.5, top - 0.5, 0).color(0.25f, 0.25f, 0.25f, 1.0f).endVertex();

            // Заполнение полоски (зеленый для энергии)
            if (energyPercent > 0) {
                float red = 1.0f - energyPercent;
                float green = energyPercent;

                buffer.vertex(left, top, 0).color(red, green, 0.0f, 1.0f).endVertex();
                buffer.vertex(left, top + 1, 0).color(red, green, 0.0f, 1.0f).endVertex();
                buffer.vertex(left + 81 * energyPercent, top + 1, 0).color(red, green, 0.0f, 1.0f).endVertex();
                buffer.vertex(left + 81 * energyPercent, top, 0).color(red, green, 0.0f, 1.0f).endVertex();
            }
        }

        tessellator.end();

        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    public static final net.minecraftforge.client.gui.overlay.IGuiOverlay POWER_ARMOR_OVERLAY = OverlayPowerArmor::onRenderOverlay;
}

