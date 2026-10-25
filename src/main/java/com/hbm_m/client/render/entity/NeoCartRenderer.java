package com.hbm_m.client.render.entity;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.entity.cart.INTMCart;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code RenderNeoCart}: Schienenlage wie die Vanilla-Lore, dann OBJ-Fahrgestell und -Wanne (mit Wackeln),
 * Textur nach Basis, Aufbau je Art (Pulver, Semtex, Zerstoerer).
 */
public class NeoCartRenderer<T extends AbstractMinecart & INTMCart> extends EntityRenderer<T> {

    private static ResourceLocation rl(String p) {
        return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, p);
    }

    private static final SimpleObjModel CART = new SimpleObjModel(rl("models/vehicles/cart.obj"));
    private static final SimpleObjModel CART_DESTROYER = new SimpleObjModel(rl("models/vehicles/cart_destroyer.obj"));
    private static final SimpleObjModel CART_POWDER = new SimpleObjModel(rl("models/vehicles/cart_powder.obj"));

    private static final ResourceLocation CART_METAL = rl("textures/entity/cart_metal.png");
    private static final ResourceLocation CART_BLANK = rl("textures/entity/cart_metal_naked.png");
    private static final ResourceLocation CART_WOOD = rl("textures/entity/cart_wood.png");
    private static final ResourceLocation CART_DESTROYER_TEX = rl("textures/entity/cart_destroyer.png");
    private static final ResourceLocation CART_POWDER_TEX = rl("textures/block/block_gunpowder.png");
    private static final ResourceLocation CART_SEMTEX_SIDE = rl("textures/block/semtex_side.png");
    private static final ResourceLocation CART_SEMTEX_TOP = rl("textures/block/semtex_bottom.png");

    public NeoCartRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.5F;
    }

    @Override
    public void render(@NotNull T cart, float rot, float interp, PoseStack ps, MultiBufferSource buffers, int light) {
        ps.pushPose();
        long rand = (long) cart.getId() * 493286711L;
        rand = rand * rand * 4392167121L + rand * 98761L;
        float randX = (((float) (rand >> 16 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
        float randY = (((float) (rand >> 20 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
        float randZ = (((float) (rand >> 24 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
        ps.translate(randX, randY, randZ);
        double interpX = Mth.lerp(interp, cart.xOld, cart.getX());
        double interpY = Mth.lerp(interp, cart.yOld, cart.getY());
        double interpZ = Mth.lerp(interp, cart.zOld, cart.getZ());
        double mult = 0.3;
        Vec3 vec3 = cart.getPos(interpX, interpY, interpZ);
        float interpPitch = Mth.lerp(interp, cart.xRotO, cart.getXRot());

        if (vec3 != null) {
            Vec3 vec31 = cart.getPosOffs(interpX, interpY, interpZ, mult);
            Vec3 vec32 = cart.getPosOffs(interpX, interpY, interpZ, -mult);

            if (vec31 == null) vec31 = vec3;
            if (vec32 == null) vec32 = vec3;

            ps.translate(vec3.x - interpX, (vec31.y + vec32.y) / 2.0D - interpY, vec3.z - interpZ);
            Vec3 vec33 = vec32.add(-vec31.x, -vec31.y, -vec31.z);

            if (vec33.length() != 0.0D) {
                vec33 = vec33.normalize();
                rot = (float) (Math.atan2(vec33.z, vec33.x) * 180.0D / Math.PI);
                interpPitch = (float) (Math.atan(vec33.y) * 73.0D);
            }
        }

        ps.translate(0.0F, 0.375F, 0.0F);
        ps.mulPose(Axis.YP.rotationDegrees(180.0F - rot));
        ps.mulPose(Axis.ZP.rotationDegrees(-interpPitch));
        float interpRoll = (float) cart.getHurtTime() - interp;
        float interpDamage = cart.getDamage() - interp;

        if (interpDamage < 0.0F) interpDamage = 0.0F;

        ps.translate(0, -0.4375F, 0);
        ps.mulPose(Axis.YP.rotationDegrees(90));
        CART.renderPart("Carriage", ps, buffers.getBuffer(RenderType.entityCutoutNoCull(texture(cart))), light);

        if (interpRoll > 0.0F) {
            ps.translate(0, 0.75F, 0);
            ps.mulPose(Axis.ZP.rotationDegrees(Mth.sin(interpRoll) * interpRoll * interpDamage / 10.0F * (float) cart.getHurtDir()));
            ps.translate(0, -0.75F, 0);
        }

        CART.renderPart("Bucket", ps, buffers.getBuffer(RenderType.entityCutoutNoCull(texture(cart))), light);

        String special = cart.specialContent();
        if ("powder".equals(special)) {
            CART_POWDER.renderPart("Powder", ps, buffers.getBuffer(RenderType.entityCutoutNoCull(CART_POWDER_TEX)), light);
        } else if ("semtex".equals(special)) {
            CART_POWDER.renderPart("SemtexTop", ps, buffers.getBuffer(RenderType.entityCutoutNoCull(CART_SEMTEX_TOP)), light);
            CART_POWDER.renderPart("SemtexSide", ps, buffers.getBuffer(RenderType.entityCutoutNoCull(CART_SEMTEX_SIDE)), light);
        } else if ("destroyer".equals(special)) {
            CART_DESTROYER.renderAll(ps, buffers.getBuffer(RenderType.entityCutoutNoCull(CART_DESTROYER_TEX)), light);
        }

        ps.popPose();
        super.render(cart, rot, interp, ps, buffers, light);
    }

    private ResourceLocation texture(INTMCart cart) {
        return switch (cart.getBase()) {
            case PAINTED -> CART_METAL;
            case WOOD -> CART_WOOD;
            default -> CART_BLANK;
        };
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull T entity) {
        return texture(entity);
    }
}
