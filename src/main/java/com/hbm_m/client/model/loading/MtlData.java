//? if fabric {
package com.hbm_m.client.model.loading;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
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
     * Результат разбора MTL: diffuse по материалу и опциональные {@code forge_TintIndex} (как в Forge OBJ).
     */
    record MtlParseResult(Map<String, String> texturesByMaterial, Map<String, Integer> tintIndexByMaterial) {
        static final MtlParseResult EMPTY = new MtlParseResult(Map.of(), Map.of());
    }

    /**
     * MTL из {@code mtllib} относительно каталога OBJ.
     */
    static MtlParseResult load(ResourceManager rm, ResourceLocation objLocation, String mtllibFile) {
        String objPath = objLocation.getPath();
        int lastSlash = objPath.lastIndexOf('/');
        String baseDir = lastSlash >= 0 ? objPath.substring(0, lastSlash + 1) : "";

        ResourceLocation mtlLoc = new ResourceLocation(objLocation.getNamespace(), baseDir + mtllibFile);
        return readMtlFile(rm, mtlLoc, "OBJ " + objLocation);
    }

    /**
     * MTL по абсолютному id (как в JSON {@code mtl_override}: {@code modid:models/.../file.mtl}).
     */
    static MtlParseResult loadAbsolute(ResourceManager rm, ResourceLocation mtlFile) {
        return readMtlFile(rm, mtlFile, mtlFile.toString());
    }

    private static MtlParseResult readMtlFile(ResourceManager rm, ResourceLocation mtlLoc, String contextForLog) {
        Resource res = rm.getResource(mtlLoc).orElse(null);
        if (res == null) {
            MainRegistry.LOGGER.warn("MTL not found ({}): {}", contextForLog, mtlLoc);
            return MtlParseResult.EMPTY;
        }

        Map<String, String> textures = new HashMap<>();
        Map<String, Integer> tints = new HashMap<>();
        String currentMtl = null;
        try (InputStream in = res.open();
             BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                if (line.startsWith("newmtl ")) {
                    currentMtl = line.substring("newmtl ".length()).trim();
                    continue;
                }
                if (currentMtl == null) continue;

                if (line.startsWith("map_Kd ")) {
                    String tex = line.substring("map_Kd ".length()).trim();
                    textures.put(currentMtl, tex);
                    continue;
                }

                String[] sp = line.split("\\s+");
                if (sp.length >= 2 && sp[0].toLowerCase(Locale.ROOT).equals("forge_tintindex")) {
                    try {
                        tints.put(currentMtl, Integer.parseInt(sp[1]));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        } catch (Exception e) {
            MainRegistry.LOGGER.warn("Failed to read MTL {} ({})", mtlLoc, contextForLog, e);
        }
        return new MtlParseResult(Map.copyOf(textures), Map.copyOf(tints));
    }
}
//?}
