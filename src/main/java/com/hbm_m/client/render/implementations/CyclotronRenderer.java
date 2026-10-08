package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.machines.MachineCyclotronBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

/**
 * 1:1 {@code RenderCyclotron}: Koerper, vier Stecker (Asche/Buch/Hammer/Muenze, gefuellt oder leer) und - sind alle
 * gesteckt - der kreisende Schriftzug in Standard-Galaktisch ({@code plures necat crapula quam gladius}, 0x600060).
 */
public class CyclotronRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineCyclotronBlockEntity> {

    static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/machines/cyclotron.obj"));
    static final String[] PLUGS = { "ashes", "book", "gavel", "coin" };
    /** Original {@code standardGalacticFontRenderer} - im Spiel die Schrift {@code minecraft:alt}. */
    static final Style GALACTIC = Style.EMPTY.withFont(ResourceLocation.fromNamespaceAndPath("minecraft", "alt"));

    static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/machines/" + name + ".png");
    }

    public CyclotronRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(MachineCyclotronBlockEntity cyc, float f, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);

        MODEL.renderPart("Body", ps, buf.getBuffer(RenderType.entityCutoutNoCull(tex("cyclotron"))), light);

        boolean plugged = true;
        for (int i = 0; i < 4; i++) {
            boolean plug = cyc.getPlug(i);
            if (!plug) plugged = false;
            String name = "cyclotron_" + PLUGS[i] + (plug ? "_filled" : "");
            MODEL.renderPart("B" + (i + 1), ps, buf.getBuffer(RenderType.entityCutoutNoCull(tex(name))), light);
        }

        if (plugged) {
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees((float) (System.currentTimeMillis() * 0.025 % 360)));
            String msg = "plures necat crapula quam gladius";
            ps.translate(0, 2, 0);
            ps.mulPose(Axis.XP.rotationDegrees(180));
            float rot = 0F;
            Font font = Minecraft.getInstance().font;

            for (char c : msg.toCharArray()) {
                ps.pushPose();
                ps.mulPose(Axis.YP.rotationDegrees(rot));
                // Original: fontRenderer.getCharWidth(c) * 2 - die normale Schrift bestimmt den Abstand
                rot -= font.width(String.valueOf(c)) * 2F;
                ps.translate(2.75, 0, 0);
                ps.mulPose(Axis.YP.rotationDegrees(-90));
                float scale = 0.1F;
                ps.scale(scale, scale, scale);
                font.drawInBatch(Component.literal(String.valueOf(c)).withStyle(GALACTIC), 0, 0, 0x600060, false,
                        ps.last().pose(), buf, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
                ps.popPose();
            }
            ps.popPose();
        }

        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(MachineCyclotronBlockEntity be) { return true; }
}
