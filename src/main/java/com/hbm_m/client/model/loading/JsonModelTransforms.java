//? if fabric {
/*package com.hbm_m.client.model.loading;

import java.util.Locale;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hbm_m.main.MainRegistry;
import com.mojang.math.Transformation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.model.ItemTransforms;

@Environment(EnvType.CLIENT)
final class JsonModelTransforms {

    private JsonModelTransforms() {}

    /^*
     * Forge root transforms:
     * - support either raw {@code matrix} or element-wise specification
     * - element-wise order: translation -> left_rotation -> scale -> right_rotation
     * - rotation/scale happen around the specified origin (pivot)
     *
     * See: Forge docs "Root Transforms" (1.20.1).
     ^/
    static Matrix4f parseRootTransformMatrix(JsonObject transformJson) {
        if (transformJson == null) return new Matrix4f().identity();

        // Option 1: raw matrix (3x4, row-major, last row omitted)
        if (transformJson.has("matrix")) {
            JsonElement e = transformJson.get("matrix");
            if (e != null && e.isJsonArray()) {
                var rows = e.getAsJsonArray();
                if (rows.size() == 3) {
                    float[][] m = new float[3][4];
                    for (int r = 0; r < 3; r++) {
                        var row = rows.get(r).getAsJsonArray();
                        for (int c = 0; c < 4; c++) {
                            m[r][c] = row.size() > c ? row.get(c).getAsFloat() : 0.0f;
                        }
                    }
                    // Row-major 3x4 -> full 4x4 (last row 0 0 0 1)
                    return new Matrix4f(
                            m[0][0], m[1][0], m[2][0], 0.0f,
                            m[0][1], m[1][1], m[2][1], 0.0f,
                            m[0][2], m[1][2], m[2][2], 0.0f,
                            m[0][3], m[1][3], m[2][3], 1.0f
                    );
                }
            }
        }

        Vector3f origin = parseOrigin(transformJson);
        Vector3f translation = parseVec3f(transformJson.get("translation"), new Vector3f(0, 0, 0));

        Quaternionf leftRot = parseRotation(transformJson.has("left_rotation")
                ? transformJson.get("left_rotation")
                : transformJson.get("rotation"));
        Vector3f scale = parseScale(transformJson.get("scale"));
        Quaternionf rightRot = parseRotation(transformJson.has("right_rotation")
                ? transformJson.get("right_rotation")
                : transformJson.get("post_rotation"));

        // Build TRSR in order: T * Rl * S * Rr.
        // Important: do NOT rely on chained Matrix4f.translate/rotate/scale semantics here,
        // because it is easy to end up with the reverse application order. Build explicit matrices.
        Matrix4f trs = new Matrix4f()
                .translation(translation)
                .mul(new Matrix4f().rotation(leftRot))
                .mul(new Matrix4f().scaling(scale))
                .mul(new Matrix4f().rotation(rightRot));

        // Apply pivot: T(origin) * trs * T(-origin)
        if (origin.lengthSquared() != 0.0f) {
            return new Matrix4f()
                    .translation(origin)
                    .mul(trs)
                    .translate(-origin.x, -origin.y, -origin.z);
        }
        return trs;
    }

    private static Vector3f parseOrigin(JsonObject obj) {
        // Forge root transforms default to "opposing-corner" (1,1,1) when origin is not specified.
        // See: https://docs.minecraftforge.net/en/1.20.1/rendering/modelextensions/transforms/
        if (!obj.has("origin")) return new Vector3f(1.0f, 1.0f, 1.0f);
        JsonElement e = obj.get("origin");
        if (e == null || e.isJsonNull()) return new Vector3f(1.0f, 1.0f, 1.0f);
        if (e.isJsonPrimitive()) {
            String s = e.getAsString().toLowerCase(Locale.ROOT);
            return switch (s) {
                case "center", "block_center" -> new Vector3f(0.5f, 0.5f, 0.5f);
                case "corner", "block_corner" -> new Vector3f(0.0f, 0.0f, 0.0f);
                case "opposing-corner", "opposing_corner" -> new Vector3f(1.0f, 1.0f, 1.0f);
                default -> new Vector3f(1.0f, 1.0f, 1.0f);
            };
        }
        if (e.isJsonArray()) {
            return parseVec3f(e, new Vector3f(0, 0, 0));
        }
        return new Vector3f(1.0f, 1.0f, 1.0f);
    }

    private static Vector3f parseScale(JsonElement e) {
        if (e == null || e.isJsonNull()) return new Vector3f(1, 1, 1);
        if (e.isJsonArray()) return parseVec3f(e, new Vector3f(1, 1, 1));
        if (e.isJsonPrimitive()) {
            float s = e.getAsFloat();
            return new Vector3f(s, s, s);
        }
        return new Vector3f(1, 1, 1);
    }

    private static Vector3f parseVec3f(JsonElement e, Vector3f fallback) {
        if (e == null || e.isJsonNull() || !e.isJsonArray()) return new Vector3f(fallback);
        var a = e.getAsJsonArray();
        float x = a.size() > 0 ? a.get(0).getAsFloat() : fallback.x;
        float y = a.size() > 1 ? a.get(1).getAsFloat() : fallback.y;
        float z = a.size() > 2 ? a.get(2).getAsFloat() : fallback.z;
        return new Vector3f(x, y, z);
    }

    /^*
     * Forge root transform rotation supports:
     * - object axis->degrees: {"x":90}
     * - array of such objects: [{"x":90},{"y":45}]
     * - array[3] degrees: [x,y,z]
     * - array[4] quaternion: [x,y,z,w]
     ^/
    private static Quaternionf parseRotation(JsonElement e) {
        if (e == null || e.isJsonNull()) return new Quaternionf();
        if (e.isJsonArray()) {
            var a = e.getAsJsonArray();
            if (a.size() == 4 && a.get(0).isJsonPrimitive()) {
                return new Quaternionf(
                        a.get(0).getAsFloat(),
                        a.get(1).getAsFloat(),
                        a.get(2).getAsFloat(),
                        a.get(3).getAsFloat()
                );
            }
            if (a.size() == 3 && a.get(0).isJsonPrimitive()) {
                float rx = (float) Math.toRadians(a.get(0).getAsFloat());
                float ry = (float) Math.toRadians(a.get(1).getAsFloat());
                float rz = (float) Math.toRadians(a.get(2).getAsFloat());
                return new Quaternionf().rotateXYZ(rx, ry, rz);
            }
            // array of axis objects
            Quaternionf q = new Quaternionf();
            for (JsonElement el : a) {
                q.mul(parseRotation(el));
            }
            return q;
        }
        if (e.isJsonObject()) {
            JsonObject o = e.getAsJsonObject();
            Quaternionf q = new Quaternionf();
            if (o.has("x")) q.mul(new Quaternionf().rotateX((float) Math.toRadians(o.get("x").getAsFloat())));
            if (o.has("y")) q.mul(new Quaternionf().rotateY((float) Math.toRadians(o.get("y").getAsFloat())));
            if (o.has("z")) q.mul(new Quaternionf().rotateZ((float) Math.toRadians(o.get("z").getAsFloat())));
            return q;
        }
        return new Quaternionf();
    }

    static Transformation parseRootTransform(JsonObject transformJson) {
        if (transformJson == null) return Transformation.identity();
        try {
            return new Transformation(parseRootTransformMatrix(transformJson));
        } catch (Throwable t) {
            MainRegistry.LOGGER.debug("Failed to parse root transform; falling back to identity", t);
            return Transformation.identity();
        }
    }

    static ItemTransforms parseItemTransforms(JsonElement displayJson, Gson baseGson) {
        if (displayJson == null || displayJson.isJsonNull()) return ItemTransforms.NO_TRANSFORMS;
        try {
            if (!displayJson.isJsonObject()) return ItemTransforms.NO_TRANSFORMS;
            JsonObject display = displayJson.getAsJsonObject();

            var thirdRight = parseItemTransform(display, "thirdperson_righthand");
            var thirdLeft = parseItemTransform(display, "thirdperson_lefthand");
            var firstRight = parseItemTransform(display, "firstperson_righthand");
            var firstLeft = parseItemTransform(display, "firstperson_lefthand");
            var head = parseItemTransform(display, "head");
            var gui = parseItemTransform(display, "gui");
            var ground = parseItemTransform(display, "ground");
            var fixed = parseItemTransform(display, "fixed");

            // ItemTransforms has a constructor but it's not exposed in mappings docs reliably; use reflection.
            try {
                var ctor = ItemTransforms.class.getConstructor(
                        net.minecraft.client.renderer.block.model.ItemTransform.class,
                        net.minecraft.client.renderer.block.model.ItemTransform.class,
                        net.minecraft.client.renderer.block.model.ItemTransform.class,
                        net.minecraft.client.renderer.block.model.ItemTransform.class,
                        net.minecraft.client.renderer.block.model.ItemTransform.class,
                        net.minecraft.client.renderer.block.model.ItemTransform.class,
                        net.minecraft.client.renderer.block.model.ItemTransform.class,
                        net.minecraft.client.renderer.block.model.ItemTransform.class
                );
                return ctor.newInstance(thirdLeft, thirdRight, firstLeft, firstRight, head, gui, ground, fixed);
            } catch (Throwable reflectiveFail) {
                MainRegistry.LOGGER.debug("Failed to instantiate ItemTransforms reflectively; falling back to NO_TRANSFORMS", reflectiveFail);
                return ItemTransforms.NO_TRANSFORMS;
            }
        } catch (Throwable t) {
            MainRegistry.LOGGER.debug("Failed to parse item transforms; falling back to NO_TRANSFORMS", t);
            return ItemTransforms.NO_TRANSFORMS;
        }
    }

    private static net.minecraft.client.renderer.block.model.ItemTransform parseItemTransform(JsonObject display, String key) {
        if (!display.has(key) || !display.get(key).isJsonObject()) {
            return net.minecraft.client.renderer.block.model.ItemTransform.NO_TRANSFORM;
        }
        JsonObject obj = display.getAsJsonObject(key);

        org.joml.Vector3f rot = parseVec3(obj, "rotation", new org.joml.Vector3f(0, 0, 0));
        // JSON translation is in pixels (1 unit = 1/16 block). Vanilla divides by 16 when deserializing.
        org.joml.Vector3f trans = parseVec3(obj, "translation", new org.joml.Vector3f(0, 0, 0)).mul(0.0625f);
        org.joml.Vector3f scale = parseVec3(obj, "scale", new org.joml.Vector3f(1, 1, 1));
        return new net.minecraft.client.renderer.block.model.ItemTransform(rot, trans, scale);
    }

    private static org.joml.Vector3f parseVec3(JsonObject obj, String key, org.joml.Vector3f fallback) {
        if (!obj.has(key) || !obj.get(key).isJsonArray()) return new org.joml.Vector3f(fallback);
        var a = obj.getAsJsonArray(key);
        float x = a.size() > 0 ? a.get(0).getAsFloat() : fallback.x;
        float y = a.size() > 1 ? a.get(1).getAsFloat() : fallback.y;
        float z = a.size() > 2 ? a.get(2).getAsFloat() : fallback.z;
        return new org.joml.Vector3f(x, y, z);
    }
}
*///?}

