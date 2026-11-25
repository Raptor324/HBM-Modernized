package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.MachineAdvancedAssemblerBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachinePrecAssBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.render.display.DisplayReference;
import com.hbm_m.client.render.display.PoseStackReferenceAdapter;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.client.render.machine.MachineRenderApi;
import com.hbm_m.client.render.machine.MachineRenderHook;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.client.render.machine.MachineSpec;
import com.hbm_m.compat.ContraptionRenderCompat;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Precision assembler ({@code machine_precass}) on the {@link MachineRenderers} factory —
 * 1:1 {@code RenderPrecAss}. Base is static; Frame is conditional (block above y+3);
 * Ring/Ring2 are GPU rotate joints; the FOUR arms are GPU joint chains over the single
 * "ArmLower1/ArmUpper1/Head1/Spike1" model parts, each rendered at a -90-degree yaw step
 * (the yaw folds into the ring-mount node: both rotate about the same Y axis through the
 * same pivot, so {@code R_y(spin) * R_y(-90*i) == R_y(spin - 90*i)}). The recipe icon is
 * an immediate hook.
 *
 * <p>Arm decomposition (legacy {@code renderArm}; all pivots in bake coordinates —
 * precass.nmesh bakes the JSON root transform T(0.5,0,0.5) into the quads):
 * <pre>
 *   mount:  R_y(spin - 90*i) through (0.5, 0, 0.5)     [node 0, ring lockstep]
 *   J0 (Lower): Rx through (0.5, 1.625, 0.5 + 0.9375)  by arm[0]
 *   J1 (Upper): Rx through (0.5, 2.375, 0.5 + 0.9375)  by arm[1]
 *   J2 (Head):  Rx through (0.5, 2.375, 0.5 + 0.4375)  by arm[2]
 *   J3 (Spike): T along Y by the lerped strikers[i]
 * </pre>
 * Each logical part declares the chain TRUNCATED at its own joint (lower = mount+J0,
 * upper = mount+J0..J1, head = mount+J0..J2, spike = mount+J0..J3) — 5 nodes / 5 words,
 * inside the chain budgets. The {@code off} = T(-0.5,0,-0.5) converts bake space to OBJ
 * space exactly like the legacy BER's baked multipart quads.
 */
public final class MachinePrecAssRenderer {

    private static final float ARM_PIVOT_Y_LOWER = 1.625f;
    private static final float ARM_PIVOT_Y_UPPER = 2.375f;
    private static final float ARM_Z_OFFSET = 0.9375f;
    private static final float ARM_HEAD_Z_OFFSET = 0.4375f;
    private static final float ARM_YAW_STEP = -90.0f;
    private static final float RECIPE_ICON_MAX_DIST_SQ = 35.0f * 35.0f;

    public static void register() {
        MachineRenderers.machine("precass", ModBlockEntities.PRECASS_BE.get(),
                MachinePrecAssBlockEntity.class)
            .staticPart("Base", MachinePrecAssRenderer::applyBakeOffset)
            .conditionalPart("Frame", MachinePrecAssRenderer::applyBakeOffset,
                    be -> be.getBlockState().hasProperty(MachineAdvancedAssemblerBlock.FRAME)
                            && be.getBlockState().getValue(MachineAdvancedAssemblerBlock.FRAME))
            // Ring + Ring2: GPU rotate around Y through the block center, lerped ring angle.
            .rotatingPart("Ring", 0f, 1f, 0f, 0.5f, 0f, 0.5f, -0.5f, 0f, -0.5f,
                    MachinePrecAssRenderer::ringAngle)
            .rotatingPart("Ring2", 0f, 1f, 0f, 0.5f, 0f, 0.5f, -0.5f, 0f, -0.5f,
                    MachinePrecAssRenderer::ringAngle)
            // 4 arms x 4 logical parts, each chain truncated at the part's own joint depth.
            .jointChainPart("ArmLower1", "ArmLower0", -0.5f, 0f, -0.5f, 0f, null, null,
                    armChainNodes(0, 1))
            .jointChainPart("ArmUpper1", "ArmUpper0", -0.5f, 0f, -0.5f, 0f, null, null,
                    armChainNodes(0, 2))
            .jointChainPart("Head1", "Head0", -0.5f, 0f, -0.5f, 0f, null, null,
                    armChainNodes(0, 3))
            .jointChainPart("Spike1", "Spike0", -0.5f, 0f, -0.5f, 0f, null, null,
                    armChainNodes(0, 4))
            .jointChainPart("ArmLower1", "ArmLower1", -0.5f, 0f, -0.5f, 0f, null, null,
                    armChainNodes(1, 1))
            .jointChainPart("ArmUpper1", "ArmUpper1", -0.5f, 0f, -0.5f, 0f, null, null,
                    armChainNodes(1, 2))
            .jointChainPart("Head1", "Head1", -0.5f, 0f, -0.5f, 0f, null, null,
                    armChainNodes(1, 3))
            .jointChainPart("Spike1", "Spike1", -0.5f, 0f, -0.5f, 0f, null, null,
                    armChainNodes(1, 4))
            .jointChainPart("ArmLower1", "ArmLower2", -0.5f, 0f, -0.5f, 0f, null, null,
                    armChainNodes(2, 1))
            .jointChainPart("ArmUpper1", "ArmUpper2", -0.5f, 0f, -0.5f, 0f, null, null,
                    armChainNodes(2, 2))
            .jointChainPart("Head1", "Head2", -0.5f, 0f, -0.5f, 0f, null, null,
                    armChainNodes(2, 3))
            .jointChainPart("Spike1", "Spike2", -0.5f, 0f, -0.5f, 0f, null, null,
                    armChainNodes(2, 4))
            .jointChainPart("ArmLower1", "ArmLower3", -0.5f, 0f, -0.5f, 0f, null, null,
                    armChainNodes(3, 1))
            .jointChainPart("ArmUpper1", "ArmUpper3", -0.5f, 0f, -0.5f, 0f, null, null,
                    armChainNodes(3, 2))
            .jointChainPart("Head1", "Head3", -0.5f, 0f, -0.5f, 0f, null, null,
                    armChainNodes(3, 3))
            .jointChainPart("Spike1", "Spike3", -0.5f, 0f, -0.5f, 0f, null, null,
                    armChainNodes(3, 4))
            .animationEpoch(MachinePrecAssRenderer::animationEpoch)
            // Frame is excluded from the item render per design (legacy item render parity
            // minus the scaffold); the arms render in their authored rest pose in item form.
            .itemExcept("Frame")
            .hook(new MachineRenderHook<MachinePrecAssBlockEntity>() {
                @Override
                public void render(MachinePrecAssBlockEntity be, float partialTick,
                                   PoseStack poseStack, MultiBufferSource bufferSource,
                                   int packedLight, int packedOverlay, MachineRenderApi api) {
                    MachinePrecAssRenderer.renderRecipeIcon(
                            be, poseStack, bufferSource, packedLight, packedOverlay, api);
                }

                @Override
                public boolean mayDrawContent(MachinePrecAssBlockEntity be) {
                    return !be.clientRecipeIcon.isEmpty();
                }
            })
            .register();
    }

    private MachinePrecAssRenderer() {}

    /** Bake-space compensation: precass.nmesh bakes the JSON root transform T(0.5,0,0.5). */
    private static boolean applyBakeOffset(MachinePrecAssBlockEntity be, float partialTick,
                                           long gameTime, PoseStack pose) {
        pose.translate(-0.5f, 0f, -0.5f);
        return true;
    }

    // -- Animation state (the BE updates its client fields in its own client tick) ------

    private static double ringAngle(MachinePrecAssBlockEntity be, float partialTick) {
        return Mth.lerp(partialTick, be.prevRing, be.ring);
    }

    /**
     * Machine animation epoch: stable only when ring, arms and strikers are stationary
     * (prev == curr) and the Frame flag is unchanged — enabling the frozen evaluation
     * skip and zero instance upload when idle.
     */
    private static long animationEpoch(MachinePrecAssBlockEntity be) {
        long h = 0x9E3779B97F4A7C15L;
        h = h * 31 + Double.hashCode(be.ring);
        h = h * 31 + Double.hashCode(be.prevRing);
        for (int i = 0; i < 3; i++) {
            h = h * 31 + Double.hashCode(be.armAngles[i]);
            h = h * 31 + Double.hashCode(be.prevArmAngles[i]);
        }
        for (int i = 0; i < 4; i++) {
            h = h * 31 + Double.hashCode(be.strikers[i]);
            h = h * 31 + Double.hashCode(be.prevStrikers[i]);
        }
        return h * 31 + (be.getBlockState().hasProperty(MachineAdvancedAssemblerBlock.FRAME)
                && be.getBlockState().getValue(MachineAdvancedAssemblerBlock.FRAME) ? 1L : 0L);
    }

    /**
     * The ring-spin mount used as NODE 0 of every arm chain. The legacy per-arm yaw
     * {@code R_y(-90*i)} commutes with the ring spin (both about the same Y axis through
     * the same pivot), so the mount angle is {@code spin - 90*i} — bit-identical to the
     * legacy {@code glRotated(spin); ...; glRotated(-90)} composition and 100% in
     * lockstep with the Ring part.
     */
    private static MachineSpec.JointNode<MachinePrecAssBlockEntity> armMountNode(int armIndex) {
        float yaw = ARM_YAW_STEP * armIndex;
        return MachineSpec.JointNode.rotate(0f, 1f, 0f, 0.5f, 0f, 0.5f,
                (be, pt, gt, out, off) -> {
                    out[off] = (float) ringAngle(be, pt) + yaw;
                    return true;
                });
    }

    @SuppressWarnings("unchecked")
    private static MachineSpec.JointNode<MachinePrecAssBlockEntity>[] armChainNodes(
            int armIndex, int depth) {
        // depth counts the ARM JOINTS (1..4); the mount joins as node 0 on top (1+depth nodes).
        MachineSpec.JointNode<MachinePrecAssBlockEntity>[] joints = armJoints(armIndex);
        MachineSpec.JointNode<MachinePrecAssBlockEntity>[] all =
                new MachineSpec.JointNode[depth + 1];
        all[0] = armMountNode(armIndex);
        System.arraycopy(joints, 0, all, 1, Math.min(depth, joints.length));
        return all;
    }

    /**
     * The arm FK chain, node-for-node the legacy {@code RenderPrecAss.renderArm} math with
     * the same pivots (in bake coordinates) and the same lerped angle sources:
     * <pre>
     *   J0 (Lower): Rx through (0.5, 1.625, 0.5 + zBase)  by armAngles[0]
     *   J1 (Upper): Rx through (0.5, 2.375, 0.5 + zBase)  by armAngles[1]
     *   J2 (Head):  Rx through (0.5, 2.375, 0.5 + headZ)  by armAngles[2]
     *   J3 (Spike): translate along Y by the lerped strikers[armIndex]
     * </pre>
     */
    private static MachineSpec.JointNode<MachinePrecAssBlockEntity>[] armJoints(int armIndex) {
        return new MachineSpec.JointNode[] {
                MachineSpec.JointNode.rotate(1f, 0f, 0f, 0.5f, ARM_PIVOT_Y_LOWER, 0.5f + ARM_Z_OFFSET,
                        armAngleFn(0)),
                MachineSpec.JointNode.rotate(1f, 0f, 0f, 0.5f, ARM_PIVOT_Y_UPPER, 0.5f + ARM_Z_OFFSET,
                        armAngleFn(1)),
                MachineSpec.JointNode.rotate(1f, 0f, 0f, 0.5f, ARM_PIVOT_Y_UPPER, 0.5f + ARM_HEAD_Z_OFFSET,
                        armAngleFn(2)),
                MachineSpec.JointNode.translate(0f, 1f, 0f,
                        strikerFn(armIndex))
        };
    }

    /** One joint's per-frame angle: the BE's lerped value. */
    private static MachineSpec.NodeParams<MachinePrecAssBlockEntity> armAngleFn(int joint) {
        return (be, pt, gt, out, off) -> {
            out[off] = (float) Mth.lerp(pt, be.prevArmAngles[joint], be.armAngles[joint]);
            return true;
        };
    }

    /** The arm's own striker lift: lerped in [-0.75, 0], legacy {@code glTranslated(0, striker, 0)}. */
    private static MachineSpec.NodeParams<MachinePrecAssBlockEntity> strikerFn(int armIndex) {
        return (be, pt, gt, out, off) -> {
            out[off] = (float) Mth.lerp(pt, be.prevStrikers[armIndex], be.strikers[armIndex]);
            return true;
        };
    }

    // -- Recipe icon (hook, legacy 35-block gate) ---------------------------------------

    private static void renderRecipeIcon(MachinePrecAssBlockEntity be, PoseStack poseStack,
                                         MultiBufferSource bufferSource,
                                         int packedLight, int packedOverlay, MachineRenderApi api) {
        // BE overload: bypass fade/cull for contraptions and the Sable sublevel.
        if (RenderSystem.isOnRenderThread()
                && !ContraptionRenderCompat.isContraptionRender(be)
                && com.hbm_m.client.render.RenderDistanceHelper.distanceSqToCamera(api.blockPos()) > RECIPE_ICON_MAX_DIST_SQ) {
            return;
        }

        ItemStack icon = be.clientRecipeIcon;
        if (icon.isEmpty()) return;

        var mc = Minecraft.getInstance();
        if (mc.player == null) return;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.translate(0.0, 1.0625, 0.0);

        boolean isBlock = icon.getItem() instanceof BlockItem;
        boolean rendered3d = false;
        if (isBlock) {
            BlockItem bi = (BlockItem) icon.getItem();
            var blockModel = mc.getBlockRenderer().getBlockModel(bi.getBlock().defaultBlockState());
            rendered3d = blockModel.isGui3d();
        }

        if (rendered3d) {
            poseStack.translate(0.0, -0.0625, 0.0);
        } else if (isBlock) {
            poseStack.translate(0.0, -0.125, 0.0);
            poseStack.scale(0.5F, 0.5F, 0.5F);
        } else {
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            poseStack.translate(0.0, -0.25, 0.0);
        }

        poseStack.scale(1.25F, 1.25F, 1.25F);

        if (rendered3d) {
            poseStack.translate(0.0, 0.1F, 0.0);
            poseStack.scale(1.25F, 1.25F, 1.25F);
            poseStack.translate(0.0, 0.05F, 0.0);
            poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
            poseStack.scale(0.25F, 0.25F, 0.25F);
        } else {
            poseStack.translate(0.0, 0.1F, 0.0);
            poseStack.scale(0.5128205F, 0.5128205F, 0.5128205F);
            poseStack.translate(0.0, -0.05F, 0.0);
        }

        mc.getItemRenderer().renderStatic(
                icon,
                ItemDisplayContext.NONE,
                packedLight,
                packedOverlay,
                poseStack,
                bufferSource,
                be.getLevel(),
                0
        );

        poseStack.popPose();
    }

    // ------------------------------------------------------------------
    // Иконка предмета: 1:1 RenderPrecAss#getRenderer (1.7.10, ItemRenderBase).
    // Как в оригинале, рендерер машины владеет и предметным рендером: класс TESR
    // реализовывал IItemRendererProvider и возвращал ItemRenderBase с общим renderArm.
    // Внешняя поза (renderInventory T(0,-2.75,0)·S(4.5) + renderCommon Ry(90)·S(0.75))
    // берётся из reference (machine_precass) — те же числа, что в 1.7.10. Тело следует
    // оригиналу: 1.7.10 рисует корпус + ЧЕТЫРЕ манипулятора (renderArm × 4 с поворотом 90°),
    // а не целой OBJ-моделью.
    // ------------------------------------------------------------------

    private static final ResourceLocation ITEM_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            RefStrings.MODID, "textures/block/machine/precass.png");
    private static SimpleObjModel itemModel;

    public static void renderItem(ItemStack stack, ItemDisplayContext ctx, PoseStack ps,
                                  MultiBufferSource buf, int light) {
        var entry = DisplayReference.entry("machine_precass");
        if (entry == null) return;
        if (!DisplayReference.applyReferencePose(new PoseStackReferenceAdapter(ps), entry, ctx, 0F)) return;

        SimpleObjModel model = itemModel();
        if (model == null) return;
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(ITEM_TEXTURE));

        model.renderPart("Base", ps, vc, light);
        // Frame (верхняя секция) в иконку не рисуем: в мире она условная (conditionalPart),
        // а с ней иконка показывает трёхблочную башню вместо станка.
        model.renderPart("Ring", ps, vc, light);
        model.renderPart("Ring2", ps, vc, light);

        double[] arm = {45, -30, 45};
        for (int i = 0; i < 4; i++) {
            renderArmIcon(ps, model, vc, arm, light);
            ps.mulPose(Axis.YP.rotationDegrees(90F));
        }
    }

    /** 1:1 {@code RenderPrecAss.renderArm}, striker заморожен в нуле (инвентарная поза). */
    private static void renderArmIcon(PoseStack ps, SimpleObjModel model, VertexConsumer vc,
                                      double[] arm, int light) {
        ps.pushPose();
        ps.translate(0, 1.625, 0.9375);
        ps.mulPose(Axis.XP.rotationDegrees((float) arm[0]));
        ps.translate(0, -1.625, -0.9375);
        model.renderPart("ArmLower1", ps, vc, light);

        ps.translate(0, 2.375, 0.9375);
        ps.mulPose(Axis.XP.rotationDegrees((float) arm[1]));
        ps.translate(0, -2.375, -0.9375);
        model.renderPart("ArmUpper1", ps, vc, light);

        ps.translate(0, 2.375, 0.4375);
        ps.mulPose(Axis.XP.rotationDegrees((float) arm[2]));
        ps.translate(0, -2.375, -0.4375);
        model.renderPart("Head1", ps, vc, light);
        model.renderPart("Spike1", ps, vc, light);
        ps.popPose();
    }

    private static SimpleObjModel itemModel() {
        SimpleObjModel m = itemModel;
        if (m == null) {
            m = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID,
                    "models/block/machines/assembly_machine.obj"));
            itemModel = m;
        }
        return m;
    }
}
