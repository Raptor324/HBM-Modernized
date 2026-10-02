package com.hbm_m.client.render.armor;

import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;

import com.hbm_m.powerarmor.ArmorBJ;
import com.hbm_m.powerarmor.ArmorBJJetpack;
import com.hbm_m.powerarmor.ArmorDesh;
import com.hbm_m.powerarmor.ArmorDiesel;
import com.hbm_m.powerarmor.ArmorDigamma;
import com.hbm_m.powerarmor.ArmorEnvsuit;
import com.hbm_m.powerarmor.ArmorHEV;
import com.hbm_m.powerarmor.ArmorNCRPA;
import com.hbm_m.powerarmor.ArmorRPA;
import com.hbm_m.powerarmor.ArmorTaurun;
import com.hbm_m.powerarmor.ArmorTrenchmaster;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Die OBJ-Ruestungsmodelle des Originals ({@code ModelArmorBJ}, {@code ModelArmorDesh}, {@code ModelArmorDiesel},
 * {@code ModelArmorDigamma}, {@code ModelArmorEnvsuit}, {@code ModelArmorHEV}, {@code ModelArmorNCRPA},
 * {@code ModelArmorRPA}, {@code ModelArmorTaurun}, {@code ModelArmorTrenchmaster}) als Render-Layer: jedes
 * Teil wird wie {@code ModelRendererObj.render} um den aktuellen Gelenkpunkt gedreht und mit 1/16 skaliert.
 */
public class ObjArmorLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {

    private static final float PX = 0.0625F;
    private static final int FULLBRIGHT = 0xF000F0;

    public ObjArmorLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    private static ResourceLocation tex(String name) {
        return ResourceLocation.tryParse("hbm_m:textures/armor/" + name + ".png");
    }

    private static ResourceLocation obj(String name) {
        return ResourceLocation.tryParse("hbm_m:models/armor/" + name + ".obj");
    }

    private static final ResourceLocation WHITE = ResourceLocation.tryParse("minecraft:textures/misc/white.png");

    private enum Mode { NORMAL, BLEND, GLOW }

    /** Zeichenkontext eines Ruestungsteils (entspricht bindTexture + GL-Zustand + ModelRendererObj.render). */
    private final class Ctx {
        PoseStack pose;
        MultiBufferSource buffer;
        int light;
        ArmorObjModel model;
        ResourceLocation texture;
        Mode mode = Mode.NORMAL;
        float r = 1, g = 1, b = 1;

        void bind(ResourceLocation t) { texture = t; }

        void render(String part, ModelPart bone, float ox, float oy, float oz) {
            pose.pushPose();
            pose.translate(bone.x * PX, bone.y * PX, bone.z * PX);
            if (bone.zRot != 0.0F || bone.yRot != 0.0F || bone.xRot != 0.0F)
                pose.mulPose(new Quaternionf().rotationZYX(bone.zRot, bone.yRot, bone.xRot));
            pose.translate(-ox * PX, -oy * PX, -oz * PX);
            pose.scale(PX, PX, PX);
            RenderType type = switch (mode) {
                case NORMAL -> RenderType.armorCutoutNoCull(texture);
                case BLEND -> RenderType.entityTranslucent(texture);
                case GLOW -> RenderType.entityTranslucent(texture);
            };
            model.renderPart(part, pose, buffer.getBuffer(type), mode == Mode.GLOW ? FULLBRIGHT : light, r, g, b, 1F);
            pose.popPose();
        }
    }

    @Override
    public void render(@NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight, @NotNull T entity,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        renderSlot(poseStack, buffer, packedLight, entity, EquipmentSlot.HEAD, 0);
        renderSlot(poseStack, buffer, packedLight, entity, EquipmentSlot.CHEST, 1);
        renderSlot(poseStack, buffer, packedLight, entity, EquipmentSlot.LEGS, 2);
        renderSlot(poseStack, buffer, packedLight, entity, EquipmentSlot.FEET, 3);
    }

    private void renderSlot(PoseStack poseStack, MultiBufferSource buffer, int light, T entity, EquipmentSlot slot, int type) {
        ItemStack stack = entity.getItemBySlot(slot);
        if (stack.isEmpty()) return;
        Item item = stack.getItem();

        Ctx c = new Ctx();
        c.pose = poseStack;
        c.buffer = buffer;
        c.light = light;
        M m = getParentModel();

        poseStack.pushPose();
        if (item instanceof ArmorBJ) {
            renderBJ(c, m, item instanceof ArmorBJJetpack ? 5 : type);
        } else if (item instanceof ArmorDesh) {
            standard(c, m, type, "steamsuit", "steamsuit", "Head", "Body", "LeftBoot", "RightBoot");
        } else if (item instanceof ArmorDiesel) {
            standard(c, m, type, "bnuuy", "bnuuy", "Head", "Body", "LeftBoot", "RightBoot");
        } else if (item instanceof ArmorDigamma) {
            renderFau(c, m, type);
        } else if (item instanceof ArmorEnvsuit) {
            renderEnvsuit(c, m, type);
        } else if (item instanceof ArmorHEV) {
            standard(c, m, type, "hev", "hev", "Head", "Body", "LeftFoot", "RightFoot");
        } else if (item instanceof ArmorNCRPA) {
            renderNCRPA(c, m, type);
        } else if (item instanceof ArmorRPA) {
            renderRPA(c, m, type);
        } else if (item instanceof ArmorTaurun) {
            trench(c, m, type, "taurun", false);
        } else if (item instanceof ArmorTrenchmaster) {
            trench(c, m, type, "trenchmaster", true);
        }
        poseStack.popPose();
    }

    private void arms(Ctx c, M m) {
        c.render("LeftArm", m.leftArm, 5.0F, 2.0F, 0.0F);
        c.render("RightArm", m.rightArm, -5.0F, 2.0F, 0.0F);
    }

    private void legs(Ctx c, M m, String left, String right) {
        c.render(left, m.leftLeg, 1.9F, 12.0F, 0.0F);
        c.render(right, m.rightLeg, -1.9F, 12.0F, 0.0F);
    }

    /** ModelArmorDesh / Diesel / HEV: Kopf, Rumpf+Arme, Beine, Fuesse mit je einer Textur. */
    private void standard(Ctx c, M m, int type, String objName, String texPrefix, String head, String body, String leftFoot, String rightFoot) {
        c.model = ArmorObjModel.get(obj(objName));
        if (type == 0) {
            c.bind(tex(texPrefix + "_helmet"));
            c.render(head, m.head, 0, 0, 0);
        }
        if (type == 1) {
            c.bind(tex(texPrefix + "_chest"));
            c.render(body, m.body, 0, 0, 0);
            c.bind(tex(texPrefix + "_arm"));
            arms(c, m);
        }
        if (type == 2) {
            c.bind(tex(texPrefix + "_leg"));
            legs(c, m, "LeftLeg", "RightLeg");
        }
        if (type == 3) {
            c.bind(tex(texPrefix + "_leg"));
            legs(c, m, leftFoot, rightFoot);
        }
    }

    private void renderBJ(Ctx c, M m, int type) {
        c.model = ArmorObjModel.get(obj("bj"));
        if (type == 0) {
            c.bind(tex("bj_eyepatch"));
            c.render("Head", m.head, 0, 0, 0);
        }
        if (type == 1 || type == 5) {
            c.bind(tex("bj_chest"));
            c.render("Body", m.body, 0, 0, 0);
            if (type == 5) {
                c.bind(tex("bj_jetpack"));
                c.render("Jetpack", m.body, 0, 0, 0);
            }
            c.bind(tex("bj_arm"));
            arms(c, m);
        }
        if (type == 2) {
            c.bind(tex("bj_leg"));
            legs(c, m, "LeftLeg", "RightLeg");
        }
        if (type == 3) {
            c.bind(tex("bj_leg"));
            legs(c, m, "LeftFoot", "RightFoot");
        }
    }

    private void renderFau(Ctx c, M m, int type) {
        c.model = ArmorObjModel.get(obj("fau"));
        if (type == 0) {
            c.bind(tex("fau_helmet"));
            c.render("Head", m.head, 0, 0, 0);
        }
        if (type == 1) {
            c.bind(tex("fau_chest"));
            c.render("Body", m.body, 0, 0, 0);
            c.mode = Mode.BLEND; // Original: GL_BLEND bleibt bis zum Ende des Brustteils an
            c.bind(tex("fau_cassette"));
            c.render("Cassette", m.body, 0, 0, 0);
            c.bind(tex("fau_arm"));
            arms(c, m);
            c.mode = Mode.NORMAL;
        }
        if (type == 2) {
            c.bind(tex("fau_leg"));
            legs(c, m, "LeftLeg", "RightLeg");
        }
        if (type == 3) {
            c.bind(tex("fau_leg"));
            legs(c, m, "LeftBoot", "RightBoot");
        }
    }

    private void renderEnvsuit(Ctx c, M m, int type) {
        c.model = ArmorObjModel.get(obj("envsuit"));
        if (type == 0) {
            c.bind(tex("envsuit_helmet"));
            c.mode = Mode.BLEND;
            c.render("Helmet", m.head, 0, 0, 0);
            /// START GLOW ///
            c.mode = Mode.GLOW;
            c.bind(WHITE);
            c.r = 1F; c.g = 1F; c.b = 0.8F;
            c.render("Lamps", m.head, 0, 0, 0);
            c.r = 1F; c.g = 1F; c.b = 1F;
            c.mode = Mode.NORMAL;
            /// END GLOW ///
        }
        if (type == 1) {
            c.bind(tex("envsuit_chest"));
            c.render("Chest", m.body, 0, 0, 0);
            c.bind(tex("envsuit_arm"));
            arms(c, m);
        }
        if (type == 2) {
            c.bind(tex("envsuit_leg"));
            legs(c, m, "LeftLeg", "RightLeg");
        }
        if (type == 3) {
            c.bind(tex("envsuit_leg"));
            legs(c, m, "LeftFoot", "RightFoot");
        }
    }

    private void renderNCRPA(Ctx c, M m, int type) {
        c.model = ArmorObjModel.get(obj("ncrpa"));
        if (type == 0) {
            c.bind(tex("ncrpa_helmet"));
            c.render("Helmet", m.head, 0, 0, 0);
            c.mode = Mode.GLOW;
            c.render("Eyes", m.head, 0, 0, 0);
            c.mode = Mode.NORMAL;
        }
        if (type == 1) {
            c.bind(tex("ncrpa_arm"));
            arms(c, m);
            c.bind(tex("ncrpa_chest"));
            c.render("Chest", m.body, 0, 0, 0);
        }
        if (type == 2) {
            c.bind(tex("ncrpa_leg"));
            legs(c, m, "LeftLeg", "RightLeg");
        }
        if (type == 3) {
            c.bind(tex("ncrpa_leg"));
            legs(c, m, "LeftBoot", "RightBoot");
        }
    }

    private void renderRPA(Ctx c, M m, int type) {
        c.model = ArmorObjModel.get(obj("remnant"));
        if (type == 0) {
            c.bind(tex("rpa_helmet"));
            c.render("Head", m.head, 0, 0, 0);
        }
        if (type == 1) {
            c.bind(tex("rpa_arm"));
            arms(c, m);

            c.bind(tex("rpa_chest"));
            c.render("Body", m.body, 0, 0, 0);

            /// START GLOW ///
            c.mode = Mode.GLOW;
            c.render("Glow", m.body, 0, 0, 0);
            c.mode = Mode.NORMAL;
            /// END GLOW ///

            /// START FAN ///
            ModelPart body = m.body;
            c.pose.pushPose();
            c.pose.translate(body.x * PX, body.y * PX, body.z * PX);
            if (body.zRot != 0.0F || body.yRot != 0.0F || body.xRot != 0.0F)
                c.pose.mulPose(new Quaternionf().rotationZYX(body.zRot, body.yRot, body.xRot));
            c.pose.translate(0, 4.875 * PX, 0);
            c.pose.mulPose(new Quaternionf().rotationZ((float) Math.toRadians(-System.currentTimeMillis() / 2D % 360)));
            c.pose.translate(0, -4.875 * PX, 0);
            c.render("Fan", body, 0, 0, 0);
            c.pose.popPose();
            /// END FAN ///
        }
        if (type == 2) {
            c.bind(tex("rpa_leg"));
            legs(c, m, "LeftLeg", "RightLeg");
        }
        if (type == 3) {
            c.bind(tex("rpa_leg"));
            legs(c, m, "LeftBoot", "RightBoot");
        }
    }

    /** ModelArmorTaurun / ModelArmorTrenchmaster (Beine leicht auseinandergeschoben). */
    private void trench(Ctx c, M m, int type, String name, boolean light) {
        c.model = ArmorObjModel.get(obj(name));
        if (type == 0) {
            c.bind(tex(name + "_helmet"));
            if (light) c.mode = Mode.BLEND;
            c.render("Helmet", m.head, 0, 0, 0);
            c.mode = Mode.NORMAL;
            if (light) {
                c.mode = Mode.GLOW;
                c.render("Light", m.head, 0, 0, 0);
                c.mode = Mode.NORMAL;
            }
        }
        if (type == 1) {
            c.bind(tex(name + "_chest"));
            c.render("Chest", m.body, 0, 0, 0);
            c.bind(tex(name + "_arm"));
            arms(c, m);
        }
        if (type == 2 || type == 3) {
            c.bind(tex(name + "_leg"));
            String l = type == 2 ? "LeftLeg" : "LeftBoot";
            String r = type == 2 ? "RightLeg" : "RightBoot";
            c.pose.translate(-0.01, 0, 0);
            c.render(l, m.leftLeg, 1.9F, 12.0F, 0.0F);
            c.pose.translate(0.02, 0, 0);
            c.render(r, m.rightLeg, -1.9F, 12.0F, 0.0F);
        }
    }
}
