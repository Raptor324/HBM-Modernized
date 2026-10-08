package com.hbm_m.client.render.armor;

import com.hbm_m.platform.StackNbt;

import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;

import com.hbm_m.armormod.item.JetpackBase;
import com.hbm_m.armormod.item.WingsMurk;
import com.hbm_m.armormod.util.ArmorModificationHelper;
import com.hbm_m.item.ModItems;
import com.hbm_m.powerarmor.ArmorAshGlasses;
import com.hbm_m.powerarmor.ArmorHat;
import com.hbm_m.powerarmor.ArmorModel;
import com.hbm_m.powerarmor.ArmorNo9;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Zubehoer-Modelle des Originals: {@code ModelGoggles} (goggles), {@code ModelGlasses} (ashglasses),
 * {@code ModelHat} (nossy_hat), {@code ModelNo9} (mit Lampe bei "isOn"), {@code ModelCloak} (Umhaenge),
 * {@code ModelJetPack} (Jetpacks, auch als eingebaute Brustplatten-Mod) und {@code ModelArmorWings}.
 */
public class ArmorAccessoryLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {

    private static final float PX = 0.0625F;
    private static final ResourceLocation WHITE = ResourceLocation.tryParse("minecraft:textures/misc/white.png");

    private ModelPart goggles;
    private ModelPart jetpack;

    public ArmorAccessoryLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    private void bake() {
        if (goggles != null) return;
        var models = Minecraft.getInstance().getEntityModels();
        goggles = models.bakeLayer(ArmorAccessoryModels.GOGGLES).getChild("google");
        jetpack = models.bakeLayer(ArmorAccessoryModels.JETPACK).getChild("jetpack");
    }

    private static ResourceLocation rl(String path) {
        return ResourceLocation.tryParse(path);
    }

    @Override
    public void render(@NotNull PoseStack pose, @NotNull MultiBufferSource buffer, int light, @NotNull T entity,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        bake();
        M m = getParentModel();

        ItemStack head = entity.getItemBySlot(EquipmentSlot.HEAD);
        if (!head.isEmpty()) {
            if (head.getItem() == ModItems.GOGGLES.get()) {
                pose.pushPose();
                m.head.translateAndRotate(pose);
                goggles.render(pose, buffer.getBuffer(RenderType.armorCutoutNoCull(rl("hbm_m:textures/models/goggles.png"))), light, OverlayTexture.NO_OVERLAY);
                pose.popPose();
            } else if (head.getItem() instanceof ArmorAshGlasses) {
                objOnHead(pose, buffer, light, m, "goggles", null, "hbm_m:textures/armor/goggles.png");
            } else if (head.getItem() instanceof ArmorHat) {
                objOnHead(pose, buffer, light, m, "hat", null, "hbm_m:textures/armor/hat.png");
            } else if (head.getItem() instanceof ArmorNo9) {
                objOnHead(pose, buffer, light, m, "no9", "Helmet", "hbm_m:textures/armor/no9.png");
                objOnHead(pose, buffer, light, m, "no9", "Insignia", "hbm_m:textures/armor/no9_insignia.png");
                if (StackNbt.has(head) && StackNbt.read(head).getBoolean("isOn")) {
                    pose.pushPose();
                    headTransform(pose, m.head);
                    ArmorObjModel.get(rl("hbm_m:models/armor/no9.obj")).renderPart("Flame", pose,
                            buffer.getBuffer(RenderType.entityTranslucent(WHITE)), 0xF000F0, 1F, 1F, 0.8F, 1F);
                    pose.popPose();
                }
            }
        }

        ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
        if (!chest.isEmpty()) {
            if (chest.getItem() instanceof ArmorModel model && (chest.getItem() == ModItems.CAPE_RADIATION.get() || chest.getItem() == ModItems.CAPE_GASMASK.get()
                    || chest.getItem() == ModItems.CAPE_SCHRABIDIUM.get() || chest.getItem() == ModItems.CAPE_HIDDEN.get())) {
                //? if forge {
                renderCloak(pose, buffer, light, entity, model.getArmorTexture(chest, entity, EquipmentSlot.CHEST, null));
                //?} else {
                /*renderCloak(pose, buffer, light, entity, cloakTexture(chest));
                *///?}
            }

            // Original ItemModTesla.modRender -> ModelBackTesla (Rueckenspule, folgt dem Koerper)
            ItemStack plateMod = ArmorModificationHelper.pryMods(chest)[ArmorModificationHelper.plate_only];
            if (plateMod != null && plateMod.getItem() instanceof com.hbm_m.armormod.item.ItemModTesla) {
                objOnBody(pose, buffer, light, m, "mod_tesla", null, "hbm_m:textures/armor/mod_tesla.png");
            }

            // Allein getragen oder als Brustplatten-Mod (Original JetpackBase.modRender)
            ItemStack jet = chest.getItem() instanceof JetpackBase ? chest : ArmorModificationHelper.pryMods(chest)[ArmorModificationHelper.plate_only];
            if (jet != null && !jet.isEmpty() && jet.getItem() instanceof JetpackBase pack) {
                if (pack instanceof WingsMurk) {
                    renderWings(pose, buffer, light, entity, m, jet.getItem() == ModItems.WINGS_MURK.get() ? 0 : 1, pack.getModelTexture());
                } else {
                    pose.pushPose();
                    m.body.translateAndRotate(pose);
                    jetpack.render(pose, buffer.getBuffer(RenderType.armorCutoutNoCull(rl(pack.getModelTexture()))), light, OverlayTexture.NO_OVERLAY);
                    pose.popPose();
                }
            }
        }

        // Original ModEventHandlerClient.onRenderArmorEvent: Spielerzubehoer nur ohne Brustplatte und ohne Unsichtbarkeit
        if (chest.isEmpty() && entity instanceof net.minecraft.world.entity.player.Player player
                && !player.hasEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY)) {
            if (com.hbm_m.util.ShadyUtil.is(player, com.hbm_m.util.ShadyUtil.HbMinecraft, "HbMinecraft"))
                renderWings(pose, buffer, light, entity, m, 2, "hbm_m:textures/armor/wings_bob.png");
            if (com.hbm_m.util.ShadyUtil.is(player, com.hbm_m.util.ShadyUtil.the_NCR, "the_NCR"))
                renderWings(pose, buffer, light, entity, m, 3, "hbm_m:textures/armor/wings_black.png");
            if (com.hbm_m.util.ShadyUtil.is(player, com.hbm_m.util.ShadyUtil.Barnaby99_x, "pheo7"))
                renderAxePack(pose, buffer, light, entity, m);
            if (com.hbm_m.util.ShadyUtil.is(player, com.hbm_m.util.ShadyUtil.LePeeperSauvage, "LePeeperSauvage"))
                objOnBody(pose, buffer, light, m, "tail_peep", "FaggyAssFuckingTailThing", "hbm_m:textures/armor/tail_peep.png");
        }
    }

    /** ModelRendererObj mit den Koerperwinkeln ({@code body.copyTo(obj)}, dann {@code obj.render(0.0625)}). */
    private void objOnBody(PoseStack pose, MultiBufferSource buffer, int light, M m, String obj, String part, String tex) {
        pose.pushPose();
        headTransform(pose, m.body);
        ArmorObjModel model = ArmorObjModel.get(rl("hbm_m:models/armor/" + obj + ".obj"));
        var vc = buffer.getBuffer(RenderType.armorCutoutNoCull(rl(tex)));
        if (part == null) model.renderAll(pose, vc, light, 1F, 1F, 1F, 1F);
        else model.renderPart(part, pose, vc, light, 1F, 1F, 1F, 1F);
        pose.popPose();
    }

    /**
     * Original {@code ModelArmorWingsPheo.render}: Axtrucksack am Koerper, dazu sechs additive Duesenflammen
     * (vollhell, ohne Textur) im Modellraum, beim Schleichen um 0.5 rad (28.6479 Grad) gekippt.
     */
    private void renderAxePack(PoseStack pose, MultiBufferSource buffer, int light, T entity, M m) {
        objOnBody(pose, buffer, light, m, "wings_pheo", "Wings", "hbm_m:textures/armor/axepack.png");

        var vc = buffer.getBuffer(com.hbm_m.client.ClientRenderHandler.CustomRenderTypes.ADDITIVE_TRIANGLES);
        double pixel = 0.0625D;

        pose.pushPose();
        if (entity.isCrouching()) pose.mulPose(Axis.XP.rotationDegrees(28.6479F));

        pose.pushPose();
        pose.translate(0, pixel * 15, pixel * 5.5);
        renderFlame(pose, vc);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, pixel * 3, pixel * 5.5);
        pose.mulPose(Axis.YP.rotationDegrees(-25));
        pose.mulPose(Axis.ZP.rotationDegrees(-90));
        pose.translate(0, pixel * 5, 0);
        renderFlame(pose, vc);
        pose.pushPose();
        pose.translate(0, -pixel * 5, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(45));
        pose.translate(-pixel, pixel * 5.5, 0);
        renderFlame(pose, vc);
        pose.popPose();
        pose.pushPose();
        pose.translate(0, -pixel * 5, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(-45));
        pose.translate(pixel, pixel * 5.5, 0);
        renderFlame(pose, vc);
        pose.popPose();
        pose.popPose();

        pose.pushPose();
        pose.translate(0, pixel * 15, pixel * 5.5);
        renderFlame(pose, vc);
        pose.popPose();

        pose.pushPose();
        pose.translate(0, pixel * 3, pixel * 5.5);
        pose.mulPose(Axis.YP.rotationDegrees(25));
        pose.mulPose(Axis.ZP.rotationDegrees(90));
        pose.translate(0, pixel * 5, 0);
        renderFlame(pose, vc);
        pose.pushPose();
        pose.translate(0, -pixel * 5, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(45));
        pose.translate(-pixel, pixel * 5.5, 0);
        renderFlame(pose, vc);
        pose.popPose();
        pose.pushPose();
        pose.translate(0, -pixel * 5, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(-45));
        pose.translate(pixel, pixel * 5.5, 0);
        renderFlame(pose, vc);
        pose.popPose();
        pose.popPose();

        pose.popPose();
    }

    /** Original {@code ModelArmorWingsPheo.renderFlame}: achtseitiger Doppelkegel, Fuss grau, Mitte tuerkis, Spitze schwarz. */
    private static void renderFlame(PoseStack pose, com.mojang.blaze3d.vertex.VertexConsumer vc) {
        org.joml.Matrix4f mat = pose.last().pose();
        float b = 0.125F;
        float t = 0.375F;
        float w = 0.0625F;
        float s2 = (float) (Math.sqrt(2D) / 2D * w);
        float[][] ring = { { w, 0 }, { s2, s2 }, { 0, w }, { -s2, s2 }, { -w, 0 }, { -s2, -s2 }, { 0, -w }, { s2, -s2 } };
        for (int k = 0; k < 2; k++) {
            float tipY = k == 0 ? 0F : t;
            int tip = k == 0 ? 0x80 : 0x00; // colorBase 0x808080 / colorTip 0x000000
            for (int i = 0; i < 8; i++) {
                float[] a = ring[i], c = ring[(i + 1) % 8];
                com.hbm_m.platform.RenderHooks.vertexColor(vc, mat, 0F, tipY, 0F, tip, tip, tip, 255);
                com.hbm_m.platform.RenderHooks.vertexColor(vc, mat, a[0], b, a[1], 0x00, 0x40, 0x40, 255);
                com.hbm_m.platform.RenderHooks.vertexColor(vc, mat, c[0], b, c[1], 0x00, 0x40, 0x40, 255);
            }
        }
    }

    /** ModelRendererObj mit den Kopfwinkeln (Gelenkpunkt 0,0,0). */
    private static void headTransform(PoseStack pose, ModelPart head) {
        pose.translate(head.x * PX, head.y * PX, head.z * PX);
        if (head.zRot != 0.0F || head.yRot != 0.0F || head.xRot != 0.0F)
            pose.mulPose(new Quaternionf().rotationZYX(head.zRot, head.yRot, head.xRot));
        pose.scale(PX, PX, PX);
    }

    private void objOnHead(PoseStack pose, MultiBufferSource buffer, int light, M m, String obj, String part, String tex) {
        pose.pushPose();
        headTransform(pose, m.head);
        ArmorObjModel model = ArmorObjModel.get(rl("hbm_m:models/armor/" + obj + ".obj"));
        var vc = buffer.getBuffer(RenderType.armorCutoutNoCull(rl(tex)));
        if (part == null) model.renderAll(pose, vc, light, 1F, 1F, 1F, 1F);
        else model.renderPart(part, pose, vc, light, 1F, 1F, 1F, 1F);
        pose.popPose();
    }

    /** Original ModelCloak.render (samt der Interpolation mit dem Skalierungsfaktor 0,0625 als Gewicht). */
    //? if neoforge {
    /*/^* Auf NeoForge hat ArmorModel kein String-getArmorTexture mehr; gleiche Zuordnung wie ArmorModel (forge). ^/
    private static String cloakTexture(ItemStack stack) {
        if (stack.getItem() == ModItems.CAPE_RADIATION.get()) return "hbm_m:textures/models/capes/caperadiation.png";
        if (stack.getItem() == ModItems.CAPE_GASMASK.get()) return "hbm_m:textures/models/capes/capegasmask.png";
        if (stack.getItem() == ModItems.CAPE_SCHRABIDIUM.get()) return "hbm_m:textures/models/capes/capeschrabidium.png";
        if (stack.getItem() == ModItems.CAPE_HIDDEN.get()) return "hbm_m:textures/models/capes/capehidden.png";
        return "hbm_m:textures/models/capes/capeunknown.png";
    }
    *///?}

    private void renderCloak(PoseStack pose, MultiBufferSource buffer, int light, T entity, String texture) {
        if (!(entity instanceof AbstractClientPlayer player) || !(getParentModel() instanceof PlayerModel<?> playerModel)) return;
        float s = 0.0625F;

        pose.pushPose();
        pose.translate(0.0F, 0.0F, 0.125F);
        double d3 = player.xCloakO + (player.xCloak - player.xCloakO) * s - (player.xo + (player.getX() - player.xo) * s);
        double d4 = player.yCloakO + (player.yCloak - player.yCloakO) * s - (player.yo + (player.getY() - player.yo) * s);
        double d0 = player.zCloakO + (player.zCloak - player.zCloakO) * s - (player.zo + (player.getZ() - player.zo) * s);
        float f4 = player.yBodyRotO + (player.yBodyRot - player.yBodyRotO) * s;
        double d1 = Mth.sin(f4 * (float) Math.PI / 180.0F);
        double d2 = -Mth.cos(f4 * (float) Math.PI / 180.0F);
        float f5 = (float) d4 * 10.0F;

        if (f5 < -6.0F) f5 = -6.0F;
        if (f5 > 32.0F) f5 = 32.0F;

        float f6 = (float) (d3 * d1 + d0 * d2) * 100.0F;
        float f7 = (float) (d3 * d2 - d0 * d1) * 100.0F;

        if (f6 < 0.0F) f6 = 0.0F;

        float f8 = player.oBob + (player.bob - player.oBob) * s;
        f5 += Mth.sin((player.walkDistO + (player.walkDist - player.walkDistO) * s) * 6.0F) * 32.0F * f8;

        if (player.isCrouching()) f5 += 25.0F;

        pose.mulPose(Axis.XP.rotationDegrees(6.0F + f6 / 2.0F + f5));
        pose.mulPose(Axis.ZP.rotationDegrees(f7 / 2.0F));
        pose.mulPose(Axis.YP.rotationDegrees(-f7 / 2.0F));
        pose.mulPose(Axis.YP.rotationDegrees(180.0F));
        playerModel.renderCloak(pose, buffer.getBuffer(RenderType.entitySolid(rl(texture))), light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    /** Original ModelArmorWings.render. */
    private void renderWings(PoseStack pose, MultiBufferSource buffer, int light, T entity, M m, int type, String texture) {
        ArmorObjModel model = ArmorObjModel.get(rl("hbm_m:models/armor/murk.obj"));
        var vc = buffer.getBuffer(RenderType.entityCutout(rl(texture)));

        double px = 0.0625D;
        double rot = Math.sin((entity.tickCount) * 0.2D) * 20;
        double rot2 = Math.sin((entity.tickCount) * 0.2D - Math.PI * 0.5) * 50 + 30;

        int pivotSideOffset = 1;
        int pivotFrontOffset = 5;
        int pivotZOffset = 3;
        int tipSideOffset = 16;
        int tipZOffset = 2;
        double inwardAngle = 10D;

        ModelPart body = m.body;
        pose.pushPose();
        pose.translate(body.x * px, body.y * px, body.z * px);
        if (body.zRot != 0.0F || body.yRot != 0.0F || body.xRot != 0.0F)
            pose.mulPose(new Quaternionf().rotationZYX(body.zRot, body.yRot, body.xRot));

        if (type != 1 && entity.onGround()) {
            rot = 20;
            rot2 = 160;
        }

        if (type == 1) {
            if (entity.onGround()) {
                rot = 30;
                rot2 = -30;
            } else if (entity.getDeltaMovement().y < -0.1) {
                rot = 0;
                rot2 = 10;
            } else {
                rot = 30;
                rot2 = 20;
            }
        }

        pose.translate(0, -2 * px, 0);

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees((float) -inwardAngle));
        pose.translate(pivotSideOffset * px, pivotFrontOffset * px, pivotZOffset * px);
        pose.mulPose(Axis.YP.rotationDegrees((float) (rot * 0.5)));
        pose.mulPose(Axis.ZP.rotationDegrees((float) (rot + 5)));
        pose.mulPose(Axis.XP.rotationDegrees(45));
        pose.translate(-pivotSideOffset * px, -pivotFrontOffset * px, -pivotZOffset * px);
        pose.translate(pivotSideOffset * px, pivotFrontOffset * px, pivotZOffset * px);
        pose.mulPose(Axis.ZP.rotationDegrees((float) rot));
        pose.translate(-pivotSideOffset * px, -pivotFrontOffset * px, -pivotZOffset * px);
        part(model, "LeftBase", pose, vc, light);
        pose.translate(tipSideOffset * px, pivotFrontOffset * px, tipZOffset * px);
        pose.mulPose(Axis.YP.rotationDegrees((float) rot2));
        pose.mulPose(Axis.ZP.rotationDegrees((float) (rot2 * 0.25 + 5)));
        pose.translate(-tipSideOffset * px, -pivotFrontOffset * px, -tipZOffset * px);
        part(model, "LeftTip", pose, vc, light);
        pose.popPose();

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees((float) inwardAngle));
        pose.translate(-pivotSideOffset * px, pivotFrontOffset * px, pivotZOffset * px);
        pose.mulPose(Axis.YP.rotationDegrees((float) (-rot * 0.5)));
        pose.mulPose(Axis.ZP.rotationDegrees((float) (-rot - 5)));
        pose.mulPose(Axis.XP.rotationDegrees(45));
        pose.translate(pivotSideOffset * px, -pivotFrontOffset * px, -pivotZOffset * px);
        pose.translate(-pivotSideOffset * px, pivotFrontOffset * px, pivotZOffset * px);
        pose.mulPose(Axis.ZP.rotationDegrees((float) -rot));
        pose.translate(pivotSideOffset * px, -pivotFrontOffset * px, -pivotZOffset * px);
        part(model, "RightBase", pose, vc, light);
        pose.translate(-tipSideOffset * px, pivotFrontOffset * px, tipZOffset * px);
        pose.mulPose(Axis.YP.rotationDegrees((float) -rot2));
        pose.mulPose(Axis.ZP.rotationDegrees((float) (-rot2 * 0.25 - 5)));
        pose.translate(tipSideOffset * px, -pivotFrontOffset * px, -tipZOffset * px);
        part(model, "RightTip", pose, vc, light);
        pose.popPose();

        pose.popPose();
    }

    private static void part(ArmorObjModel model, String name, PoseStack pose, com.mojang.blaze3d.vertex.VertexConsumer vc, int light) {
        pose.pushPose();
        pose.scale(PX, PX, PX);
        model.renderPart(name, pose, vc, light, 1F, 1F, 1F, 1F);
        pose.popPose();
    }
}
