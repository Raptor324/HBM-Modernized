package com.hbm_m.client.render.shader.modification;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hbm_m.main.MainRegistry;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Lightweight GLSL source modifier inspired by Veil's ShaderModification system,
 * but reimplemented from scratch (no glslprocessor dependency).
 * <p>
 * Supports:
 * <ul>
 *     <li>Injection of {@code #define KEY [VALUE]} directives right after the {@code #version} line.</li>
 *     <li>Insertion of arbitrary GLSL text before/after a regex marker.</li>
 *     <li>Replacement of a regex match.</li>
 * </ul>
 * <p>
 * This class is intentionally small and synchronous; it is meant to be used at shader
 * registration / load time, not during rendering. It also serves as a foundation for
 * future Iris {@code TransformPatcher} integration (Variant B).
 */
@OnlyIn(Dist.CLIENT)
public final class ShaderModification {

    /**
     * Regex matching a line starting with {@code #version}. Includes the line terminator.
     */
    private static final Pattern VERSION_LINE = Pattern.compile("(?m)^\\s*#version[^\\n]*\\n");

    /**
     * Single, ordered modification step.
     */
    private interface Step {
        String apply(String source);
    }

    private final List<Step> steps = new ArrayList<>();

    private ShaderModification() {}

    public static ShaderModification builder() {
        return new ShaderModification();
    }

    /**
     * Adds a {@code #define KEY VALUE} directive to be injected after the {@code #version} line.
     * If {@code value} is {@code null} or empty, only {@code #define KEY} is emitted.
     */
    public ShaderModification define(String key, String value) {
        if (key == null || key.isEmpty()) return this;
        String line = (value == null || value.isEmpty())
                ? "#define " + key + "\n"
                : "#define " + key + " " + value + "\n";
        steps.add(source -> insertAfterVersion(source, line));
        return this;
    }

    /**
     * Convenience: emit {@code #define KEY}.
     */
    public ShaderModification define(String key) {
        return define(key, null);
    }

    /**
     * Inserts arbitrary text after the first match of {@code markerRegex}. If the marker is not
     * found the source is returned unchanged (a debug log is emitted).
     */
    public ShaderModification insertAfter(String markerRegex, String inject) {
        Pattern p = Pattern.compile(markerRegex);
        steps.add(source -> {
            Matcher m = p.matcher(source);
            if (!m.find()) {
                MainRegistry.LOGGER.debug("ShaderModification.insertAfter: marker '{}' not found", markerRegex);
                return source;
            }
            int idx = m.end();
            return source.substring(0, idx) + inject + source.substring(idx);
        });
        return this;
    }

    /**
     * Inserts arbitrary text before the first match of {@code markerRegex}.
     */
    public ShaderModification insertBefore(String markerRegex, String inject) {
        Pattern p = Pattern.compile(markerRegex);
        steps.add(source -> {
            Matcher m = p.matcher(source);
            if (!m.find()) {
                MainRegistry.LOGGER.debug("ShaderModification.insertBefore: marker '{}' not found", markerRegex);
                return source;
            }
            int idx = m.start();
            return source.substring(0, idx) + inject + source.substring(idx);
        });
        return this;
    }

    /**
     * Replaces every match of {@code regex} with {@code replacement}.
     */
    public ShaderModification replace(String regex, String replacement) {
        Pattern p = Pattern.compile(regex);
        steps.add(source -> p.matcher(source).replaceAll(Matcher.quoteReplacement(replacement)));
        return this;
    }

    /**
     * Applies all steps to {@code source} and returns the modified text.
     */
    public String apply(String source) {
        if (source == null || source.isEmpty() || steps.isEmpty()) return source;
        String current = source;
        for (Step step : steps) {
            current = step.apply(current);
        }
        return current;
    }

    /**
     * If a {@code #version} line is present, inserts {@code text} immediately after it.
     * Otherwise prepends {@code text} to the source.
     */
    private static String insertAfterVersion(String source, String text) {
        Matcher m = VERSION_LINE.matcher(source);
        if (m.find()) {
            int idx = m.end();
            return source.substring(0, idx) + text + source.substring(idx);
        }
        return text + source;
    }
}
