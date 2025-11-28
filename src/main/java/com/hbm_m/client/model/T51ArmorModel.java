package com.hbm_m.client.model;

import com.hbm_m.main.MainRegistry; // Импорт твоего главного класса
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.Map;

public class T51ArmorModel extends HumanoidModel<LivingEntity> {

    private static final ResourceLocation OBJ_MODEL = new ResourceLocation(MainRegistry.MOD_ID, "models/armor/t51_0.obj");

    private Map<String, SimpleObjParser.Mesh> loadedMeshes;

    public T51ArmorModel(ModelPart root) {
        super(root);
        this.loadedMeshes = SimpleObjParser.load(OBJ_MODEL);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        // --- ГОЛОВА ---
        if (this.head.visible) {
            renderPart(poseStack, buffer, this.head, "head", packedLight, packedOverlay);
        }

        // --- ТЕЛО ---
        if (this.body.visible) {
            renderPart(poseStack, buffer, this.body, "body", packedLight, packedOverlay);
        }

        // --- РУКИ ---
        if (this.rightArm.visible) {
            renderPart(poseStack, buffer, this.rightArm, "right_arm", packedLight, packedOverlay);
        }
        if (this.leftArm.visible) {
            renderPart(poseStack, buffer, this.leftArm, "left_arm", packedLight, packedOverlay);
        }

        // --- НОГИ И БОТИНКИ ---
        if (this.rightLeg.visible) {
            renderPart(poseStack, buffer, this.rightLeg, "right_leg", packedLight, packedOverlay);
            renderPart(poseStack, buffer, this.rightLeg, "right_boot", packedLight, packedOverlay);
        }
        if (this.leftLeg.visible) {
            renderPart(poseStack, buffer, this.leftLeg, "left_leg", packedLight, packedOverlay);
            renderPart(poseStack, buffer, this.leftLeg, "left_boot", packedLight, packedOverlay);
        }
    }

    private void renderPart(PoseStack poseStack, VertexConsumer buffer, ModelPart bone, String objGroupName, int light, int overlay) {
        SimpleObjParser.Mesh mesh = loadedMeshes.get(objGroupName);
        if (mesh == null) return;

        poseStack.pushPose();

        // 1. Переносим координаты в точку кости
        bone.translateAndRotate(poseStack);

        // 2. Переворачиваем модель (стандарт для OBJ)
        // Если у вас тут стоит (1, 1, 1), верните (-1) по Y и Z, иначе текстуры будут зеркальными
        poseStack.scale(1.0F, -1.0F, -1.0F);

        // 3. !!! ГЛАВНОЕ ИСПРАВЛЕНИЕ !!!
        // Поднимаем модель на 1.5 блока (это ~24 пикселя, стандартная высота)
        // Если модель всё ещё низко или высоко, меняйте число 1.5D (например на 1.4D или 1.6D)
        poseStack.translate(0.0D, -1.5D, 0.0D);

        // Если модель смотрит лицом назад, можно развернуть её тут:
        // poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();

        for (SimpleObjParser.Face face : mesh.faces) {
            drawVertex(buffer, pose, normal, face.v1, light, overlay);
            drawVertex(buffer, pose, normal, face.v2, light, overlay);
            drawVertex(buffer, pose, normal, face.v3, light, overlay);
        }

        poseStack.popPose();
    }

    private void drawVertex(VertexConsumer buffer, Matrix4f pose, Matrix3f normal, SimpleObjParser.Vertex v, int light, int overlay) {
        buffer.vertex(pose, v.pos.x, v.pos.y, v.pos.z)
                .color(1f, 1f, 1f, 1f)
                .uv(v.uv.x, v.uv.y)
                .overlayCoords(overlay)
                .uv2(light)
                .normal(normal, v.normal.x, v.normal.y, v.normal.z)
                .endVertex();
    }
}