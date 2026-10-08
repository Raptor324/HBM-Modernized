package com.hbm_m.client.weapon;

import java.util.HashMap;
import java.util.List;

import org.joml.Matrix4f;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT.SmokeNode;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code ItemRenderWeaponBase}: Grundlage aller Waffen-Renderer. Die Erstperson-Darstellung laeuft wie im Original
 * mit eigener Projektion (FOV 70 bzw. FOV-Einstellung), eigenem Schwanken und Kopfwippen ({@link #setPerspectiveAndRender});
 * Drittperson, Inventar, Boden und Waffentisch gehen ueber {@link GunItemRenderer}. Gezeichnet wird ueber {@link GunGL}.
 */
public abstract class ItemRenderWeaponBase {

    public static final ResourceLocation flash_plume = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/weapons/lilmac_plume.png");
    public static final ResourceLocation laser_flash = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/weapons/laser_flash.png");

    /** Original {@code IItemRenderer.ItemRenderType}. */
    public enum ItemRenderType { ENTITY, EQUIPPED, EQUIPPED_FIRST_PERSON, INVENTORY, FIRST_PERSON_MAP }

    public static float interp;
    public static HashMap<LivingEntity, Long> flashMap = new HashMap<>();

    /** false = klassischer 1.7-IItemRenderer ohne eigene Projektion (z.B. Feuerloescher): Erstperson ueber die Vanilla-Hand. */
    public boolean customFirstPerson() { return true; }

    public boolean isAkimbo(LivingEntity entity) { return false; }
    public boolean isLeftHanded() { return false; }

    /** Original {@code renderItem(type, item, data)}: Zustand ist bereits per {@link GunGL#begin} gesetzt. */
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        GunGL.pushMatrix();
        GunGL.enableCull();
        switch (type) {
            case EQUIPPED_FIRST_PERSON -> { setupFirstPerson(item); renderFirstPerson(item); }
            case EQUIPPED -> { if (!isLeftHanded()) { setupThirdPerson(item); renderEquipped(item, data); } }
            case INVENTORY -> { setupInv(item); renderInv(item); }
            case ENTITY -> { setupEntity(item); renderEntity(item); }
            default -> { }
        }
        GunGL.popMatrix();
    }

    //entitylivingbase is second Object passed
    public void renderEquipped(ItemStack stack, Object... data) { renderOther(stack, ItemRenderType.EQUIPPED, data); }
    public void renderEquippedAkimbo(ItemStack stack, LivingEntity entity) { renderOther(stack, ItemRenderType.EQUIPPED); }
    public void renderInv(ItemStack stack) { renderOther(stack, ItemRenderType.INVENTORY); }
    public void renderEntity(ItemStack stack) { renderOther(stack, ItemRenderType.ENTITY); }

    /**
     * Original {@code setPerspectiveAndRender}: eigene Projektion, leere Modellansicht, eigenes Schwanken und Wippen.
     * Aufgerufen aus {@code RenderHandEvent} (das Vanilla-Hand-Rendering wird dann abgebrochen).
     */
    public void setPerspectiveAndRender(ItemStack stack, float partialTicks, MultiBufferSource.BufferSource buffers) {
        interp = partialTicks;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        float farPlaneDistance = mc.options.getEffectiveRenderDistance() * 16;
        Matrix4f oldProjection = RenderSystem.getProjectionMatrix();
        VertexSorting oldSorting = RenderSystem.getVertexSorting();

        Matrix4f projection = new Matrix4f().setPerspective((float) Math.toRadians(getFOVModifier(partialTicks, ModClothConfig.get().gunModelFov)),
                (float) mc.getWindow().getWidth() / (float) mc.getWindow().getHeight(), 0.05F, farPlaneDistance * 2.0F);
        RenderSystem.clear(org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        RenderSystem.setProjectionMatrix(projection, VertexSorting.DISTANCE_TO_ORIGIN);

        PoseStack pose = new PoseStack();
        if (mc.options.getCameraType().isFirstPerson() && !mc.player.isSleeping() && !mc.options.hideGui) {
            int light = LevelRenderer.getLightColor(mc.level, BlockPos.containing(mc.player.getX(), mc.player.getEyeY(), mc.player.getZ()));
            GunGL.begin(pose, buffers, light);
            this.setupTransformsAndRender(stack);
            GunGL.end();
            buffers.endBatch();
        }

        RenderSystem.setProjectionMatrix(oldProjection, oldSorting);
    }

    private float getFOVModifier(float interp, boolean useFOVSetting) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer entityplayer = mc.player;
        float fov = getBaseFOV(entityplayer.getMainHandItem());

        if (useFOVSetting) fov = mc.options.fov().get();

        if (entityplayer.getHealth() <= 0.0F) {
            float f2 = (float) entityplayer.deathTime + interp;
            fov /= (1.0F - 500.0F / (f2 + 500.0F)) * 2.0F + 1.0F;
        }

        if (mc.gameRenderer.getMainCamera().getFluidInCamera() == net.minecraft.world.level.material.FogType.WATER
                || entityplayer.isEyeInFluid(FluidTags.WATER)) fov = fov * 60.0F / 70.0F;

        return fov;
    }

    protected float getBaseFOV(ItemStack stack) { return 70F; }
    public float getViewFOV(ItemStack stack, float fov) { return fov; }
    protected float getSwayMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 0.1F : 0.5F; }
    protected float getSwayPeriod(ItemStack stack) { return 0.75F; }
    protected float getTurnMagnitude(ItemStack stack) { return 2.75F; }

    protected void setupTransformsAndRender(ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        float swayMagnitude = getSwayMagnitude(stack);
        float swayPeriod = getSwayPeriod(stack);
        float turnMagnitude = getTurnMagnitude(stack);

        //floppyness
        float armPitch = Mth.lerp(interp, player.xBobO, player.xBob);
        float armYaw = Mth.lerp(interp, player.yBobO, player.yBob);
        GunGL.rotate((player.getViewXRot(interp) - armPitch) * 0.1F * turnMagnitude, 1.0F, 0.0F, 0.0F);
        GunGL.rotate((player.getViewYRot(interp) - armYaw) * 0.1F * turnMagnitude, 0.0F, 1.0F, 0.0F);

        GunGL.color(1.0F, 1.0F, 1.0F, 1.0F);

        GunGL.pushMatrix();

        GunGL.rotate(180, 0, 1, 0);

        //viewbob
        if (mc.getCameraEntity() instanceof Player entityplayer) {
            float distanceDelta = entityplayer.walkDist - entityplayer.walkDistO;
            float distanceInterp = -(entityplayer.walkDist + distanceDelta * interp);
            float camYaw = Mth.lerp(interp, entityplayer.oBob, entityplayer.bob);
            float camPitch = 0F;
            GunGL.translate(Mth.sin(distanceInterp * (float) Math.PI * swayPeriod) * camYaw * 0.5F * swayMagnitude, -Math.abs(Mth.cos(distanceInterp * (float) Math.PI * swayPeriod) * camYaw) * swayMagnitude, 0.0F);
            GunGL.rotate(Mth.sin(distanceInterp * (float) Math.PI * swayPeriod) * camYaw * 3.0F, 0.0F, 0.0F, 1.0F);
            GunGL.rotate(Math.abs(Mth.cos(distanceInterp * (float) Math.PI * swayPeriod - 0.2F) * camYaw) * 5.0F, 1.0F, 0.0F, 0.0F);
            GunGL.rotate(camPitch, 1.0F, 0.0F, 0.0F);
        }

        this.renderItem(ItemRenderType.EQUIPPED_FIRST_PERSON, stack, null, player);
        GunGL.popMatrix();
    }

    public void setupFirstPerson(ItemStack stack) {
        GunGL.translate(0, 0, 1);

        if (Minecraft.getInstance().player.isShiftKeyDown()) {
            GunGL.translate(0, -3.875 / 8D, 0);
        } else {
            float offset = 0.8F;
            GunGL.rotate(180, 0, 1, 0);
            GunGL.translate(1.0F * offset, -0.75F * offset, -0.5F * offset);
            GunGL.rotate(180, 0, 1, 0);
        }
    }

    public void setupThirdPerson(ItemStack stack) {
        double scale = 0.125D;
        GunGL.scale(scale, scale, scale);

        GunGL.rotate(15.0F, 0.0F, 0.0F, 1.0F);
        GunGL.rotate(12.5F, 0.0F, 1.0F, 0.0F);
        GunGL.rotate(15.0F, 1.0F, 0.0F, 0.0F);

        GunGL.translate(3.5, 0, 0);
    }

    public void setupThirdPersonAkimbo(ItemStack stack) {
        double scale = 0.125D;
        GunGL.scale(scale, scale, scale);

        GunGL.rotate(15.0F, 0.0F, 0.0F, 1.0F);
        GunGL.rotate(12.5F, 0.0F, 1.0F, 0.0F);
        GunGL.rotate(10.0F, 1.0F, 0.0F, 0.0F);

        GunGL.translate(5, 0, 0);
    }

    public void setupInv(ItemStack stack) {
        GunGL.scale(1, 1, -1);
        GunGL.translate(8, 8, 0);
        GunGL.rotate(225, 0, 0, 1);
        GunGL.rotate(90, 0, 1, 0);
    }

    public void setupEntity(ItemStack stack) {
        double scale = 0.125D;
        GunGL.scale(scale, scale, scale);
        GunGL.rotate(-90, 0, 1, 0);
    }

    public void setupModTable(ItemStack stack) {
        double scale = -5D;
        GunGL.scale(scale, scale, scale);
        GunGL.rotate(90, 0, 1, 0);
    }

    public void renderModTable(ItemStack stack, int index) {
        renderOther(stack, ItemRenderType.INVENTORY);
    }

    public abstract void renderFirstPerson(ItemStack stack);
    public void renderOther(ItemStack stack, ItemRenderType type, Object... data) { }

    public static void standardAimingTransform(ItemStack stack, double sX, double sY, double sZ, double aX, double aY, double aZ) {
        float aimingProgress = ItemGunBaseNT.prevAimingProgress + (ItemGunBaseNT.aimingProgress - ItemGunBaseNT.prevAimingProgress) * interp;
        double x = sX + (aX - sX) * aimingProgress;
        double y = sY + (aY - sY) * aimingProgress;
        double z = sZ + (aZ - sZ) * aimingProgress;
        GunGL.translate(x, y, z);
    }

    public static void renderSmokeNodes(List<SmokeNode> nodes, double scale) {
        GunGL.Tess tess = GunGL.tess();

        if (nodes.size() > 1) {

            GunGL.pushAttrib();
            GunGL.enableBlend();
            GunGL.disableTexture2D();
            GunGL.disableCull();
            GunGL.blendFunc(770, 771, 1, 0);
            GunGL.depthMask(false);
            GunGL.disableLighting();

            tess.startDrawingQuads();
            tess.setNormal(0F, 1F, 0F);

            for (int i = 0; i < nodes.size() - 1; i++) {
                SmokeNode node = nodes.get(i);
                SmokeNode past = nodes.get(i + 1);

                tess.setColorRGBA_F(1F, 1F, 1F, (float) node.alpha);
                tess.addVertex(node.forward, node.lift, node.side);
                tess.setColorRGBA_F(1F, 1F, 1F, 0F);
                tess.addVertex(node.forward, node.lift, node.side + node.width * scale);
                tess.setColorRGBA_F(1F, 1F, 1F, 0F);
                tess.addVertex(past.forward, past.lift, past.side + past.width * scale);
                tess.setColorRGBA_F(1F, 1F, 1F, (float) past.alpha);
                tess.addVertex(past.forward, past.lift, past.side);

                tess.setColorRGBA_F(1F, 1F, 1F, (float) node.alpha);
                tess.addVertex(node.forward, node.lift, node.side);
                tess.setColorRGBA_F(1F, 1F, 1F, 0F);
                tess.addVertex(node.forward, node.lift, node.side - node.width * scale);
                tess.setColorRGBA_F(1F, 1F, 1F, 0F);
                tess.addVertex(past.forward, past.lift, past.side - past.width * scale);
                tess.setColorRGBA_F(1F, 1F, 1F, (float) past.alpha);
                tess.addVertex(past.forward, past.lift, past.side);
            }

            tess.draw();
            GunGL.popAttrib();
        }
    }

    public static void renderMuzzleFlash(long lastShot) {
        renderMuzzleFlash(lastShot, 75, 15);
    }

    public static void renderMuzzleFlash(long lastShot, int duration, double l) {
        GunGL.Tess tess = GunGL.tess();
        int flash = duration;

        if (System.currentTimeMillis() - lastShot < flash) {
            GunGL.pushAttrib();
            GunGL.enableBlend();
            GunGL.blendFunc(GunGL.GL_SRC_ALPHA, GunGL.GL_ONE);
            GunGL.depthMask(false);
            GunGL.disableCull();
            GunGL.pushMatrix();

            double fire = (System.currentTimeMillis() - lastShot) / (double) flash;

            double width = 6 * fire;
            double length = l * fire;
            double inset = 2;
            GunGL.bindTexture(flash_plume);
            tess.startDrawingQuads();
            tess.setBrightness(240);
            tess.setNormal(0F, 1F, 0F);
            tess.setColorRGBA_F(1F, 1F, 1F, 1F);

            tess.addVertexWithUV(0, -width, -inset, 1, 1);
            tess.addVertexWithUV(0, width, -inset, 0, 1);
            tess.addVertexWithUV(0.1, width, length - inset, 0, 0);
            tess.addVertexWithUV(0.1, -width, length - inset, 1, 0);

            tess.addVertexWithUV(0, width, inset, 0, 1);
            tess.addVertexWithUV(0, -width, inset, 1, 1);
            tess.addVertexWithUV(0.1, -width, -length + inset, 1, 0);
            tess.addVertexWithUV(0.1, width, -length + inset, 0, 0);

            tess.addVertexWithUV(0, -inset, width, 0, 1);
            tess.addVertexWithUV(0, -inset, -width, 1, 1);
            tess.addVertexWithUV(0.1, length - inset, -width, 1, 0);
            tess.addVertexWithUV(0.1, length - inset, width, 0, 0);

            tess.addVertexWithUV(0, inset, -width, 1, 1);
            tess.addVertexWithUV(0, inset, width, 0, 1);
            tess.addVertexWithUV(0.1, -length + inset, width, 0, 0);
            tess.addVertexWithUV(0.1, -length + inset, -width, 1, 0);

            tess.draw();
            GunGL.popMatrix();
            GunGL.popAttrib();
        }
    }

    public static void renderGapFlash(long lastShot) {
        GunGL.Tess tess = GunGL.tess();
        int flash = 75;

        if (System.currentTimeMillis() - lastShot < flash) {
            GunGL.pushAttrib();
            GunGL.enableBlend();
            GunGL.blendFunc(GunGL.GL_SRC_ALPHA, GunGL.GL_ONE);
            GunGL.disableCull();
            GunGL.pushMatrix();

            double fire = (System.currentTimeMillis() - lastShot) / (double) flash;

            double height = 4 * fire;
            double length = 15 * fire;
            double lift = 3 * fire;
            double offset = 1 * fire;
            double lengthOffset = 0.125;
            GunGL.bindTexture(flash_plume);
            tess.startDrawingQuads();
            tess.setBrightness(240);
            tess.setNormal(0F, 1F, 0F);
            tess.setColorRGBA_F(1F, 1F, 1F, 1F);

            tess.addVertexWithUV(0, -height, -offset, 1, 1);
            tess.addVertexWithUV(0, height, -offset, 0, 1);
            tess.addVertexWithUV(0, height + lift, length - offset, 0, 0);
            tess.addVertexWithUV(0, -height + lift, length - offset, 1, 0);

            tess.addVertexWithUV(0, height, offset, 0, 1);
            tess.addVertexWithUV(0, -height, offset, 1, 1);
            tess.addVertexWithUV(0, -height + lift, -length + offset, 1, 0);
            tess.addVertexWithUV(0, height + lift, -length + offset, 0, 0);

            tess.addVertexWithUV(0, -height, -offset, 1, 1);
            tess.addVertexWithUV(0, height, -offset, 0, 1);
            tess.addVertexWithUV(lengthOffset, height, length - offset, 0, 0);
            tess.addVertexWithUV(lengthOffset, -height, length - offset, 1, 0);

            tess.addVertexWithUV(0, height, offset, 0, 1);
            tess.addVertexWithUV(0, -height, offset, 1, 1);
            tess.addVertexWithUV(lengthOffset, -height, -length + offset, 1, 0);
            tess.addVertexWithUV(lengthOffset, height, -length + offset, 0, 0);

            tess.draw();
            GunGL.popMatrix();
            GunGL.popAttrib();
        }
    }

    public static void renderLaserFlash(long lastShot, int flash, double scale, int color) {
        GunGL.Tess tess = GunGL.tess();

        if (System.currentTimeMillis() - lastShot < flash) {
            GunGL.pushAttrib();
            GunGL.enableBlend();
            GunGL.blendFunc(GunGL.GL_SRC_ALPHA, GunGL.GL_ONE);
            GunGL.depthMask(false);
            GunGL.disableCull();
            GunGL.pushMatrix();

            double fire = (System.currentTimeMillis() - lastShot) / (double) flash;

            double size = 4 * fire * scale;

            GunGL.bindTexture(laser_flash);
            tess.startDrawingQuads();
            tess.setBrightness(240);
            tess.setNormal(0F, 1F, 0F);

            tess.setColorRGBA_I(color, 255);

            tess.addVertexWithUV(0, -size, -size, 1, 1);
            tess.addVertexWithUV(0, size, -size, 0, 1);
            tess.addVertexWithUV(0, size, size, 0, 0);
            tess.addVertexWithUV(0, -size, size, 1, 0);

            tess.draw();
            GunGL.popMatrix();
            GunGL.popAttrib();
        }
    }
}
