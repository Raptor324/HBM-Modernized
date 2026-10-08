package com.hbm_m.client.render.implementations;

import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.entity.projectile.ZirnoxDebrisEntity;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

/**
 * 1:1 {@code RenderZirnoxDebris}: je Art das Original-OBJ, um die Entity-ID gedreht und im Flug um (1,1,1) taumelnd.
 */
//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public class ZirnoxDebrisRenderer extends EntityRenderer<ZirnoxDebrisEntity> {

    private static ResourceLocation rl(String p) {
        return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, p);
    }

    private static final SimpleObjModel BLANK = new SimpleObjModel(rl("models/block/zirnox/deb_blank.obj"));
    private static final SimpleObjModel ELEMENT = new SimpleObjModel(rl("models/block/zirnox/deb_element.obj"));
    private static final SimpleObjModel SHRAPNEL = new SimpleObjModel(rl("models/block/zirnox/deb_shrapnel.obj"));
    private static final SimpleObjModel CONCRETE = new SimpleObjModel(rl("models/block/zirnox/deb_concrete.obj"));
    private static final SimpleObjModel EXCHANGER = new SimpleObjModel(rl("models/block/zirnox/deb_exchanger.obj"));
    private static final SimpleObjModel GRAPHITE = new SimpleObjModel(rl("models/rbmk/models/deb_graphite.obj"));

    private static final ResourceLocation TEX_ZIRNOX = rl("textures/block/machine/zirnox.png");
    private static final ResourceLocation TEX_ROD = rl("textures/block/machine/zirnox_deb_element.png");
    private static final ResourceLocation TEX_DESTROYED = rl("textures/block/machine/zirnox_destroyed.png");
    private static final ResourceLocation TEX_GRAPHITE = rl("textures/block/block_graphite.png");

    private static final Vector3f DIAG = new Vector3f(1, 1, 1).normalize();

    public ZirnoxDebrisRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(ZirnoxDebrisEntity debris, float entityYaw, float partialTicks,
                       PoseStack ps, MultiBufferSource buffer, int light) {
        ps.pushPose();
        ps.translate(0, 0.125D, 0);

        ps.mulPose(Axis.YP.rotationDegrees(debris.getId() % 360)); //rotate based on entity ID to add unique randomness
        ps.mulPose(Axis.of(DIAG).rotationDegrees(debris.lastRot + (debris.rot - debris.lastRot) * partialTicks));

        SimpleObjModel model;
        ResourceLocation tex;
        switch (debris.getDebrisType()) {
            case BLANK -> { model = BLANK; tex = TEX_ZIRNOX; }
            case ELEMENT -> { model = ELEMENT; tex = TEX_ROD; }
            case SHRAPNEL -> { model = SHRAPNEL; tex = TEX_ZIRNOX; }
            case GRAPHITE -> { model = GRAPHITE; tex = TEX_GRAPHITE; }
            case CONCRETE -> { model = CONCRETE; tex = TEX_DESTROYED; }
            default -> { model = EXCHANGER; tex = TEX_ZIRNOX; }
        }
        model.renderAll(ps, buffer.getBuffer(RenderType.entityCutoutNoCull(tex)), light);

        ps.popPose();
        super.render(debris, entityYaw, partialTicks, ps, buffer, light);
    }

    @Override
    public ResourceLocation getTextureLocation(ZirnoxDebrisEntity entity) {
        return TEX_GRAPHITE;
    }
}
