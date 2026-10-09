package com.hbm_m.client;

import com.hbm_m.platform.StackNbt;

import org.joml.Matrix4f;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.client.overlay.OverlayInfoToast;
import com.hbm_m.item.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 zweiter Teil von {@code BlockRebar.renderRebar}: haelt man den {@code rebar_placer} mit gesetztem ersten
 * Punkt, zeigt ein weisser Rahmen den Bereich bis zum angeschauten Block und der Hinweis "vorhanden / benoetigt"
 * (rot, wenn es nicht reicht).
 */
public final class RebarPlacerPreview {

    private RebarPlacerPreview() { }

    /** Kennung des Hinweisfelds (Original ServerProxy.ID_CABLE). */
    private static final int ID_CABLE = 3;

    public static void render(PoseStack pose, Vec3 camera) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        ItemStack held = player.getMainHandItem();

        if (held.isEmpty() || held.getItem() != ModItems.REBAR_PLACER.get() || !StackNbt.has(held) || !StackNbt.read(held).contains("pos")
                || mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK) return;

        int[] pos = StackNbt.read(held).getIntArray("pos");
        if (pos.length < 3) return;
        BlockHitResult mop = (BlockHitResult) mc.hitResult;
        BlockPos target = mop.getBlockPos().relative(mop.getDirection());
        int iX = target.getX();
        int iY = target.getY();
        int iZ = target.getZ();

        float minX = (float) (Math.min(pos[0], iX) + 0.125 - camera.x);
        float maxX = (float) (Math.max(pos[0], iX) + 0.875 - camera.x);
        float minY = (float) (Math.min(pos[1], iY) + 0.125 - camera.y);
        float maxY = (float) (Math.max(pos[1], iY) + 0.875 - camera.y);
        float minZ = (float) (Math.min(pos[2], iZ) + 0.125 - camera.z);
        float maxZ = (float) (Math.max(pos[2], iZ) + 0.875 - camera.z);

        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer tess = buffers.getBuffer(RenderType.lines());
        Matrix4f m = pose.last().pose();

        // top
        line(tess, m, minX, maxY, minZ, minX, maxY, maxZ);
        line(tess, m, minX, maxY, maxZ, maxX, maxY, maxZ);
        line(tess, m, maxX, maxY, maxZ, maxX, maxY, minZ);
        line(tess, m, maxX, maxY, minZ, minX, maxY, minZ);
        // bottom
        line(tess, m, minX, minY, minZ, minX, minY, maxZ);
        line(tess, m, minX, minY, maxZ, maxX, minY, maxZ);
        line(tess, m, maxX, minY, maxZ, maxX, minY, minZ);
        line(tess, m, maxX, minY, minZ, minX, minY, minZ);
        // sides
        line(tess, m, minX, minY, minZ, minX, maxY, minZ);
        line(tess, m, maxX, minY, minZ, maxX, maxY, minZ);
        line(tess, m, maxX, minY, maxZ, maxX, maxY, maxZ);
        line(tess, m, minX, minY, maxZ, minX, maxY, maxZ);

        buffers.endBatch(RenderType.lines());

        int rebarLeft = 0;
        for (ItemStack s : player.getInventory().items) {
            if (s.is(ModBlocks.REBAR.get().asItem())) rebarLeft += s.getCount();
        }
        int rebarRequired = (Math.max(pos[0], iX) - Math.min(pos[0], iX) + 1) * (Math.max(pos[1], iY) - Math.min(pos[1], iY) + 1) * (Math.max(pos[2], iZ) - Math.min(pos[2], iZ) + 1);
        OverlayInfoToast.show(Component.literal(rebarLeft + " / " + rebarRequired).withStyle(rebarRequired > rebarLeft ? ChatFormatting.RED : ChatFormatting.GREEN), 20, ID_CABLE);
    }

    private static void line(VertexConsumer buf, Matrix4f m, float x0, float y0, float z0, float x1, float y1, float z1) {
        float dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len == 0) len = 1;
        dx /= len; dy /= len; dz /= len;
        //? if < 1.21.1 {
        buf.vertex(m, x0, y0, z0).color(1F, 1F, 1F, 1F).normal(dx, dy, dz).endVertex();
        buf.vertex(m, x1, y1, z1).color(1F, 1F, 1F, 1F).normal(dx, dy, dz).endVertex();
        //?} else {
        /*buf.addVertex(m, x0, y0, z0).setColor(1F, 1F, 1F, 1F).setNormal(dx, dy, dz);
        buf.addVertex(m, x1, y1, z1).setColor(1F, 1F, 1F, 1F).setNormal(dx, dy, dz);
        *///?}
    }
}
