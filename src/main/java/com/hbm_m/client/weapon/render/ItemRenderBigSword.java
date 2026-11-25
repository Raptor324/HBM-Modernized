package com.hbm_m.client.weapon.render;

import com.hbm_m.client.weapon.GunGL;
import com.hbm_m.client.weapon.ItemRenderWeaponBase;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code com.hbm.render.item.weapon.ItemRenderBigSword} mit {@code ModelBigSword} (Texturbasis 128x32):
 * "Great Sword" als Quadermodell in der Hand und am Boden; Inventar bleibt das flache Symbol.
 */
public class ItemRenderBigSword extends ItemRenderWeaponBase {

    public static final ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/model_bigsword_texture.png");

    private static ModelPart bigSwordModel;

    @Override public boolean customFirstPerson() { return false; }

    @Override public boolean hasInventoryModel() { return false; }

    /** Original {@code BigSword.isFull3D}. */
    @Override public boolean isFull3D() { return true; }

    @Override public void renderFirstPerson(ItemStack stack) { }

    /** Original {@code ModelBigSword}: Rotationen in Radiant (Techne), Spiegel-Flag steht erst nach {@code addBox} und wirkt nicht. */
    private static ModelPart model() {
        if (bigSwordModel == null) {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();
            root.addOrReplaceChild("handle_bottom", CubeListBuilder.create().texOffs(1, 1).addBox(0F, 0F, 0F, 1, 3, 3), PartPose.offsetAndRotation(0F, 0F, 0F, -0.7853982F, 0F, 0F));
            root.addOrReplaceChild("handle_grip", CubeListBuilder.create().texOffs(17, 1).addBox(0F, 0F, 0F, 1, 5, 2), PartPose.offset(0F, -4F, -1F));
            root.addOrReplaceChild("handle1", CubeListBuilder.create().texOffs(25, 1).addBox(0F, -1F, 0F, 2, 1, 4), PartPose.offsetAndRotation(-0.5F, -3F, 0F, 0.2617994F, 0F, 0F));
            root.addOrReplaceChild("handle2", CubeListBuilder.create().texOffs(41, 1).addBox(0F, -1F, -4F, 2, 1, 4), PartPose.offsetAndRotation(-0.5F, -3F, 0F, -0.2617994F, 0F, 0F));
            root.addOrReplaceChild("blade", CubeListBuilder.create().texOffs(57, 1).addBox(0F, 0F, 0F, 3, 18, 1), PartPose.offsetAndRotation(0F, -22F, 1.5F, 0F, 1.570796F, 0F));
            root.addOrReplaceChild("blade_tip", CubeListBuilder.create().texOffs(2, 10).addBox(0F, 0F, 0F, 1, 2, 2), PartPose.offsetAndRotation(0F, -23.5F, 0F, -0.7853982F, 0F, 0F));
            bigSwordModel = LayerDefinition.create(mesh, 128, 32).bakeRoot();
        }
        return bigSwordModel;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        switch (type) {
            case EQUIPPED, EQUIPPED_FIRST_PERSON, ENTITY -> {
                GunGL.pushMatrix();
                GunGL.enableCull();
                GunGL.bindTexture(texture);
                GunGL.rotate(-135.0F, 0.0F, 0.0F, 1.0F);
                GunGL.rotate(90.0F, 0.0F, 1.0F, 0.0F);
                GunGL.translate(0.0F, 0.4F, -0.7F);
                model().render(GunGL.pose(), GunGL.buffer(), GunGL.light(), OverlayTexture.NO_OVERLAY);
                GunGL.popMatrix();
            }
            default -> { }
        }
    }
}
