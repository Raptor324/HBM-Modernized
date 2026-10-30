package com.hbm_m.client.weapon;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Function;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.hbm_m.client.render.SimpleObjModel;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.Util;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * GL11-Fassade fuer die 1:1-Portierung der SEDNA-Waffen-, Geschoss- und Munitionsrenderer (1.7.10 zeichnete direkt mit
 * {@code GL11} und {@code Tessellator}). Die Methoden heissen wie ihre GL11-Gegenstuecke und schreiben in den
 * aktuellen {@link PoseStack} / {@link MultiBufferSource}; Textur, Farbe, Mischung, Backface-Culling, Tiefe und
 * Beleuchtung werden als Zustand gefuehrt und beim Zeichnen in einen passenden {@link RenderType} uebersetzt.
 *
 * <p>Uebersetzungstabelle fuer Portierungen:</p>
 * <pre>
 * GL11.glPushMatrix()/glPopMatrix()        -> GunGL.pushMatrix()/popMatrix()
 * GL11.glTranslated/f(x,y,z)                -> GunGL.translate(x,y,z)
 * GL11.glRotated/f(a,x,y,z)                 -> GunGL.rotate(a,x,y,z)
 * GL11.glScaled/f(x,y,z)                    -> GunGL.scale(x,y,z)
 * bindTexture(rl) / renderEngine.bindTexture -> GunGL.bindTexture(rl)
 * GL11.glColor3f/4f                         -> GunGL.color(r,g,b[,a])
 * GL11.glEnable/glDisable(GL_BLEND)         -> GunGL.enableBlend()/disableBlend()
 * OpenGlHelper.glBlendFunc / glBlendFunc     -> GunGL.blendFunc(src, dst)  (dst == GL_ONE (1) => additiv)
 * GL11.glEnable/glDisable(GL_CULL_FACE)     -> GunGL.enableCull()/disableCull()
 * GL11.glEnable/glDisable(GL_TEXTURE_2D)    -> GunGL.enableTexture2D()/disableTexture2D()
 * GL11.glDepthMask(b)                       -> GunGL.depthMask(b)
 * GL11.glEnable/glDisable(GL_LIGHTING)      -> GunGL.enableLighting()/disableLighting()  (aus = volle Helligkeit)
 * OpenGlHelper.setLightmapTextureCoords(..240..) -> GunGL.fullbright(true)
 * GL11.glShadeModel / glAlphaFunc / RESCALE_NORMAL -> entfaellt
 * model.renderPart("X") / renderAll()        -> GunGL.renderPart(model, "X") / GunGL.renderAll(model)
 * Tessellator.instance                       -> GunGL.tess() (startDrawingQuads/startDrawing(GL_TRIANGLES)/
 *                                               setColorRGBA_F/setColorOpaque_F/setNormal/addVertex/addVertexWithUV/draw)
 * </pre>
 */
public final class GunGL {

    private GunGL() { }

    public static final int GL_ONE = 1;
    public static final int GL_SRC_ALPHA = 770;
    public static final int GL_ONE_MINUS_SRC_ALPHA = 771;
    public static final int GL_QUADS = 7;
    public static final int GL_TRIANGLES = 4;

    public static final ResourceLocation WHITE = ResourceLocation.tryParse("hbm_m:textures/misc/white.png");

    private static PoseStack pose = new PoseStack();
    private static MultiBufferSource buffers;
    private static int packedLight = LightTexture.FULL_BRIGHT;
    private static int overlay = OverlayTexture.NO_OVERLAY;

    private static ResourceLocation texture = WHITE;
    private static float r = 1F, g = 1F, b = 1F, a = 1F;
    private static boolean blend = false;
    private static boolean additive = false;
    private static boolean cull = true;
    private static boolean texture2D = true;
    private static boolean depthMask = true;
    private static boolean lighting = true;
    private static boolean fullbright = false;

    private static final Deque<State> stateStack = new ArrayDeque<>();

    private record State(ResourceLocation texture, float r, float g, float b, float a, boolean blend, boolean additive,
                         boolean cull, boolean texture2D, boolean depthMask, boolean lighting, boolean fullbright) { }

    /** Beginnt einen Zeichenabschnitt (setzt den GL-Zustand auf Vorgabe). */
    public static void begin(PoseStack poseStack, MultiBufferSource bufferSource, int light) {
        begin(poseStack, bufferSource, light, OverlayTexture.NO_OVERLAY);
    }

    public static void begin(PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlayCoords) {
        com.hbm_m.client.render.weapon.WeaponPartVboRenderer.endWeaponBatch();
        // Не выключаем lightmap здесь: begin() вызывается и посреди фазы ENTITIES
        // (трассеры пуль), а turnOffLightLayer() сносит lightmap для всех последующих сущностей.
        pose = poseStack;
        buffers = bufferSource;
        packedLight = light;
        overlay = overlayCoords;
        resetState();
    }

    public static void end() {
        com.hbm_m.client.render.weapon.WeaponPartVboRenderer.endWeaponBatch();
        com.hbm_m.client.render.weapon.WeaponPartVboRenderer.flushGunBatch();
        buffers = null;
    }

    public static void resetState() {
        texture = WHITE;
        r = g = b = a = 1F;
        blend = false;
        additive = false;
        cull = true;
        texture2D = true;
        depthMask = true;
        lighting = true;
        fullbright = false;
        stateStack.clear();
    }

    /** Sichert den Zeichenzustand (Original {@code GL11.glPushAttrib}). */
    public static void pushAttrib() {
        stateStack.push(new State(texture, r, g, b, a, blend, additive, cull, texture2D, depthMask, lighting, fullbright));
    }

    public static void popAttrib() {
        State s = stateStack.poll();
        if (s == null) return;
        texture = s.texture; r = s.r; g = s.g; b = s.b; a = s.a; blend = s.blend; additive = s.additive;
        cull = s.cull; texture2D = s.texture2D; depthMask = s.depthMask; lighting = s.lighting; fullbright = s.fullbright;
    }

    public static PoseStack pose() { return pose; }
    public static MultiBufferSource buffers() { return buffers; }
    public static int light() { return (fullbright || !lighting) ? LightTexture.FULL_BRIGHT : packedLight; }
    public static int rawLight() { return packedLight; }
    public static void setPackedLight(int light) { packedLight = light; }

    // ─── Matrizen ──────────────────────────────────────────────────────────

    public static void pushMatrix() { pose.pushPose(); }
    public static void popMatrix() { pose.popPose(); }
    public static void translate(double x, double y, double z) { pose.translate(x, y, z); }
    public static void scale(double x, double y, double z) { com.hbm_m.client.render.item.OrigInventoryTransform.scaleSafe(pose, (float) x, (float) y, (float) z); } // vJ: Normalen bei Spiegelung

    /** {@code glRotate(angle, x, y, z)}: Drehung um eine beliebige Achse (Grad, rechte Hand wie OpenGL). */
    public static void rotate(double angle, double x, double y, double z) {
        if (angle == 0) return;
        double len = Math.sqrt(x * x + y * y + z * z);
        if (len < 1.0E-6) return;
        pose.mulPose(new Quaternionf().rotateAxis((float) Math.toRadians(angle), (float) (x / len), (float) (y / len), (float) (z / len)));
    }

    /** Translates to (px, py, pz), rotates by angle (deg) around (ax, ay, az), and translates back. */
    public static void rotateAround(double angle, double ax, double ay, double az, double px, double py, double pz) {
        if (angle == 0) return;
        pose.translate(px, py, pz);
        rotate(angle, ax, ay, az);
        pose.translate(-px, -py, -pz);
    }

    // ─── Zustand ───────────────────────────────────────────────────────────

    public static void bindTexture(ResourceLocation tex) { texture = tex; }
    public static void color(double cr, double cg, double cb) { color(cr, cg, cb, 1D); }
    public static void color(double cr, double cg, double cb, double ca) { r = (float) cr; g = (float) cg; b = (float) cb; a = (float) ca; }
    public static void enableBlend() { blend = true; }
    public static void disableBlend() { blend = false; additive = false; }
    /** {@code glBlendFunc(src, dst)}: {@code dst == GL_ONE} = additiv, sonst normale Alpha-Mischung. */
    public static void blendFunc(int src, int dst) { additive = dst == GL_ONE; }
    public static void blendFunc(int src, int dst, int srcA, int dstA) { blendFunc(src, dst); }
    public static void enableCull() { cull = true; }
    public static void disableCull() { cull = false; }
    public static void enableTexture2D() { texture2D = true; }
    public static void disableTexture2D() { texture2D = false; }
    public static void depthMask(boolean mask) { depthMask = mask; }
    public static void enableLighting() { lighting = true; }
    public static void disableLighting() { lighting = false; }
    public static void fullbright(boolean on) { fullbright = on; }

    public static float red() { return r; }
    public static float green() { return g; }
    public static float blue() { return b; }
    public static float alpha() { return a; }

    // ─── Read hooks for WeaponPartVboRenderer / WeaponRenderRegistry ────
    /** Aktuell gebundene Textur. */
    public static ResourceLocation texture() { return texture2D ? texture : WHITE; }
    public static int overlay() { return overlay; }
    public static boolean isCull() { return cull; }
    public static boolean isBlend() { return blend; }
    public static boolean isAdditive() { return additive; }
    public static boolean isDepthMask() { return depthMask; }
    public static boolean lighting() { return lighting; }
    public static boolean isLighting() { return lighting; }
    public static boolean isFullbright() { return fullbright; }
    public static boolean fullbright() { return fullbright; }

    /**
     * GUI-INVENTORY draws (setupInv mirrors/rotates the model so its Y axis points
     * downward in view space) flip the diffuse light directions accordingly.
     */
    private static boolean guiLighting = false;
    public static void setGuiLighting(boolean value) { guiLighting = value; }
    public static boolean isGuiLighting() { return guiLighting; }

    // ─── RenderTypes ───────────────────────────────────────────────────────

    private record TypeKey(ResourceLocation tex, int mode, boolean cull, boolean depth) { }

    /** mode 0 = deckend, 1 = Alpha, 2 = additiv */
    private static final Function<TypeKey, RenderType> TYPES = Util.memoize(GunRenderTypes::make);

    public static RenderType currentType() {
        ResourceLocation tex = texture2D ? texture : WHITE;
        int mode = !blend ? 0 : (additive ? 2 : 1);
        return TYPES.apply(new TypeKey(tex, mode, cull, depthMask));
    }

    public static VertexConsumer buffer() {
        com.hbm_m.client.render.weapon.WeaponPartVboRenderer.endWeaponBatch();
        com.hbm_m.client.render.weapon.WeaponPartVboRenderer.markQuadsPending();
        return buffers.getBuffer(currentType());
    }

    /**
     * Немедленно отрисовывает накопленные квады (эффекты). Аддитивные спрайты (вспышки, флаеры)
     * вызывают это сразу после {@code tess.draw()}: эффект уходит в кадр собственным draw call'ом
     * со своим setup'ом и не зависит от того, какие полупрозрачные буферы (дым, трассеры)
     * остались висеть в общем BufferSource — их переключение посреди кадра сбивало блендинг.
     */
    public static void flushEffects() {
        com.hbm_m.client.render.weapon.WeaponPartVboRenderer.flushGunBatch();
    }

    private static final class GunRenderTypes extends RenderType {

        private GunRenderTypes(String n, VertexFormat f, VertexFormat.Mode m, int s, boolean c, boolean so, Runnable a, Runnable b) {
            super(n, f, m, s, c, so, a, b);
        }

        /**
         * Истинный аддитив (как 1.7.10 glBlendFunc(GL_SRC_ALPHA, GL_ONE)). ВАЖНО: включение
         * блендинга для mode 2 делает сам шейдер gun_additive.json через свой blend-блок —
         * BlendMode.apply() выполняется в ShaderInstance.apply() (после setupRenderState!)
         * и делает честные GL-вызовы при каждой смене режима; его кэш lastApplied не
         * расходится с физикой. Этот шард лишь гарантирует физически включённый блендинг
         * до apply(): кэшированный enable ноет-опится при расхождении кэша (клоббер
         * Oculus'а, VBO-батчи), поэтому disable+enable принудительно дают реальную пару
         * вызовов в любом состоянии кэша.
         */
        private static final RenderStateShard.TransparencyStateShard HONEST_ADDITIVE_TRANSPARENCY =
                new RenderStateShard.TransparencyStateShard("hbm_m_additive", () -> {
                    com.mojang.blaze3d.platform.GlStateManager._disableBlend();
                    com.mojang.blaze3d.platform.GlStateManager._enableBlend();
                    RenderSystem.blendFuncSeparate(0, 0, 0, 0);
                    RenderSystem.blendFuncSeparate(org.lwjgl.opengl.GL11.GL_SRC_ALPHA, org.lwjgl.opengl.GL11.GL_ONE,
                            org.lwjgl.opengl.GL11.GL_ONE, org.lwjgl.opengl.GL11.GL_ZERO);
                }, () -> {
                    RenderSystem.disableBlend();
                    RenderSystem.defaultBlendFunc();
                    // Аддитивный проход всегда рисует с depthMask(false): без явного
                    // восстановления glDepthMask остаётся false до конца кадра, и все
                    // следующие COLOR_DEPTH_WRITE-батчи (оружие, модель игрока) теряют
                    // запись глубины (детали поверх корпуса, облака сквозь игрока).
                    RenderSystem.depthMask(true);
                    RenderSystem.colorMask(true, true, true, true);
                });

        /**
         * Кастомный аддитивный шейдер с ресинком кэша привязки программы: после сырого
         * _glUseProgram(0) Oculus'а ванильный apply() видит совпадение id в статическом
         * кэше и пропускает честный glUseProgram — все uniform'ы уходят в program 0.
         */
        private static final RenderStateShard.ShaderStateShard RESYNCED_GUN_ADDITIVE_SHADER =
                new RenderStateShard.ShaderStateShard(() -> {
                    ShaderInstance s = com.hbm_m.client.render.shader.ModShaders.getGunAdditiveShader();
                    if (s == null) {
                        s = GameRenderer.getRendertypeEntityTranslucentEmissiveShader();
                    }
                    if (s != null) {
                        com.hbm_m.client.render.shader.ShaderBindResync.ensureFreshBind(s);
                    }
                    return s;
                });

        /**
         * Честный биндинг текстуры эффекта. Кэш GlStateManager для юнита 0 расходится с
         * физическим состоянием после клоббера Oculus'а (и после сырых биндингов VBO-батча
         * оружия): managed {@code _bindTexture(plume)} видит "уже привязано" в кэше и
         * ноет-опится, семплер читает чужую текстуру — вспышка чёрная. Двойной биндинг
         * 0 -> texId гарантирует реальный вызов в любом состоянии кэша (второй вызов
         * всегда отличается от кэша, установленного первым).
         */
        private static RenderStateShard.TextureStateShard honestTexture(ResourceLocation tex) {
            return new RenderStateShard.TextureStateShard(tex, false, false) {
                @Override
                public void setupRenderState() {
                    com.mojang.blaze3d.platform.GlStateManager._activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
                    com.mojang.blaze3d.platform.GlStateManager._bindTexture(0);
                    super.setupRenderState();
                    AbstractTexture t = net.minecraft.client.Minecraft.getInstance()
                            .getTextureManager().getTexture(tex);
                    if (t != null) {
                        com.mojang.blaze3d.platform.GlStateManager._bindTexture(t.getId());
                    }
                }
            };
        }

        static RenderType make(TypeKey key) {
            if (key.mode == 0 && key.depth) {
                return key.cull ? RenderType.entityCutout(key.tex) : RenderType.entityCutoutNoCull(key.tex);
            }
            if (key.mode == 1 && key.depth) {
                return key.cull ? RenderType.entityTranslucentCull(key.tex) : RenderType.entityTranslucent(key.tex);
            }
            if (key.mode == 2) {
                // Истинный аддитив, как в 1.7.10 glBlendFunc(GL_SRC_ALPHA, GL_ONE): чёрный фон
                // текстур вспышек/трассеров при сложении невидим. Кастомный gun_additive шейдер
                // несёт blend-блок в JSON — блендинг включается самим шейдером при каждом draw
                // (после setupRenderState) честными BlendMode-вызовами, см. HONEST_ADDITIVE.
                CompositeState state = CompositeState.builder()
                        .setShaderState(RESYNCED_GUN_ADDITIVE_SHADER)
                        .setTextureState(honestTexture(key.tex))
                        .setTransparencyState(HONEST_ADDITIVE_TRANSPARENCY)
                        .setCullState(key.cull ? CULL : NO_CULL)
                        .setLightmapState(LIGHTMAP)
                        .setOverlayState(OVERLAY)
                        // Аддитивный шейдер (gun_additive) сам выставляет depthMask(false)
                        // при apply(); ванильный COLOR_DEPTH_WRITE.clearRenderState() восстанавливает
                        // depthMask только когда writeDepth == false, поэтому для mode 2 всегда
                        // COLOR_WRITE — гарантирует возврат depthMask(true) в clearRenderState().
                        .setWriteMaskState(COLOR_WRITE)
                        .createCompositeState(false);
                return create("hbm_m_gun_2_" + (key.cull ? "c" : "n") + (key.depth ? "d" : "x") + "_" + key.tex,
                        DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 4096, false, true, state);
            }
            CompositeState state = CompositeState.builder()
                    .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(key.tex, false, false))
                    .setTransparencyState(key.mode == 1 ? TRANSLUCENT_TRANSPARENCY : NO_TRANSPARENCY)
                    .setCullState(key.cull ? CULL : NO_CULL)
                    .setLightmapState(LIGHTMAP)
                    .setOverlayState(OVERLAY)
                    .setWriteMaskState(key.depth ? COLOR_DEPTH_WRITE : COLOR_WRITE)
                    .createCompositeState(false);
            return create("hbm_m_gun_" + key.mode + "_" + (key.cull ? "c" : "n") + (key.depth ? "d" : "x") + "_" + key.tex,
                    DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 4096, false, key.mode != 0, state);
        }
    }

    // ─── Modelle ───────────────────────────────────────────────────────────

    public static void renderPart(ResourceLocation modelLocation, String part) {
        com.hbm_m.client.render.weapon.WeaponRenderRegistry.renderPart(modelLocation, part, pose());
    }

    public static void renderPart(SimpleObjModel model, String part) {
        if (model != null && model.getLocation() != null && com.hbm_m.client.render.weapon.WeaponRenderRegistry.hasPartRenderer(model.getLocation(), part)) {
            renderPart(model.getLocation(), part);
        } else if (model != null) {
            model.renderPartEntity(part, pose, buffer(), light(), overlay, r, g, b, a);
        }
    }

    public static void renderAll(ResourceLocation modelLocation) {
        com.hbm_m.client.render.weapon.WeaponRenderRegistry.renderAll(modelLocation, pose());
    }

    public static void renderAll(SimpleObjModel model) {
        if (model != null && model.getLocation() != null && com.hbm_m.client.render.weapon.WeaponRenderRegistry.hasModel(model.getLocation())) {
            renderAll(model.getLocation());
        } else if (model != null) {
            model.renderAllEntity(pose, buffer(), light(), overlay, r, g, b, a);
        }
    }

    /** Original {@code renderAllExcept(...)}. */
    public static void renderAllExcept(ResourceLocation modelLocation, String... except) {
        com.hbm_m.client.render.weapon.WeaponRenderRegistry.renderAllExcept(modelLocation, pose(), except);
    }

    public static void renderAllExcept(SimpleObjModel model, String... except) {
        if (model != null && model.getLocation() != null && com.hbm_m.client.render.weapon.WeaponRenderRegistry.hasModel(model.getLocation())) {
            renderAllExcept(model.getLocation(), except);
        } else if (model != null) {
            java.util.Set<String> skip = java.util.Set.of(except);
            for (String part : model.getPartNames()) {
                if (!skip.contains(part)) renderPart(model, part);
            }
        }
    }

    /** Original {@code renderOnly(...)}. */
    public static void renderOnly(ResourceLocation modelLocation, String... only) {
        for (String part : only) renderPart(modelLocation, part);
    }

    public static void renderOnly(SimpleObjModel model, String... only) {
        if (model != null && model.getLocation() != null && com.hbm_m.client.render.weapon.WeaponRenderRegistry.hasModel(model.getLocation())) {
            renderOnly(model.getLocation(), only);
        } else if (model != null) {
            for (String part : only) renderPart(model, part);
        }
    }

    // ─── Tessellator-Ersatz ────────────────────────────────────────────────

    private static final Tess TESS = new Tess();

    public static Tess tess() { return TESS; }

    /** Ersatz fuer {@code net.minecraft.client.renderer.Tessellator} (1.7.10) ueber den aktuellen GunGL-Zustand. */
    public static final class Tess {

        private static final int FLOATS_PER_VERT = 12;
        private int mode = GL_QUADS;
        private float[] vertData = new float[256 * FLOATS_PER_VERT];
        private int vertCount = 0;
        private float cr = 1F, cg = 1F, cb = 1F, ca = 1F;
        private boolean hasColor = false;
        private float nx = 0F, ny = 1F, nz = 0F;
        private int brightness = -1;

        public void startDrawingQuads() { startDrawing(GL_QUADS); }

        public void startDrawing(int glMode) {
            this.mode = glMode;
            this.vertCount = 0;
            this.hasColor = false;
            this.brightness = -1;
        }

        public void setColorRGBA_F(float r, float g, float b, float a) { cr = r; cg = g; cb = b; ca = a; hasColor = true; }
        public void setColorOpaque_F(float r, float g, float b) { setColorRGBA_F(r, g, b, 1F); }
        public void setColorOpaque_I(int rgb) { setColorRGBA_F((rgb >> 16 & 255) / 255F, (rgb >> 8 & 255) / 255F, (rgb & 255) / 255F, 1F); }
        public void setColorRGBA_I(int rgb, int alpha) { setColorRGBA_F((rgb >> 16 & 255) / 255F, (rgb >> 8 & 255) / 255F, (rgb & 255) / 255F, alpha / 255F); }
        public void setColorRGBA(int r, int g, int b, int a) { setColorRGBA_F(r / 255F, g / 255F, b / 255F, a / 255F); }
        public void setNormal(float x, float y, float z) { nx = x; ny = y; nz = z; }
        public void setBrightness(int packed) {
            if (packed == 240) packed = LightTexture.FULL_BRIGHT;
            brightness = packed;
        }

        public void addVertex(double x, double y, double z) { addVertexWithUV(x, y, z, 0, 0); }

        public void addVertexWithUV(double x, double y, double z, double u, double v) {
            int base = vertCount * FLOATS_PER_VERT;
            if (base + FLOATS_PER_VERT > vertData.length) {
                vertData = java.util.Arrays.copyOf(vertData, vertData.length * 2);
            }
            float vr = hasColor ? cr : GunGL.r, vg = hasColor ? cg : GunGL.g, vb = hasColor ? cb : GunGL.b, va = hasColor ? ca : GunGL.a;
            vertData[base]     = (float) x;
            vertData[base + 1] = (float) y;
            vertData[base + 2] = (float) z;
            vertData[base + 3] = (float) u;
            vertData[base + 4] = (float) v;
            vertData[base + 5] = vr;
            vertData[base + 6] = vg;
            vertData[base + 7] = vb;
            vertData[base + 8] = va;
            vertData[base + 9] = nx;
            vertData[base + 10] = ny;
            vertData[base + 11] = nz;
            vertCount++;
        }

        public void draw() {
            // Под активным шейдерпаком аддитивные квады ставятся в post-composite очередь:
            // draw кастомного gun_additive в gbuffer пака — незарегистрированный контент,
            // который Photon переинтерпретирует как данные материала, а Solas затирает
            // небом (см. PackAdditiveQueue). Без пака и в GUI/теневом проходе — легаси-путь.
            if (GunGL.isBlend() && GunGL.isAdditive()
                    && com.hbm_m.client.render.shader.PackAdditiveQueue.engaged()) {
                com.hbm_m.client.render.shader.PackAdditiveQueue.push(
                        texture2D ? texture : WHITE, mode, pose.last().pose(), vertData, vertCount * FLOATS_PER_VERT);
                vertCount = 0;
                hasColor = false;
                brightness = -1;
                return;
            }
            VertexConsumer vc = buffer();
            Matrix4f m = pose.last().pose();
            Matrix3f nm = pose.last().normal();
            int light = brightness >= 0 ? brightness : light();
            Vector3f n = new Vector3f();
            if (mode == GL_TRIANGLES) {
                for (int i = 0; i + 2 < vertCount; i += 3) {
                    for (int k = 0; k < 4; k++) {
                        put(vc, m, nm, n, vertData, (i + Math.min(k, 2)) * FLOATS_PER_VERT, light);
                    }
                }
            } else {
                for (int i = 0; i + 3 < vertCount; i += 4) {
                    for (int k = 0; k < 4; k++) {
                        put(vc, m, nm, n, vertData, (i + k) * FLOATS_PER_VERT, light);
                    }
                }
            }
            vertCount = 0;
            hasColor = false;
            brightness = -1;
        }

        private static void put(VertexConsumer vc, Matrix4f m, Matrix3f nm, Vector3f n, float[] v, int base, int light) {
            n.set(v[base + 9], v[base + 10], v[base + 11]).mul(nm);
            com.hbm_m.platform.RenderHooks.vertexFull(vc, m, v[base], v[base + 1], v[base + 2],
                    (int) (v[base + 5] * 255), (int) (v[base + 6] * 255), (int) (v[base + 7] * 255), (int) (v[base + 8] * 255),
                    v[base + 3], v[base + 4], overlay, light, n.x, n.y, n.z);
        }
    }
}
