package com.hbm_m.client.render;

import java.util.HashMap;
import java.util.Map;

import com.hbm_m.block.network.ConveyorBendableBlock;
import com.hbm_m.item.tool.ItemConveyorWand;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code RenderOverhead.setActionPreview/renderActionPreview} fuer den Foerderband-Stab: die geplante Strecke wird
 * als volle Helligkeit gerendert, tuerkis ({@code 0,1,1}) wenn sie gebaut werden kann, sonst rot. Die Berechnung
 * (inkl. Kanten-Einrasten und Cache ueber Blickziel/Seite/Drehung) stammt aus {@code ItemConveyorWand.onUpdate}.
 */
public final class ConveyorWandPreview {

    private ConveyorWandPreview() {}

    private static volatile Map<BlockPos, BlockState> preview;
    private static boolean previewSuccess;
    private static boolean clearPreview;

    private static BlockPos lastPos;
    private static Direction lastSide;
    private static float lastYaw;

    /** Prevents thread unsafe null exception */
    public static void clear() {
        clearPreview = true;
        lastPos = null;
    }

    public static void update(ItemConveyorWand wand, ItemStack stack, Level world, Player player) {
        if (!stack.hasTag()) {
            clear();
            return;
        }

        HitResult hit = Minecraft.getInstance().hitResult;
        if (!(hit instanceof BlockHitResult mop) || hit.getType() != HitResult.Type.BLOCK) {
            clear();
            return;
        }

        BlockPos pos = mop.getBlockPos();
        Direction side = mop.getDirection();

        BlockState onState = world.getBlockState(pos);
        if (onState.getBlock() instanceof ConveyorBendableBlock bendable) {
            Direction moveDir = bendable.getInputDirection(onState);
            if (world.getBlockState(pos.relative(moveDir)).canBeReplaced()) {
                side = moveDir;
            }
        }

        if (lastPos != null && pos.equals(lastPos) && side == lastSide && Math.abs(lastYaw - player.getYRot()) < 15) return;
        lastPos = pos;
        lastYaw = player.getYRot();
        lastSide = side;

        CompoundTag nbt = stack.getTag();
        BlockPos start = new BlockPos(nbt.getInt("x"), nbt.getInt("y"), nbt.getInt("z"));
        Direction sSide = Direction.from3DDataValue(nbt.getInt("side"));
        int count = nbt.getInt("count");

        Map<BlockPos, BlockState> wiaj = new HashMap<>();
        boolean pathSuccess = ItemConveyorWand.construct(world, wiaj::put, ItemConveyorWand.getType(stack), player, start, sSide, pos, side, count) > 0;

        clearPreview = false;
        preview = wiaj;
        previewSuccess = pathSuccess;
    }

    public static void render(PoseStack poseStack, Vec3 camera) {
        if (clearPreview) {
            preview = null;
            clearPreview = false;
        }

        Map<BlockPos, BlockState> blocks = preview;
        if (blocks == null || blocks.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(RenderType.cutout());
        RandomSource rand = RandomSource.create();

        float r = previewSuccess ? 0F : 1F;
        float g = previewSuccess ? 1F : 0F;
        float b = previewSuccess ? 1F : 0F;

        for (Map.Entry<BlockPos, BlockState> e : blocks.entrySet()) {
            BlockPos p = e.getKey();
            BakedModel model = mc.getBlockRenderer().getBlockModel(e.getValue());
            poseStack.pushPose();
            poseStack.translate(p.getX() - camera.x, p.getY() - camera.y, p.getZ() - camera.z);
            PoseStack.Pose pose = poseStack.last();
            for (Direction d : Direction.values()) {
                rand.setSeed(42L);
                for (BakedQuad q : model.getQuads(e.getValue(), d, rand)) {
                    consumer.putBulkData(pose, q, r, g, b, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                }
            }
            rand.setSeed(42L);
            for (BakedQuad q : model.getQuads(e.getValue(), null, rand)) {
                consumer.putBulkData(pose, q, r, g, b, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            }
            poseStack.popPose();
        }

        buffers.endBatch(RenderType.cutout());
    }
}
