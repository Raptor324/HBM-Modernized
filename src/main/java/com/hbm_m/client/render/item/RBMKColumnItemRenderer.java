package com.hbm_m.client.render.item;

import com.hbm_m.blockentity.machines.rbmk.RBMKColumnBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.BlockItem;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generic item renderer for every RBMK column block (fuel, moderator, absorber, control rods,
 * panels, the console, ...): instead of relying on a static baked item model - which for these
 * blocks is either a plain untextured cube or (for the console) a Forge-only custom model
 * loader that doesn't reliably bake in this multi-loader build - this hands
 * the item off to a throwaway {@link BlockEntity} instance at the origin and renders it through
 * the exact same {@code BlockEntityRenderer} already used in-world (RBMKColumnRenderer,
 * MachineRbmkConsoleRenderer, ...), matching the pattern the 1.18.2 community remake
 * (nucleartech's {@code CustomBEWLR}/{@code SpecialRenderingBlockEntityItem}) uses for the same
 * problem. No new rendering code needed - every fix already made to the in-world renderers
 * (lid textures, control rod caps, the real console mesh, Cherenkov glow, ...) applies to the
 * held/inventory/ground icon automatically too.
 */
public class RBMKColumnItemRenderer extends BlockEntityWithoutLevelRenderer {

    public static final RBMKColumnItemRenderer INSTANCE = new RBMKColumnItemRenderer();

    private final Map<BlockState, BlockEntity> cache = new ConcurrentHashMap<>();

    private RBMKColumnItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                              MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) return;
        BlockState state = blockItem.getBlock().defaultBlockState();

        if (!(state.getBlock() instanceof net.minecraft.world.level.block.EntityBlock entityBlock)) return;
        BlockEntity be = cache.computeIfAbsent(state, s -> entityBlock.newBlockEntity(BlockPos.ZERO, s));

        // Autoloader: direct OBJ render for both GUI (1:1 ItemRenderBase) and in-hand/world
        // contexts. Nucleus MachineBer skips throwaway BEs without a live client level, so
        // BlockEntityRenderDispatcher.renderItem draws nothing.
        if (be instanceof com.hbm_m.blockentity.machines.rbmk.RBMKAutoloaderBlockEntity) {
            java.util.Map<String, java.util.List<float[]>> obj = com.hbm_m.client.render.implementations.RBMKColumnRenderer.getObj("models/rbmk/models/autoloader.obj");
            net.minecraft.client.renderer.texture.TextureAtlasSprite sprite = com.hbm_m.client.render.implementations.RBMKColumnRenderer.sprite(com.hbm_m.lib.RefStrings.MODID, "block/rbmk/model_rbmk_autoloader");
            com.mojang.blaze3d.vertex.VertexConsumer vc = buffer.getBuffer(net.minecraft.client.renderer.RenderType.entityCutoutNoCull(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS));
            poseStack.pushPose();
            if (displayContext == ItemDisplayContext.GUI) {
                OrigInventoryTransform.apply(poseStack);
                poseStack.translate(0, -6, 0);
                poseStack.scale(1.75F, 1.75F, 1.75F);
            } else {
                applyDisplayOrBlockDefault(stack, displayContext, poseStack);
                poseStack.translate(0.5F, 0.0F, 0.5F);
                poseStack.scale(0.25F, 0.25F, 0.25F);
            }
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180));
            for (String part : new String[] {"Base", "Piston"}) {
                java.util.List<float[]> g = obj.get(part);
                if (g != null) com.hbm_m.client.render.implementations.RBMKColumnRenderer.renderObjGroup(vc, poseStack.last().pose(), g, sprite, 1f, 1f, 1f, packedLight, packedOverlay);
            }
            poseStack.popPose();
            return;
        }

        // Crane Console: direct OBJ render for both GUI (1:1 ItemRenderLibrary) and in-hand/world
        // contexts (same Nucleus throwaway-BE reason as autoloader above).
        if (be instanceof com.hbm_m.blockentity.machines.rbmk.RBMKCraneConsoleBlockEntity) {
            java.util.Map<String, java.util.List<float[]>> obj = com.hbm_m.client.render.implementations.RBMKColumnRenderer.getObj("models/rbmk/models/crane_console.obj");
            net.minecraft.client.renderer.texture.TextureAtlasSprite sprite = com.hbm_m.client.render.implementations.RBMKColumnRenderer.sprite(com.hbm_m.lib.RefStrings.MODID, "block/machine/crane_console");
            com.mojang.blaze3d.vertex.VertexConsumer vc = buffer.getBuffer(net.minecraft.client.renderer.RenderType.entityCutoutNoCull(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS));
            poseStack.pushPose();
            if (displayContext == ItemDisplayContext.GUI) {
                OrigInventoryTransform.apply(poseStack);
                poseStack.translate(0, -3, 0);
                poseStack.scale(3.5F, 3.5F, 3.5F);
            } else {
                applyDisplayOrBlockDefault(stack, displayContext, poseStack);
                poseStack.translate(0.5F, 0.0F, 0.5F);
                poseStack.scale(0.35F, 0.35F, 0.35F);
            }
            for (String part : new String[] {"Console_Coonsole", "Joystick", "Meter1", "Meter2"}) {
                java.util.List<float[]> g = obj.get(part);
                if (g != null) com.hbm_m.client.render.implementations.RBMKColumnRenderer.renderObjGroup(vc, poseStack.last().pose(), g, sprite, 1f, 1f, 1f, packedLight, packedOverlay);
            }
            poseStack.popPose();
            return;
        }

        boolean isSpecialMachine = be instanceof com.hbm_m.blockentity.machines.MachineRbmkConsoleBlockEntity;
        boolean column = be instanceof RBMKColumnBlockEntity && !isSpecialMachine;
        // RBMK columns: render directly via RBMKColumnInventoryRender in ALL contexts (GUI, hand,
        // ground, fixed, head). In-world columns moved to Nucleus MachineBer, whose AbstractPartBasedRenderer
        // skips non-world throwaway BEs (level == null), so BlockEntityRenderDispatcher.renderItem drew nothing.
        if (column) {
            String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
            RBMKColumnInventoryRender.Kind kind = RBMKColumnInventoryRender.kind(id);
            if (kind != null) {
                poseStack.pushPose();
                if (displayContext == ItemDisplayContext.GUI) {
                    RBMKColumnInventoryRender.applyGuiBlockTransform(poseStack);
                } else {
                    applyDisplayOrBlockDefault(stack, displayContext, poseStack);
                    poseStack.translate(0.5F, 0.5F, 0.5F);
                    RBMKColumnInventoryRender.captureBasePose(poseStack);
                }
                RBMKColumnInventoryRender.render(id, kind, poseStack, buffer, packedLight);
                poseStack.popPose();
                return;
            }
        }
        if (!column && !isSpecialMachine) {
            // Steam inlet/outlet are simple blocks (RBMKInlet/RBMKOutlet) in original without column renderer ->
            // render block model.
            poseStack.pushPose();
            applyDisplayOrBlockDefault(stack, displayContext, poseStack);
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, poseStack, buffer, packedLight, packedOverlay);
            poseStack.popPose();
            return;
        }

        poseStack.pushPose();
        Minecraft.getInstance().getBlockEntityRenderDispatcher()
                .renderItem(be, poseStack, buffer, packedLight, packedOverlay);
        poseStack.popPose();
    }

    /** Vanilla minecraft:block/block display fallbacks when a builtin/entity item JSON omits "display". */
    private static final net.minecraft.client.renderer.block.model.ItemTransform BLOCK_GROUND =
            new net.minecraft.client.renderer.block.model.ItemTransform(
                    new org.joml.Vector3f(0F, 0F, 0F),
                    new org.joml.Vector3f(0F, 3F / 16F, 0F),
                    new org.joml.Vector3f(0.25F, 0.25F, 0.25F));
    private static final net.minecraft.client.renderer.block.model.ItemTransform BLOCK_FIXED =
            new net.minecraft.client.renderer.block.model.ItemTransform(
                    new org.joml.Vector3f(0F, 0F, 0F),
                    new org.joml.Vector3f(0F, 0F, 0F),
                    new org.joml.Vector3f(0.5F, 0.5F, 0.5F));
    private static final net.minecraft.client.renderer.block.model.ItemTransform BLOCK_THIRD_PERSON =
            new net.minecraft.client.renderer.block.model.ItemTransform(
                    new org.joml.Vector3f(75F, 45F, 0F),
                    new org.joml.Vector3f(0F, 2.5F / 16F, 0F),
                    new org.joml.Vector3f(0.375F, 0.375F, 0.375F));
    private static final net.minecraft.client.renderer.block.model.ItemTransform BLOCK_FIRST_PERSON_RIGHT =
            new net.minecraft.client.renderer.block.model.ItemTransform(
                    new org.joml.Vector3f(0F, 45F, 0F),
                    new org.joml.Vector3f(0F, 0F, 0F),
                    new org.joml.Vector3f(0.4F, 0.4F, 0.4F));
    private static final net.minecraft.client.renderer.block.model.ItemTransform BLOCK_FIRST_PERSON_LEFT =
            new net.minecraft.client.renderer.block.model.ItemTransform(
                    new org.joml.Vector3f(0F, 225F, 0F),
                    new org.joml.Vector3f(0F, 0F, 0F),
                    new org.joml.Vector3f(0.4F, 0.4F, 0.4F));

    private static void applyDisplayOrBlockDefault(ItemStack stack, ItemDisplayContext ctx, PoseStack ps) {
        net.minecraft.client.resources.model.BakedModel displayModel = Minecraft.getInstance().getItemRenderer().getModel(stack, null, null, 0);
        boolean leftHand = ctx == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || ctx == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
        net.minecraft.client.renderer.block.model.ItemTransform transform =
                com.hbm_m.client.compat.itemtransformhelper.ItemTransformHelperCompat.resolveDisplayTransforms(displayModel,
                        com.hbm_m.client.compat.itemtransformhelper.ItemTransformHelperCompat.unwrapToDelegate(displayModel))
                        .getTransform(ctx);
        if (transform == net.minecraft.client.renderer.block.model.ItemTransform.NO_TRANSFORM) {
            transform = switch (ctx) {
                case GROUND -> BLOCK_GROUND;
                case FIXED -> BLOCK_FIXED;
                case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND -> BLOCK_THIRD_PERSON;
                case FIRST_PERSON_RIGHT_HAND -> BLOCK_FIRST_PERSON_RIGHT;
                case FIRST_PERSON_LEFT_HAND -> BLOCK_FIRST_PERSON_LEFT;
                default -> net.minecraft.client.renderer.block.model.ItemTransform.NO_TRANSFORM;
            };
        }
        ps.translate(0.5F, 0.5F, 0.5F);
        transform.apply(leftHand, ps);
        ps.translate(-0.5F, -0.5F, -0.5F);
    }

    /** Standard ItemRenderer transform: T(.5) * display(ctx) * T(-.5). */
    static void applyDisplay(ItemStack stack, ItemDisplayContext ctx, PoseStack ps) {
        net.minecraft.client.resources.model.BakedModel displayModel = Minecraft.getInstance().getItemRenderer().getModel(stack, null, null, 0);
        boolean leftHand = ctx == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || ctx == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
        ps.translate(0.5F, 0.5F, 0.5F);
        com.hbm_m.client.compat.itemtransformhelper.ItemTransformHelperCompat.resolveDisplayTransforms(displayModel,
                com.hbm_m.client.compat.itemtransformhelper.ItemTransformHelperCompat.unwrapToDelegate(displayModel))
                .getTransform(ctx).apply(leftHand, ps);
        ps.translate(-0.5F, -0.5F, -0.5F);
    }
}
