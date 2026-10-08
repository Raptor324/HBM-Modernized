package com.hbm_m.client.weapon;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.hbm_m.client.particle.CasingClientCreator;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.item.weapon.sedna.factory.Orchestras;
import com.hbm_m.item.weapon.sedna.mods.XWeaponModManager;
import com.hbm_m.particle.helper.ParticleCreators;
import com.hbm_m.render.anim.AnimationEnums.GunAnimation;
import com.hbm_m.render.anim.HbmAnimations;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Clientseitige Teile von {@link Orchestras}: die Schleifenklaenge des Originals ({@code AudioWrapper} ueber
 * {@code MainRegistry.proxy.getLoopedSound}, gespeichert in {@code ItemGunBaseNT.loopedSounds}) und der Empfaenger des
 * Muendungsfeuer-Pakets ({@code MuzzleFlashPacket.Handler}). Wird nur aus {@code isClientSide}-Zweigen aufgerufen.
 */
public final class OrchestrasClient {

	private OrchestrasClient() { }

	/** Original {@code ItemGunBaseNT.loopedSounds} */
	public static final Map<LivingEntity, LoopSound> loopedSounds = new ConcurrentHashMap<>();

	private static boolean initialized = false;

	/** Meldet die Client-Partikelempfaenger an (wird aus dem statischen Block von {@link Orchestras} aufgerufen). */
	public static synchronized void init() {
		if(initialized) return;
		initialized = true;
		ParticleCreators.particleCreators().put("muzzleflash", (world, player, rand, x, y, z, data) -> {
			// Original MuzzleFlashPacket.Handler
			Entity e = world.getEntity(data.getInt("entity"));
			if(!(e instanceof LivingEntity entity) || entity == Minecraft.getInstance().player) return; //packets are sent to the player who fired
			ItemStack stack = entity.getMainHandItem();
			if(stack.isEmpty()) return;

			if(stack.getItem() instanceof ItemGunBaseNT) {
				ItemRenderWeaponBase.flashMap.put(entity, System.currentTimeMillis());
			}
		});
		CasingClientCreator.register();
	}

	/** Original {@code MainRegistry.proxy.getLoopedSound(sound, x, y, z, volume, range, pitch, keepAlive)} - pitch wird wie im Original nicht gesetzt. */
	public static LoopSound getLoopedSound(String sound, float x, float y, float z, float volume, float range, float pitch, int keepAlive) {
		SoundEvent ev = HbmSoundsNT.get(sound);
		LoopSound audio = new LoopSound(ev);
		audio.updatePosition(x, y, z);
		audio.updateVolume(volume);
		audio.updateRange(range);
		audio.setKeepAlive(keepAlive);
		return audio;
	}

	/** Client-Teil von {@code ORCHESTRA_FLAMER} */
	public static void flamer(LivingEntity entity, GunAnimation type, int timer) {
		if(type == GunAnimation.CYCLE) {
			LoopSound runningAudio = loopedSounds.get(entity);

			if(timer < 5) {
				//start sound
				if(runningAudio == null || !runningAudio.isPlaying()) {
					LoopSound audio = getLoopedSound(Orchestras.GUN_FLAMER_LOOP, (float) entity.getX(), (float) entity.getY(), (float) entity.getZ(), 1F, 15F, 1F, 10);
					loopedSounds.put(entity, audio);
					audio.startSound();
					audio.attachTo(entity);
				}
				//keepalive
				if(runningAudio != null && runningAudio.isPlaying()) {
					runningAudio.keepAlive();
				}
			} else {
				//stop sound due to timeout
				if(runningAudio != null && runningAudio.isPlaying()) runningAudio.stopSound();
			}
		}
		//stop sound due to state change
		if(type != GunAnimation.CYCLE) {
			LoopSound runningAudio = loopedSounds.get(entity);
			if(runningAudio != null && runningAudio.isPlaying()) runningAudio.stopSound();
		}
	}

	/** Client-Teil von {@code ORCHESTRA_STINGER} */
	public static void stinger(ItemStack stack, LivingEntity entity) {
		LoopSound runningAudio = loopedSounds.get(entity);
		if(com.hbm_m.item.weapon.sedna.impl.ItemGunStinger.getLockonProgress(stack) > 0 && !ItemGunBaseNT.getIsLockedOn(stack)) {
			//start sound
			if(runningAudio == null || !runningAudio.isPlaying()) {
				LoopSound audio = getLoopedSound(Orchestras.GUN_LOCKON, (float) entity.getX(), (float) entity.getY(), (float) entity.getZ(), 1F, 15F, 1F, 10);
				loopedSounds.put(entity, audio);
				audio.startSound();
			}
			//keepalive
			if(runningAudio != null && runningAudio.isPlaying()) {
				runningAudio.keepAlive();
				runningAudio.updatePosition((float) entity.getX(), (float) entity.getY(), (float) entity.getZ());
			}
		} else {
			//stop sound due to timeout
			if(runningAudio != null && runningAudio.isPlaying()) runningAudio.stopSound();
		}
	}

	/** Client-Teil von {@code ORCHESTRA_CHEMTHROWER} */
	public static void chemthrower(LivingEntity entity, GunAnimation type, int timer) {
		if(type == GunAnimation.CYCLE) {
			LoopSound runningAudio = loopedSounds.get(entity);

			if(timer < 5) {
				//start sound
				if(runningAudio == null || !runningAudio.isPlaying()) {
					LoopSound audio = getLoopedSound(Orchestras.GUN_FLAMER_LOOP, (float) entity.getX(), (float) entity.getY(), (float) entity.getZ(), 1F, 15F, 1F, 10);
					loopedSounds.put(entity, audio);
					audio.startSound();
					audio.attachTo(entity);
				}
				//keepalive
				if(runningAudio != null && runningAudio.isPlaying()) {
					runningAudio.keepAlive();
					runningAudio.attachTo(entity);
				}
			} else {
				//stop sound due to timeout
				if(runningAudio != null && runningAudio.isPlaying()) runningAudio.stopSound();
			}
		}
		//stop sound due to state change
		if(type != GunAnimation.CYCLE) {
			LoopSound runningAudio = loopedSounds.get(entity);
			if(runningAudio != null && runningAudio.isPlaying()) runningAudio.stopSound();
		}
	}

	/** Client-Teil von {@code ORCHESTRA_TAU} */
	public static void tau(LivingEntity entity, GunAnimation type, int timer) {
		if(type == GunAnimation.SPINUP) {
			LoopSound runningAudio = loopedSounds.get(entity);

			if(timer < 300) {
				if(runningAudio == null || !runningAudio.isPlaying()) {
					LoopSound audio = getLoopedSound(Orchestras.GUN_TAU_LOOP, (float) entity.getX(), (float) entity.getY(), (float) entity.getZ(), 1F, 15F, 0.75F, 10);
					audio.updatePitch(0.75F);
					loopedSounds.put(entity, audio);
					audio.startSound();
					audio.attachTo(entity);
				}
				if(runningAudio != null && runningAudio.isPlaying()) {
					runningAudio.keepAlive();
					runningAudio.attachTo(entity);
					runningAudio.updatePitch(0.75F + timer * 0.01F);
				}
			} else {
				if(runningAudio != null && runningAudio.isPlaying()) runningAudio.stopSound();
			}
		}
		//stop sound due to state change
		if(type != GunAnimation.SPINUP) {
			LoopSound runningAudio = loopedSounds.get(entity);
			if(runningAudio != null && runningAudio.isPlaying()) runningAudio.stopSound();
		}
	}

	/** Client-Teil von {@code ORCHESTRA_DRILL} */
	public static void drill(ItemStack stack, int configIndex, LivingEntity entity, GunAnimation type) {
		double speed = HbmAnimations.getRelevantTransformation("SPEED")[0];

		LoopSound runningAudio = loopedSounds.get(entity);

		if(speed > 0) {
			//start sound
			if(runningAudio == null || !runningAudio.isPlaying()) {
				boolean electric = XWeaponModManager.hasUpgrade(stack, configIndex, XWeaponModManager.ID_ENGINE_ELECTRIC);
				LoopSound audio = getLoopedSound(electric ? Orchestras.TURBINE_LARGE_LOOP : Orchestras.ENGINE_LOOP, (float) entity.getX(), (float) entity.getY(), (float) entity.getZ(), (float) speed, 15F, (float) speed, 25);
				loopedSounds.put(entity, audio);
				audio.startSound();
				audio.attachTo(entity);
			}
			//keepalive
			if(runningAudio != null && runningAudio.isPlaying()) {
				runningAudio.keepAlive();
				runningAudio.updateVolume((float) speed);
				runningAudio.updatePitch((float) speed);
			}
		} else {
			//stop sound due to timeout
			//if(runningAudio != null && runningAudio.isPlaying()) runningAudio.stopSound();
			// for some reason this causes stutters, even though speed shouldn't be 0 then
		}

		//stop sound due to state change
		if(type != GunAnimation.CYCLE && type != GunAnimation.CYCLE_DRY) {
			LoopSound rAudio = loopedSounds.get(entity);
			if(rAudio != null && rAudio.isPlaying()) rAudio.stopSound();
		}
	}

	/**
	 * 1:1 {@code AudioWrapperClient} + {@code AudioDynamic}: Schleifenklang mit Keep-Alive-Zeitlimit, optional an eine
	 * Entity gebunden; ist die Entity der eigene Spieler, gilt immer die volle Lautstaerke.
	 */
	public static class LoopSound extends AbstractTickableSoundInstance {

		public float maxVolume = 1;
		public float range = 10;
		public int keepAlive;
		public int timeSinceKA;
		public boolean shouldExpire = false;
		public Entity parentEntity = null;

		protected LoopSound(SoundEvent sound) {
			super(sound, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
			this.looping = true;
			this.delay = 0;
			this.attenuation = SoundInstance.Attenuation.NONE;
		}

		public void updatePosition(float x, float y, float z) { this.x = x; this.y = y; this.z = z; }
		public void attachTo(Entity e) { this.parentEntity = e; }
		public void updateVolume(float volume) { this.maxVolume = volume; }
		public void updateRange(float range) { this.range = range; }
		public void updatePitch(float pitch) { this.pitch = pitch; }
		public void setKeepAlive(int keepAlive) { this.keepAlive = keepAlive; this.shouldExpire = true; }
		public void keepAlive() { this.timeSinceKA = 0; }

		@Override
		public void tick() {
			LocalPlayer player = Minecraft.getInstance().player;
			float f = 0;

			if(parentEntity != null && player != parentEntity) {
				this.updatePosition((float) parentEntity.getX(), (float) parentEntity.getY(), (float) parentEntity.getZ());
			}

			// only adjust volume over distance if the sound isn't attached to this entity
			if(player != null && player != parentEntity) {
				f = (float) Math.sqrt(Math.pow(x - player.getX(), 2) + Math.pow(y - player.getY(), 2) + Math.pow(z - player.getZ(), 2));
				volume = func(f);
			} else {
				// shitty hack that prevents stereo weirdness when using 0 0 0
				if(player != null && player == parentEntity) this.updatePosition((float) parentEntity.getX(), (float) parentEntity.getY() + 10, (float) parentEntity.getZ());
				volume = maxVolume;
			}

			if(this.shouldExpire) {
				if(this.timeSinceKA > this.keepAlive) {
					this.stopSound();
				}
				this.timeSinceKA++;
			}
		}

		public float func(float dist) {
			return (dist / range) * -maxVolume + maxVolume;
		}

		public void startSound() {
			if(!isPlaying()) Minecraft.getInstance().getSoundManager().play(this);
		}

		public void stopSound() {
			this.stop();
			Minecraft.getInstance().getSoundManager().stop(this);
		}

		public boolean isPlaying() {
			return !this.isStopped() && Minecraft.getInstance().getSoundManager().isActive(this);
		}
	}
}
