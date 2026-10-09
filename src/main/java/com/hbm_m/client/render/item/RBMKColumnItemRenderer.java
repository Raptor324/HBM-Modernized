//? if forge || neoforge {
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
 * loader that doesn't reliably bake in this multi-loader build (see task history) - this hands
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

        // vI: Autolader im Inventar 1:1 RenderRBMKAutoloader#getRenderer (ItemRenderBase):
        // renderInventory T(0,-6,0) S(1.75); renderCommon Ry(180), Base + Piston.
        if (displayContext == ItemDisplayContext.GUI && be instanceof com.hbm_m.blockentity.machines.rbmk.RBMKAutoloaderBlockEntity) {
            java.util.Map<String, java.util.List<float[]>> obj = com.hbm_m.client.render.implementations.RBMKColumnRenderer.getObj("models/rbmk/models/autoloader.obj");
            net.minecraft.client.renderer.texture.TextureAtlasSprite sprite = com.hbm_m.client.render.implementations.RBMKColumnRenderer.sprite(com.hbm_m.lib.RefStrings.MODID, "block/rbmk/model_rbmk_autoloader");
            com.mojang.blaze3d.vertex.VertexConsumer vc = buffer.getBuffer(net.minecraft.client.renderer.RenderType.solid());
            poseStack.pushPose();
            OrigInventoryTransform.apply(poseStack);
            poseStack.translate(0, -6, 0);
            poseStack.scale(1.75F, 1.75F, 1.75F);
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180));
            for (String part : new String[] {"Base", "Piston"}) {
                java.util.List<float[]> g = obj.get(part);
                if (g != null) com.hbm_m.client.render.implementations.RBMKColumnRenderer.renderObjGroup(vc, poseStack.last().pose(), g, sprite, 1f, 1f, 1f, packedLight, packedOverlay);
            }
            poseStack.popPose();
            return;
        }

        boolean column = be instanceof RBMKColumnBlockEntity;
        if (!column && !(be instanceof com.hbm_m.blockentity.machines.MachineRbmkConsoleBlockEntity)) {
            // vI: Dampfein-/-auslass sind im Original einfache Bloecke (RBMKInlet/RBMKOutlet) ohne Saeulenrenderer ->
            // Blockmodell zeichnen statt nichts (Icon war leer).
            poseStack.pushPose();
            applyDisplay(stack, displayContext, poseStack);
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, poseStack, buffer, packedLight, packedOverlay);
            poseStack.popPose();
            return;
        }

        poseStack.pushPose();
        // vI: Anzeige-Waechter unterdrueckt die Display-Werte des builtin/entity-Modells (Blockwerte aus dem Datagen)
        // -> hier anwenden, sonst stehen die Saeulen ungedreht/frontal im Slot.
        if (column) applyDisplay(stack, displayContext, poseStack);
        // RBMKColumnRenderer draws the column at its true in-world size: RBMKDials.COLUMN_HEIGHT+1
        // blocks tall, plus a 0.25 lid plate on top. Handed to the item renderer unscaled that is
        // four times the height of an item slot, so it would render as a single cube's worth of
        // column with the rest cut off above the slot. Shrink the whole thing to fit the unit cube
        // the item transforms expect, keeping it centred in X/Z and standing on the bottom face -
        // the icon then shows the complete column instead of its lowest block.
        if (be instanceof RBMKColumnBlockEntity) {
            // vI: 1:1 renderInventoryBlock des Originals (RenderRBMKRod/-Reflector: T(0,-0.675,0), RenderRBMKControl
            // fuer alle uebrigen Saeulen: T(0,-0.75,0); jeweils S(0.35)), Blockmitte als Ursprung.
            Object b = state.getBlock();
            float dy = b instanceof com.hbm_m.block.machines.rbmk.RBMKRodBlock
                    || b instanceof com.hbm_m.block.machines.rbmk.RBMKReflectorBlock ? -0.675F : -0.75F;
            poseStack.translate(0.5F, 0.5F + dy, 0.5F);
            poseStack.scale(0.35F, 0.35F, 0.35F);
            poseStack.translate(-0.5F, 0.0F, -0.5F);
        }
        Minecraft.getInstance().getBlockEntityRenderDispatcher()
                .renderItem(be, poseStack, buffer, packedLight, packedOverlay);
        poseStack.popPose();
    }

    /** Wie ItemRenderer: T(.5) * display(ctx) * T(-.5). */
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
//?}
