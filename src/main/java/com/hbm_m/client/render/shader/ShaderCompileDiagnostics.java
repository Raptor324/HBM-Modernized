package com.hbm_m.client.render.shader;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

/**
 * Shadow re-compilation of a shader JSON's stages after a failed
 * {@code ExtendedShader} construction - recovers the REAL driver diagnostic.
 *
 * <p>Why this exists: Oculus 1.8.x's {@code MixinProgram.iris$causeException}
 * handler fires inside {@code Program.compileShaderInternal} whenever
 * {@code COMPILE_STATUS == 0} and calls {@code CallbackInfo.cancel()}
 * unconditionally - but the injection is registered WITHOUT
 * {@code cancellable = true} (upstream Iris 1.7.2+ marks it cancellable). The
 * thrown {@code CancellationException} ("The call m_166612_ is not
 * cancellable") replaces vanilla's ChainedJsonException and DESTROYS the
 * driver's {@code glGetShaderInfoLog} text, so the actual GLSL error is
 * unrecoverable from the log. Observed in the wild: a deferred main-pass
 * fragment shader failed to compile on NVIDIA Maxwell and Turing (GTX 750 /
 * GTX 1650) while the shadow-pass variant sharing the same vertex stage
 * compiled, and Blackwell accepted it - the driver's exact complaint is what
 * this class prints.</p>
 *
 * <p>How: re-read the same JSON and stage sources the vanilla
 * {@code ShaderInstance} constructor just used and compile them with plain GL
 * (create - source - compile - read COMPILE_STATUS/info log - delete). Our
 * core/iris shaders contain no {@code #moj_import}, so a raw compile is
 * equivalent to vanilla's preprocessed one. The temp shader objects have no
 * bind side effects; everything is wrapped so this can never mask the
 * original failure.</p>
 */
public final class ShaderCompileDiagnostics {

    private static boolean oculusCancellationExplained;

    private ShaderCompileDiagnostics() {}

    /**
     * Called from the {@code IrisInstancedShaders.getOrCreate} catch block
     * (once per pipeline generation per slot). Never throws.
     *
     * @param shaderName the shader resource name passed to the ExtendedShader
     *                   constructor, e.g. {@code hbm_m:iris/block_lit_instanced_packed}
     * @param shadowPass which pass slot failed (for the log line only)
     * @param failure    the original construction failure
     */
    public static void reportFailedConstruction(String shaderName, boolean shadowPass, Throwable failure) {
        try {
            explainOculusCancellation(failure);
            ResourceLocation jsonId = toCoreResource(shaderName, ".json");
            if (jsonId == null) {
                return;
            }
            com.hbm_m.main.MainRegistry.LOGGER.info(
                    "[HBM-M] ShaderCompileDiagnostics: shadow-compiling {} ({}) to recover the driver's compile log",
                    jsonId, shadowPass ? "shadow pass" : "main pass");
            String json = readResource(jsonId);
            if (json == null) {
                com.hbm_m.main.MainRegistry.LOGGER.warn(
                        "[HBM-M] ShaderCompileDiagnostics: cannot read {} - skipping", jsonId);
                return;
            }
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            StringBuilder report = new StringBuilder();
            compileStage("VERTEX  ", root.get("vertex"), GL20.GL_VERTEX_SHADER, ".vsh", report);
            compileStage("FRAGMENT", root.get("fragment"), GL20.GL_FRAGMENT_SHADER, ".fsh", report);
            if (report.indexOf("COMPILE FAILED") >= 0) {
                com.hbm_m.main.MainRegistry.LOGGER.error(
                        "[HBM-M] ShaderCompileDiagnostics: GLSL compile failure in {}:\n{}",
                        jsonId, report);
            } else {
                com.hbm_m.main.MainRegistry.LOGGER.warn(
                        "[HBM-M] ShaderCompileDiagnostics: both stages of {} compiled OK in the shadow pass -"
                                + " the construction failure was NOT a GLSL compile error (suspect link,"
                                + " framebuffer args or uniform setup):\n{}", jsonId, report);
            }
        } catch (Throwable t) {
            com.hbm_m.main.MainRegistry.LOGGER.warn(
                    "[HBM-M] ShaderCompileDiagnostics: diagnostics failed: {}", t.toString());
        }
    }

    private static void compileStage(String label, com.google.gson.JsonElement value, int type,
                                     String extension, StringBuilder report) throws Exception {
        if (value == null || !value.isJsonPrimitive()) {
            report.append(label).append(": field missing in shader JSON - skipped\n");
            return;
        }
        ResourceLocation sourceId = toCoreResource(value.getAsString(), extension);
        if (sourceId == null) {
            report.append(label).append(": unparsable resource name - skipped\n");
            return;
        }
        String source = readResource(sourceId);
        if (source == null) {
            report.append(label).append(' ').append(sourceId).append(": source not found\n");
            return;
        }
        int shader = 0;
        try {
            shader = GL20.glCreateShader(type);
            GL20.glShaderSource(shader, source);
            GL20.glCompileShader(shader);
            if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) != 0) {
                report.append(label).append(' ').append(sourceId).append(": compiled OK\n");
            } else {
                String log = GL20.glGetShaderInfoLog(shader, 32768);
                report.append(label).append(' ').append(sourceId)
                        .append(": COMPILE FAILED, driver info log:\n")
                        .append(log.isBlank() ? "(empty info log)" : log.strip()).append('\n');
            }
        } finally {
            if (shader != 0) {
                GL20.glDeleteShader(shader);
            }
        }
    }

    /** {@code hbm_m:iris/x} + ".json" -> {@code hbm_m:shaders/core/iris/x.json} (vanilla layout). */
    private static ResourceLocation toCoreResource(String name, String extension) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        String namespace = "hbm_m";
        String path = name;
        int colon = name.indexOf(':');
        if (colon >= 0) {
            namespace = name.substring(0, colon);
            path = name.substring(colon + 1);
        }
        return ResourceLocation.fromNamespaceAndPath(namespace, "shaders/core/" + path + extension);
    }

    private static String readResource(ResourceLocation id) {
        try {
            ResourceManager rm = Minecraft.getInstance().getResourceManager();
            net.minecraft.server.packs.resources.Resource resource = rm.getResource(id).orElse(null);
            if (resource == null) {
                return null;
            }
            try (InputStream in = resource.open()) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * One-time explanation when the cause chain carries the Oculus
     * non-cancellable-injection bug, so the log line next to the shadow compile
     * is self-contained for bug reports.
     */
    private static void explainOculusCancellation(Throwable failure) {
        if (oculusCancellationExplained) {
            return;
        }
        Throwable t = failure;
        for (int depth = 0; t != null && depth < 8; depth++, t = t.getCause()) {
            if (t instanceof java.util.concurrent.CancellationException
                    && t.getMessage() != null && t.getMessage().contains("not cancellable")) {
                oculusCancellationExplained = true;
                com.hbm_m.main.MainRegistry.LOGGER.info(
                        "[HBM-M] ShaderCompileDiagnostics: the cause chain carries the Oculus 1.8.x MixinProgram"
                                + " bug - it calls CallbackInfo.cancel() unconditionally inside a NON-cancellable"
                                + " injection (upstream Iris marks it cancellable), which destroys the driver's"
                                + " glGetShaderInfoLog and replaces it with the generic"
                                + " \"The call m_XXXXX_ is not cancellable\". The shadow compile above recovers"
                                + " the real diagnostic.");
                return;
            }
        }
    }
}
