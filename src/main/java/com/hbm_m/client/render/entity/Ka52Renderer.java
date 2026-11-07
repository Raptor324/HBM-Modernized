package com.hbm_m.client.render.entity;

import java.util.List;

import com.hbm_m.client.render.MeshRenderCache;
import com.hbm_m.client.render.SingleMeshVboRenderer;
import com.hbm_m.client.render.raptorpath.ObjAtlasMesh;
import com.hbm_m.entity.logic.EntityKa52;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * Zeichnet den Ka-52 ueber Raptors VBO-Pfad: jedes bewegliche Teil ist ein eigener Mesh. Die Koaxial-
 * Rotoren drehen gegenlaeufig, das Fahrwerk klappt nach hinten ein, das Sensorgehaeuse unter der Nase
 * sucht die Umgebung ab, die Kanone an der rechten Rumpfseite richtet sich aufs Ziel und ihr Rohr laeuft bei
 * jedem Schuss zurueck. Das Modell schaut nach +Z, die Rotorachse liegt im Ursprung.
 *
 * <p>Sitzt der Betrachter selbst im Cockpit und schaut aus der Ich-Sicht, bleibt der Rumpf weg, sonst saehe
 * er nur die Innenseite der Zelle.
 */
//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public class Ka52Renderer extends EntityRenderer<EntityKa52> {

    private static final String OBJ = "models/machines/ka52.obj";
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            RefStrings.MODID, "textures/models/machines/ka52.png");
    private static final ResourceLocation SPRITE = ObjAtlasMesh.spriteIdOf(TEXTURE);

    private static final ObjAtlasMesh BODY = mesh("body");
    private static final ObjAtlasMesh ROTOR_LOWER = mesh("rotor_lower");
    private static final ObjAtlasMesh ROTOR_UPPER = mesh("rotor_upper");
    private static final ObjAtlasMesh GEAR_NOSE = mesh("gear_nose");
    private static final ObjAtlasMesh GEAR_LEFT = mesh("gear_left");
    private static final ObjAtlasMesh GEAR_RIGHT = mesh("gear_right");
    private static final ObjAtlasMesh SENSOR = mesh("cannon");
    private static final ObjAtlasMesh GUN_MOUNT = mesh("gun_mount");
    private static final ObjAtlasMesh GUN_BARREL = mesh("gun_barrel");

    private static final float GEAR_FOLD = 85.0F;
    private static final double GUN_RECOIL = 0.18D;

    private static ObjAtlasMesh mesh(String group) {
        return ObjAtlasMesh.groups(OBJ, SPRITE, "hbm_m/ka52/" + group, group);
    }

    public Ka52Renderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public boolean shouldRender(EntityKa52 entity, Frustum frustum, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public void render(EntityKa52 entity, float entityYaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int light) {
        BlockPos pos = entity.blockPosition();
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot())));
        float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        pose.mulPose(Axis.XP.rotationDegrees(pitch));
        pose.mulPose(Axis.ZP.rotationDegrees(entity.getRoll()));

        Minecraft minecraft = Minecraft.getInstance();
        Entity viewer = minecraft.getCameraEntity();
        boolean cockpitView = minecraft.options.getCameraType().isFirstPerson() && viewer != null && viewer.getVehicle() == entity;
        if (!cockpitView) {
            draw(BODY, pose, light, pos, buffer);
        }

        float rotor = entity.getRotorAngle(partialTick);
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(rotor));
        draw(ROTOR_LOWER, pose, light, pos, buffer);
        pose.popPose();
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-rotor));
        draw(ROTOR_UPPER, pose, light, pos, buffer);
        pose.popPose();

        float fold = entity.getGearRetraction() * GEAR_FOLD;
        gear(GEAR_NOSE, 0.0D, 1.02D, 3.33D, fold, pose, light, pos, buffer);
        gear(GEAR_LEFT, 0.86D, 1.43D, -0.85D, fold, pose, light, pos, buffer);
        gear(GEAR_RIGHT, -0.86D, 1.43D, -0.85D, fold, pose, light, pos, buffer);

        pose.pushPose();
        pose.translate(0.29D, 0.62D, 3.76D);
        pose.mulPose(Axis.YP.rotationDegrees(Mth.sin((entity.tickCount + partialTick) * 0.03F) * 50.0F));
        pose.translate(-0.29D, -0.62D, -3.76D);
        draw(SENSOR, pose, light, pos, buffer);
        pose.popPose();

        pose.pushPose();
        pose.translate(EntityKa52.GUN_X, EntityKa52.GUN_Y, EntityKa52.GUN_Z);
        pose.mulPose(Axis.YP.rotationDegrees(-entity.getCannonYaw()));
        pose.mulPose(Axis.XP.rotationDegrees(entity.getCannonPitch() - pitch));
        pose.translate(-EntityKa52.GUN_X, -EntityKa52.GUN_Y, -EntityKa52.GUN_Z);
        draw(GUN_MOUNT, pose, light, pos, buffer);
        pose.translate(0.0D, 0.0D, -entity.getGunRecoil(partialTick) * GUN_RECOIL);
        draw(GUN_BARREL, pose, light, pos, buffer);
        pose.popPose();

        pose.popPose();
        super.render(entity, entityYaw, partialTick, pose, buffer, light);
    }

    private static void gear(ObjAtlasMesh mesh, double x, double y, double z, float fold, PoseStack pose,
                             int light, BlockPos pos, MultiBufferSource buffer) {
        pose.pushPose();
        pose.translate(x, y, z);
        pose.mulPose(Axis.XP.rotationDegrees(fold));
        pose.translate(-x, -y, -z);
        draw(mesh, pose, light, pos, buffer);
        pose.popPose();
    }

    private static void draw(ObjAtlasMesh mesh, PoseStack pose, int light, BlockPos pos, MultiBufferSource buffer) {
        List<BakedQuad> quads = mesh.quads();
        if (quads.isEmpty()) {
            return;
        }
        SingleMeshVboRenderer renderer = MeshRenderCache.getOrCreateRendererFromQuadList(mesh.cacheKey(), quads);
        if (renderer != null) {
            renderer.render(pose, light, pos, null, buffer);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(EntityKa52 entity) {
        return TEXTURE;
    }
}
