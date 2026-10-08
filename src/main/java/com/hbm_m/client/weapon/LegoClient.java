package com.hbm_m.client.weapon;

import java.util.function.BiConsumer;

import com.hbm_m.client.render.util.BeamPronter;
import com.hbm_m.client.render.util.BeamPronter.EnumBeamType;
import com.hbm_m.client.render.util.BeamPronter.EnumWaveType;
import com.hbm_m.client.weapon.GunGL.Tess;
import com.hbm_m.client.weapon.render.ItemRenderFatMan;
import com.hbm_m.entity.projectile.EntityBulletBaseMK4;
import com.hbm_m.entity.projectile.EntityBulletBeamBase;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.item.weapon.sedna.WeaponItems;
import com.hbm_m.item.weapon.sedna.hud.HUDComponentAmmoCounter;
import com.hbm_m.item.weapon.sedna.hud.HUDComponentDurabilityBar;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.util.Vec3NT;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code com.hbm.items.weapon.sedna.factory.LegoClient}: HUD-Komponenten und Geschoss-Renderer. Die Renderer
 * laufen innerhalb von {@link BulletRenderers} (Pose bereits an der Entity, GunGL-Abschnitt offen); GL11/Tessellator
 * ueber {@link GunGL}, {@code RenderArcFurnace.fullbright} = {@link GunGL#fullbright(boolean)}.
 */
public class LegoClient {

	public static HUDComponentDurabilityBar HUD_COMPONENT_DURABILITY = new HUDComponentDurabilityBar();
	public static HUDComponentDurabilityBar HUD_COMPONENT_DURABILITY_MIRROR = new HUDComponentDurabilityBar(true);
	public static HUDComponentAmmoCounter HUD_COMPONENT_AMMO = new HUDComponentAmmoCounter(0);
	public static HUDComponentAmmoCounter HUD_COMPONENT_AMMO_MIRROR = new HUDComponentAmmoCounter(0).mirror();
	public static HUDComponentAmmoCounter HUD_COMPONENT_AMMO_NOCOUNTER = new HUDComponentAmmoCounter(0).noCounter();
	public static HUDComponentAmmoCounter HUD_COMPONENT_AMMO_SECOND = new HUDComponentAmmoCounter(1);

	public static BiConsumer<EntityBulletBaseMK4, Float> RENDER_STANDARD_BULLET = (bullet, interp) -> {
		double length = bullet.prevVelocity + (bullet.velocity - bullet.prevVelocity) * interp;
		if(length <= 0) return;
		renderBulletStandard(GunGL.tess(), 0xFFBF00, 0xFFFFFF, length, false);
	};

	public static BiConsumer<EntityBulletBaseMK4, Float> RENDER_FLECHETTE_BULLET = (bullet, interp) -> {
		double length = bullet.prevVelocity + (bullet.velocity - bullet.prevVelocity) * interp;
		if(length <= 0) return;
		renderBulletStandard(GunGL.tess(), 0x8C8C8C, 0xCACACA, length, false);
	};

	public static BiConsumer<EntityBulletBaseMK4, Float> RENDER_AP_BULLET = (bullet, interp) -> {
		double length = bullet.prevVelocity + (bullet.velocity - bullet.prevVelocity) * interp;
		if(length <= 0) return;
		renderBulletStandard(GunGL.tess(), 0xFF6A00, 0xFFE28D, length, false);
	};

	public static BiConsumer<EntityBulletBaseMK4, Float> RENDER_FRAGMENTATION = (bullet, interp) -> {
		double length = bullet.prevVelocity + (bullet.velocity - bullet.prevVelocity) * interp;
		if(length <= 0) return;
		renderBulletStandard(GunGL.tess(), 0xFF6A00, 0xFFE28D, length, true);
	};

	public static BiConsumer<EntityBulletBaseMK4, Float> RENDER_EXPRESS_BULLET = (bullet, interp) -> {
		double length = bullet.prevVelocity + (bullet.velocity - bullet.prevVelocity) * interp;
		if(length <= 0) return;
		renderBulletStandard(GunGL.tess(), 0x9E082E, 0xFF8A79, length, false);
	};

	public static BiConsumer<EntityBulletBaseMK4, Float> RENDER_DU_BULLET = (bullet, interp) -> {
		double length = bullet.prevVelocity + (bullet.velocity - bullet.prevVelocity) * interp;
		if(length <= 0) return;
		renderBulletStandard(GunGL.tess(), 0x5CCD41, 0xE9FF8D, length, false);
	};

	public static BiConsumer<EntityBulletBaseMK4, Float> RENDER_HE_BULLET = (bullet, interp) -> {
		double length = bullet.prevVelocity + (bullet.velocity - bullet.prevVelocity) * interp;
		if(length <= 0) return;
		renderBulletStandard(GunGL.tess(), 0xD8CA00, 0xFFF19D, length, true);
	};

	public static BiConsumer<EntityBulletBaseMK4, Float> RENDER_SM_BULLET = (bullet, interp) -> {
		double length = bullet.prevVelocity + (bullet.velocity - bullet.prevVelocity) * interp;
		if(length <= 0) return;
		renderBulletStandard(GunGL.tess(), 0x42A8DD, 0xFFFFFF, length, true);
	};

	public static BiConsumer<EntityBulletBaseMK4, Float> RENDER_BLACK_BULLET = (bullet, interp) -> {
		double length = bullet.prevVelocity + (bullet.velocity - bullet.prevVelocity) * interp;
		if(length <= 0) return;
		renderBulletStandard(GunGL.tess(), 0x000000, 0x7F006E, length, true);
	};

	public static BiConsumer<EntityBulletBaseMK4, Float> RENDER_TRACER_BULLET = (bullet, interp) -> {
		double length = bullet.prevVelocity + (bullet.velocity - bullet.prevVelocity) * interp;
		if(length <= 0) return;
		renderBulletStandard(GunGL.tess(), 0x9E082E, 0xFF8A79, length, true);
	};

	public static BiConsumer<EntityBulletBaseMK4, Float> RENDER_LEGENDARY_BULLET = (bullet, interp) -> {
		double length = bullet.prevVelocity + (bullet.velocity - bullet.prevVelocity) * interp;
		if(length <= 0) return;
		renderBulletStandard(GunGL.tess(), 0x7F006E, 0xFF7FED, length, true);
	};

	public static void renderBulletStandard(Tess tess, int dark, int light, double length, boolean fullbright) { renderBulletStandard(tess, dark, light, length, 0.03125D, 0.03125D * 0.25D, fullbright); }

	public static void renderBulletStandard(Tess tess, int dark, int light, double length, double widthF, double widthB, boolean fullbright) {

		GunGL.disableTexture2D();
		GunGL.disableCull();
		GunGL.disableLighting();
		GunGL.color(1F, 1F, 1F, 1F);

		tess.startDrawingQuads();
		if(fullbright) tess.setBrightness(240);
		tess.setNormal(0F, 1F, 0F);
		tess.setColorOpaque_I(dark);
		tess.addVertex(length, widthB, -widthB); tess.addVertex(length, widthB, widthB);
		tess.setColorOpaque_I(light);
		tess.addVertex(0, widthF, widthF); tess.addVertex(0, widthF, -widthF);
		tess.setColorOpaque_I(dark);
		tess.addVertex(length, -widthB, -widthB); tess.addVertex(length, -widthB, widthB);
		tess.setColorOpaque_I(light);
		tess.addVertex(0, -widthF, widthF); tess.addVertex(0, -widthF, -widthF);
		tess.setColorOpaque_I(dark);
		tess.addVertex(length, -widthB, widthB); tess.addVertex(length, widthB, widthB);
		tess.setColorOpaque_I(light);
		tess.addVertex(0, widthF, widthF); tess.addVertex(0, -widthF, widthF);
		tess.setColorOpaque_I(dark);
		tess.addVertex(length, -widthB, -widthB); tess.addVertex(length, widthB, -widthB);
		tess.setColorOpaque_I(light);
		tess.addVertex(0, widthF, -widthF); tess.addVertex(0, -widthF, -widthF);
		tess.setColorOpaque_I(dark);
		tess.addVertex(length, widthB, widthB); tess.addVertex(length, widthB, -widthB);
		tess.addVertex(length, -widthB, -widthB); tess.addVertex(length, -widthB, widthB);
		tess.setColorOpaque_I(light);
		tess.addVertex(0, widthF, widthF); tess.addVertex(0, widthF, -widthF);
		tess.addVertex(0, -widthF, -widthF); tess.addVertex(0, -widthF, widthF);
		tess.draw();

		GunGL.enableLighting();
		GunGL.enableCull();
		GunGL.enableTexture2D();
	}

	public static BiConsumer<EntityBulletBaseMK4, Float> RENDER_FLARE = (bullet, interp) -> { renderFlare(bullet, interp, 1F, 0.5F, 0.5F); };
	public static BiConsumer<EntityBulletBaseMK4, Float> RENDER_FLARE_SUPPLY = (bullet, interp) -> { renderFlare(bullet, interp, 0.5F, 0.5F, 1F); };
	public static BiConsumer<EntityBulletBaseMK4, Float> RENDER_FLARE_WEAPON = (bullet, interp) -> { renderFlare(bullet, interp, 0.5F, 1F, 0.5F); };

	private static final ResourceLocation flare = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/particle/flare.png");
	public static void renderFlare(Entity bullet, float interp, float r, float g, float b) {

		if(bullet.tickCount < 2) return;
		GunGL.fullbright(true);

		double scale = Math.min(5, (bullet.tickCount + interp - 2) * 0.5) * (0.8 + bullet.level().random.nextDouble() * 0.4);
		renderFlareSprite(bullet, interp, r, g, b, scale, 0.5F, 0.75F);

		GunGL.fullbright(false);
	}
	public static void renderFlareSprite(Entity bullet, float interp, float r, float g, float b, double scale, float outerAlpha, float innerAlpha) {

		GunGL.pushMatrix();
		GunGL.pushAttrib();
		GunGL.enableBlend();
		GunGL.blendFunc(GunGL.GL_SRC_ALPHA, GunGL.GL_ONE);
		GunGL.depthMask(false);
		GunGL.disableLighting();

		GunGL.bindTexture(flare);

		Tess tess = GunGL.tess();
		tess.startDrawingQuads();

		// Original ActiveRenderInfo.rotationX/Z/YZ/XY/XZ aus der Kameraausrichtung
		Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
		float yawRad = camera.getYRot() * (float) Math.PI / 180F;
		float pitchRad = camera.getXRot() * (float) Math.PI / 180F;
		float f1 = Mth.cos(yawRad);
		float f2 = Mth.sin(yawRad);
		float f3 = -f2 * Mth.sin(pitchRad);
		float f4 = f1 * Mth.sin(pitchRad);
		float f5 = Mth.cos(pitchRad);

		double posX = 0;
		double posY = 0;
		double posZ = 0;

		tess.setColorRGBA_F(r, g, b, outerAlpha);
		tess.addVertexWithUV((double) (posX - f1 * scale - f3 * scale), (double) (posY - f5 * scale), (double) (posZ - f2 * scale - f4 * scale), 1, 1);
		tess.addVertexWithUV((double) (posX - f1 * scale + f3 * scale), (double) (posY + f5 * scale), (double) (posZ - f2 * scale + f4 * scale), 1, 0);
		tess.addVertexWithUV((double) (posX + f1 * scale + f3 * scale), (double) (posY + f5 * scale), (double) (posZ + f2 * scale + f4 * scale), 0, 0);
		tess.addVertexWithUV((double) (posX + f1 * scale - f3 * scale), (double) (posY - f5 * scale), (double) (posZ + f2 * scale - f4 * scale), 0, 1);

		scale *= 0.5D;

		tess.setColorRGBA_F(1F, 1F, 1F, innerAlpha);
		tess.addVertexWithUV((double) (posX - f1 * scale - f3 * scale), (double) (posY - f5 * scale), (double) (posZ - f2 * scale - f4 * scale), 1, 1);
		tess.addVertexWithUV((double) (posX - f1 * scale + f3 * scale), (double) (posY + f5 * scale), (double) (posZ - f2 * scale + f4 * scale), 1, 0);
		tess.addVertexWithUV((double) (posX + f1 * scale + f3 * scale), (double) (posY + f5 * scale), (double) (posZ + f2 * scale + f4 * scale), 0, 0);
		tess.addVertexWithUV((double) (posX + f1 * scale - f3 * scale), (double) (posY - f5 * scale), (double) (posZ + f2 * scale - f4 * scale), 0, 1);

		tess.draw();

		GunGL.depthMask(true);
		GunGL.enableLighting();
		GunGL.disableBlend();
		GunGL.popAttrib();
		GunGL.popMatrix();
	}

	/** Original {@code RenderRBMKDebris.tex_graphite} */
	private static final ResourceLocation tex_graphite = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/block_graphite.png");

	public static BiConsumer<EntityBulletBaseMK4, Float> RENDER_GRAPHITE = (bullet, interp) -> {
		GunGL.scale(2F, 2F, 2F);
		GunGL.bindTexture(tex_graphite);
		GunGL.renderAll(WeaponResources.deb_graphite);
	};

	public static BiConsumer<EntityBulletBaseMK4, Float> RENDER_GRENADE = (bullet, interp) -> {
		GunGL.scale(0.25F, 0.25F, 0.25F);
		GunGL.rotate(90, 0, 0, 1);
		GunGL.bindTexture(WeaponResources.grenade_tex);
		GunGL.renderPart(WeaponResources.projectiles, "Grenade");
	};

	public static BiConsumer<EntityBulletBaseMK4, Float> RENDER_BIG_NUKE = (bullet, interp) -> {
		GunGL.scale(0.5F, 0.5F, 0.5F);
		GunGL.rotate(90, 0, 0, 1);
		GunGL.bindTexture(WeaponResources.rocket_mirv_tex);
		GunGL.renderPart(WeaponResources.projectiles, "MissileMIRV");
	};

	public static BiConsumer<EntityBulletBaseMK4, Float> RENDER_RPZB = (bullet, interp) -> {

		GunGL.pushMatrix();
		GunGL.scale(0.125F, 0.125F, 0.125F);
		GunGL.rotate(90, 0, -1, 0);
		GunGL.translate(0, 0, 3.5F);
		GunGL.bindTexture(WeaponResources.panzerschreck_tex);
		GunGL.renderPart(WeaponResources.panzerschreck, "Rocket");
		GunGL.popMatrix();

		GunGL.translate(0.375F, 0, 0);
		double length = bullet.prevVelocity + (bullet.velocity - bullet.prevVelocity) * interp;
		if(length > 0) renderBulletStandard(GunGL.tess(), 0x808080, 0xFFF2A7, length * 2, true);
	};

	public static BiConsumer<EntityBulletBaseMK4, Float> RENDER_QD = (bullet, interp) -> {

		GunGL.pushMatrix();
		GunGL.rotate(90, 0, 0, 1);
		GunGL.bindTexture(WeaponResources.rocket_tex);
		GunGL.renderPart(WeaponResources.projectiles, "Rocket");
		GunGL.popMatrix();

		GunGL.translate(0.375F, 0, 0);
		double length = bullet.prevVelocity + (bullet.velocity - bullet.prevVelocity) * interp;
		if(length > 0) renderBulletStandard(GunGL.tess(), 0x808080, 0xFFF2A7, length * 2, true);
	};

	public static BiConsumer<EntityBulletBaseMK4, Float> RENDER_ML = (bullet, interp) -> {

		GunGL.pushMatrix();
		GunGL.scale(0.25F, 0.25F, 0.25F);
		GunGL.rotate(-90, 0, 1, 0);
		GunGL.translate(0, -1, -4.5F);
		GunGL.bindTexture(WeaponResources.missile_launcher_tex);
		GunGL.renderPart(WeaponResources.missile_launcher, "Missile");
		GunGL.popMatrix();

		GunGL.translate(0.375F, 0, 0);
		double length = bullet.prevVelocity + (bullet.velocity - bullet.prevVelocity) * interp;
		if(length > 0) renderBulletStandard(GunGL.tess(), 0x808080, 0xFFF2A7, length * 2, true);
	};

	/** Original {@code BeamPronter.prontBeam(Vec3 delta, ...)} im aktuellen GunGL-Abschnitt */
	private static void prontBeam(Vec3NT delta, EnumWaveType wave, EnumBeamType beam, int outerColor, int innerColor, int start, int segments, float size, int layers, float thickness) {
		BeamPronter.prontBeam(GunGL.pose(), GunGL.buffers(), delta.toVec3(), wave, beam, outerColor, innerColor, start, segments, size, layers, thickness);
	}

	private static double age(EntityBulletBeamBase bullet, float interp) {
		return Mth.clamp(1D - ((double) bullet.tickCount - 2 + interp) / (double) bullet.getBulletConfig().expires, 0, 1);
	}

	public static BiConsumer<EntityBulletBeamBase, Float> RENDER_LIGHTNING = (bullet, interp) -> {

		GunGL.fullbright(true);
		GunGL.pushMatrix();
		GunGL.rotate(180 - bullet.getYRot(), 0, 1F, 0);
		GunGL.rotate(-bullet.getXRot() - 90, 1F, 0, 0);
		Vec3NT delta = Vec3NT.createVectorHelper(0, bullet.beamLength, 0);
		double age = age(bullet, interp);
		GunGL.scale(age / 2 + 0.5, 1, age / 2 + 0.5);
		double scale = 0.075D;
		int colorInner = ((int)(0x20 * age) << 16) | ((int)(0x20 * age) << 8) | (int) (0x40 * age);
		int colorOuter = ((int)(0x40 * age) << 16) | ((int)(0x40 * age) << 8) | (int) (0x80 * age);
		prontBeam(delta, EnumWaveType.RANDOM, EnumBeamType.SOLID, colorInner, colorInner, bullet.tickCount / 3, (int)(bullet.beamLength / 2 + 1), (float)scale * 1F, 4, 0.25F);
		prontBeam(delta, EnumWaveType.RANDOM, EnumBeamType.SOLID, colorOuter, colorOuter, bullet.tickCount, (int)(bullet.beamLength / 2 + 1), (float)scale * 7F, 2, 0.0625F);
		prontBeam(delta, EnumWaveType.RANDOM, EnumBeamType.SOLID, colorOuter, colorOuter, bullet.tickCount / 2, (int)(bullet.beamLength / 2 + 1), (float)scale * 7F, 2, 0.0625F);
		GunGL.popMatrix();
		GunGL.fullbright(false);
	};

	public static BiConsumer<EntityBulletBeamBase, Float> RENDER_LIGHTNING_SUB = (bullet, interp) -> {

		GunGL.fullbright(true);
		GunGL.pushMatrix();
		GunGL.rotate(180 - bullet.getYRot(), 0, 1F, 0);
		GunGL.rotate(-bullet.getXRot() - 90, 1F, 0, 0);
		Vec3NT delta = Vec3NT.createVectorHelper(0, bullet.beamLength, 0);
		double age = age(bullet, interp);
		GunGL.scale(age / 2 + 0.15, 1, age / 2 + 0.15);
		double scale = 0.075D;
		int colorInner = ((int)(0x20 * age) << 16) | ((int)(0x20 * age) << 8) | (int) (0x40 * age);
		int colorOuter = ((int)(0x40 * age) << 16) | ((int)(0x40 * age) << 8) | (int) (0x80 * age);
		prontBeam(delta, EnumWaveType.RANDOM, EnumBeamType.SOLID, colorInner, colorInner, bullet.tickCount / 3, (int)(bullet.beamLength / 2 + 1), (float)scale * 1F, 4, 0.25F);
		prontBeam(delta, EnumWaveType.RANDOM, EnumBeamType.SOLID, colorOuter, colorOuter, bullet.tickCount, (int)(bullet.beamLength / 2 + 1), (float)scale * 7F, 2, 0.0625F);
		prontBeam(delta, EnumWaveType.RANDOM, EnumBeamType.SOLID, colorOuter, colorOuter, bullet.tickCount / 2, (int)(bullet.beamLength / 2 + 1), (float)scale * 7F, 2, 0.0625F);
		GunGL.popMatrix();
		GunGL.fullbright(false);
	};

	public static BiConsumer<EntityBulletBeamBase, Float> RENDER_TAU = (bullet, interp) -> {

		GunGL.fullbright(true);
		double age = age(bullet, interp);

		GunGL.pushMatrix();
		GunGL.rotate(180 - bullet.getYRot(), 0, 1F, 0);
		GunGL.rotate(-bullet.getXRot() - 90, 1F, 0, 0);

		GunGL.pushMatrix();
		Vec3NT delta = Vec3NT.createVectorHelper(0, bullet.beamLength, 0);
		GunGL.scale(age / 2 + 0.5, 1, age / 2 + 0.5);
		double scale = 0.075D;
		int colorInner = ((int)(0x30 * age) << 16) | ((int)(0x25 * age) << 8) | (int) (0x10 * age);
		prontBeam(delta, EnumWaveType.RANDOM, EnumBeamType.SOLID, colorInner, colorInner, (bullet.tickCount + bullet.getId()) / 2, (int)(bullet.beamLength / 2 + 1), (float)scale * 4F, 2, 0.0625F);
		GunGL.popMatrix();

		GunGL.scale(age * 2, 1, age * 2);
		GunGL.translate(0, bullet.beamLength, 0);
		GunGL.rotate(-90, 0, 0, 1);
		renderBulletStandard(GunGL.tess(), 0xFFBF00, 0xFFFFFF, bullet.beamLength, true);

		GunGL.popMatrix();
		GunGL.fullbright(false);
	};

	public static BiConsumer<EntityBulletBeamBase, Float> RENDER_TAU_CHARGE = (bullet, interp) -> {

		GunGL.fullbright(true);
		double age = age(bullet, interp);

		GunGL.pushMatrix();
		GunGL.rotate(180 - bullet.getYRot(), 0, 1F, 0);
		GunGL.rotate(-bullet.getXRot() - 90, 1F, 0, 0);

		GunGL.pushMatrix();
		Vec3NT delta = Vec3NT.createVectorHelper(0, bullet.beamLength, 0);
		GunGL.scale(age / 2 + 0.5, 1, age / 2 + 0.5);
		double scale = 0.075D;
		int colorInner = ((int)(0x60 * age) << 16) | ((int)(0x50 * age) << 8) | (int) (0x30 * age);
		prontBeam(delta, EnumWaveType.RANDOM, EnumBeamType.SOLID, colorInner, colorInner, (bullet.tickCount + bullet.getId()) / 2, (int)(bullet.beamLength / 2 + 1), (float)scale * 4F, 2, 0.0625F);
		GunGL.popMatrix();

		GunGL.scale(age * 2, 1, age * 2);
		GunGL.translate(0, bullet.beamLength, 0);
		GunGL.rotate(-90, 0, 0, 1);
		renderBulletStandard(GunGL.tess(), 0xFFF0A0, 0xFFFFFF, bullet.beamLength, true);

		GunGL.popMatrix();
		GunGL.fullbright(false);
	};

	public static BiConsumer<EntityBulletBeamBase, Float> RENDER_CRACKLE = (bullet, interp) -> {

		GunGL.fullbright(true);
		double age = age(bullet, interp);

		GunGL.pushMatrix();
		GunGL.rotate(180 - bullet.getYRot(), 0, 1F, 0);
		GunGL.rotate(-bullet.getXRot() - 90, 1F, 0, 0);

		double scale = 5D;
		GunGL.scale(age * scale, 1, age * scale);
		GunGL.translate(0, bullet.beamLength, 0);
		GunGL.rotate(-90, 0, 0, 1);
		renderBulletStandard(GunGL.tess(), 0xE3D692, 0xffffff, bullet.beamLength, true);

		GunGL.popMatrix();
		GunGL.fullbright(false);
	};

	public static BiConsumer<EntityBulletBeamBase, Float> RENDER_BLACK_LIGHTNING = (bullet, interp) -> {

		GunGL.fullbright(true);
		double age = age(bullet, interp);

		GunGL.pushMatrix();
		GunGL.rotate(180 - bullet.getYRot(), 0, 1F, 0);
		GunGL.rotate(-bullet.getXRot() - 90, 1F, 0, 0);

		double scale = 5D;
		GunGL.scale(age * scale, 1, age * scale);
		GunGL.translate(0, bullet.beamLength, 0);
		GunGL.rotate(-90, 0, 0, 1);
		renderBulletStandard(GunGL.tess(), 0x4C3093, 0x000000, bullet.beamLength, true);

		GunGL.popMatrix();
		GunGL.fullbright(false);
	};

	public static BiConsumer<EntityBulletBeamBase, Float> RENDER_NI4NI_BOLT = (bullet, interp) -> {

		GunGL.fullbright(true);
		double age = age(bullet, interp);

		GunGL.pushMatrix();
		GunGL.rotate(180 - bullet.getYRot(), 0, 1F, 0);
		GunGL.rotate(-bullet.getXRot() - 90, 1F, 0, 0);

		double scale = 5D;
		GunGL.scale(age * scale, 1, age * scale);
		GunGL.translate(0, bullet.beamLength, 0);
		GunGL.rotate(-90, 0, 0, 1);
		renderBulletStandard(GunGL.tess(), 0xAAD2E5, 0xffffff, bullet.beamLength, true);

		GunGL.popMatrix();
		GunGL.fullbright(false);
	};

	public static BiConsumer<EntityBulletBeamBase, Float> RENDER_LASER_RED = (bullet, interp) -> {
		renderStandardLaser(bullet, interp, 0x80, 0x15, 0x15);
	};
	public static BiConsumer<EntityBulletBeamBase, Float> RENDER_LASER_EMERALD = (bullet, interp) -> {
		renderStandardLaser(bullet, interp, 0x15, 0x80, 0x15);
	};
	public static BiConsumer<EntityBulletBeamBase, Float> RENDER_LASER_CYAN = (bullet, interp) -> {
		renderStandardLaser(bullet, interp, 0x15, 0x15, 0x80);
	};
	public static BiConsumer<EntityBulletBeamBase, Float> RENDER_LASER_PURPLE = (bullet, interp) -> {
		renderStandardLaser(bullet, interp, 0x60, 0x15, 0x80);
	};
	public static BiConsumer<EntityBulletBeamBase, Float> RENDER_LASER_WHITE = (bullet, interp) -> {
		renderStandardLaser(bullet, interp, 0x15, 0x15, 0x15);
	};

	public static void renderStandardLaser(EntityBulletBeamBase bullet, float interp, int r, int g, int b) {

		GunGL.fullbright(true);
		GunGL.pushMatrix();
		GunGL.rotate(180 - bullet.getYRot(), 0, 1F, 0);
		GunGL.rotate(-bullet.getXRot() - 90, 1F, 0, 0);
		Vec3NT delta = Vec3NT.createVectorHelper(0, bullet.beamLength, 0);
		double age = age(bullet, interp);
		GunGL.scale(age / 2 + 0.5, 1, age / 2 + 0.5);
		int colorInner = ((int)(r * age) << 16) | ((int)(g * age) << 8) | (int) (b * age);
		prontBeam(delta, EnumWaveType.RANDOM, EnumBeamType.SOLID, colorInner, colorInner, bullet.tickCount / 3, (int)(bullet.beamLength / 2 + 1), 0F, 4, 0.025F);
		GunGL.popMatrix();
		GunGL.fullbright(false);
	}

	public static BiConsumer<EntityBulletBeamBase, Float> RENDER_FOLLY = (bullet, interp) -> {

		double age = age(bullet, interp);
		GunGL.fullbright(true);

		GunGL.pushMatrix();
		renderFlareSprite(bullet, interp, 1F, 1F, 1F, (1 - age) * 7.5 + 1.5, 0.5F * (float) age, 0.75F * (float) age);
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.blendFunc(GunGL.GL_SRC_ALPHA, GunGL.GL_ONE);
		GunGL.rotate(180 - bullet.getYRot(), 0, 1F, 0);
		GunGL.rotate(-bullet.getXRot() - 90, 1F, 0, 0);
		Vec3NT delta = Vec3NT.createVectorHelper(0, bullet.beamLength, 0);
		GunGL.scale((1 - age) * 25 + 2.5, 1, (1 - age) * 25 + 2.5);
		int colorInner = ((int)(0x20 * age) << 16) | ((int)(0x20 * age) << 8) | (int) (0x20 * age);
		prontBeam(delta, EnumWaveType.RANDOM, EnumBeamType.SOLID, colorInner, colorInner, bullet.tickCount / 3, (int)(bullet.beamLength / 2 + 1), 0F, 8, 0.0625F);
		GunGL.disableBlend();
		GunGL.popMatrix();

		GunGL.fullbright(false);
	};

	public static BiConsumer<EntityBulletBaseMK4, Float> RENDER_NUKE = (bullet, interp) -> {

		GunGL.pushMatrix();
		GunGL.scale(0.125F, 0.125F, 0.125F);
		GunGL.rotate(-90, 0, 1, 0);
		GunGL.translate(0, -1, 1F);
		GunGL.bindTexture(WeaponResources.fatman_mininuke_tex);
		GunGL.renderPart(WeaponResources.fatman, "MiniNuke");
		GunGL.popMatrix();
	};

	public static BiConsumer<EntityBulletBaseMK4, Float> RENDER_BOMB = (bullet, interp) -> {

		GunGL.pushMatrix();
		GunGL.scale(0.0625F, 0.0625F, 0.0625F);
		GunGL.rotate(-90, 0, 1, 0);
		GunGL.translate(0, -1, 1F);
		GunGL.bindTexture(WeaponResources.cluster_submunition_tex);
		GunGL.renderPart(WeaponResources.fatman, "MiniNuke");
		GunGL.popMatrix();
	};

	public static BiConsumer<EntityBulletBaseMK4, Float> RENDER_NUKE_BALEFIRE = (bullet, interp) -> {

		GunGL.pushMatrix();
		GunGL.scale(0.125F, 0.125F, 0.125F);
		GunGL.rotate(-90, 0, 1, 0);
		GunGL.translate(0, -1, 1F);
		ItemRenderFatMan.renderBalefire(interp);
		GunGL.popMatrix();
	};

	public static BiConsumer<EntityBulletBaseMK4, Float> RENDER_HIVE = (bullet, interp) -> {

		GunGL.pushMatrix();
		GunGL.scale(0.125F, 0.125F, 0.125F);
		GunGL.rotate(90, 0, -1, 0);
		GunGL.translate(0, 0, 3.5F);
		GunGL.bindTexture(WeaponResources.panzerschreck_tex);
		GunGL.renderPart(WeaponResources.panzerschreck, "Rocket");
		GunGL.popMatrix();
	};

	public static BiConsumer<EntityBulletBaseMK4, Float> RENDER_CT_HOOK = (bullet, interp) -> {

		GunGL.pushMatrix();

		GunGL.rotate(bullet.yRotO + (bullet.getYRot() - bullet.yRotO) * interp - 90.0F, 0.0F, 1.0F, 0.0F);
		GunGL.rotate(bullet.xRotO + (bullet.getXRot() - bullet.xRotO) * interp + 180, 0.0F, 0.0F, 1.0F);

		GunGL.scale(0.125F, 0.125F, 0.125F);
		GunGL.rotate(90, 0, -1, 0);
		GunGL.rotate(180, 0, 0, 1);
		GunGL.translate(0, 0, -6F);
		GunGL.bindTexture(WeaponResources.charge_thrower_hook_tex);
		GunGL.renderPart(WeaponResources.charge_thrower, "Hook");
		GunGL.popMatrix();

		if(bullet.getThrower() instanceof Player) {
			Player player = (Player) bullet.getThrower();
			ItemStack held = player.getMainHandItem();
			if(!held.isEmpty() && held.getItem() == WeaponItems.gun("gun_charge_thrower") && com.hbm_m.item.weapon.sedna.impl.ItemGunChargeThrower.getLastHook(held) == bullet.getId()) {
				renderWire(bullet, interp);
			}
		}
	};

	public static void renderWire(EntityBulletBaseMK4 bullet, float interp) {
		GunGL.bindTexture(WeaponResources.wire_greyscale_tex);

		double bx = bullet.xo + (bullet.getX() - bullet.xo) * interp;
		double by = bullet.yo + (bullet.getY() - bullet.yo) * interp;
		double bz = bullet.zo + (bullet.getZ() - bullet.zo) * interp;

		LivingEntity thrower = bullet.getThrower();
		double x = thrower.xo + (thrower.getX() - thrower.xo) * interp;
		double y = thrower.yo + (thrower.getY() - thrower.yo) * interp;
		double z = thrower.zo + (thrower.getZ() - thrower.zo) * interp;
		double eyaw = thrower.yRotO + (thrower.getYRot() - thrower.yRotO) * interp;
		double epitch = thrower.xRotO + (thrower.getXRot() - thrower.xRotO) * interp;

		Vec3NT offset = Vec3NT.createVectorHelper(0.125D, 0.25, -0.75);
		offset.rotateAroundX((float) -epitch / 180F * (float) Math.PI);
		offset.rotateAroundY((float) -eyaw / 180F * (float) Math.PI);

		Vec3NT target = Vec3NT.createVectorHelper(x - offset.xCoord, y + thrower.getEyeHeight() - offset.yCoord, z - offset.zCoord);

		GunGL.disableLighting();
		GunGL.disableCull();

		double deltaX = target.xCoord - bx;
		double deltaY = target.yCoord - by;
		double deltaZ = target.zCoord - bz;
		Vec3NT delta = Vec3NT.createVectorHelper(deltaX, deltaY, deltaZ);

		Tess tess = GunGL.tess();
		tess.startDrawingQuads();

		int count = 10;
		double hang = Math.min(delta.lengthVector() / 15D, 0.5D);

		double girth = 0.03125D;
		double hyp = Math.sqrt(delta.xCoord * delta.xCoord + delta.zCoord * delta.zCoord);
		double yaw = Math.atan2(delta.xCoord, delta.zCoord);
		double pitch = Math.atan2(delta.yCoord, hyp);
		double rotator = Math.PI * 0.5D;
		double newPitch = pitch + rotator;
		double newYaw = yaw + rotator;
		double iZ = Math.cos(yaw) * Math.cos(newPitch) * girth;
		double iX = Math.sin(yaw) * Math.cos(newPitch) * girth;
		double iY = Math.sin(newPitch) * girth;
		double jZ = Math.cos(newYaw) * girth;
		double jX = Math.sin(newYaw) * girth;

		for(float j = 0; j < count; j++) {

			float k = j + 1;

			double sagJ = Math.sin(j / count * Math.PI) * hang;
			double sagK = Math.sin(k / count * Math.PI) * hang;
			double sagMean = (sagJ + sagK) / 2D;

			double ja = j + 0.5D;
			double ix = bx + deltaX / (double)(count) * ja;
			double iy = by + deltaY / (double)(count) * ja - sagMean;
			double iz = bz + deltaZ / (double)(count) * ja;

			// GunGL.Tess kennt nur eine Helligkeit je draw() - es gilt die des letzten Segments
			int brightness = LevelRenderer.getLightColor(bullet.level(), BlockPos.containing(ix, iy, iz));
			tess.setBrightness(brightness);

			tess.setColorOpaque_I(0x606060);

			drawLineSegment(tess,
					(deltaX * j / count),
					(deltaY * j / count) - sagJ,
					(deltaZ * j / count),
					(deltaX * k / count),
					(deltaY * k / count) - sagK,
					(deltaZ * k / count),
					iX, iY, iZ, jX, jZ);
		}

		tess.draw();
		GunGL.enableLighting();
		GunGL.enableCull();
	}

	public static void drawLineSegment(Tess tessellator, double x, double y, double z, double a, double b, double c, double iX, double iY, double iZ, double jX, double jZ) {

		double deltaX = a - x;
		double deltaY = b - y;
		double deltaZ = c - z;
		double length = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
		int wrap = (int) Math.ceil(length * 8);

		if(deltaX + deltaZ < 0) {
			wrap *= -1;
			jZ *= -1;
			jX *= -1;
		}

		tessellator.addVertexWithUV(x + iX, y + iY, z + iZ, 0, 0);
		tessellator.addVertexWithUV(x - iX, y - iY, z - iZ, 0, 1);
		tessellator.addVertexWithUV(a - iX, b - iY, c - iZ, wrap, 1);
		tessellator.addVertexWithUV(a + iX, b + iY, c + iZ, wrap, 0);
		tessellator.addVertexWithUV(x + jX, y, z + jZ, 0, 0);
		tessellator.addVertexWithUV(x - jX, y, z - jZ, 0, 1);
		tessellator.addVertexWithUV(a - jX, b, c - jZ, wrap, 1);
		tessellator.addVertexWithUV(a + jX, b, c + jZ, wrap, 0);
	}

	public static BiConsumer<EntityBulletBaseMK4, Float> RENDER_CT_MORTAR = (bullet, interp) -> {

		GunGL.pushMatrix();
		GunGL.scale(0.125F, 0.125F, 0.125F);
		GunGL.rotate(90, 0, -1, 0);
		GunGL.rotate(180, 0, 0, 1);
		GunGL.translate(0, 0, -6F);
		GunGL.bindTexture(WeaponResources.charge_thrower_mortar_tex);
		GunGL.renderPart(WeaponResources.charge_thrower, "Mortar");
		GunGL.popMatrix();
	};

	public static BiConsumer<EntityBulletBaseMK4, Float> RENDER_CT_MORTAR_CHARGE = (bullet, interp) -> {

		GunGL.pushMatrix();
		GunGL.scale(0.125F, 0.125F, 0.125F);
		GunGL.rotate(90, 0, -1, 0);
		GunGL.rotate(180, 0, 0, 1);
		GunGL.translate(0, 0, -6F);
		GunGL.bindTexture(WeaponResources.charge_thrower_mortar_tex);
		GunGL.renderPart(WeaponResources.charge_thrower, "Mortar");
		GunGL.renderPart(WeaponResources.charge_thrower, "Oomph");
		GunGL.popMatrix();
	};
}
