package com.hbm_m.client.render.implementations;

import com.hbm_m.block.decorations.StatueBlock;
import com.hbm_m.blockentity.decorations.StatueBlockEntity;
import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code RenderDecoBlockAlt} fuer {@code TileEntityDecoBlockAltF}: {@code ModelStatue} (64x64), davor die Uhr als
 * Gegenstand im Rahmenmodus ({@code RenderDecoItem}, {@code renderInFrame}) und das {@code ModelGun}-Gewehr (64x64,
 * halbe Groesse, -20 Grad geneigt). Die {@code mirror = true}-Zeilen der Modelle stehen erst nach {@code addBox} und
 * wirken im Original nicht.
 */
//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public class StatueRenderer implements com.hbm_m.client.render.HbmBerBounds<StatueBlockEntity> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/model_statue.png");
    private static final ResourceLocation GUN_TEXTURE = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/model_gun.png");

    private final ModelPart statue = createStatue().bakeRoot();
    private final ModelPart gun = createGun().bakeRoot();

    public StatueRenderer(BlockEntityRendererProvider.Context ctx) {}

    private static void box(PartDefinition root, String name, int u, int v, float x, float y, float z, int w, int h, int d,
                            float px, float py, float pz, float rx, float ry, float rz) {
        root.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, v).addBox(x, y, z, w, h, d),
                PartPose.offsetAndRotation(px, py, pz, rx, ry, rz));
    }

    /** Original {@code ModelStatue}. */
    private static LayerDefinition createStatue() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();
        box(r, "shape1", 0, 0, 0F, 0F, 0F, 16, 8, 16, -8F, 16F, -8F, 0F, 0F, 0F);
        box(r, "shape2", 0, 24, 0F, 0F, 0F, 4, 12, 4, -4F, 4F, -2F, 0F, 0F, 0F);
        box(r, "shape3", 16, 24, 0F, 0F, 0F, 4, 12, 4, 0F, 4F, -2F, 0F, 0F, 0F);
        box(r, "shape4", 32, 40, 0F, 0F, 0F, 8, 12, 4, -4F, -8F, -2F, 0F, 0F, 0F);
        box(r, "shape6", 0, 40, 0F, 0F, -2F, 4, 8, 4, 4F, -8F, 0F, 0.5235988F, 0F, 0F);
        box(r, "shape7", 16, 40, -4F, 0F, -2F, 4, 8, 4, -4F, -8F, 0F, -0.0872665F, 0F, 0F);
        box(r, "shape8", 0, 52, -2F, 0F, -2F, 4, 8, 4, 6F, -2F, 3F, 1.22173F, 0F, 0F);
        box(r, "shape9", 16, 52, 0F, 0F, -2F, 4, 8, 4, -8F, -1F, -0.5F, 0.2617994F, 0F, 0F);
        box(r, "shape10", 32, 24, -4F, -8F, -4F, 8, 8, 8, 0F, -8F, 0F, -0.1745329F, 0F, 0F);
        return LayerDefinition.create(mesh, 64, 64);
    }

    /** Original {@code ModelGun}. */
    private static LayerDefinition createGun() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();
        box(r, "shape1", 0, 0, 0F, 0F, 0F, 6, 12, 4, 0F, -4F, -1F, 0F, 0F, 0F);
        box(r, "shape2", 52, 0, 0F, 0F, 0F, 3, 3, 3, 4F, -7F, -0.5F, 0F, 0F, 0F);
        box(r, "shape3", 28, 58, 0F, 0F, 0F, 15, 3, 3, -15F, -7F, -0.5F, 0F, 0F, 0F);
        box(r, "shape4", 0, 61, 0F, 0F, 0F, 1, 2, 1, 2F, -3F, 0.5F, 0F, 0F, 0.715585F);
        box(r, "shape5", 0, 57, 0F, 0F, 0F, 2, 2, 1, -13.5F, -8F, 0.5F, 0F, 0F, 0.6108652F);
        box(r, "shape6", 52, 7, 0F, 0F, 0F, 4, 3, 2, 0F, -6.5F, 0F, 0F, 0F, 0F);
        box(r, "shape7", 46, 49, 0F, 0F, 0F, 6, 5, 3, -15F, -3F, -0.5F, 0F, 0F, 0F);
        box(r, "shape8", 22, 0, 0F, 0F, 0F, 12, 1, 2, -15F, -4F, 0F, 0F, 0F, 0F);
        box(r, "shape9", 52, 13, 0F, 0F, 0F, 3, 3, 3, -3F, -4F, -0.5F, 0F, 0F, 0F);
        box(r, "shape10", 11, 60, 0F, 0F, 0F, 6, 2, 2, -9F, -3F, 0F, 0F, 0F, 0F);
        box(r, "shape11", 35, 50, 0F, 0F, 0F, 2, 4, 3, -9F, -1F, -0.5F, 0F, 0F, 0F);
        box(r, "shape12", 12, 57, 0F, 0F, 0F, 7, 1, 1, -7F, 2F, 0.5F, 0F, 0F, 0F);
        box(r, "shape13", 0, 51, 0F, 0F, 0F, 4, 1, 4, 0F, -5F, -1F, 0F, 0F, 0F);
        box(r, "shape14", 0, 43, 0F, 0F, 0F, 3, 5, 2, 7F, -7F, 0F, 0F, 0F, 0.7853982F);
        box(r, "shape15", 0, 38, 0F, 0F, 0F, 3, 1, 3, -9F, 3F, -0.5F, 0F, 0F, -2.792527F);
        box(r, "shape16", 0, 17, 0F, 0F, 0F, 2, 3, 1, -1F, -2F, 0.5F, 0F, 0F, 0.2617994F);
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void render(StatueBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        pose.pushPose();
        pose.translate(0.5F, 1.5F, 0.5F);
        pose.mulPose(Axis.ZP.rotationDegrees(180));

        // Original: Meta 4 -> 90, 2 -> 180, 5 -> 270, 3 -> 0
        Direction dir = be.getBlockState().hasProperty(StatueBlock.FACING) ? be.getBlockState().getValue(StatueBlock.FACING) : Direction.SOUTH;
        float rot = switch (dir) {
            case WEST -> 90F;
            case NORTH -> 180F;
            case EAST -> 270F;
            default -> 0F;
        };
        pose.mulPose(Axis.YP.rotationDegrees(rot));

        statue.render(pose, buffer.getBuffer(RenderType.entityCutout(TEXTURE)), light, OverlayTexture.NO_OVERLAY);

        float g = 0.0625F;
        float q = g * 2 + 0.0625F / 3;
        pose.translate(0.0F, -2 * g, q);
        pose.mulPose(Axis.ZP.rotationDegrees(180));

        // Uhr (TileEntityDecoBlockAltW/F): EntityItem mit renderInFrame - Groesse 0.5128205, -0.05 tiefer, um 180 Grad
        // gedreht, das flache Symbol unten bei -0.25 ausgerichtet (renderDroppedItem)
        pose.pushPose();
        pose.scale(0.5128205F, 0.5128205F, 0.5128205F);
        pose.translate(0.0F, -0.05F, 0.0F);
        pose.mulPose(Axis.YP.rotationDegrees(180));
        pose.translate(0.0F, 0.25F, 0.0F);
        Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(ModItems.WATCH.get()), ItemDisplayContext.NONE,
                light, OverlayTexture.NO_OVERLAY, pose, buffer, be.getLevel(), 0);
        pose.popPose();

        pose.translate(0.0F, 2 * g, -q);
        pose.mulPose(Axis.ZP.rotationDegrees(180));
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.scale(0.5F, 0.5F, 0.5F);
        pose.translate(-g * 20, g * 4, g * 11);
        pose.mulPose(Axis.ZP.rotationDegrees(-20));

        // Gewehr (TileEntityDecoBlockAltG/F)
        gun.render(pose, buffer.getBuffer(RenderType.entityCutout(GUN_TEXTURE)), light, OverlayTexture.NO_OVERLAY);

        pose.popPose();
    }
}
