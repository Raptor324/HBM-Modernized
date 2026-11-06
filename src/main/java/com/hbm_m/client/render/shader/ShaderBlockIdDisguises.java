package com.hbm_m.client.render.shader;

import com.hbm_m.main.MainRegistry;

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
 * <p>
 * Delegates reflection and pack access to {@link IrisReflectionBridge}.
 */
public final class ShaderBlockIdDisguises {

    /** block registry name ("ns:path") -> vanilla block whose pack id it inherits. */
    private static final String[][] DISGUISES = {
            {"hbm_m:lamp_demon", "minecraft:soul_torch"},
    };

    /** Identity of the ShaderPack instance the disguises were last applied to. */
    private static Object lastPack;

    private ShaderBlockIdDisguises() {}

    /** Polled once per frame from ShaderCompatibilityDetector. Cheap no-op until an unapplied pack shows up. */
    public static void applyIfPackChanged() {
        if (!IrisReflectionBridge.isIrisPresent()) {
            return;
        }
        try {
            Object pack = IrisReflectionBridge.getCurrentPack();
            if (pack == null || pack == lastPack) {
                return;
            }
            lastPack = pack;
            Object idMap = IrisReflectionBridge.getIdMap(pack);
            apply(idMap);
        } catch (Throwable t) {
            MainRegistry.LOGGER.info("ShaderBlockIdDisguises: apply failed ({}: {})",
                    t.getClass().getSimpleName(), t.getMessage());
        }
    }

    /**
     * Applies the disguise table to one IdMap. Returns false when the structure
     * does not match the expected {@code Int2ObjectMap<List<BlockEntry>>}.
     */
    private static boolean apply(Object idMap) {
        if (idMap == null) return false;
        Map<Integer, List<Object>> map = IrisReflectionBridge.getBlockPropertiesMap(idMap);
        if (map == null) {
            MainRegistry.LOGGER.info("ShaderBlockIdDisguises: blockPropertiesMap unreadable or missing");
            return false;
        }

        for (String[] disguise : DISGUISES) {
            String ourBlock = disguise[0];
            String standIn = disguise[1];
            Integer standInId = findIdOf(map, standIn);
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
                if (ourBlock.equals(IrisReflectionBridge.getBlockEntryId(entry))) {
                    already = true;
                    break;
                }
            }
            if (already) {
                continue;
            }
            Object newEntry = IrisReflectionBridge.createBlockEntry(ourBlock);
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

    /** Returns the id whose entry list contains a BlockEntry for the given block name, or null. */
    private static Integer findIdOf(Map<Integer, List<Object>> map, String blockName) {
        for (Map.Entry<Integer, List<Object>> e : map.entrySet()) {
            List<Object> entries = e.getValue();
            if (entries == null) {
                continue;
            }
            for (Object entry : entries) {
                if (blockName.equals(IrisReflectionBridge.getBlockEntryId(entry))) {
                    return e.getKey();
                }
            }
        }
        return null;
    }
}
