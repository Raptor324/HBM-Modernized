package com.hbm_m.client.weapon.render;

import java.util.List;
import java.util.Optional;

import org.joml.Matrix4f;

import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.weapon.GunGL;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.util.MovingObjectPosition;
import com.hbm_m.util.Vec3NT;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Hilfen fuer die portierten Waffenrenderer (Gruppe A), die {@link GunGL} nicht abdeckt:
 * {@code MainRegistry.proxy.me()}, {@code FontRenderer.drawString} im Modellraum, Item-Icons aus dem Atlas und
 * der Glanzeffekt mit Texturmatrix + {@code glDepthFunc(GL_EQUAL)} ({@code ItemRenderFatMan.renderBalefire}).
 */
public final class RenderHelperA {

    private RenderHelperA() { }

    /** Original {@code RenderMiscEffects.glintBF}. */
    public static final ResourceLocation glintBF = new ResourceLocation(RefStrings.MODID, "textures/misc/glint_bf.png");

    /** Original {@code MainRegistry.proxy.me()}. */
    public static Player me() {
        return Minecraft.getInstance().player;
    }

    /** Original {@code MainRegistry.proxy.me().inventory}. */
    public static Inventory meInventory() {
        Player player = me();
        return player != null ? player.getInventory() : null;
    }

    /** Original {@code fontRenderer.getStringWidth(s)}. */
    public static int getStringWidth(String s) {
        return Minecraft.getInstance().font.width(s);
    }

    /** Original {@code fontRenderer.drawString(s, x, y, color)} im aktuellen GunGL-Modellraum. */
    public static void drawString(String s, float x, float y, int color) {
        Font font = Minecraft.getInstance().font;
        font.drawInBatch(s, x, y, 0xFF000000 | color, false, GunGL.pose().last().pose(), GunGL.buffers(),
                Font.DisplayMode.NORMAL, 0, GunGL.light());
    }

    /**
     * Original {@code bindTexture(getResourceLocation(item.getSpriteNumber()))} + {@code item.getIconFromDamage(0)}:
     * bindet den Blockatlas und liefert das Sprite der Item-Textur (z.B. {@code "minecraft:item/golden_sword"}).
     */
    public static TextureAtlasSprite bindItemIcon(String spriteName) {
        GunGL.bindTexture(InventoryMenu.BLOCK_ATLAS);
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(new ResourceLocation(spriteName));
    }

    /** Original {@code new Color(r, g, b).getRGB()} (Gleitkommaanteile 0..1, deckend). */
    public static int colorRGB(float r, float g, float b) {
        int ir = (int) (r * 255 + 0.5F), ig = (int) (g * 255 + 0.5F), ib = (int) (b * 255 + 0.5F);
        return 0xFF000000 | (ir & 255) << 16 | (ig & 255) << 8 | (ib & 255);
    }

    /**
     * Original {@code EntityDamageUtil.getMouseOver(player, reach)} (fehlt im Port-{@code EntityDamageUtil}):
     * Blockstrahl, dann naechstes anvisiertes Wesen; ohne Treffer {@code typeOfHit == MISS} wie im Original.
     */
    public static MovingObjectPosition getMouseOver(Player attacker, double reach) {
        return getMouseOver(attacker, reach, 0D);
    }

    /** Delegiert an das Original {@code EntityDamageUtil.getMouseOver}. */
    public static MovingObjectPosition getMouseOver(Player attacker, double reach, double threshold) {
        return com.hbm_m.util.EntityDamageUtil.getMouseOver(attacker, reach, threshold);
    }

    /**
     * Eine Glanzschicht wie im Original: {@code glDepthFunc(GL_EQUAL)}, {@code glBlendFunc(GL_SRC_COLOR, GL_ONE)},
     * {@code glDepthMask(false)}, Farbe {@code glColor4f(r, g, b, 1)} und Texturmatrix
     * {@code glScalef(scale) * glRotatef(rot, 0, 0, 1) * glTranslatef(0, movement, 0)}; danach wird das Teil gezeichnet.
     */
    public static void renderGlintPart(SimpleObjModel model, String part, ResourceLocation tex, float r, float g, float b,
                                       float scale, float rot, float movement) {
        RenderType type = GlintType.make(tex, r, g, b, scale, rot, movement);
        model.renderPartEntity(part, GunGL.pose(), GunGL.buffers().getBuffer(type), GunGL.light(), OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F);
    }

    private static final class GlintType extends RenderType {

        private GlintType(String n, VertexFormat f, VertexFormat.Mode m, int s, boolean c, boolean so, Runnable a, Runnable b) {
            super(n, f, m, s, c, so, a, b);
        }

        static RenderType make(ResourceLocation tex, float r, float g, float b, float scale, float rot, float movement) {
            CompositeState state = CompositeState.builder()
                    .setShaderState(RENDERTYPE_ENTITY_GLINT_SHADER)
                    .setTextureState(new TextureStateShard(tex, true, false))
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .setDepthTestState(EQUAL_DEPTH_TEST)
                    .setTransparencyState(new TransparencyStateShard("hbm_m_glint_src_color", () -> {
                        RenderSystem.enableBlend();
                        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_COLOR, GlStateManager.DestFactor.ONE);
                    }, () -> {
                        RenderSystem.disableBlend();
                        RenderSystem.defaultBlendFunc();
                    }))
                    .setTexturingState(new TexturingStateShard("hbm_m_glint_texturing", () -> {
                        RenderSystem.setTextureMatrix(new Matrix4f().scale(scale).rotateZ((float) Math.toRadians(rot)).translate(0F, movement, 0F));
                        RenderSystem.setShaderColor(r, g, b, 1F);
                    }, () -> {
                        RenderSystem.resetTextureMatrix();
                        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
                    }))
                    .createCompositeState(false);
            return create("hbm_m_gun_glint", DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS, 256, false, false, state);
        }
    }
}
