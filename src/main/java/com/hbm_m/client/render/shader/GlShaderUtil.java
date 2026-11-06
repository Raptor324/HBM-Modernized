package com.hbm_m.client.render.shader;

import com.hbm_m.client.render.debug.GlDebugMarkers;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.platform.RenderHooks;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL43;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Unified OpenGL shader compilation and linking utility.
 * Guarantees zero GL resource leaks on compilation and link failures.
 */
public final class GlShaderUtil {

    private GlShaderUtil() {}

    /**
     * Compiles a single shader stage. Returns shader ID > 0 on success;
     * on failure deletes the shader object, logs a warning, and returns 0.
     */
    public static int compileShader(int type, String source, String debugName) {
        int shader = GL20.glCreateShader(type);
        RenderHooks.safeShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetShaderInfoLog(shader, 4096);
            GL20.glDeleteShader(shader);
            String formattedSource = formatSourceWithLineNumbers(source);
            MainRegistry.LOGGER.warn("[HBM-M] GlShaderUtil: {} compile failed:\n{}\nSource:\n{}", debugName, log, formattedSource);
            return 0;
        }
        return shader;
    }

    /**
     * Formats shader source code with 1-based line numbers for diagnostic logging.
     *
     * @param source the GLSL shader source code
     * @return line-numbered source string ("1: ...\n2: ...\n")
     */
    public static String formatSourceWithLineNumbers(String source) {
        if (source == null) {
            return "<null source>";
        }
        String[] lines = source.split("\\r?\\n");
        StringBuilder sb = new StringBuilder(source.length() + lines.length * 8);
        for (int i = 0; i < lines.length; i++) {
            sb.append(i + 1).append(": ").append(lines[i]).append('\n');
        }
        return sb.toString();
    }

    /**
     * Compiles and links a compute shader program from source string.
     * Deletes shader object after attach/link. Returns program ID > 0 on success,
     * or 0 on failure (with complete resource cleanup).
     */
    public static int createComputeProgram(String source, String debugName) {
        int cs = compileShader(GL43.GL_COMPUTE_SHADER, source, debugName);
        if (cs == 0) {
            return 0;
        }

        int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, cs);
        GL20.glLinkProgram(program);
        GL20.glDeleteShader(cs);

        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetProgramInfoLog(program, 4096);
            GL20.glDeleteProgram(program);
            MainRegistry.LOGGER.warn("[HBM-M] GlShaderUtil: {} link failed: {}", debugName, log);
            return 0;
        }
        if (GlDebugMarkers.ENABLED) {
            GlDebugMarkers.labelProgram(program, debugName);
        }
        return program;
    }

    /**
     * Compiles and links a raster program (vertex + fragment).
     * Binds attribute locations if provided.
     * Guaranteed zero GL leaks on compile or link failure.
     */
    public static int createRasterProgram(String vshSource, String fshSource,
                                          Map<Integer, String> attribLocations,
                                          String debugName) {
        int vs = compileShader(GL20.GL_VERTEX_SHADER, vshSource, debugName + "_VS");
        int fs = compileShader(GL20.GL_FRAGMENT_SHADER, fshSource, debugName + "_FS");
        if (vs == 0 || fs == 0) {
            if (vs != 0) GL20.glDeleteShader(vs);
            if (fs != 0) GL20.glDeleteShader(fs);
            return 0;
        }

        int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vs);
        GL20.glAttachShader(program, fs);

        if (attribLocations != null) {
            for (Map.Entry<Integer, String> entry : attribLocations.entrySet()) {
                GL20.glBindAttribLocation(program, entry.getKey(), entry.getValue());
            }
        }

        GL20.glLinkProgram(program);
        GL20.glDeleteShader(vs);
        GL20.glDeleteShader(fs);

        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetProgramInfoLog(program, 4096);
            GL20.glDeleteProgram(program);
            MainRegistry.LOGGER.warn("[HBM-M] GlShaderUtil: {} link failed: {}", debugName, log);
            return 0;
        }
        if (GlDebugMarkers.ENABLED) {
            GlDebugMarkers.labelProgram(program, debugName);
        }
        return program;
    }

    /**
     * Loads raw text from a classpath resource path using the GlShaderUtil classloader.
     */
    public static String loadResource(String path) {
        return loadResource(GlShaderUtil.class, path);
    }

    /**
     * Loads raw text from a classpath resource path using the given anchor class.
     */
    public static String loadResource(Class<?> anchor, String path) {
        try (InputStream in = anchor.getResourceAsStream(path)) {
            if (in == null) throw new IllegalArgumentException("Resource not found: " + path);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
                return sb.toString();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to read shader resource: " + path, e);
        }
    }
}
