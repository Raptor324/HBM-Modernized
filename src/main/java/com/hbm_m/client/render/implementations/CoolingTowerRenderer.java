package com.hbm_m.client.render.implementations;

import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1:1 {@code RenderSmallTower} / {@code RenderLargeTower}: das statische Turmmodell, auf den Kern zentriert,
 * ungedreht (das Original kennt keine Ausrichtung), Rueckseiten nicht verworfen ({@code GL_CULL_FACE} aus).
 * Sichtweite 256 wie {@code getMaxRenderDistanceSquared 65536}.
 */
public class CoolingTowerRenderer<T extends BlockEntity> implements com.hbm_m.client.render.HbmBerBounds<T> {

    public static final SimpleObjModel TOWER_SMALL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/block/machines/tower_small.obj"));
    public static final ResourceLocation TOWER_SMALL_TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/machine/tower_small.png");
    public static final SimpleObjModel TOWER_LARGE = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/block/machines/tower_large.obj"));
    public static final ResourceLocation TOWER_LARGE_TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/machine/tower_large.png");

    private final SimpleObjModel model;
    private final ResourceLocation texture;

    private CoolingTowerRenderer(SimpleObjModel model, ResourceLocation texture) {
        this.model = model;
        this.texture = texture;
    }

    public static <T extends BlockEntity> CoolingTowerRenderer<T> small(BlockEntityRendererProvider.Context ctx) {
        return new CoolingTowerRenderer<>(TOWER_SMALL, TOWER_SMALL_TEX);
    }

    public static <T extends BlockEntity> CoolingTowerRenderer<T> large(BlockEntityRendererProvider.Context ctx) {
        return new CoolingTowerRenderer<>(TOWER_LARGE, TOWER_LARGE_TEX);
    }

    @Override
    public void render(T te, float f, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);
        model.renderAll(ps, buf.getBuffer(RenderType.entityCutoutNoCull(texture)), light);
        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(T te) { return true; }
    @Override public int getViewDistance() { return 256; }
}
