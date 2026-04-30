//? if fabric {
package com.hbm_m.client.model.loading;

import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.hbm_m.main.MainRegistry;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

@Environment(EnvType.CLIENT)
public interface ForgeLikeUnbakedModel extends UnbakedModel {

    static ForgeLikeUnbakedModel parse(ResourceLocation id, String loader, JsonObject json, ResourceManager rm, Gson gson) {
        try {
            return switch (loader) {
                case "forge:obj" -> ForgeObjUnbakedModel.fromJson(id, json, rm, gson);
                case "forge:composite" -> ForgeCompositeUnbakedModel.fromJson(id, json, rm, gson);

                // HBM custom loaders (Forge registers them via geometry loaders).
                // On Fabric we support the same JSON "loader" ids by interpreting them here.
                case "hbm_m:press_loader" -> HbmLoaderAdapters.press(id, json, rm, gson);
                case "hbm_m:heating_oven_loader" -> HbmLoaderAdapters.heatingOven(id, json, rm, gson);
                case "hbm_m:machine_assembler_loader" -> HbmLoaderAdapters.machineAssembler(id, json, rm, gson);
                case "hbm_m:advanced_assembly_machine_loader" -> HbmLoaderAdapters.advancedAssembler(id, json, rm, gson);
                case "hbm_m:chemical_plant_loader" -> HbmLoaderAdapters.chemicalPlant(id, json, rm, gson);
                case "hbm_m:hydraulic_frackining_tower_loader" -> HbmLoaderAdapters.hydraulicTower(id, json, rm, gson);
                case "hbm_m:fluid_tank_loader" -> HbmLoaderAdapters.fluidTank(id, json, rm, gson);
                case "hbm_m:battery_socket_loader" -> HbmLoaderAdapters.batterySocket(id, json, rm, gson);
                case "hbm_m:door" -> HbmLoaderAdapters.door(id, json, rm, gson);

                default -> null;
            };
        } catch (Exception e) {
            MainRegistry.LOGGER.error("Failed to parse forge-like model {} (loader={})", id, loader, e);
            return null;
        }
    }

    /**
     * Textures map from JSON ("textures": {"particle": "...", "default": "..."}).
     * Kept so OBJ material references like {@code map_Kd #default} can resolve.
     */
    Map<String, ResourceLocation> textures();
}
//?}

