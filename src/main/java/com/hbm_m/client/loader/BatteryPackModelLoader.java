// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm_m.client.loader;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.hbm_m.client.model.BatteryPackBakedModel;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

/**
 * Лоадер item-моделей больших батарей-паков (бэкпорт оригинальных "hbm:obj" JSON'ов
 * battery_pack_*.json). Печёт одну OBJ-часть battery-сокета (Battery или Capacitor)
 * с навязанной текстурой тира — ретекстур происходит на этапе запекания.
 *
 * JSON:
 * {@code
 * { "loader": "hbm_m:battery_pack_loader",
 *   "model": "hbm_m:models/block/machines/machine_battery_socket.obj",
 *   "part": "Battery",                                  // или "Capacitor"
 *   "texture": "hbm_m:block/machine/battery_redstone",  // спрайт из block-атласа
 *   "flip_v": true }
 * }
 */
public class BatteryPackModelLoader extends AbstractObjPartModelLoader<BatteryPackBakedModel> {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "battery_pack");

    @Override
    public ObjPartGeometry<BatteryPackBakedModel> read(JsonObject jsonObject, JsonDeserializationContext deserializationContext) {
        String modelStr = GsonHelper.getAsString(jsonObject, "model");
        ResourceLocation model = ResourceLocation.tryParse(modelStr);
        String part = GsonHelper.getAsString(jsonObject, "part", "Battery");
        String texture = GsonHelper.getAsString(jsonObject, "texture");
        boolean flipV = GsonHelper.getAsBoolean(jsonObject, "flip_v", true);
        // Map-режим: имя части -> {группа OBJ = та же часть, текстура-оверрайд тира}.
        return new ObjPartGeometry<>(model, Set.of(), flipV, this,
                Map.of(part, new PartSpec(null, ResourceLocation.tryParse(texture))));
    }

    @Override
    protected Set<String> getPartNames(JsonObject jsonObject) {
        return Set.of(GsonHelper.getAsString(jsonObject, "part", "Battery"));
    }

    @Override
    protected BatteryPackBakedModel createBakedModel(HashMap<String, BakedModel> bakedParts,
                                                     ItemTransforms transforms,
                                                     ResourceLocation modelLocation) {
        return new BatteryPackBakedModel(bakedParts, transforms);
    }
}
