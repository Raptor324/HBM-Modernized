package com.hbm_m.client.particle;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.google.common.collect.Lists;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.weapon.GunGL;
import com.hbm_m.client.weapon.WeaponResources;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.particle.SpentCasing;
import com.hbm_m.particle.nt.ParticleEngineNT;
import com.hbm_m.particle.nt.ParticleNT;
import com.hbm_m.sound.HbmSoundsNT;
import com.hbm_m.util.BobMathUtil;
import com.hbm_m.util.Vec3NT;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1:1 {@code com.hbm.particle.ParticleSpentCasing}: ausgeworfene Patronenhuelse mit eigener Kollision, Abprallen,
 * Drehimpuls, Aufprallgeraeusch und optionaler Rauchfahne. Laeuft ueber {@link ParticleEngineNT}; gezeichnet wird
 * ueber {@link GunGL} (Modell {@code models/effect/casings.obj}, Textur {@link WeaponResources#casings_tex}).
 */
public class ParticleSpentCasing extends ParticleNT {

	public static final Random rand = new Random();
	private static float dScale = 0.05F, smokeJitter = 0.001F;

	/** Original {@code ResourceManager.casings}. */
	public static final SimpleObjModel casings = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/effect/casings.obj"));

	private int maxSmokeGen = 120;
	private double smokeLift = 0.5D;
	private int nodeLife = 30;

	/** Original {@code Pair<Vec3, Double>}. */
	private static class SmokeNode {
		final Vec3NT key;
		double value;
		SmokeNode(Vec3NT key, double value) { this.key = key; this.value = value; }
	}

	private final List<SmokeNode> smokeNodes = new ArrayList<>();

	private final SpentCasing config;
	private boolean isSmoking;

	private float momentumPitch, momentumYaw;

	public float rotationYaw, rotationPitch, prevRotationYaw, prevRotationPitch;

	/** 1.7-Entity-Felder */
	private float width, height, yOffset;
	private boolean isCollidedHorizontally, isCollidedVertically, isCollided;
	/** EntityFX aktualisiert inWater nie (kein onEntityUpdate) - bleibt wie im Original immer false. */
	private boolean inWater = false;

	public ParticleSpentCasing(ClientLevel world, double x, double y, double z, double mx, double my, double mz, float momentumPitch, float momentumYaw, SpentCasing config, boolean smoking, int smokeLife, double smokeLift, int nodeLife) {
		super(world, x, y, z);
		this.momentumPitch = momentumPitch;
		this.momentumYaw = momentumYaw;
		this.config = config;

		this.lifetime = config.getMaxAge();
		this.setSize(2 * dScale * Math.max(config.getScaleX(), config.getScaleZ()), dScale * config.getScaleY());
		this.yOffset = this.height / 2F;

		this.isSmoking = smoking;
		this.maxSmokeGen = smokeLife;
		this.smokeLift = smokeLift;
		this.nodeLife = nodeLife;

		this.xo = x;
		this.yo = y;
		this.zo = z;

		this.xd = mx;
		this.yd = my;
		this.zd = mz;

		// i am at a loss for words as to what the fuck is going on here, but this is needed, stop asking, fuck you
		this.setPosition(x, y, z);

		this.gravity = 1F;
	}

	private void setSize(float w, float h) {
		this.width = w;
		this.height = h;
		this.bbWidth = w;
		this.bbHeight = h;
	}

	/** 1.7.10 {@code Entity.setPosition}: Boundingbox um yOffset nach unten versetzt. */
	private void setPosition(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
		float f = this.width / 2.0F;
		this.setBoundingBox(new AABB(x - f, y - this.yOffset, z - f, x + f, y - this.yOffset + this.height, z + f));
	}

	@Override
	public void tick() {

		this.xo = this.x;
		this.yo = this.y;
		this.zo = this.z;

		if (this.age++ >= this.lifetime) {
			this.dead = true;
		}

		this.yd -= 0.04D * (double) this.gravity;
		this.moveEntity(this.xd, this.yd, this.zd);
		this.xd *= 0.98D;
		this.yd *= 0.98D;
		this.zd *= 0.98D;

		if (this.onGround) {
			this.xd *= 0.7D;
			this.zd *= 0.7D;

			this.rotationPitch = (float) (Math.floor(this.rotationPitch / 180F + 0.5F)) * 180F;
			this.momentumYaw *= 0.7F;
			this.onGround = false;
		}

		if (age > maxSmokeGen && !smokeNodes.isEmpty())
			smokeNodes.clear();

		if (isSmoking && age <= maxSmokeGen) {

			for (SmokeNode pair : smokeNodes) {
				Vec3NT node = pair.key;

				node.xCoord += rand.nextGaussian() * smokeJitter;
				node.zCoord += rand.nextGaussian() * smokeJitter;
				node.yCoord += smokeLift * dScale;

				pair.value = Math.max(0, pair.value - (1D / (double) nodeLife));
			}

			if (age < maxSmokeGen || inWater) {
				smokeNodes.add(new SmokeNode(Vec3NT.createVectorHelper(0, 0, 0), smokeNodes.isEmpty() ? 0.0D : 1D));
			}
		}

		prevRotationPitch = rotationPitch;
		prevRotationYaw = rotationYaw;

		rotationPitch += momentumPitch;
		rotationYaw += momentumYaw;

		if (Math.abs(prevRotationPitch - rotationPitch) > 180) {
			if (prevRotationPitch < rotationPitch) prevRotationPitch += 360;
			if (prevRotationPitch > rotationPitch) prevRotationPitch -= 360;
		}

		if (Math.abs(prevRotationYaw - rotationYaw) > 180) {
			if (prevRotationYaw < rotationYaw) prevRotationYaw += 360;
			if (prevRotationYaw > rotationYaw) prevRotationYaw -= 360;
		}
	}

	public void moveEntity(double motionX, double motionY, double motionZ) {
		// isInWeb wird fuer Partikel nie gesetzt (keine Blockkollisions-Callbacks), ySize ist immer 0

		//Handle block collision
		double initMoX = motionX;
		double initMoY = motionY;
		double initMoZ = motionZ;

		AABB box = this.getBoundingBox();
		AABB sweep = box.expandTowards(motionX, motionY, motionZ);
		List<VoxelShape> list = Lists.newArrayList(this.level.getBlockCollisions(null, sweep));
		list.addAll(this.level.getEntityCollisions(null, sweep));

		motionY = Shapes.collide(Direction.Axis.Y, box, list, motionY);
		box = box.move(0.0D, motionY, 0.0D);

		motionX = Shapes.collide(Direction.Axis.X, box, list, motionX);
		box = box.move(motionX, 0.0D, 0.0D);

		motionZ = Shapes.collide(Direction.Axis.Z, box, list, motionZ);
		box = box.move(0.0D, 0.0D, motionZ);

		this.setBoundingBox(box);

		this.x = (box.minX + box.maxX) / 2.0D;
		this.y = box.minY + (double) this.yOffset;
		this.z = (box.minZ + box.maxZ) / 2.0D;
		this.isCollidedHorizontally = initMoX != motionX || initMoZ != motionZ;
		this.isCollidedVertically = initMoY != motionY;
		this.onGround = initMoY != motionY && initMoY < 0.0D;
		this.isCollided = this.isCollidedHorizontally || this.isCollidedVertically;
		this.horizontalCollision = this.isCollidedHorizontally;
		this.verticalCollision = this.isCollidedVertically;

		//Handles bounces
		if (initMoX != motionX) {
			this.xd *= -0.25D;

			if (Math.abs(momentumYaw) > 1e-7)
				momentumYaw *= -0.75F;
			else
				momentumYaw = (float) rand.nextGaussian() * 10F * this.config.getBounceYaw();
		}

		if (initMoY != motionY) {
			this.yd *= -0.5D;

			boolean rotFromSpeed = Math.abs(this.yd) > 0.04;
			if (rotFromSpeed || Math.abs(momentumPitch) > 1e-7) {
				momentumPitch *= -0.75F;
				if (rotFromSpeed) {
					float mult = (float) BobMathUtil.safeClamp(initMoY / 0.2F, -1F, 1F);
					momentumPitch += rand.nextGaussian() * 10F * this.config.getBouncePitch() * mult;
					momentumYaw += (float) rand.nextGaussian() * 10F * this.config.getBounceYaw() * mult;
				}
			}
		}

		if (initMoZ != motionZ) {
			this.zd *= -0.25D;

			if (Math.abs(momentumYaw) > 1e-7)
				momentumYaw *= -0.75F;
			else
				momentumYaw = (float) rand.nextGaussian() * 10F * this.config.getBounceYaw();
		}

		if (this.config.getSound() != null && isCollidedVertically && Math.abs(initMoY) >= 0.2) {
			SoundEvent sound = HbmSoundsNT.get(this.config.getSound());
			if (sound != null) {
				this.level.playLocalSound(x, y, z, sound, SoundSource.PLAYERS, SpentCasing.PLINK_LARGE.equals(this.config.getSound()) ? 1F : 0.5F, 1F + rand.nextFloat() * 0.2F, false);
			}
		}
	}

	/** Used for frame-perfect translation of smoke */
	private boolean setupDeltas = false;
	private double prevRenderX;
	private double prevRenderY;
	private double prevRenderZ;

	@Override
	public void render(VertexConsumer ignored, Camera camera, float interp, PoseStack levelPoseStack) {

		double pX = xo + (x - xo) * interp;
		double pY = yo + (y - yo) * interp;
		double pZ = zo + (z - zo) * interp;

		if (!setupDeltas) {
			prevRenderX = pX;
			prevRenderY = pY;
			prevRenderZ = pZ;
			setupDeltas = true;
		}

		int brightness = LevelRenderer.getLightColor(level, BlockPos.containing(pX, pY, pZ));

		// Kameraposition statt interpolierter Spielerposition (1.7.10: posY des Spielers = Augenhoehe)
		Vec3 cam = camera.getPosition();
		double dX = cam.x;
		double dY = cam.y;
		double dZ = cam.z;

		// eigener PoseStack nur mit Verschiebung, Kameradrehung steckt in der ModelView-Matrix (wie ParticleDebrisNT)
		PoseStack pose = new PoseStack();
		GunGL.begin(pose, ParticleEngineNT.buffer(), brightness);

		GunGL.pushMatrix();
		GunGL.enableLighting();
		GunGL.disableBlend();
		GunGL.enableCull();
		GunGL.depthMask(true);

		GunGL.bindTexture(WeaponResources.casings_tex);

		GunGL.translate(pX - dX, pY - dY - this.height / 4 + config.getScaleY() * 0.01, pZ - dZ);

		GunGL.scale(dScale, dScale, dScale);

		GunGL.rotate(180 - (float) BobMathUtil.interp(prevRotationYaw, rotationYaw, interp), 0, 1, 0);
		GunGL.rotate((float) -BobMathUtil.interp(prevRotationPitch, rotationPitch, interp), 1, 0, 0);

		GunGL.scale(config.getScaleX(), config.getScaleY(), config.getScaleZ());

		int index = 0;
		for (String name : config.getType().partNames) {
			int col = this.config.getColors()[index]; //unsafe on purpose, set your colors properly or else...!
			GunGL.color(((col >> 16) & 255) / 255F, ((col >> 8) & 255) / 255F, (col & 255) / 255F);
			GunGL.renderPart(casings, name);
			index++;
		}

		GunGL.color(1F, 1F, 1F);
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(pX - dX, pY - dY - this.height / 4, pZ - dZ);

		if (!smokeNodes.isEmpty()) {
			GunGL.Tess tessellator = GunGL.tess();
			tessellator.startDrawingQuads();
			tessellator.setNormal(0F, 1F, 0F);

			Player player = Minecraft.getInstance().player;
			float scale = config.getScaleX() * 0.5F * dScale;
			Vec3NT vec = Vec3NT.createVectorHelper(scale, 0, 0);
			float yaw = player == null ? camera.getYRot() : player.yRotO + (player.getYRot() - player.yRotO) * interp;
			vec.rotateAroundY((float) Math.toRadians(-yaw));

			double deltaX = prevRenderX - pX;
			double deltaY = prevRenderY - pY;
			double deltaZ = prevRenderZ - pZ;

			for (SmokeNode pair : smokeNodes) {
				Vec3NT pos = pair.key;
				double mult = 1D;
				pos.xCoord += deltaX * mult;
				pos.yCoord += deltaY * mult;
				pos.zCoord += deltaZ * mult;
			}

			for (int i = 0; i < smokeNodes.size() - 1; i++) {
				final SmokeNode node = smokeNodes.get(i), past = smokeNodes.get(i + 1);
				final Vec3NT nodeLoc = node.key, pastLoc = past.key;
				float nodeAlpha = (float) node.value;
				float pastAlpha = (float) past.value;

				double timeAlpha = 1D - (double) age / (double) maxSmokeGen;
				nodeAlpha *= timeAlpha;
				pastAlpha *= timeAlpha;

				tessellator.setNormal(0F, 1F, 0F);
				tessellator.setColorRGBA_F(1F, 1F, 1F, nodeAlpha);
				tessellator.addVertex(nodeLoc.xCoord, nodeLoc.yCoord, nodeLoc.zCoord);
				tessellator.setColorRGBA_F(1F, 1F, 1F, 0F);
				tessellator.addVertex(nodeLoc.xCoord + vec.xCoord, nodeLoc.yCoord, nodeLoc.zCoord + vec.zCoord);
				tessellator.setColorRGBA_F(1F, 1F, 1F, 0F);
				tessellator.addVertex(pastLoc.xCoord + vec.xCoord, pastLoc.yCoord, pastLoc.zCoord + vec.zCoord);
				tessellator.setColorRGBA_F(1F, 1F, 1F, pastAlpha);
				tessellator.addVertex(pastLoc.xCoord, pastLoc.yCoord, pastLoc.zCoord);

				tessellator.setColorRGBA_F(1F, 1F, 1F, nodeAlpha);
				tessellator.addVertex(nodeLoc.xCoord, nodeLoc.yCoord, nodeLoc.zCoord);
				tessellator.setColorRGBA_F(1F, 1F, 1F, 0F);
				tessellator.addVertex(nodeLoc.xCoord - vec.xCoord, nodeLoc.yCoord, nodeLoc.zCoord - vec.zCoord);
				tessellator.setColorRGBA_F(1F, 1F, 1F, 0F);
				tessellator.addVertex(pastLoc.xCoord - vec.xCoord, pastLoc.yCoord, pastLoc.zCoord - vec.zCoord);
				tessellator.setColorRGBA_F(1F, 1F, 1F, pastAlpha);
				tessellator.addVertex(pastLoc.xCoord, pastLoc.yCoord, pastLoc.zCoord);
			}

			GunGL.enableBlend();
			GunGL.blendFunc(GunGL.GL_SRC_ALPHA, GunGL.GL_ONE_MINUS_SRC_ALPHA);
			GunGL.disableTexture2D();
			GunGL.disableCull();
			tessellator.draw();
			GunGL.enableCull();
			GunGL.enableTexture2D();
			GunGL.disableBlend();
		}

		GunGL.popMatrix();
		GunGL.end();

		prevRenderX = pX;
		prevRenderY = pY;
		prevRenderZ = pZ;
	}

	@Override
	public RenderType getRenderType() {
		return RenderType.entityCutout(WeaponResources.casings_tex);
	}

	/** Original {@code getBrightnessForRender}. */
	public int getBrightnessForRender(float interp) {
		int bx = Mth.floor(this.x);
		int bz = Mth.floor(this.z);
		BlockPos probe = new BlockPos(bx, 0, bz);

		if (this.level.hasChunkAt(probe)) {
			double d0 = (this.getBoundingBox().maxY - this.getBoundingBox().minY) * 0.66D;
			int by = Mth.floor(this.y - (double) this.yOffset + d0);
			return LevelRenderer.getLightColor(this.level, new BlockPos(bx, by, bz));
		} else {
			return 0;
		}
	}
}
