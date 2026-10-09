//? if forge || neoforge {
package com.hbm_m.client.model;

import java.util.List;
import java.util.Map;

import org.joml.Matrix4f;

import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.Util;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
//? if forge {
import net.minecraftforge.client.model.BakedModelWrapper;
//?} else {
/*import net.neoforged.neoforge.client.model.BakedModelWrapper;
*///?}

/**
 * 1:1 {@code ItemRendererMeteorSword}: das flache Schwertsymbol plus ein eingefaerbter Glanz in zwei Lagen
 * ({@code glColor4f(r * 0.36, g * 0.36, b * 0.36)}, {@code glDepthFunc(GL_EQUAL)}).
 * <ul>
 * <li>Inventar: {@code renderGlintFlat} - Texturkoordinaten {@code (anim + x + y * sizeMultU) / 256}, {@code y / 256}
 * ueber das 16x16-Feld, Lauf 3000 bzw. 4873 ms, Scherung 4 bzw. -1.</li>
 * <li>In der Hand: {@code renderGlint3D} - Texturmatrix {@code glScalef(0.125) * glTranslatef(+-offset) *
 * glRotatef(-50 bzw. 10)} auf {@code renderItemIn2D(0, 0, 1, 1)}.</li>
 * <li>Am Boden ({@code ENTITY}) zeichnet das Original kein Eigenes - dort bleibt das normale Itemmodell.</li>
 * </ul>
 * Die Atlas-UVs der Itemquads werden per Texturmatrix auf 0..1 ueber das Symbol zurueckgerechnet. Mischung
 * {@code SRC_COLOR, ONE} wie in {@code renderGlint3D} (die GUI-Variante {@code 772, 1} haengt am Zielalpha, das es im
 * 1.20-Bildpuffer so nicht gibt).
 */
public class MeteorSwordGlintModel extends BakedModelWrapper<BakedModel> {

    private static final ResourceLocation GLINT = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/misc/glint.png");

    private final Pass guiPass;
    private final Pass handPass;

    public MeteorSwordGlintModel(BakedModel original, float r, float g, float b) {
        super(original);
        TextureAtlasSprite sprite = original.getParticleIcon();
        float in = 0.36F;
        float cr = r * in, cg = g * in, cb = b * in;
        this.guiPass = new Pass(original, List.of(Sheets.translucentItemSheet(),
                GlintType.make("gui0", sprite, cr, cg, cb, true, 0), GlintType.make("gui1", sprite, cr, cg, cb, true, 1)));
        this.handPass = new Pass(original, List.of(Sheets.translucentItemSheet(),
                GlintType.make("hand0", sprite, cr, cg, cb, false, 0), GlintType.make("hand1", sprite, cr, cg, cb, false, 1)));
    }

    @Override
    public BakedModel applyTransform(ItemDisplayContext ctx, PoseStack pose, boolean leftHand) {
        BakedModel base = originalModel.applyTransform(ctx, pose, leftHand);
        return switch (ctx) {
            case GUI -> guiPass.with(base);
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> handPass.with(base);
            default -> base;
        };
    }

    /** Ein Durchgang mit dem Grundbild und den beiden Glanzlagen in dieser Reihenfolge. */
    private static final class Pass extends BakedModelWrapper<BakedModel> {

        private final List<RenderType> types;

        Pass(BakedModel base, List<RenderType> types) {
            super(base);
            this.types = types;
        }

        Pass with(BakedModel base) {
            return base == originalModel ? this : new Pass(base, types);
        }

        @Override
        public List<RenderType> getRenderTypes(ItemStack stack, boolean fabulous) {
            return types;
        }

        @Override
        public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) {
            return List.of(this);
        }
    }

    private static final class GlintType extends RenderType {

        private GlintType(String n, VertexFormat f, VertexFormat.Mode m, int s, boolean c, boolean so, Runnable a, Runnable b) {
            super(n, f, m, s, c, so, a, b);
        }

        static RenderType make(String id, TextureAtlasSprite sprite, float r, float g, float b, boolean flat, int layer) {
            // Atlas-UV -> 0..1 ueber das Symbol
            float du = sprite.getU1() - sprite.getU0();
            float dv = sprite.getV1() - sprite.getV0();
            Matrix4f toLocal = new Matrix4f().scale(1F / du, 1F / dv, 1F).translate(-sprite.getU0(), -sprite.getV0(), 0F);

            CompositeState state = CompositeState.builder()
                    .setShaderState(RENDERTYPE_GLINT_SHADER)
                    .setTextureState(new TextureStateShard(MeteorSwordGlintModel.GLINT, true, false)) // qualifiziert: 1.21.1 erbt RenderType.GLINT
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .setDepthTestState(EQUAL_DEPTH_TEST)
                    .setTransparencyState(new TransparencyStateShard("hbm_m_meteor_glint_blend", () -> {
                        RenderSystem.enableBlend();
                        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_COLOR, GlStateManager.DestFactor.ONE);
                    }, () -> {
                        RenderSystem.disableBlend();
                        RenderSystem.defaultBlendFunc();
                    }))
                    .setTexturingState(new TexturingStateShard("hbm_m_meteor_glint_texturing", () -> {
                        RenderSystem.setTextureMatrix(flat ? flatMatrix(layer).mul(toLocal) : handMatrix(layer).mul(toLocal));
                        RenderSystem.setShaderColor(r, g, b, 1F);
                    }, () -> {
                        RenderSystem.resetTextureMatrix();
                        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
                    }))
                    .createCompositeState(false);
            return create("hbm_m_meteor_glint_" + id, DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS, 256, false, false, state);
        }

        /** renderGlintFlat: u = (anim + 16 ul + 16 vl * sizeMultU) / 256, v = 16 vl / 256. */
        private static Matrix4f flatMatrix(int j) {
            long period = 3000L + j * 1873L;
            float anim = (float) (Util.getMillis() % period) / (float) period * 256.0F;
            float sizeMultU = j == 1 ? -1.0F : 4.0F;
            return new Matrix4f()
                    .m00(1F / 16F).m10(sizeMultU / 16F).m30(anim / 256F)
                    .m11(1F / 16F);
        }

        /** renderGlint3D: scale(0.125) * translate(+-offset) * rotate(-50 bzw. 10), auf renderItemIn2D(0, 0, 1, 1) (u gespiegelt). */
        private static Matrix4f handMatrix(int j) {
            float scale = 0.125F;
            Matrix4f mat = new Matrix4f().scale(scale, scale, scale);
            if (j == 0) {
                float offset = (float) (Util.getMillis() % 3000L) / 3000.0F * 8.0F;
                mat.translate(offset, 0F, 0F).rotateZ((float) Math.toRadians(-50.0F));
            } else {
                float offset = (float) (Util.getMillis() % 4873L) / 4873.0F * 8.0F;
                mat.translate(-offset, 0F, 0F).rotateZ((float) Math.toRadians(10.0F));
            }
            // renderItemIn2D(tess, 0, 0, 1, 1): maxU = 0, minU = 1 -> u' = 1 - u
            return mat.mul(new Matrix4f().m00(-1F).m30(1F));
        }
    }

    /** Farben wie in ClientProxy.registerItemRenderer(... new ItemRendererMeteorSword(r, g, b)). */
    // Schluessel: 1.20.1 ResourceLocation (ModelResourceLocation ist Unterklasse), 1.21.1 ModelResourceLocation
    public static void wrapAll(Map<? super ModelResourceLocation, BakedModel> models) {
        wrap(models, ModItems.METEORITE_SWORD_SEARED, 1.0F, 0.5F, 0.0F);
        wrap(models, ModItems.METEORITE_SWORD_REFORGED, 0.5F, 1.0F, 1.0F);
        wrap(models, ModItems.METEORITE_SWORD_HARDENED, 0.25F, 0.25F, 0.25F);
        wrap(models, ModItems.METEORITE_SWORD_ALLOYED, 0.0F, 0.5F, 1.0F);
        wrap(models, ModItems.METEORITE_SWORD_MACHINED, 1.0F, 1.0F, 0.0F);
        wrap(models, ModItems.METEORITE_SWORD_TREATED, 0.5F, 1.0F, 0.5F);
        wrap(models, ModItems.METEORITE_SWORD_ETCHED, 1.0F, 1.0F, 0.5F);
        wrap(models, ModItems.METEORITE_SWORD_BRED, 0.5F, 0.5F, 0.0F);
        wrap(models, ModItems.METEORITE_SWORD_IRRADIATED, 0.75F, 1.0F, 0.0F);
        wrap(models, ModItems.METEORITE_SWORD_FUSED, 1.0F, 0.0F, 0.5F);
        wrap(models, ModItems.METEORITE_SWORD_BALEFUL, 0.0F, 1.0F, 0.0F);
    }

    private static void wrap(Map<? super ModelResourceLocation, BakedModel> models, dev.architectury.registry.registries.RegistrySupplier<? extends Item> item,
                             float r, float g, float b) {
        ModelResourceLocation loc = new ModelResourceLocation(item.getId(), "inventory");
        BakedModel baked = models.get(loc);
        if (baked == null || baked instanceof MeteorSwordGlintModel) return;
        models.put(loc, new MeteorSwordGlintModel(baked, r, g, b));
    }
}
//?}
