//? if forge {
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

    /** Matches the lid plate thickness in {@code RBMKColumnRenderer}. */
    private static final float LID_HEIGHT = 0.25F;

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
        if (!(be instanceof RBMKColumnBlockEntity)
                && !(be instanceof com.hbm_m.blockentity.machines.MachineRbmkConsoleBlockEntity)) return;

        poseStack.pushPose();
        // RBMKColumnRenderer draws the column at its true in-world size: RBMKDials.COLUMN_HEIGHT+1
        // blocks tall, plus a 0.25 lid plate on top. Handed to the item renderer unscaled that is
        // four times the height of an item slot, so it would render as a single cube's worth of
        // column with the rest cut off above the slot. Shrink the whole thing to fit the unit cube
        // the item transforms expect, keeping it centred in X/Z and standing on the bottom face -
        // the icon then shows the complete column instead of its lowest block.
        if (be instanceof RBMKColumnBlockEntity) {
            float columnHeight = com.hbm_m.handler.rbmk.RBMKDials.COLUMN_HEIGHT + 1 + LID_HEIGHT;
            float scale = 1.0F / columnHeight;
            poseStack.translate(0.5F, 0.0F, 0.5F);
            poseStack.scale(scale, scale, scale);
            poseStack.translate(-0.5F, 0.0F, -0.5F);
        }
        Minecraft.getInstance().getBlockEntityRenderDispatcher()
                .renderItem(be, poseStack, buffer, packedLight, packedOverlay);
        poseStack.popPose();
    }
}
//?}
