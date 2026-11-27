package com.hbm_m.platform.legacy.client.model;

import java.util.Map;

import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.resources.Identifier;

/** [26.x] Teile-Container (siehe {@link AbstractMultipartBakedModel}); ersetzt den gleichnamigen Typ der alten Versionen. */
public class ConfiguredMultipartBakedModel extends AbstractMultipartBakedModel {

    public ConfiguredMultipartBakedModel(Identifier modelId, Map<String, BakedModel> parts, ItemTransforms transforms) {
        super(modelId, parts, transforms);
    }

    /** Auf 26.x ohne Wirkung: der Chunk zeichnet BER-Blöcke ohnehin nicht (RenderShape.INVISIBLE). */
    public void applyMachineConfig(java.util.@org.jspecify.annotations.Nullable List<String> itemParts, Object chunkRenderTypes) {
    }
}
