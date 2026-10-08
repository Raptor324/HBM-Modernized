package com.hbm_m.client.render.implementations;

import java.util.Random;

import com.hbm_m.blockentity.machines.BatterySocketBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.render.util.BeamPronter;
import com.hbm_m.client.render.util.HorsePronter;
import com.hbm_m.item.fekal_electric.ItemBatteryPack;
import com.hbm_m.item.fekal_electric.ItemBatterySC;
import com.hbm_m.item.fekal_electric.ItemCreativeBattery;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code RenderBatterySocket} fuer die beweglichen Teile: Sockel, Batteriepacks und SC-Batterien bleiben im
 * Blockmodell ({@code MachineBatterySocketBakedModel}); hier die Stuetzen ({@code frame}: Block zwei Felder ueber dem
 * Kern), die kreative Batterie als drehendes Sonnenpony und jede andere Batterie als drehender Gegenstand, beide mit
 * Funkenbogen zu den Ecken.
 */
public class BatterySocketRenderer implements com.hbm_m.client.render.HbmBerBounds<BatterySocketBlockEntity> {

    static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/machines/battery.obj"));
    static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/machine/battery_socket.png");
    static final ResourceLocation BLORBO = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/horse/sunburst.png");

    public BatterySocketRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(BatterySocketBlockEntity socket, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        Level level = socket.getLevel();
        if (level == null) return;

        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        switch (ObjBerHelper.meta(socket)) {
            case 2 -> ObjBerHelper.rotY(ps, 90);
            case 4 -> ObjBerHelper.rotY(ps, 180);
            case 3 -> ObjBerHelper.rotY(ps, 270);
            case 5 -> ObjBerHelper.rotY(ps, 0);
        }
        ps.translate(-0.5, 0, 0.5);

        // Original: frame = Block zwei Felder ueber dem Kern ist nicht Luft
        if (!level.getBlockState(socket.getBlockPos().above(2)).isAir()) {
            MODEL.renderPart("Supports", ps, buf.getBuffer(RenderType.entityCutout(TEX)), light);
        }

        ItemStack render = socket.getItemHandler().getStackInSlot(0);
        long time = level.getGameTime();
        if (!render.isEmpty() && !(render.getItem() instanceof ItemBatteryPack) && !(render.getItem() instanceof ItemBatterySC)) {
            if (render.getItem() instanceof ItemCreativeBattery) {
                ps.pushPose();
                ps.scale(0.75F, 0.75F, 0.75F);
                ps.mulPose(Axis.YN.rotationDegrees((float) ((time % 360 + interp) * 25D)));
                HorsePronter.reset();
                HorsePronter.enableHorn();
                HorsePronter.pront(ps, buf.getBuffer(RenderType.entityCutoutNoCull(BLORBO)), light);
                ps.popPose();
            } else {
                ps.pushPose();
                ps.translate(0, 0.5, 0);
                ps.scale(1.5F, 1.5F, 1.5F);
                ps.mulPose(Axis.YN.rotationDegrees((float) ((time % 360 + interp) * 2.5D)));
                // EntityItem im Rahmenmodus, ohne Wippen
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
        }

        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(BatterySocketBlockEntity be) { return true; }
}
