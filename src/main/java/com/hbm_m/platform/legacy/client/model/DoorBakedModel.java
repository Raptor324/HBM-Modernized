package com.hbm_m.platform.legacy.client.model;

import java.util.Map;

import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.resources.Identifier;

/** [26.x] Teile-Container (siehe {@link AbstractMultipartBakedModel}); ersetzt den gleichnamigen Typ der alten Versionen. */
public class DoorBakedModel extends AbstractMultipartBakedModel {

    public DoorBakedModel(Identifier modelId, Map<String, BakedModel> parts, ItemTransforms transforms) {
        super(modelId, parts, transforms);
    }

    /** Identität der gebackenen Geometrie (Cache-Schlüssel der Tür-VBOs). */
    public Object geometryToken() {
        return this;
    }
}
