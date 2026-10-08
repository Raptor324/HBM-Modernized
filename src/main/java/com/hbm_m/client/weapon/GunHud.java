package com.hbm_m.client.weapon;

import com.hbm_m.item.weapon.sedna.Crosshair;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.item.weapon.sedna.hud.HUDComponentAmmoCounter;
import com.hbm_m.item.weapon.sedna.hud.HUDComponentDurabilityBar;
import com.hbm_m.item.weapon.sedna.mags.IMagazine;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Original {@code RenderScreenOverlay.renderCustomCrosshairs/renderScope} und die HUD-Bausteine
 * {@code HUDComponentAmmoCounter}/{@code HUDComponentDurabilityBar}.
 */
public final class GunHud {

    private GunHud() { }

    public static final ResourceLocation MISC = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/misc/overlay_misc.png");

    public static void renderCustomCrosshairs(GuiGraphics g, Crosshair cross) {
        if (cross == Crosshair.NONE) return;
        int size = cross.size;
        int w = g.guiWidth(), h = g.guiHeight();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        g.blit(MISC, w / 2 - (size / 2), h / 2 - (size / 2), cross.x, cross.y, size, size);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    public static void renderScope(GuiGraphics g, ResourceLocation tex) {
        double w = g.guiWidth();
        double h = g.guiHeight();

        double smallest = Math.min(w, h);
        double divisor = smallest / (9D / 16D);
        smallest = 9D / 16D;
        double largest = Math.max(w, h) / divisor;

        double hMin = h < w ? 0.5 - smallest / 2D : 0.5 - largest / 2D;
        double hMax = h < w ? 0.5 + smallest / 2D : 0.5 + largest / 2D;
        double wMin = w < h ? 0.5 - smallest / 2D : 0.5 - largest / 2D;
        double wMax = w < h ? 0.5 + smallest / 2D : 0.5 + largest / 2D;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, tex);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        var m = g.pose().last().pose();
        BufferBuilder buf = Tesselator.getInstance().getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buf.vertex(m, 0, (float) h, 0).uv((float) wMin, (float) hMax).endVertex();
        buf.vertex(m, (float) w, (float) h, 0).uv((float) wMax, (float) hMax).endVertex();
        buf.vertex(m, (float) w, 0, 0).uv((float) wMax, (float) hMin).endVertex();
        buf.vertex(m, 0, 0, 0).uv((float) wMin, (float) hMin).endVertex();
        Tesselator.getInstance().end();
        RenderSystem.disableBlend();
    }

    public static void ammoCounter(HUDComponentAmmoCounter c, Object gui, Player player, ItemStack stack, int bottomOffset, int gunIndex) {
        GuiGraphics g = (GuiGraphics) gui;
        Minecraft mc = Minecraft.getInstance();
        int pX = g.guiWidth() / 2 + (c.mirrored ? -(62 + 36 + 52) : (62 + 36)) + (c.noCounter ? 14 : 0);
        int pZ = g.guiHeight() - bottomOffset - 18;

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        IMagazine<?> mag = gun.getConfig(stack, gunIndex).getReceivers(stack)[c.receiver].getMagazine(stack);

        if (!c.noCounter) g.drawString(mc.font, mag.reportAmmoStateForHUD(stack, player), pX + 17, pZ + 6, 0xFFFFFF);

        ItemStack icon = mag.getIconForHUD(stack, player);
        if (!icon.isEmpty()) g.renderItem(icon, pX, pZ);
    }

    public static void durabilityBar(HUDComponentDurabilityBar c, Object gui, Player player, ItemStack stack, int bottomOffset, int gunIndex) {
        GuiGraphics g = (GuiGraphics) gui;
        int pX = g.guiWidth() / 2 + (c.mirrored ? -(62 + 36 + 52) : (62 + 36));
        int pZ = g.guiHeight() - 21;

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        int dura = (int) (50 * ItemGunBaseNT.getWear(stack, gunIndex) / gun.getConfig(stack, gunIndex).getDurability(stack));

        g.blit(MISC, pX, pZ + 16, 94, 0, 52, 3);
        g.blit(MISC, pX + 1, pZ + 16, 95, 3, 50 - dura, 3);
    }
}
