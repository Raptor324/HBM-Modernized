package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.MachineMassStorageBlock;
import com.hbm_m.blockentity.machines.MachineMassStorageBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

/**
 * 1:1 {@code RenderMassStorage}: Frontanzeige der Massenlager - Symbol des Filtergegenstands, Vorrat als Text
 * (gruen mit Schatten) und Fuellbalken (rot -> gruen), alles vollhell auf der Frontseite (FACING).
 */
public class MassStorageRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineMassStorageBlockEntity> {

    public MassStorageRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(MachineMassStorageBlockEntity storage, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ItemStack type = storage.getInventory().getStackInSlot(MachineMassStorageBlockEntity.SLOT_FILTER);
        if (type.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        Direction facing = storage.getBlockState().hasProperty(MachineMassStorageBlock.FACING)
                ? storage.getBlockState().getValue(MachineMassStorageBlock.FACING) : Direction.NORTH;
        // Original: dir = meta / 4 (0 Nord, 1 Sued, 2 West, 3 Ost)
        int dir = switch (facing) {
            case SOUTH -> 1;
            case WEST -> 2;
            case EAST -> 3;
            default -> 0;
        };
        int full = LightTexture.FULL_BRIGHT;

        ps.pushPose();
        // align item (and flip)
        ps.translate(0.5F, 0.5F, 0.5F);
        ps.mulPose(Axis.ZP.rotationDegrees(180.0F));
        switch (dir) {
            case 1 -> ps.mulPose(Axis.YP.rotationDegrees(180.0F));
            case 2 -> ps.mulPose(Axis.YP.rotationDegrees(-90.0F));
            case 3 -> ps.mulPose(Axis.YP.rotationDegrees(90.0F));
            default -> { }
        }
        ps.translate(-0.5F, -0.5F, -0.5F);

        ps.translate(0, 0, -0.005F); // offset to prevent z-fighting
        ps.scale(1.0F / 16.0F, 1.0F / 16.0F, -0.0001F); // scale to block size

        // Symbol: renderItemIntoGUI bei (0,0) nach translate(4, 2.5) und 8 Pixeln Breite
        ps.pushPose();
        ps.translate(4.0F, 2.5F, 0);
        ps.scale(8.0F / 16.0F, 8.0F / 16.0F, 1);
        ps.translate(8.0F, 8.0F, 0);
        ps.scale(16.0F, -16.0F, 16.0F);
        ItemStack one = type.copy();
        one.setCount(1);
        mc.getItemRenderer().renderStatic(one, ItemDisplayContext.GUI, full, OverlayTexture.NO_OVERLAY, ps, buf, storage.getLevel(), 0);
        ps.popPose();

        boolean unicode = mc.options.forceUnicodeFont().get();
        String text = getTextForCount(storage.getStockpile(), unicode);
        Font font = mc.font;
        int textX = 32 - font.width(text) / 2;
        int textY = 44;

        ps.pushPose();
        ps.scale(4.0F / 16.0F, 4.0F / 16.0F, 4.0F / 16.0F);
        int fontColor = 0x00FF00;
        // funky text shadow rendering with no z-fighting
        int shadow = (fontColor & 16579836) >> 2 | fontColor & -16777216;
        font.drawInBatch(text, textX + 1, textY + 1, 0xFF000000 | shadow, false, ps.last().pose(), buf, Font.DisplayMode.NORMAL, 0, full);
        ps.translate(0, 0, 1);
        font.drawInBatch(text, textX, textY, 0xFF00FF00, false, ps.last().pose(), buf, Font.DisplayMode.NORMAL, 0, full);
        ps.popPose();

        double fraction = storage.getCapacity() > 0 ? (double) storage.getStockpile() / (double) storage.getCapacity() : 0D;
        float r = (float) (1.0 - fraction), g = (float) fraction;
        float bMinX = 2, bMaxX = (float) (2 + fraction * 12), bMinY = 13.5F, bMaxY = 14;
        Matrix4f m = ps.last().pose();
        VertexConsumer vc = buf.getBuffer(RenderType.debugQuads());
        int ir = (int) (r * 255), ig = (int) (g * 255);
        com.hbm_m.platform.RenderHooks.vertexColor(vc, m, bMinX, bMaxY, 0, ir, ig, 0, 255);
        com.hbm_m.platform.RenderHooks.vertexColor(vc, m, bMaxX, bMaxY, 0, ir, ig, 0, 255);
        com.hbm_m.platform.RenderHooks.vertexColor(vc, m, bMaxX, bMinY, 0, ir, ig, 0, 255);
        com.hbm_m.platform.RenderHooks.vertexColor(vc, m, bMinX, bMinY, 0, ir, ig, 0, 255);

        ps.popPose();
    }

    private static String getTextForCount(long stackSize, boolean isUnicode) {
        if (stackSize >= 100000000 || (stackSize >= 1000000 && isUnicode)) return String.format("%.0fM", stackSize / 1000000f);
        if (stackSize >= 1000000) return String.format("%.1fM", stackSize / 1000000f);
        if (stackSize >= 100000 || (stackSize >= 10000 && isUnicode)) return String.format("%.0fK", stackSize / 1000f);
        if (stackSize >= 10000) return String.format("%.1fK", stackSize / 1000f);
        return String.valueOf(stackSize);
    }
}
