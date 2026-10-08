package com.hbm_m.client.render.implementations;

import com.hbm_m.block.decorations.TrinketBlock;
import com.hbm_m.block.decorations.TrinketTypes;
import com.hbm_m.block.decorations.TrinketTypes.BobbleType;
import com.hbm_m.block.decorations.TrinketTypes.PlushieType;
import com.hbm_m.block.decorations.TrinketTypes.SnowglobeType;
import com.hbm_m.blockentity.decorations.TrinketBlockEntity;
import com.hbm_m.client.ClientRenderHandler;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.render.util.HorsePronter;
import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code RenderBobble}, {@code RenderSnowglobe}, {@code RenderPlushie}: gleiche Teile, Texturen und Matrizenfolgen.
 * GL-Zustaende sind auf RenderTypes abgebildet (Culling aus -> NoCull, Mischung -> translucent, Lichtkarte 240 ->
 * volle Helligkeit, additiv -> eyes). {@code renderItemIn2D} = flaches Itemmodell von (0,0) bis (1,1), 1/16 dick.
 */
public class TrinketRenderer implements com.hbm_m.client.render.HbmBerBounds<TrinketBlockEntity> {

    private static ResourceLocation rl(String path) { return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, path); }

    public static final int FULLBRIGHT = 0xF000F0;

    // ---- Wackelkopf ----
    public static final SimpleObjModel BOBBLE = new SimpleObjModel(rl("models/trinkets/bobble.obj"));
    public static final ResourceLocation SOCKET = rl("textures/models/trinkets/socket.png");
    public static final ResourceLocation GLOW = rl("textures/models/trinkets/glow.png");
    public static final ResourceLocation LAMP = rl("textures/models/trinkets/fluorescent_lamp.png");
    public static final ResourceLocation UNIVERSAL = rl("textures/models/TheGadget3_.png");
    public static final ResourceLocation MELLOW_GLOW = rl("textures/models/trinkets/mellowrpg8_glow.png");
    public static final ResourceLocation ABEL_GLOW = rl("textures/models/trinkets/abel_glow.png");

    public static final SimpleObjModel ARMOR_HEV = new SimpleObjModel(rl("models/armor/hev.obj"));
    public static final ResourceLocation HEV_HELMET = rl("textures/armor/hev_helmet.png");
    public static final SimpleObjModel N_I_4_N_I = new SimpleObjModel(rl("models/weapons/n_i_4_n_i.obj"));
    public static final ResourceLocation N_I_4_N_I_TEX = rl("textures/models/weapons/n_i_4_n_i.png");
    public static final SimpleObjModel SHIMMER_AXE = new SimpleObjModel(rl("models/shimmer_axe.obj"));
    public static final ResourceLocation SHIMMER_AXE_TEX = rl("textures/models/shimmer_axe.png");
    public static final SimpleObjModel FATMAN = new SimpleObjModel(rl("models/weapons/fatman.obj"));
    public static final ResourceLocation FATMAN_MININUKE_TEX = rl("textures/models/weapons/fatman_mininuke.png");
    public static final SimpleObjModel DOUBLE_BARREL = new SimpleObjModel(rl("models/weapons/sacred_dragon.obj"));
    public static final ResourceLocation DOUBLE_BARREL_SACRED_DRAGON_TEX = rl("textures/models/weapons/double_barrel_sacred_dragon.png");
    public static final SimpleObjModel ARMOR_HAT = new SimpleObjModel(rl("models/armor/hat.obj"));
    public static final ResourceLocation HAT = rl("textures/armor/hat.png");

    // ---- Schneekugel ----
    public static final SimpleObjModel SNOWGLOBE = new SimpleObjModel(rl("models/trinkets/snowglobe.obj"));
    public static final ResourceLocation SNOW_SOCKET = rl("textures/models/trinkets/snowglobe.png");
    public static final ResourceLocation SNOW_GLASS = rl("textures/models/trinkets/snowglobe_glass.png");
    public static final ResourceLocation SNOW_FEATURES = rl("textures/models/trinkets/snowglobe_features.png");

    // ---- Plueschtier ----
    public static final SimpleObjModel YOMI = new SimpleObjModel(rl("models/trinkets/yomi.obj"));
    public static final SimpleObjModel HUNDUN = new SimpleObjModel(rl("models/trinkets/hundun.obj"));
    public static final SimpleObjModel DERG = new SimpleObjModel(rl("models/trinkets/derg.obj"));
    public static final ResourceLocation YOMI_TEX = rl("textures/models/trinkets/yomi.png");
    public static final ResourceLocation NUMBERNINE_TEX = rl("textures/models/horse/numbernine.png");
    public static final ResourceLocation HUNDUN_TEX = rl("textures/models/trinkets/hundun.png");
    public static final ResourceLocation DERG_TEX = rl("textures/models/trinkets/derg.png");
    public static final SimpleObjModel ARMOR_NO9 = new SimpleObjModel(rl("models/armor/no9.obj"));
    public static final ResourceLocation NO9 = rl("textures/armor/no9.png");
    public static final ResourceLocation NO9_INSIGNIA = rl("textures/armor/no9_insignia.png");

    public TrinketRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(TrinketBlockEntity te, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        BlockState state = te.getBlockState();
        if (!(state.getBlock() instanceof TrinketBlock block)) return;
        float rot = 22.5F * state.getValue(TrinketBlock.ROTATION) + 90F;

        ps.pushPose();
        ps.translate(0.5D, 0.0D, 0.5D);
        switch (block.kind) {
            case BOBBLE -> {
                ps.scale(0.25F, 0.25F, 0.25F);
                ps.mulPose(Axis.YN.rotationDegrees(rot));
                renderBobble(ps, buf, TrinketTypes.safe(BobbleType.class, te.type), light);
            }
            case SNOWGLOBE -> {
                ps.mulPose(Axis.YN.rotationDegrees(rot));
                renderSnowglobe(ps, buf, TrinketTypes.safe(SnowglobeType.class, te.type), light);
            }
            case PLUSHIE -> {
                ps.mulPose(Axis.YN.rotationDegrees(rot));
                PlushieType type = TrinketTypes.safe(PlushieType.class, te.type);
                if (te.squishTimer > 0) {
                    double squish = te.squishTimer - interp;
                    ps.scale(1F, (float) (1 + (-(Math.sin(squish)) * squish) * 0.025), 1F);
                }
                switch (type) {
                    case YOMI -> ps.scale(0.5F, 0.5F, 0.5F);
                    case NUMBERNINE -> ps.scale(0.75F, 0.75F, 0.75F);
                    default -> { }
                }
                renderPlushie(ps, buf, type, te.squishTimer > 0, light);
            }
        }
        ps.popPose();
    }

    // =========================== Wackelkopf ===========================

    private static ResourceLocation skin(BobbleType type) {
        return switch (type) {
            case STRENGTH, PERCEPTION, ENDURANCE, CHARISMA, INTELLIGENCE, AGILITY, LUCK -> rl("textures/models/trinkets/vaultboy.png");
            case BOB -> rl("textures/models/trinkets/hbm.png");
            case PU238 -> rl("textures/models/trinkets/pellet.png");
            case FRIZZLE -> rl("textures/models/trinkets/frizzle.png");
            case VT -> rl("textures/models/trinkets/vt.png");
            case DOC -> rl("textures/models/trinkets/doctor17ph.png");
            case BLUEHAT -> rl("textures/models/trinkets/thebluehat.png");
            case PHEO -> rl("textures/models/trinkets/pheo.png");
            case CIRNO -> rl("textures/models/trinkets/cirno.png");
            case ADAM29 -> rl("textures/models/trinkets/adam29.png");
            case UFFR -> rl("textures/models/trinkets/uffr.png");
            case VAER -> rl("textures/models/trinkets/vaer.png");
            case NOS -> rl("textures/models/trinkets/nos.png");
            case DRILLGON -> rl("textures/models/trinkets/drillgon200.png");
            case MICROWAVE -> rl("textures/models/trinkets/microwave.png");
            case PEEP -> rl("textures/models/trinkets/peep.png");
            case MELLOW -> rl("textures/models/trinkets/mellowrpg8.png");
            case ABEL -> rl("textures/models/trinkets/abel.png");
            default -> UNIVERSAL;
        };
    }

    public static void renderBobble(PoseStack ps, MultiBufferSource buf, BobbleType type, int light) {
        long time = System.currentTimeMillis();
        BOBBLE.renderPart("Socket", ps, buf.getBuffer(RenderType.entityCutout(SOCKET)), light);

        ResourceLocation skin = skin(type);
        switch (type) {
            case PU238 -> renderPellet(ps, buf, skin, time);
            case UFFR -> renderFumo(ps, buf, skin, light, time);
            case DRILLGON -> BOBBLE.renderPart("Drillgon", ps, buf.getBuffer(RenderType.entityCutout(skin)), light);
            default -> renderGuy(ps, buf, type, skin, light, time);
        }

        ps.pushPose();
        renderPost(ps, buf, type, light, time);
        ps.popPose();

        renderSocket(ps, buf, type, light);
    }

    /** {@code setupFigurineRotation}: {leftArm, rightArm, leftLeg, rightLeg, head} je {x, y, z} und der Koerper. */
    private static double[][] figurine(BobbleType type) {
        double[][] r = new double[6][3];
        double[] body = r[5];
        switch (type) {
            case STRENGTH -> { r[0] = d(0, 25, 135); r[1] = d(0, -45, 135); r[2] = d(0, 0, -5); r[3] = d(0, 0, 5); r[4] = d(15, 0, 0); }
            case PERCEPTION -> { r[0] = d(0, -15, 135); r[1] = d(-5, 0, 0); }
            case ENDURANCE -> { body[0] = 45; r[0] = d(0, -25, 30); r[1] = d(0, 45, 30); r[4] = d(0, -45, 0); }
            case CHARISMA -> { body[0] = 45; r[1] = d(0, -45, 90); r[2] = d(0, 0, -5); r[3] = d(0, 0, 5); r[4] = d(-5, -45, 0); }
            case INTELLIGENCE -> { r[4] = d(0, 30, 0); r[0] = d(5, 0, 0); r[1] = d(15, 0, 170); }
            case AGILITY -> { r[0] = d(0, 0, 60); r[1] = d(0, 0, -45); r[2] = d(0, 0, -15); r[3] = d(0, 0, 45); }
            case LUCK -> { r[0] = d(135, 45, 0); r[1] = d(-135, -45, 0); r[3] = d(-5, 0, 0); }
            case VT -> { r[0] = d(0, -45, 60); r[1] = d(0, 0, 45); r[2] = d(2, 0, 0); r[3] = d(-2, 0, 0); }
            case BLUEHAT -> r[0] = d(0, 90, 60);
            case FRIZZLE -> { r[0] = d(0, 15, 45); r[1] = d(0, 0, 80); r[2] = d(0, 0, 2); r[3] = d(0, 0, -2); }
            case ADAM29 -> r[1] = d(0, 0, 60);
            case PHEO -> { r[0] = d(0, 0, 80); r[1] = d(0, 0, 45); }
            case VAER -> { r[0] = d(0, -5, 45); r[1] = d(0, 15, 45); }
            case PEEP -> { r[0] = d(0, 0, 1); r[1] = d(0, 0, 1); }
            case MELLOW -> { r[0] = d(0, 10, 0); r[1] = d(0, -10, 0); r[2] = d(3, 5, 2); r[3] = d(-3, -5, 0); }
            case ABEL -> { r[0] = d(0, 80, 90); r[1] = d(0, -80, 90); }
            default -> { }
        }
        return r;
    }

    private static double[] d(double x, double y, double z) { return new double[] { x, y, z }; }

    private static void limb(PoseStack ps, double ox, double oy, double oz, double[] rot) {
        ps.translate(ox, oy, oz);
        ps.mulPose(Axis.XP.rotationDegrees((float) rot[0]));
        ps.mulPose(Axis.YP.rotationDegrees((float) rot[1]));
        ps.mulPose(Axis.ZP.rotationDegrees((float) rot[2]));
        ps.translate(-ox, -oy, -oz);
    }

    public static void renderGuy(PoseStack ps, MultiBufferSource buf, BobbleType type, ResourceLocation tex, int light, long time) {
        double[][] r = figurine(type);

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees((float) r[5][0]));

        if (type == BobbleType.PEEP) BOBBLE.renderPart("PeepTail", ps, buf.getBuffer(RenderType.entityCutout(tex)), light);

        var vc = buf.getBuffer(RenderType.entityTranslucent(tex));
        String suffix = type.skinLayers ? "" : "17";

        ps.pushPose(); limb(ps, 0, 1, -0.125, r[2]); BOBBLE.renderPart("LL" + suffix, ps, vc, light); ps.popPose();
        ps.pushPose(); limb(ps, 0, 1, 0.125, r[3]); BOBBLE.renderPart("RL" + suffix, ps, vc, light); ps.popPose();
        ps.pushPose(); limb(ps, 0, 1.625, -0.25, r[0]); BOBBLE.renderPart("LA" + suffix, ps, vc, light); ps.popPose();
        ps.pushPose(); limb(ps, 0, 1.625, 0.25, r[1]); BOBBLE.renderPart("RA" + suffix, ps, vc, light); ps.popPose();

        BOBBLE.renderPart("Body" + suffix, ps, vc, light);

        double speed = 0.005, amplitude = 1;
        ps.pushPose();
        ps.translate(0, 1.75, 0);
        ps.mulPose(Axis.XP.rotationDegrees((float) (Math.sin(time * speed) * amplitude)));
        ps.mulPose(Axis.ZP.rotationDegrees((float) (Math.sin(time * speed + (Math.PI * 0.5)) * amplitude)));
        ps.mulPose(Axis.XP.rotationDegrees((float) r[4][0]));
        ps.mulPose(Axis.YP.rotationDegrees((float) r[4][1]));
        ps.mulPose(Axis.ZP.rotationDegrees((float) r[4][2]));
        ps.translate(0, -1.75, 0);
        BOBBLE.renderPart("Head" + suffix, ps, vc, light);

        if (type == BobbleType.VT) BOBBLE.renderPart("Horn", ps, vc, light);
        if (type == BobbleType.PEEP) BOBBLE.renderPart("PeepHat", ps, vc, light);

        if (type == BobbleType.VAER) {
            ps.translate(0.25, 1.9, 0.075);
            ps.mulPose(Axis.ZP.rotationDegrees(-60));
            ps.scale(0.5F, 0.5F, 0.5F);
            renderItem2D(ps, buf, new ItemStack(ModItems.CIGARETTE.get()), light);
        }

        if (type == BobbleType.NOS) {
            ps.translate(0, 1.75, 0);
            ps.mulPose(Axis.XP.rotationDegrees(180));
            ps.scale(0.095F, 0.095F, 0.095F);
            ARMOR_HAT.renderAll(ps, buf.getBuffer(RenderType.entityCutoutNoCull(HAT)), light);
        }
        ps.popPose();
        ps.popPose();
    }

    private static void renderPellet(PoseStack ps, MultiBufferSource buf, ResourceLocation tex, long time) {
        BOBBLE.renderPart("Pellet", ps, buf.getBuffer(RenderType.entityCutout(tex)), FULLBRIGHT);
        BOBBLE.renderPartColor("PelletShine", ps, buf.getBuffer(ClientRenderHandler.CustomRenderTypes.ORBITAL_ORB),
                1.0F, 1.0F, 0.0F, 0.1F + (float) Math.sin(time * 0.001D) * 0.05F);
    }

    private static void renderFumo(PoseStack ps, MultiBufferSource buf, ResourceLocation tex, int light, long time) {
        BOBBLE.renderPart("Fumo", ps, buf.getBuffer(RenderType.entityCutout(tex)), light);
        double speed = 0.005, amplitude = 1;
        ps.pushPose();
        ps.translate(0, 0.75, 0);
        ps.mulPose(Axis.XP.rotationDegrees((float) (Math.sin(time * speed) * amplitude)));
        ps.mulPose(Axis.ZP.rotationDegrees((float) (Math.sin(time * speed + (Math.PI * 0.5)) * amplitude)));
        ps.translate(0, -0.75, 0);
        BOBBLE.renderPart("FumoHead", ps, buf.getBuffer(RenderType.entityCutoutNoCull(tex)), light);
        ps.popPose();
    }

    private static void renderPost(PoseStack ps, MultiBufferSource buf, BobbleType type, int light, long time) {
        switch (type) {
            case BLUEHAT -> {
                double scale = 0.0625D;
                ps.translate(0D, 0.875D, -0.5D);
                ps.mulPose(Axis.YP.rotationDegrees(-90));
                ps.mulPose(Axis.ZP.rotationDegrees(-160));
                ps.scale((float) scale, (float) scale, (float) scale);
                ARMOR_HEV.renderPart("Head", ps, buf.getBuffer(RenderType.entityCutout(HEV_HELMET)), light);
            }
            case FRIZZLE -> {
                ps.pushPose();
                ps.translate(0.8, 1.6, 0.4);
                ps.scale(0.125F, 0.125F, 0.125F);
                ps.mulPose(Axis.YP.rotationDegrees(90));
                ps.mulPose(Axis.XP.rotationDegrees(10));
                var vc = buf.getBuffer(RenderType.entityCutout(N_I_4_N_I_TEX));
                for (String p : new String[] { "FrameDark", "Grip", "FrameLight", "Cylinder", "Barrel" }) N_I_4_N_I.renderPart(p, ps, vc, light);
                ps.popPose();

                ps.translate(0.3, 1.4, -0.2);
                ps.mulPose(Axis.XP.rotationDegrees(-100));
                ps.scale(0.5F, 0.5F, 0.5F);
                renderItem2D(ps, buf, new ItemStack(com.hbm_m.item.weapon.sedna.WeaponItems.WEAPON_MOD_SPECIAL.get(com.hbm_m.item.weapon.sedna.factory.GunFactory.EnumModSpecial.DOUBLOONS).get()), light);
            }
            case ADAM29 -> {
                ps.translate(0.4, 1.15, 0.4);
                ps.scale(0.5F, 0.5F, 0.5F);
                renderItem2D(ps, buf, new ItemStack(ModItems.CAN_REDBOMB.get()), light);
            }
            case PHEO -> {
                ps.translate(0.5, 1.15, 0.45);
                ps.mulPose(Axis.XP.rotationDegrees(-60));
                ps.scale(2F, 2F, 2F);
                SHIMMER_AXE.renderAll(ps, buf.getBuffer(RenderType.entityCutout(SHIMMER_AXE_TEX)), light);
            }
            case BOB -> {
                ps.pushPose();
                ps.translate(0, 0.6875F, 0.625F);
                ps.mulPose(Axis.XP.rotationDegrees(-90));
                ps.scale(0.125F, 0.125F, 0.125F);
                var vc = buf.getBuffer(RenderType.entityCutout(FATMAN_MININUKE_TEX));
                ps.translate(-6, 0, 0);
                for (int i = -1; i <= 1; i++) {
                    ps.translate(3, 0, 0);
                    FATMAN.renderPart("MiniNuke", ps, vc, light);
                }
                ps.popPose();
                ps.pushPose();
                ps.translate(0.25F, 0.3125F, -0.5F);
                ps.mulPose(Axis.XP.rotationDegrees(-90));
                ps.mulPose(Axis.YP.rotationDegrees(90));
                ps.scale(0.1F, 0.1F, 0.1F);
                var vc2 = buf.getBuffer(RenderType.entityCutout(DOUBLE_BARREL_SACRED_DRAGON_TEX));
                for (String p : new String[] { "Stock", "BarrelShort", "Buckle", "Lever" }) DOUBLE_BARREL.renderPart(p, ps, vc2, light);
                ps.popPose();
            }
            case MELLOW -> {
                renderGuy(ps, buf, type, MELLOW_GLOW, FULLBRIGHT, time);
                BOBBLE.renderPart("Fluoro", ps, buf.getBuffer(RenderType.eyes(LAMP)), FULLBRIGHT);
                BOBBLE.renderPart("Glow", ps, buf.getBuffer(RenderType.eyes(GLOW)), FULLBRIGHT);
            }
            case ABEL -> renderGuy(ps, buf, type, ABEL_GLOW, FULLBRIGHT, time);
            default -> { }
        }
    }

    private static void renderSocket(PoseStack ps, MultiBufferSource buf, BobbleType type, int light) {
        Font font = Minecraft.getInstance().font;
        float f3 = 0.01F;
        ps.translate(0.63, 0.175F, 0.0);
        ps.scale(f3, -f3, f3);
        ps.translate(0, 0, font.width(type.label) * 0.5D);
        ps.mulPose(Axis.YP.rotationDegrees(90));
        ps.translate(0, 1, 0);
        font.drawInBatch(type.label, 0, 0, type == BobbleType.VT ? 0xff0000 : 0xffffff, false, ps.last().pose(), buf, Font.DisplayMode.POLYGON_OFFSET, 0, light);
    }

    // =========================== Schneekugel ===========================

    public static void renderSnowglobe(PoseStack ps, MultiBufferSource buf, SnowglobeType type, int light) {
        double scale = 0.0625D;
        ps.scale((float) scale, (float) scale, (float) scale);

        SNOWGLOBE.renderPart("Socket", ps, buf.getBuffer(RenderType.entityCutoutNoCull(SNOW_SOCKET)), light);
        SNOWGLOBE.renderPart("Glass", ps, buf.getBuffer(RenderType.entityCutoutNoCull(SNOW_GLASS)), light);

        var vc = buf.getBuffer(RenderType.entityCutoutNoCull(SNOW_FEATURES));
        switch (type) {
            case RIVETCITY -> SNOWGLOBE.renderPart("RivetCity", ps, vc, light);
            case TENPENNYTOWER -> SNOWGLOBE.renderPart("TenpennyTower", ps, vc, light);
            case LUCKY38 -> SNOWGLOBE.renderPart("Lucky38", ps, vc, light);
            case SIERRAMADRE -> SNOWGLOBE.renderPart("SierraMadre", ps, vc, light);
            case PRYDWEN -> SNOWGLOBE.renderPart("Prydwen", ps, vc, light);
            default -> { }
        }

        Font font = Minecraft.getInstance().font;
        float f3 = 0.05F;
        ps.translate(4.025, 0.5, 0);
        ps.scale(f3, -f3, f3);
        ps.translate(0, -font.lineHeight / 2F, font.width(type.label) * 0.5D);
        ps.mulPose(Axis.YP.rotationDegrees(90));
        ps.translate(0, 1, 0);
        font.drawInBatch(type.label, 0, 0, 0xffffff, false, ps.last().pose(), buf, Font.DisplayMode.POLYGON_OFFSET, 0, light);
    }

    // =========================== Plueschtier ===========================

    public static void renderPlushie(PoseStack ps, MultiBufferSource buf, PlushieType type, boolean squish, int light) {
        switch (type) {
            case YOMI -> YOMI.renderAll(ps, buf.getBuffer(RenderType.entityCutout(YOMI_TEX)), light);
            case NUMBERNINE -> {
                ps.mulPose(Axis.YP.rotationDegrees(90));
                ps.mulPose(Axis.XN.rotationDegrees(15));
                ps.translate(0, -0.25, 0.75);
                HorsePronter.reset();
                double r = 45;
                HorsePronter.pose(HorsePronter.id_body, 0, -r, 0);
                HorsePronter.pose(HorsePronter.id_tail, 0, 60, 90);
                HorsePronter.pose(HorsePronter.id_lbl, 0, -75 + r, 35);
                HorsePronter.pose(HorsePronter.id_rbl, 0, -75 + r, -35);
                HorsePronter.pose(HorsePronter.id_lfl, 0, r - 25, 5);
                HorsePronter.pose(HorsePronter.id_rfl, 0, r - 25, -5);
                HorsePronter.pose(HorsePronter.id_head, 0, r + 15, 0);
                HorsePronter.pront(ps, buf.getBuffer(RenderType.entityCutoutNoCull(NUMBERNINE_TEX)), light);
                ps.mulPose(Axis.XP.rotationDegrees(15));
                ps.pushPose();
                ps.translate(0, 1, -0.6875);
                double s = 1.125D;
                ps.scale((float) (0.0625 * s), (float) (0.0625 * s), (float) (0.0625 * s));
                ps.mulPose(Axis.XP.rotationDegrees(180));
                ARMOR_NO9.renderPart("Helmet", ps, buf.getBuffer(RenderType.entityCutoutNoCull(NO9)), light);
                ARMOR_NO9.renderPart("Insignia", ps, buf.getBuffer(RenderType.entityCutoutNoCull(NO9_INSIGNIA)), light);
                ps.popPose();
                double scale = 0.25;
                ps.translate(-0.06, 1.13, -0.42);
                ps.scale((float) scale, (float) scale, (float) scale);
                ps.mulPose(Axis.YN.rotationDegrees(90));
                ps.mulPose(Axis.ZN.rotationDegrees(60));
                renderItem2D(ps, buf, new ItemStack(ModItems.CIGARETTE.get()), light);
            }
            case HUNDUN -> HUNDUN.renderPart("goober_posed", ps, buf.getBuffer(RenderType.entityCutout(HUNDUN_TEX)), light);
            case DERG -> {
                var vc = buf.getBuffer(RenderType.entityCutout(DERG_TEX));
                DERG.renderPart("Derg", ps, vc, light);
                DERG.renderPart(squish ? "Blep" : "ColonThree", ps, vc, light);
            }
            default -> { }
        }
    }

    /** {@code ItemRenderer.renderItemIn2D}: flaches Symbol von (0,0,0) bis (1,1,-1/16). */
    public static void renderItem2D(PoseStack ps, MultiBufferSource buf, ItemStack stack, int light) {
        ps.pushPose();
        ps.translate(0.5, 0.5, -0.03125);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY, ps, buf, null, 0);
        ps.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(TrinketBlockEntity te) {
        return false;
    }
}
