package com.hbm_m.client.render;


import net.minecraft.client.resources.model.BakedModel;
//? if forge {
/*import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
*///?}
//? if fabric {
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;//?}
//? if forge {
/*@OnlyIn(Dist.CLIENT)
*///?}
//? if fabric {
@Environment(EnvType.CLIENT)//?}
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
