package com.hbm_m.client.render;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ObjModelVboBuilder {

    private ObjModelVboBuilder() {}

    public static SingleMeshVboRenderer.VboData buildSinglePart(BakedModel modelPart) {
        return buildSinglePart(modelPart, "unknown");
    }

    public static SingleMeshVboRenderer.VboData buildSinglePart(BakedModel modelPart, String partName) {
        PartGeometry g = PartGeometry.compile(modelPart, partName);
        if (g.isEmpty()) {
            return null;
        }
        return g.toVboData(partName);
    }
}
