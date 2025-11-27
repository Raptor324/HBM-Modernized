// package com.hbm_m.client.model;

// import com.mojang.blaze3d.vertex.PoseStack;
// import com.mojang.blaze3d.vertex.VertexConsumer;
// import net.minecraft.client.model.geom.ModelPart;
// import net.minecraft.client.model.geom.PartPose;
// import net.minecraft.client.model.geom.builders.CubeListBuilder;
// import net.minecraft.client.model.geom.builders.LayerDefinition;
// import net.minecraft.client.model.geom.builders.MeshDefinition;
// import net.minecraft.client.model.geom.builders.PartDefinition;

// import java.util.ArrayList;
// import java.util.List;

// /**
//  * Класс-адаптер для эмуляции поведения ModelRenderer из 1.7.10.
//  * Позволяет копипастить старый код моделей с минимальными правками.
//  */
// public class HbmModelRenderer {
    
//     // Данные для построения (Baking)
//     private final List<StoredBox> boxes = new ArrayList<>();
//     private final List<HbmModelRenderer> children = new ArrayList<>();
    
//     public float rotationPointX, rotationPointY, rotationPointZ;
//     public float rotateAngleX, rotateAngleY, rotateAngleZ;
//     public boolean mirror;
//     public boolean showModel = true;

//     private float textureWidth = 64.0F;
//     private float textureHeight = 32.0F;
    
//     private final int texOffX, texOffY;
//     private ModelPart bakedPart; // Скомпилированная часть (для рендера)

//     public HbmModelRenderer(int texOffX, int texOffY) {
//         this.texOffX = texOffX;
//         this.texOffY = texOffY;
//     }

    

//     // === Методы 1.7.10 ===

//     public void addBox(float x, float y, float z, int w, int h, int d) {
//         boxes.add(new StoredBox(texOffX, texOffY, x, y, z, w, h, d, 0.0F, mirror));
//     }

//     public void setRotationPoint(float x, float y, float z) {
//         this.rotationPointX = x;
//         this.rotationPointY = y;
//         this.rotationPointZ = z;
//     }

//     public void addChild(HbmModelRenderer child) {
//         this.children.add(child);
//     }

//     // === Метод Рендера (Runtime) ===
    
//     public void render(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay) {
//         if (!showModel) return;
//         if (bakedPart == null) {
//             bake();
//         }

//         // Передаем вращение и позицию в ванильный ModelPart
//         bakedPart.xRot = rotateAngleX;
//         bakedPart.yRot = rotateAngleY;
//         bakedPart.zRot = rotateAngleZ;
//         bakedPart.x = rotationPointX;
//         bakedPart.y = rotationPointY;
//         bakedPart.z = rotationPointZ;
        
//         // Рендерим этот кусок
//         bakedPart.render(poseStack, buffer, packedLight, packedOverlay);
        
//         // Рендерим детей (мы их не добавляем в bakedPart.children, а рендерим отдельно, чтобы сохранить контроль)
//         // Важно: Дети должны рендериться ОТНОСИТЕЛЬНО родителя.
//         if (!children.isEmpty()) {
//              poseStack.pushPose();
//              bakedPart.translateAndRotate(poseStack); // Применяем трансформацию родителя
//              for (HbmModelRenderer child : children) {
//                  child.render(poseStack, buffer, packedLight, packedOverlay);
//              }
//              poseStack.popPose();
//         }
//     }
    
//     // === Внутренняя магия (Baking) ===
    
//     private void bake() {
//         MeshDefinition mesh = new MeshDefinition();
//         PartDefinition root = mesh.getRoot();
//         CubeListBuilder builder = CubeListBuilder.create();
        
//         for (StoredBox box : boxes) {
//             builder.texOffs(box.u, box.v)
//                    .mirror(box.mirror)
//                    .addBox(box.x, box.y, box.z, box.w, box.h, box.d);
//         }
        
//         // Создаем слой с размерами текстуры, которые были заданы через setTextureSize
//         root.addOrReplaceChild("main", builder, PartPose.ZERO);
//         this.bakedPart = LayerDefinition.create(mesh, (int)textureWidth, (int)textureHeight).bakeRoot().getChild("main");
//     }
    
//     private record StoredBox(int u, int v, float x, float y, float z, float w, float h, float d, float inflate, boolean mirror) {}
// }
