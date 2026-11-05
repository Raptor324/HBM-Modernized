package com.hbm_m.client.render.implementations;

import java.util.List;
import java.util.Map;

import org.joml.Matrix4f;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.PantsirBlockEntity;
import com.hbm_m.client.render.HbmBerBounds;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.client.render.machine.MachineSpecBuilder;
import com.hbm_m.client.render.raptorpath.ObjAtlasMesh;
import com.hbm_m.client.render.raptorpath.RaptorParts;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.RenderHooks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Zeichnet den Pantsir-S1. Alle Teile liegen als Gruppen in einer OBJ-Datei und teilen eine Textur:
 * Fahrzeug fest, darauf der drehbare Turm, darauf Radar und Werfer als eigene Gelenke.
 */
//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public class PantsirRenderer implements HbmBerBounds<PantsirBlockEntity> {

    private static final String OBJ = "models/machines/pantsir_s1.obj";
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            RefStrings.MODID, "textures/models/machines/pantsir_s1.png");

    private static final String[] VEHICLE_GROUPS = {"chassis", "door1", "door2", "wheels_front", "wheels"};

    private static final ResourceLocation SPRITE = ObjAtlasMesh.spriteIdOf(TEXTURE);
    private static final ObjAtlasMesh VEHICLE = ObjAtlasMesh.groups(OBJ, SPRITE, "hbm_m/pantsir_s1/vehicle", VEHICLE_GROUPS);
    private static final ObjAtlasMesh TURRET = ObjAtlasMesh.groups(OBJ, SPRITE, "hbm_m/pantsir_s1/turret", "turret");
    private static final ObjAtlasMesh RADAR = ObjAtlasMesh.groups(OBJ, SPRITE, "hbm_m/pantsir_s1/radar", "radar");
    private static final ObjAtlasMesh LAUNCHER = ObjAtlasMesh.groups(OBJ, SPRITE, "hbm_m/pantsir_s1/launcher", "launcher");

    public PantsirRenderer(BlockEntityRendererProvider.Context context) {
    }

    /**
     * Raptors Renderpfad: VBO je Teil, Instancing, Occlusion-Culling. Ersetzt den immediate-Renderer
     * dieser Klasse, der nur noch fuer {@code -Dhbm_m.legacyMachineRender=true} angemeldet bleibt.
     */
    public static void registerRaptorPath() {
        MachineSpecBuilder<PantsirBlockEntity> spec = MachineRenderers
                .machine("hbm_m/pantsir_s1", ModBlockEntities.PANTSIR_S1_BE.get(), PantsirBlockEntity.class)
                .blockTransform((be, animator) -> {
                    animator.translate(0.5D, 0.0D, 0.5D);
                    animator.rotate(PantsirBlockEntity.blockYaw(facing(be)), 0.0F, 1.0F, 0.0F);
                });
        RaptorParts.mesh(spec, "vehicle", VEHICLE);
        RaptorParts.mesh(spec, "turret", (be, partial, time, pose) -> {
            aroundY(pose, PantsirBlockEntity.TURRET_AXIS_Z, be.getTurretYaw(partial));
            return true;
        }, TURRET);
        RaptorParts.mesh(spec, "radar", (be, partial, time, pose) -> {
            aroundY(pose, PantsirBlockEntity.TURRET_AXIS_Z, be.getTurretYaw(partial));
            aroundY(pose, PantsirBlockEntity.RADAR_AXIS_Z, be.getRadarYaw(partial));
            return true;
        }, RADAR);
        RaptorParts.mesh(spec, "launcher", (be, partial, time, pose) -> {
            aroundY(pose, PantsirBlockEntity.TURRET_AXIS_Z, be.getTurretYaw(partial));
            elevate(pose, be.getLauncherPitch(partial));
            return true;
        }, LAUNCHER);
        spec.viewDistance(256).register();
    }

    private static Direction facing(PantsirBlockEntity be) {
        return be.getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? be.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING)
                : Direction.SOUTH;
    }

    private static void elevate(PoseStack poseStack, float degrees) {
        poseStack.translate(0.0D, PantsirBlockEntity.LAUNCHER_AXIS_Y, PantsirBlockEntity.TURRET_AXIS_Z);
        poseStack.mulPose(Axis.XP.rotationDegrees(-degrees));
        poseStack.translate(0.0D, -PantsirBlockEntity.LAUNCHER_AXIS_Y, -PantsirBlockEntity.TURRET_AXIS_Z);
    }

    @Override
    public void render(PantsirBlockEntity be, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        Map<String, List<float[]>> model = RBMKColumnRenderer.getObj(OBJ);
        if (model.isEmpty()) {
            return;
        }
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(PantsirBlockEntity.blockYaw(facing(be))));

        for (String group : VEHICLE_GROUPS) {
            draw(vc, poseStack, model.get(group), packedLight);
        }

        poseStack.pushPose();
        aroundY(poseStack, PantsirBlockEntity.TURRET_AXIS_Z, be.getTurretYaw(partialTicks));
        draw(vc, poseStack, model.get("turret"), packedLight);

        poseStack.pushPose();
        aroundY(poseStack, PantsirBlockEntity.RADAR_AXIS_Z, be.getRadarYaw(partialTicks));
        draw(vc, poseStack, model.get("radar"), packedLight);
        poseStack.popPose();

        poseStack.pushPose();
        elevate(poseStack, be.getLauncherPitch(partialTicks));
        draw(vc, poseStack, model.get("launcher"), packedLight);
        poseStack.popPose();

        poseStack.popPose();
        poseStack.popPose();
    }

    private static void aroundY(PoseStack poseStack, double axisZ, float degrees) {
        poseStack.translate(0.0D, 0.0D, axisZ);
        poseStack.mulPose(Axis.YP.rotationDegrees(degrees));
        poseStack.translate(0.0D, 0.0D, -axisZ);
    }

    /** Wie im Topol-Renderer: UVs direkt, V gespiegelt, der vierte Durchlauf doppelt den dritten Punkt. */
    private static void draw(VertexConsumer vc, PoseStack poseStack, List<float[]> triangles, int light) {
        if (triangles == null) {
            return;
        }
        Matrix4f matrix = poseStack.last().pose();
        for (float[] tri : triangles) {
            for (int pass = 0; pass < 4; pass++) {
                int base = Math.min(pass, 2) * 8;
                RenderHooks.vertexFull(vc, matrix,
                        tri[base], tri[base + 1], tri[base + 2],
                        255, 255, 255, 255,
                        tri[base + 3], 1.0F - tri[base + 4],
                        OverlayTexture.NO_OVERLAY, light,
                        tri[base + 5], tri[base + 6], tri[base + 7]);
            }
        }
    }
}
