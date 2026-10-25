package com.hbm_m.client.render.entity;

import com.hbm_m.entity.mob.EntityUndeadSoldier;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.AbstractZombieModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * 1:1 {@code RenderUndeadSoldier} (RenderBiped): je nach Typ das Zombie- oder das Skelettmodell mit Vanilla-Textur.
 * {@code ModelSkeletonNT} ist ein Zombiemodell (vorgestreckte Arme) mit duennen 2x12x2-Gliedmassen - genau das ergibt
 * die Skelett-Modellschicht mit der Zombie-Armhaltung.
 */
public class UndeadSoldierRenderer extends HumanoidMobRenderer<EntityUndeadSoldier, UndeadSoldierRenderer.SoldierModel> {

    public static final ResourceLocation textureZombie = ResourceLocation.withDefaultNamespace("textures/entity/zombie/zombie.png");
    public static final ResourceLocation textureSkeleton = ResourceLocation.withDefaultNamespace("textures/entity/skeleton/skeleton.png");

    private final SoldierModel modelZombie;
    private final SoldierModel modelSkeleton;

    public UndeadSoldierRenderer(EntityRendererProvider.Context ctx) {
        this(ctx, new SoldierModel(ctx.bakeLayer(ModelLayers.ZOMBIE)), new SoldierModel(ctx.bakeLayer(ModelLayers.SKELETON)));
    }

    private UndeadSoldierRenderer(EntityRendererProvider.Context ctx, SoldierModel zombie, SoldierModel skeleton) {
        super(ctx, zombie, 0.5F);
        this.modelZombie = zombie;
        this.modelSkeleton = skeleton;
        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE_INNER_ARMOR)),
                new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE_OUTER_ARMOR)),
                ctx.getModelManager()));
    }

    @Override
    public void render(EntityUndeadSoldier living, float yaw, float interp, PoseStack pose, MultiBufferSource buffers, int light) {
        // preRenderCallback: Modell nach Typ waehlen
        byte type = living.getSoldierType();
        if (type == EntityUndeadSoldier.TYPE_ZOMBIE) this.model = modelZombie;
        if (type == EntityUndeadSoldier.TYPE_SKELETON) this.model = modelSkeleton;
        super.render(living, yaw, interp, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityUndeadSoldier living) {
        byte type = living.getSoldierType();
        if (type == EntityUndeadSoldier.TYPE_SKELETON) return textureSkeleton;
        return textureZombie;
    }

    /** {@code ModelZombie}/{@code ModelSkeletonNT}: Zombie-Armhaltung auf beliebiger Biped-Geometrie. */
    public static class SoldierModel extends AbstractZombieModel<EntityUndeadSoldier> {

        public SoldierModel(ModelPart root) {
            super(root);
        }

        @Override
        public boolean isAggressive(EntityUndeadSoldier entity) {
            return entity.isAggressive();
        }
    }
}
