package com.hbm_m.client;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.handler.ability.AvailableAbilities;
import com.hbm_m.handler.ability.ToolPreset;
import com.hbm_m.inventory.gui.GUIScreenToolAbility;
import com.hbm_m.item.tool.ItemToolAbility;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

/** Clientteil von {@link ItemToolAbility}: Einstellungsfenster und Fadenkreuz-Anzeige ({@code renderHUD}). */
public final class ToolAbilityClient {

    private ToolAbilityClient() {}

    public static void openGui(AvailableAbilities abilities) {
        Minecraft.getInstance().setScreen(new GUIScreenToolAbility(abilities));
    }

    /** Original {@code ItemToolAbility.renderHUD} bei {@code ElementType.CROSSHAIRS}. */
    public static void renderHUD(GuiGraphics gfx, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || !mc.options.getCameraType().isFirstPerson()) return;

        ItemStack stack = mc.player.getMainHandItem();
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemToolAbility tool)) return;

        ToolPreset preset = tool.getConfiguration(stack).getActivePreset();
        int[] uv = ItemToolAbility.abilityGui.get(preset.areaAbility);

        if (uv == null) return;

        int size = 16;
        int ox = ModClothConfig.get().toolHudIndicatorX;
        int oy = ModClothConfig.get().toolHudIndicatorY;

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        gfx.blit(GUIScreenToolAbility.TEXTURE, screenWidth / 2 - size - 8 + ox, screenHeight / 2 + 8 + oy, uv[0], uv[1], size, size);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    //? if forge {
    public static final net.minecraftforge.client.gui.overlay.IGuiOverlay OVERLAY = (gui, gfx, partialTick, screenWidth, screenHeight) ->
            renderHUD(gfx, screenWidth, screenHeight);
    //?}
}
