package com.hbm_m.client.render.entity.mob;

import com.hbm_m.lib.RefStrings;

import net.minecraft.client.renderer.entity.CreeperRenderer;
import net.minecraft.client.renderer.entity.layers.CreeperPowerLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Creeper;

import com.mojang.blaze3d.vertex.PoseStack;

/**
 * Universal creeper renderer with a custom texture and overlay when {@code isPowered()}.
 * Port of {@link com.hbm.render.entity.mob.RenderCreeperUniversal} (1.7.10).
 */
public class RenderCreeperUniversal extends CreeperRenderer {

    public static final ResourceLocation TAINTED_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/entity/creeper_tainted.png");
    public static final ResourceLocation TAINTED_ARMOR_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/entity/creeper_armor_taint.png");
    public static final ResourceLocation VOLATILE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/entity/creeper_volatile.png");
    public static final ResourceLocation PHOSGENE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/entity/creeper_phosgene.png");
    public static final ResourceLocation GOLD_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/entity/creeper_gold.png");
    public static final ResourceLocation VANILLA_CREEPER_ARMOR_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/creeper/creeper_armor.png");
    public static final ResourceLocation NUCLEAR_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/entity/creeper.png");
    public static final ResourceLocation NUCLEAR_ARMOR_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/entity/creeper_armor.png");

    private final ResourceLocation creeperTexture;
    private final ResourceLocation armoredCreeperTexture;
    private float swellMod = 1.0F;

    public RenderCreeperUniversal(EntityRendererProvider.Context context, ResourceLocation texture, ResourceLocation overlay) {
        super(context);
        this.creeperTexture = texture;
        this.armoredCreeperTexture = overlay;
        // Replace vanilla creeper_armor.png with the provided overlay.
        this.layers.removeIf(layer -> layer instanceof CreeperPowerLayer);
        this.addLayer(new CreeperUniversalPowerLayer(this, context.getModelSet(), overlay));
    }

    public RenderCreeperUniversal setSwellMod(float mod) {
        this.swellMod = mod;
        return this;
    }

    @Override
    public ResourceLocation getTextureLocation(Creeper creeper) {
        return this.creeperTexture;
    }

    @Override
    protected void scale(Creeper creeper, PoseStack poseStack, float partialTick) {
        float swell = creeper.getSwelling(partialTick);
        float flash = 1.0F + Mth.sin(swell * 100.0F) * swell * 0.01F;

        swell = Mth.clamp(swell, 0.0F, 1.0F);
        swell *= swell;
        swell *= swell;
        swell *= this.swellMod;

        float scaleHorizontal = (1.0F + swell * 0.4F) * flash;
        float scaleVertical = (1.0F + swell * 0.1F) / flash;
        poseStack.scale(scaleHorizontal, scaleVertical, scaleHorizontal);
    }

    /** Tainted creeper - as in {@link com.hbm.main.ClientProxy} 1.7.10. */
    public static RenderCreeperUniversal tainted(EntityRendererProvider.Context context) {
        return new RenderCreeperUniversal(context, TAINTED_TEXTURE, TAINTED_ARMOR_TEXTURE);
    }

    /** Volatile creeper - {@code creeper_volatile.png} + vanilla {@code creeper_armor}. */
    public static RenderCreeperUniversal volatileCreeper(EntityRendererProvider.Context context) {
        return new RenderCreeperUniversal(context, VOLATILE_TEXTURE, VANILLA_CREEPER_ARMOR_TEXTURE);
    }

    /** Phosgene creeper - {@code creeper_phosgene.png} + vanilla {@code creeper_armor}. */
    public static RenderCreeperUniversal phosgene(EntityRendererProvider.Context context) {
        return new RenderCreeperUniversal(context, PHOSGENE_TEXTURE, VANILLA_CREEPER_ARMOR_TEXTURE);
    }

    /** Gold creeper - {@code creeper_gold.png} + vanilla {@code creeper_armor}. */
    public static RenderCreeperUniversal goldCreeper(EntityRendererProvider.Context context) {
        return new RenderCreeperUniversal(context, GOLD_TEXTURE, VANILLA_CREEPER_ARMOR_TEXTURE);
    }

    /** Nuclear creeper - {@code creeper.png} + {@code creeper_armor.png}, swellMod 5F (GIT ClientProxy). */
    public static RenderCreeperUniversal nuclear(EntityRendererProvider.Context context) {
        return new RenderCreeperUniversal(context, NUCLEAR_TEXTURE, NUCLEAR_ARMOR_TEXTURE).setSwellMod(5.0F);
    }

    /** For future variants (phosgene, gold, ...) - same paths as in the GIT {@code ClientProxy}. */
    public static RenderCreeperUniversal create(
            EntityRendererProvider.Context context,
            String texturePath,
            String armorPath) {
        return new RenderCreeperUniversal(
                context,
                ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, texturePath),
                ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, armorPath));
    }

    public ResourceLocation getArmoredTexture() {
        return this.armoredCreeperTexture;
    }
}
