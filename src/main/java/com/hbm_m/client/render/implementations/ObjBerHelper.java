package com.hbm_m.client.render.implementations;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Gemeinsame Kleinigkeiten fuer die BERs, die vorher nur statische Blockmodelle waren (Audit 6): Original-Metadate aus
 * FACING, damit die Original-Drehschalter der TESRs unveraendert uebernommen werden koennen.
 */
//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public final class ObjBerHelper {

    private ObjBerHelper() {}

    /** Ausrichtung aus HORIZONTAL_FACING / FACING, sonst Norden. */
    public static Direction facing(BlockEntity be) {
        BlockState state = be.getBlockState();
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        if (state.hasProperty(BlockStateProperties.FACING)) {
            Direction d = state.getValue(BlockStateProperties.FACING);
            return d.getAxis().isHorizontal() ? d : Direction.NORTH;
        }
        // eigene "facing"-Properties (z.B. BarrelTankBlock)
        for (net.minecraft.world.level.block.state.properties.Property<?> p : state.getProperties()) {
            if ("facing".equals(p.getName()) && state.getValue(p) instanceof Direction d && d.getAxis().isHorizontal()) return d;
        }
        return Direction.NORTH;
    }

    /**
     * Original-Richtungsmetadate (ForgeDirection-Ordinal 2 Nord, 3 Sued, 4 West, 5 Ost; bei BlockDummyable
     * {@code meta - offset}). Der Port setzt FACING wie das Original auf die dem Spieler zugewandte Seite.
     */
    public static int meta(BlockEntity be) {
        return switch (facing(be)) {
            case SOUTH -> 3;
            case WEST -> 4;
            case EAST -> 5;
            default -> 2;
        };
    }

    /** Wie {@link #meta}, aber mit oben/unten (ForgeDirection-Ordinal 0 unten, 1 oben) fuer 6-seitige Bloecke. */
    public static int meta6(BlockEntity be) {
        BlockState state = be.getBlockState();
        if (state.hasProperty(BlockStateProperties.FACING)) return state.getValue(BlockStateProperties.FACING).get3DDataValue();
        return meta(be);
    }

    /** {@code GL11.glRotatef(deg, 0, 1, 0)}. */
    public static void rotY(PoseStack ps, float deg) {
        ps.mulPose(Axis.YP.rotationDegrees(deg));
    }

    /** Blockstate-y-Drehung (north 0, east 90, south 180, west 270) auf den Stapel legen, Drehpunkt Blockmitte. */
    public static void rotateLikeBlockstate(PoseStack ps, Direction facing) {
        float y = (facing.toYRot() + 180F) % 360F;
        ps.mulPose(Axis.YP.rotationDegrees(-y));
    }

    /**
     * {@code RenderDecoItem} mit {@code RenderItem.renderInFrame = true} (kein Wippen, ein Stueck): Rahmen-Skalierung
     * 0.5128205 und -0.05; Bloecke 1.25-fach, +0.05, -90 Grad, Viertelgroesse; flache Gegenstaende um 180 Grad
     * gedreht, halbe Groesse, Symbol von -0.25 bis 0.75 (Mitte +0.25).
     */
    public static void renderDecoItemInFrame(PoseStack ps, net.minecraft.client.renderer.MultiBufferSource buf,
                                             net.minecraft.world.item.ItemStack stack, int light,
                                             @org.jetbrains.annotations.Nullable net.minecraft.world.level.Level level) {
        if (stack.isEmpty()) return;
        net.minecraft.world.item.ItemStack one = stack.copy();
        one.setCount(1);
        ps.pushPose();
        ps.scale(0.5128205F, 0.5128205F, 0.5128205F);
        ps.translate(0.0F, -0.05F, 0.0F);
        if (one.getItem() instanceof net.minecraft.world.item.BlockItem) {
            ps.scale(1.25F, 1.25F, 1.25F);
            ps.translate(0.0F, 0.05F, 0.0F);
            ps.mulPose(Axis.YP.rotationDegrees(-90.0F));
            ps.scale(0.25F, 0.25F, 0.25F);
        } else {
            ps.mulPose(Axis.YP.rotationDegrees(180.0F));
            ps.scale(0.5F, 0.5F, 0.5F);
            ps.translate(0.0F, 0.25F, 0.0F);
        }
        net.minecraft.client.Minecraft.getInstance().getItemRenderer().renderStatic(one,
                net.minecraft.world.item.ItemDisplayContext.NONE, light,
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, ps, buf, level, 0);
        ps.popPose();
    }
}
