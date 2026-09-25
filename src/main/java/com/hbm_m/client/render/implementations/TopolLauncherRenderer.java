package com.hbm_m.client.render.implementations;

import java.util.List;
import java.util.Map;

import com.hbm_m.blockentity.machines.TopolLaunchPadBlockEntity;
import com.hbm_m.client.render.HbmBerBounds;
import com.hbm_m.client.render.missile.MissileRenderData;
import com.hbm_m.client.render.missile.MissileRenderRegistry;
import com.hbm_m.item.missile.MissileItem;
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
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import org.joml.Matrix4f;

/**
 * Zeichnet den Topol-M-Werfer: Fahrgestell fest, Startbehaelter drehbar.
 *
 * <p>Beides sind eigene OBJ-Dateien mit eigener Textur, weil sie sich gegeneinander bewegen. Der
 * Behaelter liegt im Modell laengs nach +Z und hat seinen Ursprung im Aufrichtgelenk - zum
 * Aufrichten genuegt darum eine Drehung um die X-Achse um bis zu -90 Grad.
 *
 * <p>Gezeichnet wird mit den UVs der Modelle direkt, nicht ueber den Blockatlas: die
 * Fahrzeugtextur misst 1024 Pixel im Quadrat und hat auf dem Atlas nichts verloren.
 */
//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public class TopolLauncherRenderer implements HbmBerBounds<TopolLaunchPadBlockEntity> {

    private static final String CHASSIS_OBJ = "models/machines/topol_chassis.obj";
    private static final String CANISTER_OBJ = "models/machines/topol_canister.obj";

    private static final ResourceLocation TEX_CHASSIS = ResourceLocation.fromNamespaceAndPath(
            RefStrings.MODID, "textures/models/machines/topol_chassis.png");
    private static final ResourceLocation TEX_CANISTER = ResourceLocation.fromNamespaceAndPath(
            RefStrings.MODID, "textures/models/machines/topol_canister.png");

    /** Seitlicher Versatz des Aufrichtgelenks; das Fahrzeug ist nicht ganz symmetrisch. */
    private static final float PIVOT_X = (float) TopolLaunchPadBlockEntity.PIVOT_LATERAL;

    /** Lage des Raketenhecks im Rohr, gemessen vom Aufrichtgelenk entlang der Behaelterachse. */
    private static final float EJECT_START = 0.5F;

    /**
     * Wohin das Heck bis zum Ende des Auswurfs wandert: einen halben Block ueber die Muendung
     * hinaus, also genau dorthin, wo gleich die Raketenentitaet entsteht.
     */
    private static final float EJECT_END =
            (float) TopolLaunchPadBlockEntity.CANISTER_LENGTH + 0.5F;

    public TopolLauncherRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(TopolLaunchPadBlockEntity be, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        Map<String, List<float[]>> chassis = RBMKColumnRenderer.getObj(CHASSIS_OBJ);
        Map<String, List<float[]>> canister = RBMKColumnRenderer.getObj(CANISTER_OBJ);
        if (chassis.isEmpty() && canister.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);

        Direction facing = Direction.NORTH;
        if (be.getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            facing = be.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        switch (facing) {
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            default -> { }
        }

        drawMesh(buffer.getBuffer(RenderType.entityCutoutNoCull(TEX_CHASSIS)),
                poseStack.last().pose(), chassis, packedLight);

        poseStack.pushPose();
        // Gedreht wird um einen Punkt, der vor dem Rohrende liegt; der Behaelter wird davor
        // entsprechend zurueckgeschoben. Dadurch sinkt er beim Aufrichten auf die Startplatte
        // statt hinter das Fahrzeug zu schwenken - siehe HINGE_OFFSET.
        poseStack.translate(PIVOT_X,
                TopolLaunchPadBlockEntity.PIVOT_HEIGHT,
                -TopolLaunchPadBlockEntity.PIVOT_FORWARD + TopolLaunchPadBlockEntity.HINGE_OFFSET);
        float progress = be.getErectorProgress(partialTicks);
        if (progress > 0.0F) {
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F * progress));
        }
        poseStack.translate(0.0D, 0.0D, -TopolLaunchPadBlockEntity.HINGE_OFFSET);
        drawMesh(buffer.getBuffer(RenderType.entityCutoutNoCull(TEX_CANISTER)),
                poseStack.last().pose(), canister, packedLight);

        // Auswurf: die geladene Rakete faehrt entlang der Behaelterachse aus dem Rohr. Gezeichnet
        // wird sie im Koordinatensystem des Behaelters, sie folgt also seiner Neigung mit.
        drawEjectingMissile(be, partialTicks, poseStack, buffer, packedLight);

        poseStack.popPose();
        poseStack.popPose();
    }

    /**
     * Zeichnet die Rakete waehrend des Auswurfs.
     *
     * <p>Der Weg ist quadratisch ueber der Zeit - der Gasgenerator schiebt sie an, sie wird also
     * schneller, je weiter sie heraus ist. Im Rohr selbst sieht man von ihr nichts, weil sie erst
     * bei einem Viertel des Auswurfs die Muendung erreicht.
     */
    private static void drawEjectingMissile(TopolLaunchPadBlockEntity be, float partialTicks,
                                            PoseStack poseStack, MultiBufferSource buffer, int light) {
        float progress = be.getEjectProgress(partialTicks);
        if (progress < 0.0F) {
            return;
        }

        ItemStack missileStack = be.getMissilePreviewStack();
        if (missileStack.isEmpty() || !(missileStack.getItem() instanceof MissileItem)) {
            return;
        }
        MissileRenderData data = MissileRenderRegistry.get(missileStack);
        if (data == null) {
            return;
        }

        // Beschleunigt, aber nicht so hart wie ein Quadrat: bei p² passiert vier Fuenftel der
        // Bewegung in der letzten halben Sekunde, und der Auswurf sieht aus wie ein Aufploppen.
        float eased = (float) Math.pow(progress, 1.4D);
        float offset = Mth.lerp(eased, EJECT_START, EJECT_END);

        poseStack.pushPose();
        // Auf die Rohrachse heben: der Behaelterursprung liegt an seiner Unterkante.
        poseStack.translate(0.0D, TopolLaunchPadBlockEntity.CANISTER_RADIUS, offset);
        // Das Raketenmodell steht auf seiner Y-Achse; im Behaelterraum liegt die Achse auf +Z.
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        data.render(poseStack, light, be.getBlockPos(), buffer, be);
        poseStack.popPose();
    }

    /**
     * Wie {@code RBMKColumnRenderer.renderObjGroup}, aber ohne Atlas-Sprite: die UVs der Modelle
     * gehen unveraendert durch, nur V wird gespiegelt (Blender zaehlt von unten, die PNG von oben).
     */
    private static void drawMesh(VertexConsumer vc, Matrix4f matrix,
                                 Map<String, List<float[]>> obj, int light) {
        for (List<float[]> triangles : obj.values()) {
            if (triangles == null) {
                continue;
            }
            for (float[] tri : triangles) {
                for (int pass = 0; pass < 4; pass++) {   // vierter Durchlauf doppelt den dritten
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
}
