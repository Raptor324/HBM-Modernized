package com.hbm_m.client.render.raptorpath;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.render.implementations.RBMKColumnRenderer;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.platform.RenderHooks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

/**
 * Bruecke vom immediate-OBJ-Pfad der HBM-BERs ({@link SimpleObjModel} bzw. die Dreieckslisten aus
 * {@link RBMKColumnRenderer#getObj}) zu Raptors VBO-Renderpfad ({@code MachineRenderers.dynamicPart}).
 *
 * <p>Ein {@link Painter} zeichnet die Geometrie genau so wie der alte BER (gleiche Teile, gleiche Reihenfolge,
 * gleiche Spiegelung {@code v -> 1 - v}), aber in einen Erfassungspuffer. Daraus entstehen {@link BakedQuad}s mit
 * Blockatlas-UVs, denn Raptors Shader bindet nur den Blockatlas. Feste Transformationen (Drehungen, Versaetze,
 * auch Skalierungen) koennen in die Geometrie eingebacken werden ({@link Baker#pose}); Raptors Instancing selbst
 * kennt nur Position + Drehung.
 *
 * <p>Texturen ausserhalb {@code textures/block/**} muessen ueber {@code assets/minecraft/atlases/blocks.json}
 * ("single") im Atlas stehen. UVs werden auf [0, 1] begrenzt (sonst laese der Atlas Nachbarsprites); Modelle mit
 * echter Kachelung bleiben deshalb immediate.
 *
 * <p>Neuladen: Der Atlas vergibt neue Sprite-Objekte; dann wird neu gebacken und {@link #cacheKey()} bekommt eine
 * neue Generation. Ist ein Modell noch nicht lesbar, liefert der Schluessel hoechstens 30-mal im Sekundenabstand
 * einen neuen "pending"-Wert (kein dauerhaft leerer VBO, aber auch kein unbegrenztes Wachstum von Raptors Cache).
 *
 * <p>Nur Render-Thread.
 */
//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public final class ObjAtlasMesh {

    /** Zeichnet die Teilgeometrie in den {@link Baker} (wie der alte BER, nur ohne Bildschirm). */
    @FunctionalInterface
    public interface Painter {
        void paint(Baker baker);
    }

    private static final int MAX_PENDING = 30;

    private final String key;
    private final Painter painter;

    @Nullable private List<BakedQuad> quads;
    private int bakedGeneration = -1;
    private String readyKey = "";
    private boolean pending;
    private int pendingTries;
    private long nextTry;
    private String pendingKey = "";
    private boolean warned;

    private ObjAtlasMesh(String key, Painter painter) {
        this.key = key;
        this.painter = painter;
    }

    public static ObjAtlasMesh of(String key, Painter painter) {
        return new ObjAtlasMesh(key, painter);
    }

    /** Genau diese Gruppen in dieser Reihenfolge ({@code renderPart}). */
    public static ObjAtlasMesh parts(SimpleObjModel model, ResourceLocation texture, String key, String... names) {
        return new ObjAtlasMesh(key, b -> b.obj(model, texture, names));
    }

    /** Alle Gruppen ({@code renderAll}). */
    public static ObjAtlasMesh all(SimpleObjModel model, ResourceLocation texture, String key) {
        return new ObjAtlasMesh(key, b -> b.objAll(model, texture));
    }

    /** Gruppen einer {@link RBMKColumnRenderer#getObj}-Datei ({@code renderObjGroup}, Sprite = Atlas-ID). */
    public static ObjAtlasMesh groups(String objPath, ResourceLocation spriteId, String key, String... groups) {
        return new ObjAtlasMesh(key, b -> {
            for (String g : groups) b.group(objPath, g, spriteId);
        });
    }

    /** Teilgeometrie fuer {@code QuadResolver} (leer, solange Modell/Atlas nicht bereit sind). */
    public List<BakedQuad> quads() {
        refresh();
        return quads == null ? List.of() : quads;
    }

    /** Schluessel fuer {@code dynamicPart(..., cacheKeyFn)}. */
    public String cacheKey() {
        refresh();
        return pending ? pendingKey : readyKey;
    }

    /**
     * Zeichnet die gebackene Geometrie immediate (Hooks, die dasselbe Teil mehrfach oder mit wechselnder
     * Anzahl brauchen). Blockatlas, {@code RenderType.cutout()}, Licht wie der Aufrufer.
     */
    public void renderImmediate(PoseStack ps, MultiBufferSource buf, int light) {
        List<BakedQuad> q = quads();
        if (q.isEmpty()) return;
        VertexConsumer vc = buf.getBuffer(RenderType.cutout());
        PoseStack.Pose pose = ps.last();
        for (BakedQuad quad : q) {
            RenderHooks.putBulkData(vc, pose, quad, 1F, 1F, 1F, 1F, light, OverlayTexture.NO_OVERLAY, false);
        }
    }

    private void refresh() {
        int gen = atlasGeneration();
        if (gen != bakedGeneration) {
            bakedGeneration = gen;
            quads = null;
            pending = false;
            pendingTries = 0;
            nextTry = 0L;
            readyKey = key + "#" + gen;
        }
        if (quads == null || pending) {
            long now = System.currentTimeMillis();
            if (quads != null && now < nextTry) return;
            Baker baker = new Baker();
            try {
                painter.paint(baker);
            } catch (Throwable t) {
                baker.incomplete = true;
                if (!warned) {
                    warned = true;
                    MainRegistry.LOGGER.error("[hbm_m] ObjAtlasMesh {}: Backen fehlgeschlagen", key, t);
                }
            }
            quads = List.copyOf(baker.out);
            if (baker.incomplete && pendingTries < MAX_PENDING) {
                pending = true;
                pendingTries++;
                nextTry = now + 1000L;
                pendingKey = key + "#pending" + pendingTries;
            } else {
                pending = false;
            }
        }
    }

    // ── Atlas-Generation ───────────────────────────────────────────────────────────────

    @Nullable private static TextureAtlasSprite lastMissing;
    private static int generation;

    /** Steigt bei jedem Neuaufbau des Blockatlas (neues Fehlt-Sprite-Objekt). */
    static int atlasGeneration() {
        TextureAtlasSprite missing = blockAtlas().getSprite(MissingTextureAtlasSprite.getLocation());
        if (missing != lastMissing) {
            lastMissing = missing;
            generation++;
        }
        return generation;
    }

    static TextureAtlas blockAtlas() {
        AbstractTexture tex = Minecraft.getInstance().getTextureManager().getTexture(InventoryMenu.BLOCK_ATLAS);
        return (TextureAtlas) tex;
    }

    /** {@code ns:textures/x/y.png} -> Atlas-Sprite {@code ns:x/y}; ohne Praefix/Endung unveraendert. */
    public static ResourceLocation spriteIdOf(ResourceLocation texture) {
        String path = texture.getPath();
        if (path.startsWith("textures/")) path = path.substring("textures/".length());
        if (path.endsWith(".png")) path = path.substring(0, path.length() - 4);
        return ResourceLocation.fromNamespaceAndPath(texture.getNamespace(), path);
    }

    // ── Backen ─────────────────────────────────────────────────────────────────────────

    /** Erfasst die Zeichenaufrufe eines {@link Painter} als BakedQuads im Blockatlas. */
    public static final class Baker {

        /** Backtransformation (Ausgangslage Identitaet); Painter duerfen push/pop/translate/rotate/scale. */
        public final PoseStack pose = new PoseStack();
        final List<BakedQuad> out = new ArrayList<>();
        boolean incomplete;

        private Baker() {}

        public TextureAtlasSprite sprite(ResourceLocation spriteId) {
            TextureAtlasSprite s = blockAtlas().getSprite(spriteId);
            if (MissingTextureAtlasSprite.getLocation().equals(s.contents().name()) && !MISSING_WARNED.contains(spriteId)) {
                MISSING_WARNED.add(spriteId);
                MainRegistry.LOGGER.warn("[hbm_m] ObjAtlasMesh: Sprite {} fehlt im Blockatlas (atlases/blocks.json?)", spriteId);
            }
            return s;
        }

        /** {@code model.renderPart(name)} fuer jede Gruppe in dieser Reihenfolge; Textur = Datei unter textures/. */
        public void obj(SimpleObjModel model, ResourceLocation texture, String... names) {
            if (model.getPartNames().isEmpty()) { incomplete = true; return; }
            TextureAtlasSprite s = sprite(spriteIdOf(texture));
            Capture c = new Capture();
            for (String n : names) model.renderPart(n, pose, c, 0);
            emit(c.vertices, s);
        }

        /** {@code model.renderAll}. */
        public void objAll(SimpleObjModel model, ResourceLocation texture) {
            if (model.getPartNames().isEmpty()) { incomplete = true; return; }
            TextureAtlasSprite s = sprite(spriteIdOf(texture));
            Capture c = new Capture();
            model.renderAll(pose, c, 0);
            emit(c.vertices, s);
        }

        /** Eine Gruppe aus {@link RBMKColumnRenderer#getObj} ({@code renderObjGroup}, v gespiegelt). */
        public void group(String objPath, String group, ResourceLocation spriteId) {
            Map<String, List<float[]>> obj = RBMKColumnRenderer.getObj(objPath);
            if (obj.isEmpty()) { incomplete = true; return; }
            triangles(obj.get(group), sprite(spriteId));
        }

        /** Alle Gruppen einer {@link RBMKColumnRenderer#getObj}-Datei in Dateireihenfolge. */
        public void allGroups(String objPath, ResourceLocation spriteId) {
            Map<String, List<float[]>> obj = RBMKColumnRenderer.getObj(objPath);
            if (obj.isEmpty()) { incomplete = true; return; }
            TextureAtlasSprite s = sprite(spriteId);
            for (List<float[]> tris : obj.values()) triangles(tris, s);
        }

        /** Dreiecke im Format x,y,z,u,v,nx,ny,nz je Ecke (OBJ-UV, v wird wie ueberall gespiegelt). */
        public void triangles(@Nullable List<float[]> tris, TextureAtlasSprite s) {
            if (tris == null) return;
            Matrix4f m = pose.last().pose();
            Matrix3f n = pose.last().normal();
            List<float[]> v = new ArrayList<>(tris.size() * 4);
            Vector3f p = new Vector3f();
            Vector3f nn = new Vector3f();
            for (float[] tri : tris) {
                for (int pass = 0; pass < 4; pass++) {
                    int b = Math.min(pass, 2) * 8;
                    m.transformPosition(tri[b], tri[b + 1], tri[b + 2], p);
                    nn.set(tri[b + 5], tri[b + 6], tri[b + 7]).mul(n);
                    v.add(new float[] { p.x, p.y, p.z, tri[b + 3], 1F - tri[b + 4], nn.x, nn.y, nn.z });
                }
            }
            emit(v, s);
        }

        /**
         * Viereck mit bereits fertigen Atlas-UVs (flache RBMK-Saeulenflaechen). Ecken in Zeichenreihenfolge,
         * je {x, y, z, u, v}; Normale in Modellkoordinaten vor der Backtransformation.
         */
        public void atlasQuad(TextureAtlasSprite s, float[][] corners, float nx, float ny, float nz) {
            Matrix4f m = pose.last().pose();
            Matrix3f n = pose.last().normal();
            Vector3f nn = new Vector3f(nx, ny, nz).mul(n);
            Vector3f p = new Vector3f();
            int[] data = new int[32];
            for (int i = 0; i < 4; i++) {
                float[] c = corners[i];
                m.transformPosition(c[0], c[1], c[2], p);
                put(data, i, p.x, p.y, p.z, c[3], c[4], nn.x, nn.y, nn.z);
            }
            out.add(new BakedQuad(data, -1, Direction.getNearest(nn.x, nn.y, nn.z), s, true));
        }

        private void emit(List<float[]> v, TextureAtlasSprite s) {
            float u0 = s.getU0(), du = s.getU1() - u0;
            float v0 = s.getV0(), dv = s.getV1() - v0;
            int count = v.size() / 4;
            for (int q = 0; q < count; q++) {
                int[] data = new int[32];
                float nxSum = 0, nySum = 0, nzSum = 0;
                for (int i = 0; i < 4; i++) {
                    float[] p = v.get(q * 4 + i);
                    put(data, i, p[0], p[1], p[2], u0 + clamp01(p[3]) * du, v0 + clamp01(p[4]) * dv, p[5], p[6], p[7]);
                    nxSum += p[5];
                    nySum += p[6];
                    nzSum += p[7];
                }
                out.add(new BakedQuad(data, -1, Direction.getNearest(nxSum, nySum, nzSum), s, true));
            }
        }

        private static void put(int[] data, int i, float x, float y, float z, float u, float v, float nx, float ny, float nz) {
            int o = i * 8;
            data[o] = Float.floatToRawIntBits(x);
            data[o + 1] = Float.floatToRawIntBits(y);
            data[o + 2] = Float.floatToRawIntBits(z);
            data[o + 3] = -1;
            data[o + 4] = Float.floatToRawIntBits(u);
            data[o + 5] = Float.floatToRawIntBits(v);
            data[o + 6] = 0;
            data[o + 7] = packNormal(nx, ny, nz);
        }
    }

    private static final java.util.Set<ResourceLocation> MISSING_WARNED = new java.util.HashSet<>();

    private static float clamp01(float f) {
        return f < 0F ? 0F : (f > 1F ? 1F : f);
    }

    private static int packNormal(float x, float y, float z) {
        float len = (float) Math.sqrt(x * x + y * y + z * z);
        if (len > 1.0E-6F) { x /= len; y /= len; z /= len; }
        return ((byte) Math.round(x * 127F) & 0xFF)
                | (((byte) Math.round(y * 127F) & 0xFF) << 8)
                | (((byte) Math.round(z * 127F) & 0xFF) << 16);
    }

    /** Erfasst x, y, z, u, v, nx, ny, nz je Eckpunkt (SimpleObjModel schreibt Dreiecke als entartete Vierecke). */
    private static final class Capture implements VertexConsumer {
        final List<float[]> vertices = new ArrayList<>();
        private float[] cur = new float[8];

        private VertexConsumer begin(float x, float y, float z) {
            cur = new float[8];
            cur[0] = x;
            cur[1] = y;
            cur[2] = z;
            vertices.add(cur);
            return this;
        }

        //? if < 1.21.1 {
        @Override public VertexConsumer vertex(double x, double y, double z) { return begin((float) x, (float) y, (float) z); }
        @Override public VertexConsumer color(int r, int g, int b, int a) { return this; }
        @Override public VertexConsumer uv(float u, float v) { cur[3] = u; cur[4] = v; return this; }
        @Override public VertexConsumer overlayCoords(int u, int v) { return this; }
        @Override public VertexConsumer uv2(int u, int v) { return this; }
        @Override public VertexConsumer normal(float x, float y, float z) { cur[5] = x; cur[6] = y; cur[7] = z; return this; }
        @Override public void endVertex() { }
        @Override public void defaultColor(int r, int g, int b, int a) { }
        @Override public void unsetDefaultColor() { }
        //?} else {
        /*@Override public VertexConsumer addVertex(float x, float y, float z) { return begin(x, y, z); }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) { return this; }
        @Override public VertexConsumer setUv(float u, float v) { cur[3] = u; cur[4] = v; return this; }
        @Override public VertexConsumer setUv1(int u, int v) { return this; }
        @Override public VertexConsumer setUv2(int u, int v) { return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { cur[5] = x; cur[6] = y; cur[7] = z; return this; }
        *///?}
    }
}
