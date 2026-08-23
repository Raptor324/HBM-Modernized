package com.hbm_m.inventory.gui;

import com.hbm_m.platform.PlatformHooks;
import com.hbm_m.client.GuiCompat;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.inventory.menu.MachineAssemblerMenu;
import com.hbm_m.item.industrial.ItemAssemblyTemplate;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.util.EnergyFormatter;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class GUIMachineAssembler extends GuiInfoScreen<MachineAssemblerMenu> {

    private static final ResourceLocation TEXTURE =
            //? if fabric && < 1.21.1 {
            /*new ResourceLocation(RefStrings.MODID, "textures/gui/gui_assembler.png");
            *///?} else {
                        ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/gui_assembler.png");
            //?}

    
    // РљРѕРЅСЃС‚Р°РЅС‚Р° РґР»СЏ РєРѕСЂСЂРµРєС‚РЅРѕРіРѕ РёРЅРґРµРєСЃР° СЃР»РѕС‚Р° С€Р°Р±Р»РѕРЅР°
    private static final int TEMPLATE_SLOT_GUI_INDEX = 36 + 4; // 36 СЃР»РѕС‚РѕРІ РёРіСЂРѕРєР° + 4 СЃР»РѕС‚Р° РјР°С€РёРЅС‹

    public GUIMachineAssembler(MachineAssemblerMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.titleLabelY = 6;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = this.leftPos;
        int y = this.topPos;

        // Р РёСЃСѓРµРј С„РѕРЅ
        RenderSystem.setShaderTexture(0, TEXTURE);
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // РћС‚СЂРёСЃРѕРІРєР° СЌРЅРµСЂРіРёРё
        int energyBarHeight = 52;
        int energy = this.menu.getEnergyScaled(energyBarHeight);
        guiGraphics.blit(TEXTURE, x + 116, y + 70 - energy, 176, 52 - energy, 16, energy);

        // РћС‚СЂРёСЃРѕРІРєР° РїСЂРѕРіСЂРµСЃСЃР°
        if(menu.isCrafting()) {
            int progressWidth = 83;
            guiGraphics.blit(TEXTURE, x + 45, y + 82, 2, 222, menu.getProgressScaled(progressWidth), 32);
        }

        // РРЅС„РѕСЂРјР°С†РёРѕРЅРЅС‹Рµ РїР°РЅРµР»Рё (Р’РђР–РќРћ: РїСЂРёРІСЏР·С‹РІР°РµРј С‚РµРєСЃС‚СѓСЂСѓ Р·Р°РЅРѕРІРѕ)
        RenderSystem.setShaderTexture(0, TEXTURE);
        drawInfoPanels(guiGraphics);
        renderGhostItems(guiGraphics);
    }

    /**
     * РћС‚СЂРёСЃРѕРІС‹РІР°РµС‚ РёРЅС„РѕСЂРјР°С†РёРѕРЅРЅС‹Рµ РїР°РЅРµР»Рё (РёРєРѕРЅРєРё Р·Р° РїСЂРµРґРµР»Р°РјРё GUI)
     */
    private void drawInfoPanels(GuiGraphics guiGraphics) {
        drawInfoPanel(guiGraphics, -16, 16, PanelType.LARGE_GRAY_STAR);

        ItemStack templateStack = menu.getSlot(TEMPLATE_SLOT_GUI_INDEX).getItem();
        if (templateStack.isEmpty() || !(templateStack.getItem() instanceof ItemAssemblyTemplate)) {
            drawInfoPanel(guiGraphics, -16, 36, PanelType.LARGE_RED_EXCLAMATION);
        }
    }

    private void renderGhostItems(GuiGraphics guiGraphics) {
        if (this.menu.getBlockEntity() == null) return; // тайл может отсутствовать в реплее Flashback
        NonNullList<ItemStack> ghostItems = this.menu.getBlockEntity().getGhostItems();
        
        if (ghostItems.isEmpty()) {
            return;
        }
        
        // Р“СЂСѓРїРїРёСЂСѓРµРј РѕРґРёРЅР°РєРѕРІС‹Рµ РїСЂРµРґРјРµС‚С‹ Рё СЃСѓРјРјРёСЂСѓРµРј РёС… РєРѕР»РёС‡РµСЃС‚РІРѕ
        java.util.Map<ItemStack, Integer> groupedItems = new java.util.LinkedHashMap<>();
        for (ItemStack stack : ghostItems) {
            if (stack.isEmpty()) {
                continue;
            }
            
            // РС‰РµРј СѓР¶Рµ СЃСѓС‰РµСЃС‚РІСѓСЋС‰РёР№ РїСЂРµРґРјРµС‚ РІ РіСЂСѓРїРїРµ
            ItemStack found = null;
            for (ItemStack key : groupedItems.keySet()) {
                if (PlatformHooks.isSameItemSameTags(key, stack)) {
                    found = key;
                    break;
                }
            }
            
            if (found != null) {
                // РЈРІРµР»РёС‡РёРІР°РµРј РєРѕР»РёС‡РµСЃС‚РІРѕ
                groupedItems.put(found, groupedItems.get(found) + stack.getCount());
            } else {
                // Р”РѕР±Р°РІР»СЏРµРј РЅРѕРІС‹Р№ РїСЂРµРґРјРµС‚
                ItemStack copy = stack.copy();
                copy.setCount(1); // РќРѕСЂРјР°Р»РёР·СѓРµРј РєРѕР»РёС‡РµСЃС‚РІРѕ РґР»СЏ РєР»СЋС‡Р°
                groupedItems.put(copy, stack.getCount());
            }
        }
        
        // РЎР»РѕС‚С‹ 6-17 (handler) СЃРѕРѕС‚РІРµС‚СЃС‚РІСѓСЋС‚ СЃР»РѕС‚Р°Рј 42-53 РІ menu (36 СЃР»РѕС‚РѕРІ РёРіСЂРѕРєР° + 6 РјР°С€РёРЅС‹)
        int inputSlotsStart = 36 + 6; // 42
        int inputSlotsCount = 12;
        
        // РћС‚СЂРёСЃРѕРІС‹РІР°РµРј СЃРіСЂСѓРїРїРёСЂРѕРІР°РЅРЅС‹Рµ РїСЂРµРґРјРµС‚С‹
        int slotOffset = 0;
        for (java.util.Map.Entry<ItemStack, Integer> entry : groupedItems.entrySet()) {
            if (slotOffset >= inputSlotsCount) {
                break; // РџСЂРµРІС‹С€РµРЅ Р»РёРјРёС‚ СЃР»РѕС‚РѕРІ
            }
            
            ItemStack ghostStack = entry.getKey().copy();
            ghostStack.setCount(entry.getValue()); // РЈСЃС‚Р°РЅР°РІР»РёРІР°РµРј СЃСѓРјРјР°СЂРЅРѕРµ РєРѕР»РёС‡РµСЃС‚РІРѕ
            
            // РџРѕР»СѓС‡Р°РµРј СЃР»РѕС‚
            int slotIndex = inputSlotsStart + slotOffset;
            if (slotIndex >= this.menu.slots.size()) break;
            
            net.minecraft.world.inventory.Slot slot = this.menu.slots.get(slotIndex);
            
            // РћС‚СЂРёСЃРѕРІС‹РІР°РµРј РїСЂРёР·СЂР°Рє С‚РѕР»СЊРєРѕ РµСЃР»Рё СЃР»РѕС‚ РїСѓСЃС‚
            if (!slot.hasItem()) {
                // РћС‚СЂРёСЃРѕРІРєР° РїСЂРёР·СЂР°С‡РЅРѕРіРѕ РїСЂРµРґРјРµС‚Р° СЃ РїРѕР»СѓРїСЂРѕР·СЂР°С‡РЅРѕСЃС‚СЊСЋ
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(0, 0, 100); // z-level 100
                
                // РџРѕР»СѓРїСЂРѕР·СЂР°С‡РЅРѕСЃС‚СЊ
                RenderSystem.enableBlend();
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.5F);
                
                int x = this.leftPos + slot.x;
                int y = this.topPos + slot.y;
                
                guiGraphics.renderItem(ghostStack, x, y);
                
                // РћРўР РРЎРћР’РљРђ РљРћР›РР§Р•РЎРўР’Рђ (РµСЃР»Рё > 1)
                if (ghostStack.getCount() > 1) {
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F); // РџРѕР»РЅР°СЏ РЅРµРїСЂРѕР·СЂР°С‡РЅРѕСЃС‚СЊ РґР»СЏ С‚РµРєСЃС‚Р°
                    guiGraphics.renderItemDecorations(this.font, ghostStack, x, y);
                }
                
                // Р’РѕСЃСЃС‚Р°РЅР°РІР»РёРІР°РµРј С†РІРµС‚
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                RenderSystem.disableBlend();
                
                guiGraphics.pose().popPose();
            }
            
            slotOffset++;
        }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, delta);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(@NotNull GuiGraphics pGuiGraphics, int pX, int pY) {
        super.renderTooltip(pGuiGraphics, pX, pY);

        // РџРѕРґСЃРєР°Р·РєР° РґР»СЏ СЌРЅРµСЂРіРёРё
        renderEnergyTooltip(pGuiGraphics, pX, pY);
        
        // РџРѕРґСЃРєР°Р·РєРё РґР»СЏ РёРЅС„РѕСЂРјР°С†РёРѕРЅРЅС‹С… РїР°РЅРµР»РµР№
        renderInfoPanelTooltips(pGuiGraphics, pX, pY);
    }

    /**
     * РћС‚СЂРёСЃРѕРІС‹РІР°РµС‚ РїРѕРґСЃРєР°Р·РєСѓ РґР»СЏ С€РєР°Р»С‹ СЌРЅРµСЂРіРёРё
     */
    private void renderEnergyTooltip(GuiGraphics pGuiGraphics, int pX, int pY) {
        int energyBarX = this.leftPos + 116;
        int energyBarY = this.topPos + 18;
        int energyBarWidth = 16;
        int energyBarHeight = 52;

        if (pX >= energyBarX && pX < energyBarX + energyBarWidth &&
                pY >= energyBarY && pY < energyBarY + energyBarHeight) {

            List<Component> tooltip = new ArrayList<>();

        long energy = this.menu.getEnergyLong();
        long maxEnergy = this.menu.getMaxEnergyLong();

            String energyStr = EnergyFormatter.format(energy);
            String maxEnergyStr = EnergyFormatter.format(maxEnergy);

            tooltip.add(Component.literal(energyStr + " / " + maxEnergyStr + " HE"));

            pGuiGraphics.renderTooltip(this.font, tooltip, java.util.Optional.empty(), pX, pY);
        }
    }

    /**
     * РћС‚СЂРёСЃРѕРІС‹РІР°РµС‚ РїРѕРґСЃРєР°Р·РєРё РґР»СЏ РёРЅС„РѕСЂРјР°С†РёРѕРЅРЅС‹С… РїР°РЅРµР»РµР№
     */
    private void renderInfoPanelTooltips(GuiGraphics pGuiGraphics, int pX, int pY) {
        // РџР°РЅРµР»СЊ С€Р°Р±Р»РѕРЅР°
        if (isPointInRect(-16, 16, 16, 16, pX, pY)) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.translatable("desc.gui.template"));
            pGuiGraphics.renderTooltip(this.font, tooltip, java.util.Optional.empty(), pX, pY);
        }

        // РџР°РЅРµР»СЊ РїСЂРµРґСѓРїСЂРµР¶РґРµРЅРёСЏ (С‚РѕР»СЊРєРѕ РµСЃР»Рё РЅРµС‚ С€Р°Р±Р»РѕРЅР°)
        ItemStack templateStack = menu.getSlot(TEMPLATE_SLOT_GUI_INDEX).getItem();
        if (templateStack.isEmpty() || !(templateStack.getItem() instanceof ItemAssemblyTemplate)) {
            if (isPointInRect(-16, 36, 16, 16, pX, pY)) {
                List<Component> tooltip = new ArrayList<>();
                tooltip.add(Component.translatable("desc.gui.assembler.warning").withStyle(ChatFormatting.RED));
                pGuiGraphics.renderTooltip(this.font, tooltip, java.util.Optional.empty(), pX, pY);
            }
        }
    }

}
