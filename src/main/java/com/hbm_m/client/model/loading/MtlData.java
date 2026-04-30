//? if fabric {
package com.hbm_m.client.model.loading;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.hbm_m.main.MainRegistry;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

@Environment(EnvType.CLIENT)
final class MtlData {

    private MtlData() {}

    /**
     * Reads {@code map_Kd} for each {@code newmtl}.
     * Returns map: materialName -> raw map_Kd value (e.g. "#surface" or "hbm_m:block/airbomb").
     */
    static Map<String, String> load(ResourceManager rm, ResourceLocation objLocation, String mtllibFile) {
        // mtllib uses a file name relative to the OBJ file location.
        String objPath = objLocation.getPath();
        int lastSlash = objPath.lastIndexOf('/');
        String baseDir = lastSlash >= 0 ? objPath.substring(0, lastSlash + 1) : "";

        ResourceLocation mtlLoc = new ResourceLocation(objLocation.getNamespace(), baseDir + mtllibFile);
        Resource res = rm.getResource(mtlLoc).orElse(null);
        if (res == null) {
            MainRegistry.LOGGER.warn("MTL not found for OBJ {}: {}", objLocation, mtlLoc);
            return Map.of();
        }

        Map<String, String> out = new HashMap<>();
        String currentMtl = null;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(res.open(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                if (line.startsWith("newmtl ")) {
                    currentMtl = line.substring("newmtl ".length()).trim();
                    continue;
                }
                if (currentMtl != null && line.startsWith("map_Kd ")) {
                    String tex = line.substring("map_Kd ".length()).trim();
                    out.put(currentMtl, tex);
                }
            }
        } catch (Exception e) {
            MainRegistry.LOGGER.warn("Failed to read MTL {}", mtlLoc, e);
        }
        return out;
    }
}
//?}

