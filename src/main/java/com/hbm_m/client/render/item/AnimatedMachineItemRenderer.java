//? if forge || neoforge {
package com.hbm_m.client.render.item;

import com.hbm_m.block.machines.MachineStirlingBlock;
import com.hbm_m.client.ClientRenderHandler;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.render.implementations.AutosawRenderer;
import com.hbm_m.client.render.implementations.CompressorRenderer;
import com.hbm_m.client.render.implementations.IndustrialTurbineRenderer;
import com.hbm_m.client.render.implementations.LanternRenderer;
import com.hbm_m.client.render.implementations.PumpRenderer;
import com.hbm_m.client.render.implementations.SawmillRenderer;
import com.hbm_m.client.render.implementations.SteamEngineRenderer;
import com.hbm_m.client.render.implementations.StirlingRenderer;
import com.hbm_m.client.render.implementations.ThresherRenderer;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Audit 7: Gegenstaende, deren Original-Itemrenderer ({@code ItemRenderBase}) mit der Uhr animiert
 * ({@code System.currentTimeMillis()}): Stirling-Zahnrad, Saegewerk, Dampfmaschine, Pumpen, Autosaege, Dreschwerk,
 * Kompressor, Klystron, MHD-Turbine, Torus, Industrieturbine, Laterne, Grossradar.
 *
 * <p>Gezeichnet wird in den Koordinaten des bisherigen statischen Itemmodells (gleiche Wurzel-Transformation), damit
 * die Display-Werte der Kontexte gleich bleiben; die GUI-Werte der {@code item/<id>_anim.json} sind aus der
 * Original-Inventarkette errechnet (Audit-7-Skript a7_anim_items.py). Danach folgen die Teile und Bewegungen genau
 * wie im Original nach dem ersten Zeichenaufruf.</p>
 */
public class AnimatedMachineItemRenderer extends BlockEntityWithoutLevelRenderer {

    private static AnimatedMachineItemRenderer instance;

    public static AnimatedMachineItemRenderer instance() {
        if (instance == null) instance = new AnimatedMachineItemRenderer();
        return instance;
    }

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, path);
    }

    private static final ResourceLocation STIRLING = rl("textures/block/machine/stirling.png");
    private static final ResourceLocation STIRLING_STEEL = rl("textures/block/machine/stirling_steel.png");
    private static final ResourceLocation STIRLING_CREATIVE = rl("textures/block/machine/stirling_creative.png");
    private static final SimpleObjModel KLYSTRON = new SimpleObjModel(rl("models/block/machines/klystron.obj"));
    private static final SimpleObjModel MHDT = new SimpleObjModel(rl("models/block/machines/mhdt.obj"));
    private static final SimpleObjModel TORUS = new SimpleObjModel(rl("models/block/machines/torus.obj"));
    private static final SimpleObjModel RADAR_LARGE = new SimpleObjModel(rl("models/block/machines/radar_large.obj"));

    private AnimatedMachineItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        double time = System.currentTimeMillis();

        ps.pushPose();
        // vI: Der Anzeige-Waechter (ItemTransformHelperCompat.HbmItemDisplayWrapper) unterdrueckt fuer builtin/entity-
        // Modelle die Display-Transformation vor renderByItem -> hier selbst anwenden, in derselben Reihenfolge wie
        // ItemRenderer (T(.5) * display * T(-.5)); sonst wird das Modell im Inventar unskaliert/ungedreht gezeichnet.
        net.minecraft.client.resources.model.BakedModel displayModel = Minecraft.getInstance().getItemRenderer().getModel(stack, null, null, 0);
        boolean leftHand = ctx == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || ctx == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
        ps.translate(0.5F, 0.5F, 0.5F);
        com.hbm_m.client.compat.itemtransformhelper.ItemTransformHelperCompat.resolveDisplayTransforms(displayModel,
                com.hbm_m.client.compat.itemtransformhelper.ItemTransformHelperCompat.unwrapToDelegate(displayModel))
                .getTransform(ctx).apply(leftHand, ps);
        ps.translate(-0.5F, -0.5F, -0.5F);
        switch (id) {
            case "stirling", "stirling_steel", "stirling_creative" -> {
                // RenderStirling#renderCommonWithStack: cog = Schaden != 1; Textur 0 normal, 1 Stahl, 2 kreativ
                boolean cog = !MachineStirlingBlock.isNoCog(stack);
                ResourceLocation tex = id.equals("stirling") ? STIRLING : id.equals("stirling_creative") ? STIRLING_CREATIVE : STIRLING_STEEL;
                ps.translate(0.5, 0, 0.5);
                StirlingRenderer.renderCommon(ps, buf.getBuffer(RenderType.entityCutout(tex)), light,
                        cog ? (float) (time % 3600 * 0.1F) : 0F, cog);
            }
            case "sawmill" -> {
                boolean blade = !(com.hbm_m.platform.StackNbt.has(stack) && com.hbm_m.platform.StackNbt.read(stack).getBoolean(com.hbm_m.block.machines.MachineSawmillBlock.NO_BLADE));
                ps.translate(0.5, 0, 0.5);
                SawmillRenderer.renderCommon(ps, buf.getBuffer(RenderType.entityCutout(SawmillRenderer.TEX)), light,
                        blade ? (float) (time % 3600 * 0.1F) : 0F, blade);
            }
            case "steam_engine" -> {
                ps.translate(0.5, 0, 0.5);
                SteamEngineRenderer.renderCommon(ps, buf.getBuffer(RenderType.entityCutout(SteamEngineRenderer.TEX)), light, time % 3600 * 0.1D);
            }
            case "pump_steam", "pump_electric" -> {
                ps.translate(0.5, 0, 0.5);
                ResourceLocation tex = id.equals("pump_steam") ? PumpRenderer.STEAM_TEX : PumpRenderer.ELECTRIC_TEX;
                PumpRenderer.renderCommon(ps, buf.getBuffer(RenderType.entityCutout(tex)), light, time % 3600 * 0.1F);
            }
            case "autosaw" -> {
                ps.translate(0.5, 0, 0.5);
                AutosawRenderer.renderCommon(ps, buf.getBuffer(RenderType.entityCutout(AutosawRenderer.TEX)), light, 0D, 80D, time % 3600 * 0.1D, 0);
            }
            case "thresher" -> {
                ps.translate(0.5, 0, 0.5);
                ThresherRenderer.renderCommon(ps, buf.getBuffer(RenderType.entityCutout(ThresherRenderer.TEX)), light, 80D, time % 3600 * 0.25D, 0);
            }
            case "compressor" -> {
                ps.translate(0.5, 0, 0.5);
                VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(CompressorRenderer.TEX));
                CompressorRenderer.MODEL.renderPart("Compressor", ps, vc, light);
                double lift = (time * 0.005) % 9;
                if (lift > 3) lift = 3 - (lift - 3) / 2D;
                ps.pushPose();
                ps.translate(0, -lift, 0);
                CompressorRenderer.MODEL.renderPart("Pump", ps, vc, light);
                ps.popPose();
                ps.pushPose();
                ps.translate(0, 1.5, 0);
                ps.mulPose(Axis.XP.rotationDegrees((float) ((time * 0.25) % 360D)));
                ps.translate(0, -1.5, 0);
                CompressorRenderer.MODEL.renderPart("Fan", ps, vc, light);
                ps.popPose();
            }
            case "klystron", "klystron_creative" -> {
                // Wurzel des Blockmodells: (0.5, 0, 1.5), 90 Grad
                ps.translate(0.5, 0, 1.5);
                ps.mulPose(Axis.YP.rotationDegrees(90));
                VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(rl("textures/block/machine/" + id + ".png")));
                KLYSTRON.renderPart("Klystron", ps, vc, light);
                double rot = ((long) time / 10) % 360D;
                ps.translate(0, 2.5, 0);
                ps.mulPose(Axis.XP.rotationDegrees((float) rot));
                ps.translate(0, -2.5, 0);
                KLYSTRON.renderPart("Rotor", ps, vc, light);
            }
            case "mhdt" -> {
                ps.translate(0.5, 0, 0.5);
                ps.mulPose(Axis.YP.rotationDegrees(90));
                VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(rl("textures/block/machine/mhdt.png")));
                MHDT.renderPart("Turbine", ps, vc, light);
                double rot = ((long) time / 5) % 30D;
                rot -= 15;
                ps.translate(0, 1.5, 0);
                ps.mulPose(Axis.XP.rotationDegrees((float) rot));
                ps.translate(0, -1.5, 0);
                MHDT.renderPart("Coils", ps, vc, light);
            }
            case "torus" -> {
                ps.translate(0.5, 0, 0.5);
                VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(rl("textures/block/machine/torus.png")));
                TORUS.renderPart("Torus", ps, vc, light);
                ps.pushPose();
                double rot = ((long) time / 5 % 360);
                ps.mulPose(Axis.YP.rotationDegrees((float) rot));
                TORUS.renderPart("Magnet", ps, vc, light);
                ps.popPose();
            }
            case "industrial_turbine" -> {
                // Wurzel des Blockmodells (forge:obj, Ursprung gegenueberliegende Ecke): (0.5, 0, 2.5), 90 Grad
                ps.translate(0.5, 0, 2.5);
                ps.mulPose(Axis.YP.rotationDegrees(90));
                VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(IndustrialTurbineRenderer.TEX));
                IndustrialTurbineRenderer.MODEL.renderPart("Turbine", ps, vc, light);
                ps.translate(0, 1.5, 0);
                ps.mulPose(Axis.ZP.rotationDegrees(135));
                ps.translate(0, -1.5, 0);
                IndustrialTurbineRenderer.MODEL.renderPart("Gauge", ps, vc, light);
                double rot = ((long) time / 5) % 336D;
                ps.translate(0, 1.5, 0);
                ps.mulPose(Axis.ZN.rotationDegrees((float) rot));
                ps.translate(0, -1.5, 0);
                IndustrialTurbineRenderer.MODEL.renderPart("Flywheel", ps, vc, light);
            }
            case "lantern" -> {
                ps.translate(0.5, 0, 0.5);
                LanternRenderer.LANTERN.renderPart("Lantern", ps, buf.getBuffer(RenderType.entityCutoutNoCull(LanternRenderer.TEX)), light);
                float mult = (float) (Math.sin(time / 200D) / 2 + 0.5) * 0.1F + 0.9F;
                LanternRenderer.LANTERN.renderPartColor("Light", ps, buf.getBuffer(ClientRenderHandler.CustomRenderTypes.SOLID_COLOR_NOCULL), 1F * mult, 1F * mult, 0.7F * mult, 1F);
            }
            case "large_radar" -> {
                ps.translate(0.5, 0, 0.5);
                VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(rl("textures/block/machine/radar_large.png")));
                RADAR_LARGE.renderPart("Radar", ps, vc, light);
                ps.mulPose(Axis.YN.rotationDegrees((float) (time % 3600 * 0.1D)));
                RADAR_LARGE.renderPart("Dish", ps, vc, light);
            }
            default -> { }
        }
        ps.popPose();
    }
}
//?}
