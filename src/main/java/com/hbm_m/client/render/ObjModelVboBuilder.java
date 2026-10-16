package com.hbm_m.client.render;


import net.minecraft.client.resources.model.BakedModel;

//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
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
