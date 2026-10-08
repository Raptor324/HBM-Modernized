//? if forge {
package com.hbm_m.client.render;

import com.hbm_m.client.render.armor.ArmorObjModel;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.mixin.LivingEntityRendererInvoker;
import com.hbm_m.network.PermaSyncMemePacket;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;

/**
 * 1:1 {@code ModelMan} + {@code ModEventHandlerRenderer}: Spieler aus {@link PermaSyncMemePacket#boykissers}
 * werden statt mit dem Skin mit {@code models/armor/player_fem.obj} / {@code textures/entity/player_fem.png}
 * gezeichnet. {@code onRenderPlayerPre} blendet alle Skin-Teile aus, die Teile von {@code ModelMan} uebernehmen
 * Drehung und Gelenkpunkt des Spielermodells ({@code copyRotationFrom}) und werden wie
 * {@code ModelRendererObj.render} gezeichnet. Das Halten von Gegenstaenden ({@code onRenderHeldItem}) haengt am
 * rechten Arm mit gleicher Lage wie Vanilla und bleibt deshalb bei der {@code ItemInHandLayer}.
 */
public class ModelManLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private static final float PX = 0.0625F;
    private static final ResourceLocation TEX = ResourceLocation.tryParse(MainRegistry.MOD_ID + ":textures/entity/player_fem.png");
    private static final ResourceLocation OBJ = ResourceLocation.tryParse(MainRegistry.MOD_ID + ":models/armor/player_fem.obj");

    public ModelManLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    public static boolean isManly(AbstractClientPlayer player) {
        return PermaSyncMemePacket.boykissers.contains(player.getId());
    }

    @Override
    public void render(@NotNull PoseStack ps, @NotNull MultiBufferSource buffer, int light, @NotNull AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!isManly(player)) return;

        PlayerModel<AbstractClientPlayer> m = getParentModel();
        ArmorObjModel model = ArmorObjModel.get(OBJ);
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(TEX));

        part(model, ps, vc, light, "Head", m.head, 0.0F, 0.0F, 0.0F);
        part(model, ps, vc, light, "Body", m.body, 0.0F, 0.0F, 0.0F);
        part(model, ps, vc, light, "LeftArm", m.leftArm, 5.0F, 2.0F, 0.0F);
        part(model, ps, vc, light, "RightArm", m.rightArm, -5.0F, 2.0F, 0.0F);
        part(model, ps, vc, light, "LeftLeg", m.leftLeg, 1.9F, 12.0F, 0.0F);
        part(model, ps, vc, light, "RightLeg", m.rightLeg, -1.9F, 12.0F, 0.0F);
    }

    /** {@code ModelRendererObj.render}: Gelenkpunkt, Drehung Z-Y-X, Ursprung zurueck, Skala 1/16. */
    private static void part(ArmorObjModel model, PoseStack ps, VertexConsumer vc, int light, String name,
                             ModelPart bone, float ox, float oy, float oz) {
        ps.pushPose();
        ps.translate(bone.x * PX, bone.y * PX, bone.z * PX);
        if (bone.zRot != 0.0F || bone.yRot != 0.0F || bone.xRot != 0.0F)
            ps.mulPose(new Quaternionf().rotationZYX(bone.zRot, bone.yRot, bone.xRot));
        ps.translate(-ox * PX, -oy * PX, -oz * PX);
        ps.scale(PX, PX, PX);
        model.renderPart(name, ps, vc, light, 1F, 1F, 1F, 1F);
        ps.popPose();
    }

    /** Original {@code onRenderPlayerPre} / {@code onRenderPlayerPost}: Skin-Teile aus- und wieder einblenden. */
    @Mod.EventBusSubscriber(modid = MainRegistry.MOD_ID, value = Dist.CLIENT)
    public static final class ForgeEvents {

        private static final boolean[] partsHidden = new boolean[12];

        private static ModelPart[] parts(PlayerModel<?> m) {
            return new ModelPart[] { m.head, m.hat, m.body, m.jacket, m.leftArm, m.leftSleeve, m.rightArm, m.rightSleeve,
                    m.leftLeg, m.leftPants, m.rightLeg, m.rightPants };
        }

        @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
        public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
            if (!(event.getEntity() instanceof AbstractClientPlayer player)) return;
            ModelPart[] p = parts(event.getRenderer().getModel());
            boolean manly = isManly(player);
            for (int j = 0; j < p.length; j++) {
                partsHidden[j] = false;
                if (manly && p[j].visible) {
                    partsHidden[j] = true;
                    p[j].visible = false;
                }
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
        public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
            ModelPart[] p = parts(event.getRenderer().getModel());
            for (int j = 0; j < p.length; j++) {
                if (partsHidden[j]) {
                    p[j].visible = true;
                    partsHidden[j] = false;
                }
            }
        }

        private ForgeEvents() {}
    }

    @Mod.EventBusSubscriber(modid = MainRegistry.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModEvents {

        @SubscribeEvent
        @SuppressWarnings({"unchecked", "rawtypes"})
        public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
            for (String skin : event.getSkins()) {
                LivingEntityRenderer renderer = event.getSkin(skin);
                if (renderer == null) continue;
                ((LivingEntityRendererInvoker) renderer).hbm_m$getLayers().add(new ModelManLayer((RenderLayerParent) renderer));
            }
        }

        private ModEvents() {}
    }
}
//?}
