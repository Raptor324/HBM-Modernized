package com.hbm_m.platform.legacy.client.model;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.hbm_m.client.model.nmesh.NMeshLoader;

import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.GsonHelper;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.AbstractUnbakedModel;
import net.neoforged.neoforge.client.model.StandardModelParameters;
import net.neoforged.neoforge.client.model.UnbakedModelLoader;

/**
 * [26.x] Einziger Modell-Loader des Mods auf 26.x: alle {@code hbm_m:*}-Loader-IDs der Modell-JSONs
 * laufen über den NMesh-Pfad (Entscheidung „nur NMesh"). JSONs ohne {@code model}-Schlüssel (DAE,
 * Template, Missile) ergeben eine leere Geometrie; diese Objekte rendert Nucleus ohnehin selbst.
 */
public final class NMeshModelLoader26 implements UnbakedModelLoader<NMeshModelLoader26.Model>, ResourceManagerReloadListener {

    public static final NMeshModelLoader26 INSTANCE = new NMeshModelLoader26();

    /** Loader-IDs aus den Modell-JSONs (siehe {@code ClientSetup#onModelRegister} der alten Versionen). */
    public static final String[] IDS = {
            "nmesh_obj", "machine_parts_loader", "door", "box_cable", "battery_pack", "dae",
            "template_loader", "missile_loader", "soyuz_rocket_loader", "weapon_obj_parts", "weapon_parts_loader",
            "t51_armor_parts", "ajr_armor_parts", "bismuth_armor_parts", "dnt_armor_parts", "hazmat_armor_parts"
    };

    private NMeshModelLoader26() {}

    public static void register(ModelEvent.RegisterLoaders event) {
        for (String id : IDS) {
            event.register(Identifier.fromNamespaceAndPath("hbm_m", id), INSTANCE);
        }
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        NMeshLoader.clearCache();
        NMeshPartRegistry.clear();
    }

    @Override
    public Model read(JsonObject json, JsonDeserializationContext ctx) throws JsonParseException {
        StandardModelParameters params = StandardModelParameters.parse(json, ctx);
        if (!json.has("model")) {
            return new Model(params, UnbakedGeometry.EMPTY);
        }
        Identifier model = Identifier.tryParse(GsonHelper.getAsString(json, "model"));
        if (model == null) throw new JsonParseException("hbm_m: ungültiger 'model'-Pfad");
        Identifier texture = json.has("texture") && !json.get("texture").isJsonNull()
                ? Identifier.tryParse(GsonHelper.getAsString(json, "texture")) : null;
        List<String> parts = null;
        if (json.has("parts") && json.get("parts").isJsonArray()) {
            parts = new ArrayList<>();
            for (JsonElement e : json.getAsJsonArray("parts")) parts.add(e.getAsString());
            // Tür-JSONs listen alle Teile auf; für statische Geometrie zählt das als „alles".
            String loader = GsonHelper.getAsString(json, "loader", "");
            if (loader.endsWith(":door") || loader.endsWith(":machine_parts_loader")) parts = null;
        }
        var settings = new NMeshGeometry.Settings(model,
                GsonHelper.getAsBoolean(json, "flip_v", true),
                GsonHelper.getAsBoolean(json, "shade_quads", true),
                GsonHelper.getAsBoolean(json, "automatic_culling", true),
                GsonHelper.getAsBoolean(json, "emissive_ambient", true),
                GsonHelper.getAsInt(json, "tint_index", -1),
                texture, parts);
        return new Model(params, new NMeshGeometry(settings));
    }

    public static final class Model extends AbstractUnbakedModel {
        private final UnbakedGeometry geometry;

        Model(StandardModelParameters params, UnbakedGeometry geometry) {
            super(params);
            this.geometry = geometry;
        }

        @Override
        public UnbakedGeometry geometry() {
            return geometry;
        }
    }
}
