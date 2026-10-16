package com.hbm_m.client.render.shader;

import com.hbm_m.main.MainRegistry;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * Registers mod blocks into the LOADED shader pack's block id map so packs treat
 * their terrain geometry as a chosen vanilla block. Shaderpacks key per-block
 * effects (soul-flame emissive + colored voxel light in Photon/Complementary/BSL)
 * on the pack's own {@code block.properties} id, which Iris writes into the
 * {@code mc_Entity} vertex attribute; a mod block can never join that mapping by
 * data (Iris loads block.properties from the pack only), so we add our entry to
 * the already-parsed map in memory - no shaderpack files are touched.
 *
 * <p>Target structure (Oculus 1.8.0 / Iris 1.7.x):
 * {@code IdMap.blockPropertiesMap: Int2ObjectMap<List<BlockEntry>>} - id -> block
 * entries. We find the id whose entry list contains the stand-in vanilla block
 * and append a {@code BlockEntry} for ours to the same list. Idempotent per pack
 * instance; re-applies automatically when the pack is reloaded (new IdMap).
 *
 * <p>Driven from {@link ShaderCompatibilityDetector} - {@code applyIfPackChanged}
 * is polled once per frame with cached reflection handles, so the cost is one
 * Optional identity compare.
 */
public final class ShaderBlockIdDisguises {

    /** block registry name ("ns:path") -> vanilla block whose pack id it inherits. */
    private static final String[][] DISGUISES = {
            {"hbm_m:lamp_demon", "minecraft:soul_torch"},
    };

    private static final String[] IRIS_CLASSES = {
            "net.irisshaders.iris.shaderpack.Iris",     // Iris / modern Oculus
            "net.coderbot.iris.shaderpack.Iris",        // legacy Iris package
    };

    private static boolean initialized;
    private static Method getInstanceMethod;
    private static Method getCurrentPackMethod;
    private static Method getIdMapMethod;
    private static boolean failed;

    /** Identity of the ShaderPack instance the disguises were last applied to. */
    private static Object lastPack;

    private ShaderBlockIdDisguises() {}

    /** Polled once per frame from ShaderCompatibilityDetector. Cheap no-op until an unapplied pack shows up. */
    public static void applyIfPackChanged() {
        if (failed) {
            return;
        }
        if (!initialized) {
            initialized = true;
            try {
                Class<?> irisClass = null;
                for (String name : IRIS_CLASSES) {
                    try {
                        irisClass = Class.forName(name);
                        break;
                    } catch (ClassNotFoundException ignored) {
                    }
                }
                if (irisClass == null) {
                    failed = true;
                    MainRegistry.LOGGER.info("ShaderBlockIdDisguises: Iris/Oculus not present");
                    return;
                }
                getInstanceMethod = irisClass.getMethod("getInstance");
                getCurrentPackMethod = irisClass.getMethod("getCurrentPack");
                Object instance = getInstanceMethod.invoke(null);
                Object packInstance = getCurrentPackMethod.invoke(instance);
                getIdMapMethod = packInstance.getClass().getMethod("getIdMap");
            } catch (Throwable t) {
                failed = true;
                MainRegistry.LOGGER.info("ShaderBlockIdDisguises: unavailable ({}: {})",
                        t.getClass().getSimpleName(), t.getMessage());
                return;
            }
        }
        try {
            Object instance = getInstanceMethod.invoke(null);
            Object packOptional = getCurrentPackMethod.invoke(instance);
            if (!(packOptional instanceof java.util.Optional)) {
                return;
            }
            Object pack = ((java.util.Optional<?>) packOptional).orElse(null);
            if (pack == null || pack == lastPack) {
                return;
            }
            Object idMap = getIdMapMethod.invoke(pack);
            if (apply(idMap)) {
                lastPack = pack;
            } else {
                failed = true; // wrong structure - do not retry every frame
            }
        } catch (Throwable t) {
            failed = true;
            MainRegistry.LOGGER.info("ShaderBlockIdDisguises: apply failed ({}: {})",
                    t.getClass().getSimpleName(), t.getMessage());
        }
    }

    /**
     * Applies the disguise table to one IdMap. Returns false when the structure
     * does not match the expected {@code Int2ObjectMap<List<BlockEntry>>}.
     */
    private static boolean apply(Object idMap) {
        Field field;
        try {
            field = idMap.getClass().getDeclaredField("blockPropertiesMap");
            field.setAccessible(true);
        } catch (Throwable t) {
            MainRegistry.LOGGER.info("ShaderBlockIdDisguises: blockPropertiesMap not found ({}: {})",
                    t.getClass().getSimpleName(), t.getMessage());
            return false;
        }
        Object raw;
        try {
            raw = field.get(idMap);
        } catch (Throwable t) {
            MainRegistry.LOGGER.info("ShaderBlockIdDisguises: blockPropertiesMap unreadable ({})",
                    t.getClass().getSimpleName());
            return false;
        }
        if (!(raw instanceof Map)) {
            MainRegistry.LOGGER.info("ShaderBlockIdDisguises: unexpected blockPropertiesMap type {}",
                    raw == null ? "null" : raw.getClass().getName());
            return false;
        }
        Map<Integer, List<Object>> map = asIntKeyedMap(raw);
        if (map == null) {
            MainRegistry.LOGGER.info("ShaderBlockIdDisguises: unexpected blockPropertiesMap type {}",
                    raw.getClass().getName());
            return false;
        }

        Class<?> nsIdClass;
        Class<?> blockEntryClass;
        try {
            nsIdClass = Class.forName("net.irisshaders.iris.shaderpack.materialmap.NamespacedId");
            blockEntryClass = Class.forName("net.irisshaders.iris.shaderpack.materialmap.BlockEntry");
        } catch (ClassNotFoundException e) {
            MainRegistry.LOGGER.info("ShaderBlockIdDisguises: materialmap classes not found");
            return false;
        }

        for (String[] disguise : DISGUISES) {
            String ourBlock = disguise[0];
            String standIn = disguise[1];
            Integer standInId = findIdOf(map, blockEntryClass, standIn);
            if (standInId == null) {
                MainRegistry.LOGGER.info("ShaderBlockIdDisguises: SKIPPED - this pack does not map {}", standIn);
                continue;
            }
            List<Object> entries = map.get(standInId);
            if (entries == null) {
                continue;
            }
            boolean already = false;
            for (Object entry : entries) {
                if (standIn.equals(blockEntryId(blockEntryClass, entry))) {
                    already = true;
                    break;
                }
            }
            if (already) {
                continue;
            }
            Object newEntry = newBlockEntry(nsIdClass, blockEntryClass, ourBlock);
            if (newEntry == null) {
                MainRegistry.LOGGER.info("ShaderBlockIdDisguises: could not construct BlockEntry for {}", ourBlock);
                continue;
            }
            entries.add(newEntry);
            MainRegistry.LOGGER.info("ShaderBlockIdDisguises: applied - {} inherits pack id {} of {}",
                    ourBlock, standInId, standIn);
        }
        return true;
    }

    /** The map is keyed by Integer; go through the raw Map view. */
    @SuppressWarnings("unchecked")
    private static Map<Integer, List<Object>> asIntKeyedMap(Object raw) {
        try {
            return (Map<Integer, List<Object>>) raw;
        } catch (ClassCastException e) {
            return null;
        }
    }

    /** Returns the id whose entry list contains a BlockEntry for the given block name, or null. */
    private static Integer findIdOf(Map<Integer, List<Object>> map, Class<?> blockEntryClass, String blockName) {
        for (Map.Entry<Integer, List<Object>> e : map.entrySet()) {
            List<Object> entries = e.getValue();
            if (entries == null) {
                continue;
            }
            for (Object entry : entries) {
                if (blockName.equals(blockEntryId(blockEntryClass, entry))) {
                    return e.getKey();
                }
            }
        }
        return null;
    }

    /** BlockEntry.id() -> NamespacedId -> "namespace:path", null on mismatch. */
    private static String blockEntryId(Class<?> blockEntryClass, Object entry) {
        try {
            Object id = blockEntryClass.getMethod("id").invoke(entry);
            if (id == null) {
                return null;
            }
            String ns = (String) id.getClass().getMethod("getNamespace").invoke(id);
            String name = (String) id.getClass().getMethod("getName").invoke(id);
            return ns + ":" + name;
        } catch (Throwable t) {
            return null;
        }
    }

    /** new BlockEntry(new NamespacedId(ns, path), Map.of()) */
    private static Object newBlockEntry(Class<?> nsIdClass, Class<?> blockEntryClass, String nsAndPath) {
        try {
            int colon = nsAndPath.indexOf(':');
            Object id = nsIdClass.getConstructor(String.class, String.class)
                    .newInstance(nsAndPath.substring(0, colon), nsAndPath.substring(colon + 1));
            return blockEntryClass.getConstructor(nsIdClass, Map.class)
                    .newInstance(id, java.util.Collections.emptyMap());
        } catch (Throwable t) {
            return null;
        }
    }
}
