package com.hbm_m.platform.legacy.client.model;

import java.util.Map;

import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.resources.Identifier;

/** [26.x] Teile-Container (siehe {@link AbstractMultipartBakedModel}); ersetzt den gleichnamigen Typ der alten Versionen. */
public class MissileBakedModel extends AbstractMultipartBakedModel {

    public MissileBakedModel(Identifier modelId, Map<String, BakedModel> parts, ItemTransforms transforms) {
        super(modelId, parts, transforms);
    }

    /** Teile in fester Reihenfolge (alte API: getRenderablePartNames). */
    public java.util.List<String> getRenderablePartNames() {
        return java.util.List.of(getPartNames());
    }
}
