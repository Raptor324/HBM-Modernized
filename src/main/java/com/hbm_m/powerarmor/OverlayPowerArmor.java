package com.hbm_m.powerarmor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.systems.RenderSystem;

public class OverlayPowerArmor {

    public static void onRenderOverlay(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
    
        if (player == null || mc.options.hideGui) {
            return;
        }
    
        if (!ModPowerArmorItem.hasFSBArmor(player)) {
            return;
        }
    
        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!(chestplate.getItem() instanceof ModPowerArmorItem armorItem)) {
            return;
        }
    
        ItemStack[] armorPieces = new ItemStack[] {
            player.getItemBySlot(EquipmentSlot.HEAD),
            player.getItemBySlot(EquipmentSlot.CHEST),
            player.getItemBySlot(EquipmentSlot.LEGS),
            player.getItemBySlot(EquipmentSlot.FEET)
        };
    
        boolean noHelmet = armorItem.getSpecs().noHelmetRequired;
        int piecesToShow = noHelmet ? 3 : 4;
    
        // Базовая позиция слева (центр - 91)
        int left = screenWidth / 2 - 91;
        int topAnchor;
        
        // --- ЛОГИКА ПОЗИЦИОНИРОВАНИЯ ---
        if (player.isCreative()) {
            // Креатив: фиксированная позиция над хотбаром + сдвиг вправо
            left += 1; 
            topAnchor = screenHeight - 26;
        } else {
            // Выживание:
            // ТАК КАК МЫ РЕГИСТРИРУЕМСЯ ПОСЛЕ БРОНИ (см. пункт 2 ниже),
            // gui.leftHeight УЖЕ учитывает высоту брони.
            // Нам просто нужно встать поверх текущего уровня.
            left += 1;
            topAnchor = screenHeight - gui.leftHeight + 5;
        }
        
        int height = topAnchor;
        // -------------------------------
    
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
    
        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();
    
        buffer.begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
    
        for (int i = 0; i < piecesToShow; i++) {
            ItemStack stack = armorPieces[i];
            if (!(stack.getItem() instanceof ModPowerArmorItem pieceArmor)) {
                continue;
            }
    
            long currentEnergy = pieceArmor.getEnergy(stack);
            long maxEnergy = pieceArmor.getModifiedCapacity(stack); 
    
            if (maxEnergy <= 0) continue;
    
            float energyPercent = (float) currentEnergy / maxEnergy;
    
            int top = height - (piecesToShow - 1 - i) * 3;
    
            // Фон
            buffer.vertex(left - 0.5, top - 0.5, 0).color(0.25f, 0.25f, 0.25f, 1.0f).endVertex();
            buffer.vertex(left - 0.5, top + 1.5, 0).color(0.25f, 0.25f, 0.25f, 1.0f).endVertex();
            buffer.vertex(left + 81.5, top + 1.5, 0).color(0.25f, 0.25f, 0.25f, 1.0f).endVertex();
            buffer.vertex(left + 81.5, top - 0.5, 0).color(0.25f, 0.25f, 0.25f, 1.0f).endVertex();
    
            // Полоска
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
        RenderSystem.disableBlend();
        
        // В выживании увеличиваем leftHeight, чтобы СЛЕДУЮЩИЕ элементы (например, воздух)
        // рисовались над нашими полосками.
        if (!player.isCreative()) {
            gui.leftHeight += (piecesToShow * 3) + 2; 
        }
    }

    public static final net.minecraftforge.client.gui.overlay.IGuiOverlay POWER_ARMOR_OVERLAY = OverlayPowerArmor::onRenderOverlay;
}