package com.hbm_m.inventory.gui;

import com.hbm_m.platform.PlatformHooks;
import com.hbm_m.client.GuiCompat;

import com.hbm_m.recipe.AssemblerRecipe;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.inventory.menu.MachineAdvancedAssemblerMenu;
import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import com.hbm_m.util.EnergyFormatter;


// TODO: РќСѓР¶РµРЅ СѓС‚РёР»РёС‚Р°СЂРЅС‹Р№ РєР»Р°СЃСЃ РґР»СЏ РѕС‚СЂРёСЃРѕРІРєРё РїРѕРґСЃРєР°Р·РѕРє Рё Р¶РёРґРєРѕСЃС‚РµР№.
// Р­С‚РѕС‚ С„СѓРЅРєС†РёРѕРЅР°Р» СЃРµР№С‡Р°СЃ РІСЃС‚СЂРѕРµРЅ РІ СЌС‚РѕС‚ РєР»Р°СЃСЃ

public class GUIMachineAdvancedAssembler extends AbstractContainerScreen<MachineAdvancedAssemblerMenu> {

    // РўРµРєСЃС‚СѓСЂР° РёР· СЃС‚Р°СЂРѕРіРѕ GUI
    //? if fabric && < 1.21.1 {
    /*private static final ResourceLocation TEXTURE = new ResourceLocation(RefStrings.MODID, "textures/gui/processing/gui_assembler.png");
    *///?} else {
        private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_assembler.png");
    //?}

    // private static final ResourceLocation TEMPLATE_FOLDER_ICON = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/item/template_folder.png");

    public GUIMachineAdvancedAssembler(MachineAdvancedAssemblerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 256;
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        
        // РћС‚СЂРёСЃРѕРІРєР° РѕСЃРЅРѕРІРЅРѕР№ С‚РµРєСЃС‚СѓСЂС‹
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        // РћС‚СЂРёСЃРѕРІРєР° СЌРЅРµСЂРіРёРё
        long energyStored = this.menu.getEnergyLong();
        long maxEnergy = this.menu.getMaxEnergyLong();
        if (maxEnergy > 0) {
            int energyBarHeight = (int) (energyStored * 61L / maxEnergy);
            if (energyBarHeight > 61) energyBarHeight = 61; // Р—Р°С‰РёС‚Р°

            guiGraphics.blit(TEXTURE, this.leftPos + 152, this.topPos + 79 - energyBarHeight,
                    176, 61 - energyBarHeight, 16, energyBarHeight);
        }

        // РРЎРџР РђР’Р›Р•РќРћ: РћС‚СЂРёСЃРѕРІРєР° РїСЂРѕРіСЂРµСЃСЃР° - РёСЃРїРѕР»СЊР·СѓРµРј ContainerData С‡РµСЂРµР· menu
        int progress = this.menu.getProgress(); // <-- РР—РњР•РќР•РќРћ: РёСЃРїРѕР»СЊР·СѓРµРј menu РІРјРµСЃС‚Рѕ blockEntity
        if (progress > 0) {
            int maxProgress = this.menu.getMaxProgress(); // <-- РР—РњР•РќР•РќРћ
            if (maxProgress > 0) {
                int progressWidth = (int) Math.ceil(70.0 * progress / maxProgress);
                guiGraphics.blit(TEXTURE, this.leftPos + 62, this.topPos + 126, 176, 61, progressWidth, 16);
            }
        }

        // РџРѕР»СѓС‡Р°РµРј С‚РµРєСѓС‰РёР№ СЂРµС†РµРїС‚
        ResourceLocation selectedRecipeId = this.menu.getBlockEntity().getSelectedRecipeId();
        AssemblerRecipe recipe = null;
        if (selectedRecipeId != null && this.minecraft != null && this.minecraft.level != null) {
            recipe = this.minecraft.level.getRecipeManager()
                    .byKey(selectedRecipeId)
                    .filter(r -> r instanceof AssemblerRecipe)
                    .map(r -> (AssemblerRecipe) r)
                    .orElse(null);
        }

        boolean hasRecipe = recipe != null;
        boolean canProcess = hasRecipe && energyStored >= 100;
        
        // РћС‚СЂРёСЃРѕРІРєР° СЃРІРµС‚РѕРґРёРѕРґРѕРІ (LEDs) - РёСЃРїРѕР»СЊР·СѓРµРј isCrafting() РёР· menu
        if (this.menu.isCrafting()) { // <-- РџР РђР’РР›Р¬РќРћ: РёСЃРїРѕР»СЊР·СѓРµС‚СЃСЏ menu
            // Р›РµРІС‹Р№ LED (Р·РµР»РµРЅС‹Р№)
            guiGraphics.blit(TEXTURE, this.leftPos + 51, this.topPos + 121, 195, 0, 3, 6);
            // РџСЂР°РІС‹Р№ LED (Р·РµР»РµРЅС‹Р№)
            guiGraphics.blit(TEXTURE, this.leftPos + 56, this.topPos + 121, 195, 0, 3, 6);
        } else if (hasRecipe) {
            // Р›РµРІС‹Р№ LED (Р¶РµР»С‚С‹Р№)
            guiGraphics.blit(TEXTURE, this.leftPos + 51, this.topPos + 121, 192, 0, 3, 6);
            if (canProcess) {
                // РџСЂР°РІС‹Р№ LED (Р¶РµР»С‚С‹Р№)
                guiGraphics.blit(TEXTURE, this.leftPos + 56, this.topPos + 121, 192, 0, 3, 6);
            }
        }

        // РћС‚СЂРёСЃРѕРІРєР° "РїСЂРёР·СЂР°С‡РЅС‹С…" РїСЂРµРґРјРµС‚РѕРІ РІ РїСѓСЃС‚С‹С… СЃР»РѕС‚Р°С…
        renderGhostItems(guiGraphics);
        
        // TODO: РћС‚СЂРёСЃРѕРІРєР° Р¶РёРґРєРѕСЃС‚РµР№ РІ С‚Р°РЅРєР°С…
        // РљРѕРіРґР° Сѓ BlockEntity Р±СѓРґСѓС‚ РјРµС‚РѕРґС‹ getInputTank() Рё getOutputTank(), СЂР°СЃРєРѕРјРјРµРЅС‚РёСЂСѓР№С‚Рµ:
        /*
        FluidTank inputTank = this.menu.getBlockEntity().getInputTank();
        FluidTank outputTank = this.menu.getBlockEntity().getOutputTank();
        
        if (inputTank != null) {
            renderFluidTank(guiGraphics, inputTank, this.leftPos + 8, this.topPos + 115, 52, 16);
        }
        if (outputTank != null) {
            renderFluidTank(guiGraphics, outputTank, this.leftPos + 80, this.topPos + 115, 52, 16);
        }
        */
    }

    /**
     * РћС‚СЂРёСЃРѕРІС‹РІР°РµС‚ РїСЂРёР·СЂР°С‡РЅС‹Рµ РїСЂРµРґРјРµС‚С‹ РІ РїСѓСЃС‚С‹С… РІС…РѕРґРЅС‹С… СЃР»РѕС‚Р°С….
     * Р“СЂСѓРїРїРёСЂСѓРµС‚ РѕРґРёРЅР°РєРѕРІС‹Рµ РёРЅРіСЂРµРґРёРµРЅС‚С‹ Рё РїРѕРєР°Р·С‹РІР°РµС‚ СЃСѓРјРјР°СЂРЅРѕРµ РєРѕР»РёС‡РµСЃС‚РІРѕ.
     */

    private void renderGhostItems(GuiGraphics guiGraphics) {
        // РРЎРџРћР›Р¬Р—РЈР•Рњ РјРµС‚РѕРґ РёР· BlockEntity, РєРѕС‚РѕСЂС‹Р№ РїРѕР»СѓС‡Р°РµС‚ РґР°РЅРЅС‹Рµ РёР· РјРѕРґСѓР»СЏ
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

        // РЎР»РѕС‚С‹ 4-15 (handler) СЃРѕРѕС‚РІРµС‚СЃС‚РІСѓСЋС‚ СЃР»РѕС‚Р°Рј 40-51 РІ menu
        int inputSlotsStart = 4; // 40
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
                int x = this.leftPos + slot.x;
                int y = this.topPos + slot.y;

                // setShaderColor(alpha) РЅРµ РІР»РёСЏРµС‚ РЅР° 3D-Р±Р»РѕРєРё (РєРІР°РґС‹ СЃ
                // С„РёРєСЃРёСЂРѕРІР°РЅРЅС‹Рј С†РІРµС‚РѕРј РІРµСЂС€РёРЅ) вЂ” РёСЃРїРѕР»СЊР·СѓРµРј РѕР±С‰РёР№ СѓС‚РёР»РёС‚,
                // РєРѕС‚РѕСЂС‹Р№ СѓРјРЅРѕР¶Р°РµС‚ Р°Р»СЊС„Сѓ РЅР° СѓСЂРѕРІРЅРµ РІРµСЂС€РёРЅ.
                GhostItemRenderUtil.renderTranslucent(guiGraphics, ghostStack, x, y, 0.5F);

                // РћРўР РРЎРћР’РљРђ РљРћР›РР§Р•РЎРўР’Рђ (РµСЃР»Рё > 1)
                if (ghostStack.getCount() > 1) {
                    guiGraphics.renderItemDecorations(this.font, ghostStack, x, y);
                }
            }
            
            slotOffset++;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTicks);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int pMouseX, int pMouseY) {
        super.renderTooltip(guiGraphics, pMouseX, pMouseY);
        
        // РРЎРџР РђР’Р›Р•РќРћ: РџРѕРґСЃРєР°Р·РєР° РґР»СЏ С€РєР°Р»С‹ СЌРЅРµСЂРіРёРё - РёСЃРїРѕР»СЊР·СѓРµРј ContainerData
        if (isMouseOver(pMouseX, pMouseY, 152, 18, 16, 61)) {
            List<Component> tooltip = new ArrayList<>();

            // РџРѕР»СѓС‡Р°РµРј long Р·РЅР°С‡РµРЅРёСЏ
            long energy = this.menu.getEnergyLong();
            long maxEnergy = this.menu.getMaxEnergyLong();

            // Р¤РѕСЂРјР°С‚РёСЂСѓРµРј РёС…
            String energyStr = EnergyFormatter.format(energy);
            String maxEnergyStr = EnergyFormatter.format(maxEnergy);

            // РџРµСЂРІР°СЏ СЃС‚СЂРѕРєР°: С‚РµРєСѓС‰Р°СЏ / РјР°РєСЃРёРјР°Р»СЊРЅР°СЏ СЌРЅРµСЂРіРёСЏ
            tooltip.add(Component.literal(energyStr + " / " + maxEnergyStr + " HE")
                    .withStyle(ChatFormatting.GREEN));

            guiGraphics.renderTooltip(this.font, tooltip, java.util.Optional.empty(), pMouseX, pMouseY);

        }
        
        // РџРѕРґСЃРєР°Р·РєР° РґР»СЏ С€РєР°Р»С‹ СЌРЅРµСЂРіРё
        
        // РџР РћР”Р’РРќРЈРўРђРЇ РџРћР”РЎРљРђР—РљРђ Р”Р›РЇ РљРќРћРџРљР Р’Р«Р‘РћР Рђ Р Р•Р¦Р•РџРўРђ
        if (isMouseOver(pMouseX, pMouseY, 7, 125, 18, 18)) {
            ResourceLocation selectedRecipeId = this.menu.getBlockEntity().getSelectedRecipeId();
            if (selectedRecipeId != null && this.minecraft != null && this.minecraft.level != null) {
                this.minecraft.level.getRecipeManager().byKey(selectedRecipeId).ifPresent(recipe -> {
                    if (recipe instanceof AssemblerRecipe assemblerRecipe) {
                        List<Component> tooltip = new ArrayList<>();
                        
                        // РќР°Р·РІР°РЅРёРµ РІС‹С…РѕРґРЅРѕРіРѕ РїСЂРµРґРјРµС‚Р°
                        ItemStack output = assemblerRecipe.getResultItem(null);
                        tooltip.add(output.getHoverName());
                        
                        // Р”РћР‘РђР’Р›РЇР•Рњ РџР РћР”Р’РРќРЈРўР«Р™ РўРЈР›РўРРџ РЎ Р”Р•РўРђР›РЇРњР Р Р•Р¦Р•РџРўРђ
                        com.hbm_m.util.TemplateTooltipUtil.buildRecipeTooltip(assemblerRecipe, tooltip);
                        
                        guiGraphics.renderTooltip(this.font, tooltip, java.util.Optional.empty(),
                                                pMouseX, pMouseY);
                    }
                });
            } else {
                // Р•СЃР»Рё СЂРµС†РµРїС‚ РЅРµ РІС‹Р±СЂР°РЅ, РїРѕРєР°Р·С‹РІР°РµРј РїРѕРґСЃРєР°Р·РєСѓ "Set Recipe"
                guiGraphics.renderTooltip(this.font, 
                    Component.translatable("gui.recipe.setRecipe").withStyle(ChatFormatting.YELLOW),
                    pMouseX, pMouseY);
            }
        }
        
        // TODO: РџРѕРґСЃРєР°Р·РєРё РґР»СЏ С‚Р°РЅРєРѕРІ - Р’РћРЎРЎРўРђРќРћР’Р›Р•РќРћ РёР· РѕСЂРёРіРёРЅР°Р»Р°
        /*
        if (isMouseOver(pMouseX, pMouseY, 8, 99, 52, 16)) {
            FluidTank inputTank = this.menu.getBlockEntity().getInputTank();
            if (inputTank != null) {
                inputTank.renderTankInfo(guiGraphics, pMouseX, pMouseY);
            }
        }
        
        if (isMouseOver(pMouseX, pMouseY, 80, 99, 52, 16)) {
            FluidTank outputTank = this.menu.getBlockEntity().getOutputTank();
            if (outputTank != null) {
                outputTank.renderTankInfo(guiGraphics, pMouseX, pMouseY);
            }
        }
        */
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // РџСЂРѕРІРµСЂСЏРµРј РєР»РёРє РїРѕ РєРЅРѕРїРєРµ РІС‹Р±РѕСЂР° СЂРµС†РµРїС‚Р°
        if (isMouseOver((int)mouseX, (int)mouseY, 7, 125, 18, 18)) {
            openRecipeSelector();
            return true;
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void openRecipeSelector() {
        if (this.minecraft == null || this.minecraft.level == null) return;
        
        ResourceLocation currentRecipe = this.menu.getBlockEntity().getSelectedRecipeId();
        
        // РЈР±РёСЂР°РµРј РїРµСЂРµРґР°С‡Сѓ СЃРїРёСЃРєР° СЂРµС†РµРїС‚РѕРІ
        this.minecraft.setScreen(new GUIScreenRecipeSelector(
            this.menu.getBlockEntity().getBlockPos(),
            currentRecipe,
            this
        ));
    }


    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Р¦РµРЅС‚СЂРёСЂРѕРІР°РЅРЅРѕРµ РЅР°Р·РІР°РЅРёРµ
        String name = this.title.getString();
        guiGraphics.drawString(this.font, name, 
                            70 - this.font.width(name) / 2, 6, 0x404040, false);
        
        // РќР°Р·РІР°РЅРёРµ РёРЅРІРµРЅС‚Р°СЂСЏ РёРіСЂРѕРєР°
        guiGraphics.drawString(this.font, this.playerInventoryTitle,
                            8, this.imageHeight - 96 + 2, 0x404040, false);
        
        // РћРўР РРЎРћР’РљРђ РРљРћРќРљР Р Р•Р¦Р•РџРўРђ - РўРћР›Р¬РљРћ Р•РЎР›Р Р­РўРћ РћРЎРќРћР’РќРћР™ Р­РљР РђРќ
        // РџСЂРѕРІРµСЂСЏРµРј, С‡С‚Рѕ С‚РµРєСѓС‰РёР№ Р°РєС‚РёРІРЅС‹Р№ СЌРєСЂР°РЅ - СЌС‚Рѕ РёРјРµРЅРЅРѕ СЌС‚РѕС‚ СЌРєСЂР°РЅ
        if (this.minecraft != null && this.minecraft.screen == this) {
            ResourceLocation selectedRecipeId = this.menu.getBlockEntity().getSelectedRecipeId();
            if (selectedRecipeId != null && this.minecraft.level != null) {
                this.minecraft.level.getRecipeManager().byKey(selectedRecipeId).ifPresent(recipe -> {
                    if (recipe instanceof AssemblerRecipe assemblerRecipe) {
                        ItemStack icon = assemblerRecipe.getResultItem(null);
                        guiGraphics.renderItem(icon, 8, 126);
                    }
                });
            } else {
                // РћС‚СЂРёСЃРѕРІС‹РІР°РµРј РёРєРѕРЅРєСѓ РїР°РїРєРё С€Р°Р±Р»РѕРЅРѕРІ
                ItemStack folderIcon = new ItemStack(ModItems.TEMPLATE_FOLDER.get());
                guiGraphics.renderItem(folderIcon, 8, 126);
            }
        }
    }
    
    // TODO: РњРµС‚РѕРґ РґР»СЏ РѕС‚СЂРёСЃРѕРІРєРё Р¶РёРґРєРѕСЃС‚РµР№ РІ С‚Р°РЅРєР°С…
    /*
    private void renderFluidTank(GuiGraphics guiGraphics, FluidTank tank, int x, int y, int width, int height) {
        if (tank.getFluid().isEmpty()) return;
        
        // Р›РѕРіРёРєР° СЂРµРЅРґРµСЂРёРЅРіР° Р¶РёРґРєРѕСЃС‚Рё
        // РСЃРїРѕР»СЊР·СѓР№С‚Рµ RenderSystem Рё FluidRenderer РёР· Forge
    }
    */
    private boolean isMouseOver(int pMouseX, int pMouseY, int pX, int pY, int pWidth, int pHeight) {
        return pMouseX >= this.leftPos + pX && pMouseX < this.leftPos + pX + pWidth &&
                pMouseY >= this.topPos + pY && pMouseY < this.topPos + pY + pHeight;
    }
}
