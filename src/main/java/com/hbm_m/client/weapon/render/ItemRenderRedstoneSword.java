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
 * 1:1 {@code com.hbm.render.item.weapon.ItemRenderRedstoneSword} mit {@code ModelSword} (Texturgroesse 64x32):
 * Griff, Parierstange und Klinge als Quader in der Hand und am Boden; Inventar bleibt das flache Symbol.
 */
public class ItemRenderRedstoneSword extends ItemRenderWeaponBase {

    public static final ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/model_sword_redstone.png");

    private static ModelPart swordModel;

    @Override public boolean customFirstPerson() { return false; }

    @Override public void renderFirstPerson(ItemStack stack) { }

    /** Original {@code ModelSword}: alle Teile ohne Drehung ({@code mirror = true} steht erst nach {@code addBox} und wirkt nicht). */
    private static ModelPart model() {
        if (swordModel == null) {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();
            root.addOrReplaceChild("grip_bottom", CubeListBuilder.create().texOffs(0, 17).addBox(0F, 0F, 0F, 3, 3, 1), PartPose.offset(0F, 0F, 0F));
            root.addOrReplaceChild("grip_handle", CubeListBuilder.create().texOffs(8, 2).addBox(0F, 0F, 0F, 2, 5, 1), PartPose.offset(0.5F, -5F, 0F));
            root.addOrReplaceChild("shield", CubeListBuilder.create().texOffs(14, 5).addBox(0F, 0F, 0F, 6, 1, 3), PartPose.offset(-1.5F, -6F, -1F));
            root.addOrReplaceChild("blade", CubeListBuilder.create().texOffs(0, 0).addBox(0F, 0F, 0F, 3, 16, 1), PartPose.offset(0F, -22F, 0F));
            root.addOrReplaceChild("blade_tip", CubeListBuilder.create().texOffs(8, 0).addBox(0F, 0F, 0F, 2, 1, 1), PartPose.offset(0.5F, -23F, 0F));
            root.addOrReplaceChild("shield1", CubeListBuilder.create().texOffs(14, 0).addBox(0F, 0F, 0F, 1, 1, 4), PartPose.offset(-2F, -6.5F, -1.5F));
            root.addOrReplaceChild("shield2", CubeListBuilder.create().texOffs(24, 0).addBox(0F, 0F, 0F, 1, 1, 4), PartPose.offset(4F, -6.5F, -1.5F));
            swordModel = LayerDefinition.create(mesh, 64, 32).bakeRoot();
        }
        return swordModel;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        switch (type) {
            case EQUIPPED, EQUIPPED_FIRST_PERSON, ENTITY -> {
                GunGL.pushMatrix();
                GunGL.enableCull();
                GunGL.bindTexture(texture);
                GunGL.rotate(-135.0F, 0.0F, 0.0F, 1.0F);
                GunGL.translate(-0.8F, 0.4F, -0.1F);
                model().render(GunGL.pose(), GunGL.buffer(), GunGL.light(), OverlayTexture.NO_OVERLAY);
                GunGL.popMatrix();
            }
            default -> { }
        }
    }
}
