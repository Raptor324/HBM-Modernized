package com.hbm_m.platform.legacy.client.model;

import java.util.Map;

import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

import com.hbm_m.client.model.nmesh.NMeshData;
import com.hbm_m.client.model.nmesh.NMeshLoader;
import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.platform.Transparency;
import com.mojang.math.Transformation;

import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextMap;
import net.neoforged.neoforge.client.model.ExtendedUnbakedGeometry;
import net.neoforged.neoforge.client.model.NeoForgeModelProperties;
import net.neoforged.neoforge.client.model.UnbakedElementsHelper;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;

/**
 * [26.x] NMesh-Geometrie für das neue Modellsystem (UnbakedGeometry → QuadCollection).
 *
 * <p>Ersetzt auf 26.x die alten {@code IGeometryLoader}/{@code BakedModel}-Loader. Gebacken wird
 * immer die ganze {@link NMeshData} (optional gefiltert über {@code parts} bzw. NeoForge
 * {@code visibility}); bewegliche Teile zeichnet Nucleus weiterhin per BER/VBO, die zugehörigen
 * Blöcke sind auf 26.x {@code RenderShape.INVISIBLE}, d. h. das Chunk-Mesh enthält sie nicht.
 * Die Quad-Semantik folgt {@code NMeshPartBakedModel#buildQuadsIfNeeded} (Forge ObjModel):
 * Kd als Vertexfarbe, Ka als Fake-Lightmap bei {@code emissive_ambient}, Richtung aus der Normalen
 * von Vertex 0, Auto-Culling nur für Flächen exakt auf der Blockgrenze.
 */
public final class NMeshGeometry implements ExtendedUnbakedGeometry {

    public record Settings(Identifier model, boolean flipV, boolean shadeQuads, boolean automaticCulling,
                           boolean emissiveAmbient, int tintIndex, @Nullable Identifier textureOverride,
                           java.util.@Nullable List<String> parts) {}

    private final Settings settings;

    public NMeshGeometry(Settings settings) {
        this.settings = settings;
    }

    @Override
    public QuadCollection bake(TextureSlots slots, ModelBaker baker, ModelState state, ModelDebugName debugName,
                               ContextMap props) {
        NMeshData mesh = NMeshLoader.load(settings.model());
        if (mesh == null) {
            MainRegistry.LOGGER.error("[26.x] NMesh-Modell {} nicht gefunden ({})", settings.model(), debugName.debugName());
            return QuadCollection.EMPTY;
        }
        Transformation root = props.getOrDefault(NeoForgeModelProperties.TRANSFORM, Transformation.IDENTITY);
        ModelState composed = UnbakedElementsHelper.composeRootTransformIntoModelState(state, root);
        Transformation t = composed.transformation();
        boolean hasTransform = !t.isIdentity();
        Transformation tc = hasTransform ? t.blockCenterToCorner() : t;
        Map<String, Boolean> visibility = props.getOrDefault(NeoForgeModelProperties.PART_VISIBILITY, Map.of());

        QuadCollection.Builder out = new QuadCollection.Builder();
        Vector4f pos = new Vector4f();
        Vector3f nrm = new Vector3f();
        float[] tx = new float[4], ty = new float[4], tz = new float[4];
        float[] nx = new float[4], ny = new float[4], nz = new float[4];

        for (NMeshData.SubMesh sm : mesh.subMeshes()) {
            String rootName = !sm.objectName.isEmpty() ? sm.objectName : (!sm.groupName.isEmpty() ? sm.groupName : "default");
            if (!visibility.getOrDefault(rootName, true)) continue;
            if (settings.parts() != null && !settings.parts().isEmpty()
                    && settings.parts().stream().noneMatch(sm::matchesPart)) continue;

            Material.Baked material = resolveMaterial(slots, baker, debugName, sm);
            Transparency transparency = material.forceTranslucent() ? Transparency.TRANSLUCENT : material.sprite().transparency();

            int fakeLight = 0;
            Direction shadeOverride = settings.shadeQuads() ? null : Direction.UP;
            if (settings.emissiveAmbient()) {
                fakeLight = (int) ((sm.kaR + sm.kaG + sm.kaB) * 15 / 3.0f);
                shadeOverride = fakeLight == 0 && settings.shadeQuads() ? null : Direction.UP;
            }

            float[] v = sm.vertices;
            int[] idx = sm.indices;
            for (int i = 0; i + 2 < idx.length; i += 3) {
                int[] tri = { idx[i], idx[i + 1], idx[i + 2], idx[i + 2] };
                QuadBakingVertexConsumer qb = new QuadBakingVertexConsumer();
                qb.setSprite(material, transparency);
                qb.setTintIndex(settings.tintIndex());
                qb.setLightEmission(fakeLight);
                qb.setShadeOverride(shadeOverride);
                for (int c = 0; c < 4; c++) {
                    int b = tri[c] * 8;
                    pos.set(v[b], v[b + 1], v[b + 2], 1f);
                    nrm.set(v[b + 3], v[b + 4], v[b + 5]);
                    if (hasTransform) {
                        tc.transformPosition(pos);
                        tc.transformNormal(nrm);
                    }
                    tx[c] = pos.x; ty[c] = pos.y; tz[c] = pos.z;
                    nx[c] = nrm.x; ny[c] = nrm.y; nz[c] = nrm.z;
                    if (c == 0) qb.setDirection(Direction.getApproximateNearest(nrm.x, nrm.y, nrm.z));
                    float u = v[b + 6];
                    float vv = settings.flipV() ? 1f - v[b + 7] : v[b + 7];
                    qb.addVertex(pos.x, pos.y, pos.z);
                    qb.setColor(sm.kdR * 255f > 255f ? 255 : (int) (sm.kdR * 255f + .5f),
                            sm.kdG * 255f > 255f ? 255 : (int) (sm.kdG * 255f + .5f),
                            sm.kdB * 255f > 255f ? 255 : (int) (sm.kdB * 255f + .5f), 255);
                    qb.setUv(material.sprite().getU(u), material.sprite().getV(vv));
                    qb.setNormal(nrm.x, nrm.y, nrm.z);
                }
                var quad = qb.bakeQuad(baker.interner());
                Direction cull = settings.automaticCulling() ? cullFace(tx, ty, tz, nx, ny, nz) : null;
                if (cull == null) out.addUnculledFace(quad);
                else out.addCulledFace(cull, quad);
            }
        }
        return out.build();
    }

    /** Gleiche Auflösung wie der alte Loader: Override, {@code #ref}, direkter Pfad, Materialname, Standard-Slots. */
    private Material.Baked resolveMaterial(TextureSlots slots, ModelBaker baker, ModelDebugName name, NMeshData.SubMesh sm) {
        var materials = baker.materials();
        if (settings.textureOverride() != null) {
            return materials.get(new Material(settings.textureOverride()), name);
        }
        String mapKd = sm.mapKdTexture;
        if (!mapKd.isEmpty() && mapKd.charAt(0) == '#') {
            String ref = mapKd.substring(1);
            if (slots.getMaterial(ref) != null) return materials.resolveSlot(slots, ref, name);
        } else if (!mapKd.isEmpty()) {
            Identifier tex = Identifier.tryParse(mapKd);
            if (tex != null) return materials.get(new Material(tex), name);
        }
        for (String key : new String[] { sm.materialName, sm.objectName, sm.groupName, "default", "surface", "texture" }) {
            if (!key.isEmpty() && slots.getMaterial(key) != null) return materials.resolveSlot(slots, key, name);
        }
        return materials.resolveSlot(slots, "particle", name);
    }

    private static @Nullable Direction cullFace(float[] x, float[] y, float[] z, float[] nx, float[] ny, float[] nz) {
        if (eq4(x, 0f) && nx[0] < 0) return Direction.WEST;
        if (eq4(x, 1f) && nx[0] > 0) return Direction.EAST;
        if (eq4(z, 0f) && nz[0] < 0) return Direction.NORTH;
        if (eq4(z, 1f) && nz[0] > 0) return Direction.SOUTH;
        if (eq4(y, 0f) && ny[0] < 0) return Direction.DOWN;
        if (eq4(y, 1f) && ny[0] > 0) return Direction.UP;
        return null;
    }

    private static boolean eq4(float[] v, float p) {
        return Math.abs(v[0] - p) < 1e-5f && Math.abs(v[1] - p) < 1e-5f && Math.abs(v[2] - p) < 1e-5f && Math.abs(v[3] - p) < 1e-5f;
    }
}
