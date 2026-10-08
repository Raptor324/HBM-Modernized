package com.hbm_m.client.weapon.render;

import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.weapon.GunGL;
import com.hbm_m.client.weapon.ItemRenderWeaponBase;
import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code com.hbm.render.item.weapon.ItemRenderShim}: Schimmer-Vorschlaghammer/-Axt und die drei Schilder
 * (stopsign, sopsign, chernobylsign) als 3D-Modell in der Hand und am Boden; Inventar bleibt das flache Symbol.
 */
public class ItemRenderShim extends ItemRenderWeaponBase {

    public static final SimpleObjModel shimmer_sledge = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/shimmer_sledge.obj"));
    public static final SimpleObjModel shimmer_axe = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/shimmer_axe.obj"));
    public static final SimpleObjModel stopsign = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/weapons/stopsign.obj"));
    public static final ResourceLocation shimmer_sledge_tex = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/shimmer_sledge.png");
    public static final ResourceLocation shimmer_axe_tex = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/shimmer_axe.png");
    public static final ResourceLocation stopsign_tex = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/weapons/stopsign.png");
    public static final ResourceLocation sopsign_tex = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/weapons/sopsign.png");
    public static final ResourceLocation chernobylsign_tex = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/weapons/chernobylsign.png");

    @Override public boolean customFirstPerson() { return false; }

    @Override public void renderFirstPerson(ItemStack stack) { }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        GunGL.pushMatrix();
        GunGL.enableCull();

        Item it = item.getItem();
        boolean sign = it == ModItems.STOPSIGN.get() || it == ModItems.SOPSIGN.get() || it == ModItems.CHERNOBYLSIGN.get();
        boolean shim = it == ModItems.SHIMMER_SLEDGE.get() || it == ModItems.SHIMMER_AXE.get();

        switch (type) {
            case EQUIPPED_FIRST_PERSON, EQUIPPED, ENTITY -> {
                // Original: EQUIPPED_FIRST_PERSON faellt nach den Schild-Drehungen in EQUIPPED/ENTITY durch
                if (type == ItemRenderType.EQUIPPED_FIRST_PERSON && sign) {
                    GunGL.rotate(180, 0.0F, 1.0F, 0.0F);
                    GunGL.rotate(-90.0F, 0.0F, 0.0F, 1.0F);
                    GunGL.translate(-1.0F, -1.5F, 0.0F);
                }

                if (it == ModItems.SHIMMER_SLEDGE.get()) GunGL.bindTexture(shimmer_sledge_tex);
                if (it == ModItems.SHIMMER_AXE.get()) GunGL.bindTexture(shimmer_axe_tex);
                if (it == ModItems.STOPSIGN.get()) GunGL.bindTexture(stopsign_tex);
                if (it == ModItems.SOPSIGN.get()) GunGL.bindTexture(sopsign_tex);
                if (it == ModItems.CHERNOBYLSIGN.get()) GunGL.bindTexture(chernobylsign_tex);

                if (shim) {
                    GunGL.rotate(-135.0F, 0.0F, 0.0F, 1.0F);
                    GunGL.rotate(180F, 0.0F, 0.0F, 1.0F);
                    GunGL.scale(1.5F, 1.5F, 1.5F);
                    GunGL.translate(0.45F, -0.3F, 0.0F);
                }
                if (sign) {
                    GunGL.rotate(45.0F, 0.0F, 0.0F, 1.0F);
                    GunGL.scale(0.35F, 0.35F, 0.35F);
                    GunGL.translate(2.0F, -2.0F, 0.0F);
                    GunGL.rotate(90F, 0.0F, 1.0F, 0.0F);
                }

                if (it == ModItems.SHIMMER_SLEDGE.get()) GunGL.renderAll(shimmer_sledge);
                if (it == ModItems.SHIMMER_AXE.get()) GunGL.renderAll(shimmer_axe);
                if (sign) GunGL.renderAll(stopsign);
            }
            default -> { }
        }

        GunGL.popMatrix();
    }
}
