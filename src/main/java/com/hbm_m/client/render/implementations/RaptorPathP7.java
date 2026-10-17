package com.hbm_m.client.render.implementations;

import static com.hbm_m.client.render.raptorpath.RaptorParts.mesh;
import static com.hbm_m.client.render.raptorpath.RaptorParts.rotY;
import static com.hbm_m.client.render.raptorpath.RaptorParts.when;

import java.util.List;
import java.util.Map;
import java.util.Random;

import org.joml.Matrix4f;

import com.hbm_m.block.machines.MachineBatteryREDDBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.BatteryREDDBlockEntity;
import com.hbm_m.blockentity.machines.BatterySocketBlockEntity;
import com.hbm_m.blockentity.machines.MachineAssemblyFactoryBlockEntity;
import com.hbm_m.blockentity.machines.fusion.FusionPlasmaForgeBlockEntity;
import com.hbm_m.blockentity.machines.fusion.FusionTorusBlockEntity;
import com.hbm_m.client.ClientRenderHandler;
import com.hbm_m.client.render.LegacyAnimator;
import com.hbm_m.client.render.RenderDistanceHelper;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.client.render.machine.MachineSpecBuilder;
import com.hbm_m.client.render.raptorpath.ObjAtlasMesh;
import com.hbm_m.client.render.util.BeamPronter;
import com.hbm_m.client.render.util.HorsePronter;
import com.hbm_m.item.fekal_electric.ItemBatteryPack;
import com.hbm_m.item.fekal_electric.ItemBatterySC;
import com.hbm_m.item.fekal_electric.ItemCreativeBattery;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.AssemblerRecipe;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Render-Migration Paket P7 (Kategorie B, grosse Maschinen mit Gelenkarmen und Plasma): Arme/Ringe auf Raptors
 * Pfad, Plasma (UV-Lauf, additiv), Funken, Gegenstaende und Blitze als immediate-Hook (Code der alten BERs).
 */
//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public final class RaptorPathP7 {

    private RaptorPathP7() {}

    private static ResourceLocation rl(String p) {
        return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, p);
    }

    private static ObjAtlasMesh p(SimpleObjModel m, ResourceLocation t, String key, String... parts) {
        return ObjAtlasMesh.parts(m, t, "hbm_m/" + key, parts);
    }

    // ── Assembly factory ─────────────────────────────────────────────────────────────────

    private static final ObjAtlasMesh AF_BASE = p(AssemblyFactoryRenderer.MODEL, AssemblyFactoryRenderer.TEX, "assembly_factory/base", "Base");
    private static final ObjAtlasMesh AF_FRAME = p(AssemblyFactoryRenderer.MODEL, AssemblyFactoryRenderer.TEX, "assembly_factory/frame", "Frame");
    private static final String[] AF_STAGES = { "Slider", "ArmLower", "ArmUpper", "Head", "Striker", "Blade" };

    private static ObjAtlasMesh afPart(int group, int stage) {
        String name = AF_STAGES[stage] + (group + 1);
        return p(AssemblyFactoryRenderer.MODEL, AssemblyFactoryRenderer.TEX, "assembly_factory/" + name.toLowerCase(java.util.Locale.ROOT), name);
    }

    /**
     * Gelenkkette {@code group} (0 Stoessel 1, 1 Saege 1, 2 Stoessel 2, 3 Saege 2) bis Glied {@code stage}
     * (0 Schlitten, 1 Unterarm, 2 Oberarm, 3 Kopf, 4 Stoessel, 5 Saegeblatt) wie {@code AssemblyFactoryRenderer.render}.
     */
    private static boolean afChain(MachineAssemblyFactoryBlockEntity be, float interp, PoseStack ps, int group, int stage) {
        int anim = group / 2;
        boolean saw = group % 2 == 1;
        double slide = be.animations[anim].getSlider(interp);
        double[] arm = saw ? be.animations[anim].saw.getPositions(interp) : be.animations[anim].striker.getPositions(interp);
        // Gruppen 0 und 3 laufen auf der -z-Seite (Pivots negativ, Winkel negiert), 1 und 2 auf +z
        boolean neg = group == 0 || group == 3;
        double slideX = (group == 0 || group == 3) ? 0.5 - slide : -0.5 + slide;
        double zs = neg ? -1 : 1;
        double as = neg ? -1 : 1;
        ps.translate(slideX, 0, 0);
        if (stage == 0) return true;
        pivot(ps, 1.625, 0.9375 * zs, arm[0] * as);
        if (stage == 1) return true;
        pivot(ps, 2.375, 0.9375 * zs, arm[1] * as);
        if (stage == 2) return true;
        pivot(ps, 2.375, 0.4375 * zs, arm[2] * as);
        if (stage == 3) return true;
        ps.translate(0, arm[3], 0);
        if (stage == 4) return true;
        pivot(ps, 1.625, 0.3125 * zs, -arm[4] * as);
        return true;
    }

    private static void pivot(PoseStack ps, double y, double z, double angle) {
        ps.translate(0, y, z);
        ps.mulPose(Axis.XP.rotationDegrees((float) angle));
        ps.translate(0, -y, -z);
    }

    private static void registerAssemblyFactory() {
        MachineSpecBuilder<MachineAssemblyFactoryBlockEntity> b = MachineRenderers.machine("hbm_m/assembly_factory", ModBlockEntities.ASSEMBLY_FACTORY_BE.get(), MachineAssemblyFactoryBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5, 0, 0.5);
                    rotY(a, 90);
                    Direction facing = be.getBlockState().hasProperty(HorizontalDirectionalBlock.FACING)
                            ? be.getBlockState().getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
                    switch (facing) {
                        case WEST -> rotY(a, 90);
                        case SOUTH -> rotY(a, 180);
                        case EAST -> rotY(a, 270);
                        default -> { }
                    }
                });
        mesh(b, "base", AF_BASE);
        when(b, "frame", be -> be.frame, AF_FRAME);
        for (int g = 0; g < 4; g++) {
            int stages = (g % 2 == 1) ? 6 : 5;
            for (int s = 0; s < stages; s++) {
                final int group = g, stage = s;
                mesh(b, "g" + g + "_" + s, (be, f, t, ps) -> afChain(be, f, ps, group, stage), afPart(g, s));
            }
        }
        b.hook(RaptorPathP7::assemblyFactoryHook).viewDistance(256).register();
    }

    /** Rezeptsymbole und Funken aus {@code AssemblyFactoryRenderer} (unveraendert). */
    private static void assemblyFactoryHook(MachineAssemblyFactoryBlockEntity assemfac, float interp, PoseStack ps, MultiBufferSource buf,
                                            int light, int overlay, com.hbm_m.client.render.machine.MachineRenderApi api) {
        var me = Minecraft.getInstance().player;
        if (me == null || me.distanceToSqr(assemfac.getBlockPos().getX() + 0.5, assemfac.getBlockPos().getY() + 1, assemfac.getBlockPos().getZ() + 0.5) >= 35 * 35) return;
        double slide1 = assemfac.animations[0].getSlider(interp);
        double slide2 = assemfac.animations[1].getSlider(interp);
        double[] arm2 = assemfac.animations[0].saw.getPositions(interp);
        double[] arm4 = assemfac.animations[1].saw.getPositions(interp);

        for (int i = 0; i < 4; i++) {
            ps.pushPose();
            ps.translate(1.5 - i, 0, 0);
            ps.mulPose(Axis.YP.rotationDegrees(90));
            ps.translate(0, 1.0625, 0);
            AssemblerRecipe recipe = assemfac.getRecipe(i);
            if (recipe != null) {
                ItemStack stack = recipe.getResultItemSafe().copy();
                if (!stack.isEmpty()) {
                    stack.setCount(1);
                    AssemblyFactoryRenderer.renderIcon(assemfac, stack, ps, buf, light);
                }
            }
            ps.popPose();
        }

        VertexConsumer sparks = buf.getBuffer(RenderType.entityTranslucent(AssemblyFactoryRenderer.SPARKS_TEX));
        double wide = 0.1875D;
        double narrow = 0.00D;
        double length = 1.25D;
        double uMin = ((assemfac.getLevel().getGameTime() / 10D + interp)) % 10;
        double uMax = uMin + 1;
        double epsilon = 0.01D;

        ps.pushPose(); if (arm2[3] <= -0.375D) {
            ps.translate(0.5 + slide1, 1.0625D, -arm2[2] / 45D);
            Matrix4f m = ps.last().pose();
            AssemblyFactoryRenderer.v(sparks, m, -epsilon, -wide, length, uMin + 0.5, 0, 0F);
            AssemblyFactoryRenderer.v(sparks, m, -epsilon, wide, length, uMin + 0.5, 1, 0F);
            AssemblyFactoryRenderer.v(sparks, m, -epsilon, narrow, 0, uMax + 0.5, 1, 1F);
            AssemblyFactoryRenderer.v(sparks, m, -epsilon, -narrow, 0, uMax + 0.5, 0, 1F);
            AssemblyFactoryRenderer.v(sparks, m, epsilon, -wide, length, uMin, 1, 0F);
            AssemblyFactoryRenderer.v(sparks, m, epsilon, wide, length, uMin, 0, 0F);
            AssemblyFactoryRenderer.v(sparks, m, epsilon, narrow, 0, uMax, 0, 1F);
            AssemblyFactoryRenderer.v(sparks, m, epsilon, -narrow, 0, uMax, 1, 1F);
        } ps.popPose();

        ps.pushPose(); if (arm4[3] <= -0.375D) {
            ps.translate(-0.5 - slide2, 1.0625D, arm4[2] / 45D);
            Matrix4f m = ps.last().pose();
            AssemblyFactoryRenderer.v(sparks, m, -epsilon, -wide, -length, uMin + 0.5, 0, 0F);
            AssemblyFactoryRenderer.v(sparks, m, -epsilon, wide, -length, uMin + 0.5, 1, 0F);
            AssemblyFactoryRenderer.v(sparks, m, -epsilon, narrow, 0, uMax + 0.5, 1, 1F);
            AssemblyFactoryRenderer.v(sparks, m, -epsilon, -narrow, 0, uMax + 0.5, 0, 1F);
            AssemblyFactoryRenderer.v(sparks, m, epsilon, -wide, -length, uMin, 1, 0F);
            AssemblyFactoryRenderer.v(sparks, m, epsilon, wide, -length, uMin, 0, 0F);
            AssemblyFactoryRenderer.v(sparks, m, epsilon, narrow, 0, uMax, 0, 1F);
            AssemblyFactoryRenderer.v(sparks, m, epsilon, -narrow, 0, uMax, 1, 1F);
        } ps.popPose();
    }

    // ── REDD-Batterie ────────────────────────────────────────────────────────────────────

    private static final ObjAtlasMesh REDD_BASE = p(BatteryREDDRenderer.MODEL, BatteryREDDRenderer.TEX, "battery_redd/base", "Base");
    private static final ObjAtlasMesh REDD_WHEEL = p(BatteryREDDRenderer.MODEL, BatteryREDDRenderer.TEX, "battery_redd/wheel", "Wheel");

    private static boolean reddWheel(BatteryREDDBlockEntity redd, float interp, long t, PoseStack ps) {
        double rot = redd.prevRotation + (redd.rotation - redd.prevRotation) * interp;
        ps.translate(0, 5.5, 0);
        ps.mulPose(Axis.XP.rotationDegrees((float) rot));
        ps.translate(0, -5.5, 0);
        return true;
    }

    private static void registerRedd() {
        MachineSpecBuilder<BatteryREDDBlockEntity> b = MachineRenderers.machine("hbm_m/battery_redd", ModBlockEntities.BATTERY_REDD_BE.get(), BatteryREDDBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5, 0, 0.5);
                    switch (be.getBlockState().getValue(MachineBatteryREDDBlock.FACING)) {
                        case NORTH -> rotY(a, 270);
                        case SOUTH -> rotY(a, 90);
                        case EAST -> rotY(a, 180);
                        default -> { }
                    }
                });
        mesh(b, "base", REDD_BASE);
        mesh(b, "wheel", RaptorPathP7::reddWheel, REDD_WHEEL);
        b.hook((redd, interp, ps, buffers, light, overlay, api) -> {
            float speed = redd.getSpeed();
            ps.pushPose();
            reddWheel(redd, interp, 0L, ps);
            BatteryREDDRenderer.MODEL.renderPart("Lights", ps, buffers.getBuffer(RenderType.entityCutout(BatteryREDDRenderer.TEX)), LightTexture.FULL_BRIGHT);
            ps.pushPose();
            ps.translate(0, 5.5, 0);
            BatteryREDDRenderer.renderStreaks(ps, buffers.getBuffer(ClientRenderHandler.CustomRenderTypes.ADDITIVE_TRIANGLES), speed);
            ps.popPose();
            BatteryREDDRenderer.renderSparkle(redd, ps, buffers);
            ps.popPose();
            if (speed > 0) BatteryREDDRenderer.renderZaps(redd, ps, buffers);
        }).viewDistance(256).register();
    }

    // ── Batteriesockel ───────────────────────────────────────────────────────────────────

    private static final ObjAtlasMesh SOCKET_SUPPORTS = p(BatterySocketRenderer.MODEL, BatterySocketRenderer.TEX, "battery_socket/supports", "Supports");

    private static void registerSocket() {
        MachineSpecBuilder<BatterySocketBlockEntity> b = MachineRenderers.machine("hbm_m/battery_socket", ModBlockEntities.BATTERY_SOCKET_BE.get(), BatterySocketBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5, 0, 0.5);
                    switch (ObjBerHelper.meta(be)) {
                        case 2 -> rotY(a, 90);
                        case 4 -> rotY(a, 180);
                        case 3 -> rotY(a, 270);
                        default -> { }
                    }
                    a.translate(-0.5, 0, 0.5);
                });
        when(b, "supports", be -> be.getLevel() != null && !be.getLevel().getBlockState(be.getBlockPos().above(2)).isAir(), SOCKET_SUPPORTS);
        b.hook((socket, interp, ps, buf, light, overlay, api) -> {
            Level level = socket.getLevel();
            if (level == null) return;
            ItemStack render = socket.getItemHandler().getStackInSlot(0);
            long time = level.getGameTime();
            if (render.isEmpty() || render.getItem() instanceof ItemBatteryPack || render.getItem() instanceof ItemBatterySC) return;
            if (render.getItem() instanceof ItemCreativeBattery) {
                ps.pushPose();
                ps.scale(0.75F, 0.75F, 0.75F);
                ps.mulPose(Axis.YN.rotationDegrees((float) ((time % 360 + interp) * 25D)));
                HorsePronter.reset();
                HorsePronter.enableHorn();
                HorsePronter.pront(ps, buf.getBuffer(RenderType.entityCutoutNoCull(BatterySocketRenderer.BLORBO)), light);
                ps.popPose();
            } else {
                ps.pushPose();
                ps.translate(0, 0.5, 0);
                ps.scale(1.5F, 1.5F, 1.5F);
                ps.mulPose(Axis.YN.rotationDegrees((float) ((time % 360 + interp) * 2.5D)));
                ItemStack one = render.copy();
                one.setCount(1);
                ps.scale(0.5128205F, 0.5128205F, 0.5128205F);
                ps.translate(0.0F, -0.05F, 0.0F);
                ps.mulPose(Axis.YP.rotationDegrees(180));
                ps.scale(0.5F, 0.5F, 0.5F);
                ps.translate(0.0F, 0.25F, 0.0F);
                Minecraft.getInstance().getItemRenderer().renderStatic(one, ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY, ps, buf, level, 0);
                ps.popPose();
            }
            Random rand = new Random(time / 5);
            rand.nextBoolean();
            for (int i = -1; i <= 1; i += 2) for (int j = -1; j <= 1; j += 2) if (rand.nextInt(4) == 0) {
                ps.pushPose();
                ps.translate(0, 0.75, 0);
                int start = (int) (System.currentTimeMillis() % 1000) / 50;
                BeamPronter.prontBeam(ps, buf, new Vec3(0.4375 * i, 1.1875, 0.4375 * j), BeamPronter.EnumWaveType.RANDOM, BeamPronter.EnumBeamType.SOLID, 0x404040, 0x002040, start, 15, 0.0625F, 3, 0.025F);
                BeamPronter.prontBeam(ps, buf, new Vec3(0.4375 * i, 1.1875, 0.4375 * j), BeamPronter.EnumWaveType.RANDOM, BeamPronter.EnumBeamType.SOLID, 0x404040, 0x002040, start, 1, 0, 3, 0.025F);
                ps.popPose();
            }
        }).viewDistance(64).register();
    }

    // ── Plasmaschmiede ───────────────────────────────────────────────────────────────────

    private static final String FORGE_OBJ = "models/block/machines/plasma_forge.obj";
    private static final String TORUS_OBJ = "models/block/machines/torus.obj";
    private static final ResourceLocation FORGE_SPRITE = rl("block/machine/plasma_forge");
    private static final ResourceLocation TORUS_SPRITE = rl("block/machine/torus");
    private static final FusionPlasmaForgeRenderer FORGE_LEGACY = new FusionPlasmaForgeRenderer(null);
    private static final String[] STRIKER_PARTS = { "SliderStriker", "ArmLowerStriker", "ArmUpperStriker", "StrikerMount", "StrikerRight", "PistonRight", "StrikerLeft", "PistonLeft" };
    private static final String[] JET_PARTS = { "SliderJet", "ArmLowerJet", "ArmUpperJet", "Jet" };
    private static final ObjAtlasMesh FORGE_BOLTS = ObjAtlasMesh.of("hbm_m/plasma_forge/bolts", b -> {
        b.pose.translate(-2D, 0D, 0D);
        b.group(TORUS_OBJ, "Bolts1", TORUS_SPRITE);
    });

    private static ObjAtlasMesh forgePart(String group) {
        return ObjAtlasMesh.groups(FORGE_OBJ, FORGE_SPRITE, "hbm_m/plasma_forge/" + group.toLowerCase(java.util.Locale.ROOT), group);
    }

    /** {@code FusionTorusRenderer.applyFacing(state, pose, baseYaw)} auf den Blocktransform. */
    private static void applyFacing(BlockState state, LegacyAnimator a, float baseYaw) {
        if (!state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            if (baseYaw != 0F) rotY(a, baseYaw);
            return;
        }
        float rot = switch (state.getValue(HorizontalDirectionalBlock.FACING)) {
            case SOUTH -> 180F;
            case EAST -> 270F;
            case WEST -> 90F;
            default -> 0F;
        } + baseYaw;
        if (rot != 0F) rotY(a, rot);
    }

    /** Stoesselarm bis Glied {@code stage} (Reihenfolge {@link #STRIKER_PARTS}). */
    private static boolean striker(FusionPlasmaForgeBlockEntity be, float pt, PoseStack ps, int stage) {
        double[] a = be.armStriker.getPositions(pt);
        ps.mulPose(Axis.YP.rotationDegrees((float) (be.prevRing + (be.ring - be.prevRing) * pt)));
        if (stage == 0) return true;
        ps.translate(-2.75D, 2.5D, 0D);
        ps.mulPose(Axis.ZP.rotationDegrees((float) -a[0]));
        ps.translate(2.75D, -2.5D, 0D);
        if (stage == 1) return true;
        ps.translate(-2.75D, 3.75D, 0D);
        ps.mulPose(Axis.ZP.rotationDegrees((float) -a[1]));
        ps.translate(2.75D, -3.75D, 0D);
        if (stage == 2) return true;
        ps.translate(-1.5D, 3.75D, 0D);
        ps.mulPose(Axis.ZP.rotationDegrees((float) -a[2]));
        ps.translate(1.5D, -3.75D, 0D);
        if (stage == 3) return true;
        if (stage == 4 || stage == 5) {
            ps.translate(0D, 3.375D, 0.5D);
            ps.mulPose(Axis.XP.rotationDegrees((float) a[3]));
            ps.translate(0D, -3.375D, -0.5D);
            if (stage == 5) ps.translate(0D, -a[4], 0D);
            return true;
        }
        ps.translate(0D, 3.375D, -0.5D);
        ps.mulPose(Axis.XP.rotationDegrees((float) -a[3]));
        ps.translate(0D, -3.375D, 0.5D);
        if (stage == 7) ps.translate(0D, -a[5], 0D);
        return true;
    }

    /** Duesenarm bis Glied {@code stage} (Reihenfolge {@link #JET_PARTS}). */
    private static boolean jet(FusionPlasmaForgeBlockEntity be, float pt, PoseStack ps, int stage) {
        double[] a = be.armJet.getPositions(pt);
        ps.mulPose(Axis.YP.rotationDegrees((float) (be.prevRing + (be.ring - be.prevRing) * pt)));
        if (stage == 0) return true;
        ps.translate(2.75D, 2.5D, 0D);
        ps.mulPose(Axis.ZP.rotationDegrees((float) a[0]));
        ps.translate(-2.75D, -2.5D, 0D);
        if (stage == 1) return true;
        ps.translate(2.75D, 3.75D, 0D);
        ps.mulPose(Axis.ZP.rotationDegrees((float) a[1]));
        ps.translate(-2.75D, -3.75D, 0D);
        if (stage == 2) return true;
        ps.translate(1.5D, 3.75D, 0D);
        ps.mulPose(Axis.ZP.rotationDegrees((float) a[2]));
        ps.translate(-1.5D, -3.75D, 0D);
        return true;
    }

    private static void registerPlasmaForge() {
        MachineSpecBuilder<FusionPlasmaForgeBlockEntity> b = MachineRenderers.machine("hbm_m/fusion_plasma_forge", ModBlockEntities.FUSION_PLASMA_FORGE_BE.get(), FusionPlasmaForgeBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5D, 0D, 0.5D);
                    applyFacing(be.getBlockState(), a, 90F);
                    rotY(a, 90F);
                });
        when(b, "bolts", be -> be.connected, FORGE_BOLTS);
        for (int i = 0; i < STRIKER_PARTS.length; i++) {
            final int stage = i;
            mesh(b, "striker" + i, (be, f, t, ps) -> striker(be, f, ps, stage), forgePart(STRIKER_PARTS[i]));
        }
        for (int i = 0; i < JET_PARTS.length; i++) {
            final int stage = i;
            mesh(b, "jet" + i, (be, f, t, ps) -> jet(be, f, ps, stage), forgePart(JET_PARTS[i]));
        }
        b.hook((be, pt, pose, buffer, light, overlay, api) -> {
            Map<String, List<float[]>> obj = RBMKColumnRenderer.getObj(FORGE_OBJ);
            if (obj.isEmpty()) return;
            TextureAtlasSprite sprite = RBMKColumnRenderer.sprite(RefStrings.MODID, "block/machine/plasma_forge");
            VertexConsumer vc = buffer.getBuffer(RenderType.cutout());
            FORGE_LEGACY.renderPlasma(be, vc, pose, buffer, obj, sprite, light, overlay);
            FORGE_LEGACY.renderRecipeItem(be, pose, buffer, pt, light);
            FORGE_LEGACY.renderBeam(be, pose, buffer, pt);
            if (be.didProcess && be.armJet.angles[2] == be.armJet.prevAngles[2] && be.armJet.angles[2] != 0) {
                pose.pushPose();
                jet(be, pt, pose, 3);
                FORGE_LEGACY.renderJetFlame(be, pose, buffer);
                pose.popPose();
            }
        }).viewDistance(192).register();
    }

    // ── Fusionstorus ─────────────────────────────────────────────────────────────────────

    private static final FusionTorusRenderer TORUS_LEGACY = new FusionTorusRenderer(null);
    private static final ObjAtlasMesh TORUS_MAGNET = ObjAtlasMesh.groups(TORUS_OBJ, TORUS_SPRITE, "hbm_m/torus/magnet", "Magnet");
    /** Bolzenkraenze - Zuordnung 1:1 aus dem Original (0->Bolts2, 1->Bolts4, 2->Bolts3, 3->Bolts1). */
    private static final ObjAtlasMesh[] TORUS_BOLTS = {
            ObjAtlasMesh.groups(TORUS_OBJ, TORUS_SPRITE, "hbm_m/torus/bolts2", "Bolts2"),
            ObjAtlasMesh.groups(TORUS_OBJ, TORUS_SPRITE, "hbm_m/torus/bolts4", "Bolts4"),
            ObjAtlasMesh.groups(TORUS_OBJ, TORUS_SPRITE, "hbm_m/torus/bolts3", "Bolts3"),
            ObjAtlasMesh.groups(TORUS_OBJ, TORUS_SPRITE, "hbm_m/torus/bolts1", "Bolts1") };

    private static void registerTorus() {
        MachineSpecBuilder<FusionTorusBlockEntity> b = MachineRenderers.machine("hbm_m/fusion_torus", ModBlockEntities.FUSION_TORUS_BE.get(), FusionTorusBlockEntity.class)
                .blockTransform((be, a) -> {
                    a.translate(0.5D, 0D, 0.5D);
                    if (be.tilted) {
                        a.translate(0, -1, 0);
                        a.rotate(10, 0, 0, 1);
                        a.rotate(5, 0, 1, 0);
                    }
                });
        mesh(b, "magnet", (be, f, t, ps) -> {
            ps.mulPose(Axis.YP.rotationDegrees(Mth.lerp(f, be.prevMagnet, be.magnet)));
            return true;
        }, TORUS_MAGNET);
        // Der Magnetring ist das groesste Teil der Maschine: jenseits der Animations-Sichtweite in Modelllage statt leer
        when(b, "magnet_far", be -> RenderDistanceHelper.computeAnimatedFade(be) < 0, TORUS_MAGNET);
        for (int i = 0; i < 4; i++) {
            final int c = i;
            when(b, "bolts" + i, be -> be.connections[c], TORUS_BOLTS[i]);
        }
        b.hook((be, pt, pose, buffer, light, overlay, api) -> {
            Map<String, List<float[]>> obj = RBMKColumnRenderer.getObj(TORUS_OBJ);
            if (obj.isEmpty()) return;
            TORUS_LEGACY.renderPlasma(be, pose, buffer, obj);
        }).viewDistance(256).register();
    }

    public static void register() {
        registerAssemblyFactory();
        registerRedd();
        registerSocket();
        registerPlasmaForge();
        registerTorus();
    }
}
