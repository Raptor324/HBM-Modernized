package com.hbm_m.item.weapon.sedna.factory;

import java.util.List;
import java.util.function.BiConsumer;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.damagesource.ModDamageSources;
import com.hbm_m.damagesource.ModDamageTypes;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.item.weapon.sedna.Receiver;
import com.hbm_m.item.weapon.sedna.WeaponItems;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.hbm_m.item.weapon.sedna.mags.IMagazine;
import com.hbm_m.item.weapon.sedna.mods.XWeaponModManager;
import com.hbm_m.particle.SpentCasing;
import com.hbm_m.particle.helper.CasingCreator;
import com.hbm_m.particle.helper.IParticleCreator;
import com.hbm_m.render.anim.AnimationEnums.GunAnimation;
import com.hbm_m.sound.HbmSoundsNT;
import com.hbm_m.util.MovingObjectPosition;
import com.hbm_m.util.Vec3NT;

import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Orchestras are server-side components that run along client-side animations.
 * The orchestra only knows what animation is or was playing and how long it started, but not if it is still active.
 * Orchestras are useful for things like playing server-side sound, spawning casings or sending particle packets.
 *
 * <p>Port: die Lambdas laufen auf beiden Seiten (Server fuer alle, Client nur fuer den eigenen gehaltenen Spieler,
 * siehe {@code ItemGunBaseNT.inventoryTick}). Server: Klaenge ueber {@link Lego#playSound}, Huelsen ueber
 * {@link CasingCreator} (Paket "casingNT"), Muendungsfeuer ueber {@link #sendMuzzleFlash} (Paket "muzzleflash",
 * Ersatz fuer {@code MuzzleFlashPacket}). Client: Schleifenklaenge ({@code AudioWrapper}) in
 * {@code com.hbm_m.client.weapon.OrchestrasClient}, nur innerhalb von {@code isClientSide}-Zweigen aufgerufen.</p>
 */
public class Orchestras {

	static {
		// Client-Partikelempfaenger ("muzzleflash", "casingNT") anmelden - die Klasse wird beim Registrieren der Waffen geladen
		EnvExecutor.runInEnv(Env.CLIENT, () -> () -> com.hbm_m.client.weapon.OrchestrasClient.init());
	}

	/* NTMSounds-Schluessel des Originals (alle in HbmSoundsNT vorhanden) */
	public static final String GUN_REVOLVER_COCK = "hbm:weapon.reload.revolverCock";
	public static final String GUN_REVOLVER_CLOSE = "hbm:weapon.reload.revolverClose";
	public static final String GUN_REVOLVER_SPIN = "hbm:weapon.reload.revolverSpin";
	public static final String GUN_PISTOL_COCK = "hbm:weapon.reload.pistolCock";
	public static final String GUN_MAG_SMALL_REMOVE = "hbm:weapon.reload.magSmallRemove";
	public static final String GUN_MAG_SMALL_INSERT = "hbm:weapon.reload.magSmallInsert";
	public static final String GUN_MAG_REMOVE = "hbm:weapon.reload.magRemove";
	public static final String GUN_MAG_INSERT = "hbm:weapon.reload.magInsert";
	public static final String GUN_CANISTER_INSERT = "hbm:weapon.reload.insertCanister";
	public static final String GUN_ROCKET_INSERT = "hbm:weapon.reload.insertRocket";
	public static final String GUN_BOLT_OPEN = "hbm:weapon.reload.boltOpen";
	public static final String GUN_BOLT_CLOSE = "hbm:weapon.reload.boltClose";
	public static final String GUN_RIFLE_COCK = "hbm:weapon.reload.rifleCock";
	public static final String GUN_LEVER_COCK = "hbm:weapon.reload.leverCock";
	public static final String GUN_SHOTGUN_LOAD = "hbm:weapon.reload.shotgunReload";
	public static final String GUN_SHOTGUN_OPEN = "hbm:weapon.reload.shotgunCockOpen";
	public static final String GUN_SHOTGUN_CLOSE = "hbm:weapon.reload.shotgunCockClose";
	public static final String GUN_SHOTGUN_COCK = "hbm:weapon.reload.shotgunCock";
	public static final String GUN_GRENADE_RELOAD = "hbm:weapon.glReload";
	public static final String GUN_GRENADE_OPEN = "hbm:weapon.glOpen";
	public static final String GUN_GRENADE_CLOSE = "hbm:weapon.glClose";
	public static final String GUN_SCREW = "hbm:weapon.reload.screw";
	public static final String GUN_COIL_RELOAD = "hbm:weapon.coilgunReload";
	public static final String GUN_IMPACT = "hbm:weapon.reload.impact";
	public static final String GUN_LATCH_OPEN = "hbm:weapon.reload.openLatch";
	public static final String GUN_VALVE = "hbm:weapon.reload.pressureValve";
	public static final String GUN_FATMAN_RELOAD = "hbm:weapon.reload.fatmanFull";
	public static final String GUN_WHACK = "hbm:weapon.foley.gunWhack";
	public static final String GUN_LOCKON = "hbm:weapon.fire.lockon";
	public static final String GUN_SMACK = "hbm:weapon.fire.smack";
	public static final String GUN_STAB_A_FUCKER = "hbm:weapon.fire.stab";
	public static final String GUN_SHREDDER_CYCLE = "hbm:weapon.fire.shredderCycle";
	public static final String GUN_DRY_FIRE = "hbm:weapon.reload.dryFireClick";
	public static final String GUN_FLAMER_LOOP = "hbm:weapon.fire.flameLoop";
	public static final String GUN_TAU_FIRE = "hbm:weapon.fire.tau";
	public static final String GUN_TAU_LOOP = "hbm:weapon.fire.tauLoop";
	public static final String GUN_TESLA_BLAST = "hbm:entity.ufoBlast";
	public static final String TURRET_CIWS_RELOAD = "hbm:turret.howard_reload";
	public static final String PLAYER_GULP = "hbm:player.gulp";
	public static final String PLAYER_GROAN = "hbm:player.groan";
	public static final String BLOCK_PLUSHY = "hbm:block.squeakyToy";
	public static final String ENGINE_LOOP = "hbm:block.engine";
	public static final String TURBINE_LARGE_LOOP = "hbm:block.largeTurbineRunning";
	public static final String VANILLA_FIREWORKS_BANG = "fireworks.blast";

	/** Original {@code PacketDispatcher.wrapper.sendToAllAround(new MuzzleFlashPacket(entity), new TargetPoint(dim, x, y, z, 100))} */
	public static void sendMuzzleFlash(LivingEntity entity) {
		if(!(entity.level() instanceof ServerLevel server)) return;
		CompoundTag data = new CompoundTag();
		data.putString("type", "muzzleflash");
		data.putInt("entity", entity.getId());
		IParticleCreator.sendPacket(server, entity.getX(), entity.getY(), entity.getZ(), 100, data);
	}

	/** Original {@code worldObj.playSoundEffect(x, y, z, sound, vol, pitch)} - Vanilla-Schluessel ("fireworks.blast") werden uebersetzt. */
	public static void playSoundEffect(Level world, double x, double y, double z, String sound, float volume, float pitch) {
		SoundEvent ev = VANILLA_FIREWORKS_BANG.equals(sound) ? SoundEvents.FIREWORK_ROCKET_BLAST : HbmSoundsNT.get(sound);
		if(ev != null) world.playSound(null, x, y, z, ev, SoundSource.PLAYERS, volume, pitch);
	}

	/** Port-Hilfe: Original {@code EntityDamageUtil.getMouseOver(player, reach)} (fehlt im Port-EntityDamageUtil). */
	public static MovingObjectPosition getMouseOver(Player attacker, double reach) {
		return getMouseOver(attacker, reach, 0D);
	}

	/** Port-Hilfe: Original {@code EntityDamageUtil.getMouseOver(player, reach, threshold)}. */
	/** Delegiert an das Original {@code EntityDamageUtil.getMouseOver}. */
	public static MovingObjectPosition getMouseOver(Player attacker, double reach, double threshold) {
		return com.hbm_m.util.EntityDamageUtil.getMouseOver(attacker, reach, threshold);
	}

	public static BiConsumer<ItemStack, LambdaContext> DEBUG_ORCHESTRA = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);

		if(type == GunAnimation.RELOAD) {
			if(timer == 3) Lego.playSound(entity, GUN_REVOLVER_COCK, 1F, 1F);
			if(timer == 10) Lego.playSound(entity, GUN_MAG_SMALL_REMOVE, 1F, 1F);
			if(timer == 34) Lego.playSound(entity, GUN_MAG_SMALL_INSERT, 1F, 1F);
			if(timer == 40) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 1F);

			if(timer == 16) {
				Receiver rec = ctx.config.getReceivers(stack)[0];
				IMagazine<?> mag = rec.getMagazine(stack);
				SpentCasing casing = mag.getCasing(stack, ctx.inventory);
				if(casing != null) for(int i = 0; i < mag.getCapacity(stack); i++) CasingCreator.composeEffect(entity.level(), entity, 0.25, -0.125, -0.125, -0.05, 0, 0, 0.01, casing.getName());
			}
		}
		if(type == GunAnimation.CYCLE) {
			if(timer == 0) sendMuzzleFlash(entity);
			if(timer == 11) Lego.playSound(entity, GUN_REVOLVER_COCK, 1F, 1F);
		}
		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 2) Lego.playSound(entity, GUN_DRY_FIRE, 1F, 1F);
			if(timer == 11) Lego.playSound(entity, GUN_REVOLVER_COCK, 1F, 1F);
		}
		if(type == GunAnimation.INSPECT) {
			if(timer == 3) Lego.playSound(entity, GUN_REVOLVER_COCK, 1F, 1F);
			if(timer == 16) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 1F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_PEPPERBOX = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);

		if(type == GunAnimation.RELOAD) {
			if(timer == 24) Lego.playSound(entity, GUN_MAG_SMALL_INSERT, 1F, 1F);
			if(timer == 55) Lego.playSound(entity, GUN_REVOLVER_SPIN, 1F, 1F);
		}
		if(type == GunAnimation.CYCLE) {
			if(timer == 0) sendMuzzleFlash(entity);
			if(timer == 21) Lego.playSound(entity, GUN_REVOLVER_COCK, 1F, 0.6F);
		}
		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 2) Lego.playSound(entity, GUN_DRY_FIRE, 1F, 0.8F);
			if(timer == 11) Lego.playSound(entity, GUN_REVOLVER_COCK, 1F, 0.6F);
		}
		if(type == GunAnimation.INSPECT) {
			if(timer == 3) Lego.playSound(entity, GUN_REVOLVER_SPIN, 1F, 1F);
		}
		if(type == GunAnimation.JAMMED) {
			if(timer == 28) Lego.playSound(entity, GUN_DRY_FIRE, 1F, 0.75F);
			if(timer == 45) Lego.playSound(entity, GUN_DRY_FIRE, 1F, 0.6F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_ATLAS = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);

		if(type == GunAnimation.RELOAD) {
			if(timer == 2) Lego.playSound(entity, GUN_MAG_SMALL_REMOVE, 1F, 1F);
			if(timer == 36) Lego.playSound(entity, GUN_MAG_SMALL_INSERT, 1F, 1F);
			if(timer == 44) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 1F);
		}
		if(type == GunAnimation.CYCLE) {
			if(timer == 0) sendMuzzleFlash(entity);
			if(timer == 5) Lego.playSound(entity, GUN_REVOLVER_COCK, 1F, 0.9F);
		}
		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 2) Lego.playSound(entity, GUN_DRY_FIRE, 1F, 1F);
			if(timer == 5) Lego.playSound(entity, GUN_REVOLVER_COCK, 1F, 0.9F);
		}
		if(type == GunAnimation.INSPECT) {
			if(timer == 2) Lego.playSound(entity, GUN_MAG_SMALL_REMOVE, 1F, 1F);
			if(timer == 24) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 1F);
		}
		if(type == GunAnimation.JAMMED) {
			if(timer == 12) Lego.playSound(entity, GUN_MAG_SMALL_REMOVE, 1F, 1F);
			if(timer == 34) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 1F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_DANI = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);

		if(type == GunAnimation.RELOAD) {
			if(timer == 2) Lego.playSound(entity, GUN_MAG_SMALL_REMOVE, 1F, 1F);
			if(timer == 36) Lego.playSound(entity, GUN_MAG_SMALL_INSERT, 1F, 1F);
			if(timer == 44) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 1F);
		}
		if(type == GunAnimation.CYCLE) {
			if(timer == 0) sendMuzzleFlash(entity);
			if(timer == 5) Lego.playSound(entity, GUN_REVOLVER_COCK, 1F, 0.9F);
		}
		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 2) Lego.playSound(entity, GUN_DRY_FIRE, 1F, 1F);
			if(timer == 5) Lego.playSound(entity, GUN_REVOLVER_COCK, 1F, 0.9F);
		}
		if(type == GunAnimation.INSPECT) {
			if(timer == 2) Lego.playSound(entity, GUN_MAG_SMALL_REMOVE, 1F, 1F);
			if(timer == 24) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 1F);
		}
		if(type == GunAnimation.JAMMED) {
			if(timer == 12) Lego.playSound(entity, GUN_MAG_SMALL_REMOVE, 1F, 1F);
			if(timer == 34) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 1F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_HENRY = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		boolean aiming = ItemGunBaseNT.getIsAiming(stack);

		if(type == GunAnimation.RELOAD) {
			if(timer == 8) Lego.playSound(entity, GUN_MAG_SMALL_REMOVE, 1F, 1F);
			if(timer == 16) Lego.playSound(entity, GUN_MAG_SMALL_INSERT, 1F, 1F);
		}
		if(type == GunAnimation.RELOAD_CYCLE) {
			if(timer == 0) Lego.playSound(entity, GUN_MAG_SMALL_INSERT, 1F, 1F);
		}
		if(type == GunAnimation.RELOAD_END) {
			if(timer == 0) Lego.playSound(entity, GUN_MAG_SMALL_REMOVE, 1F, 0.9F);
			if(timer == 12 && ctx.config.getReceivers(stack)[0].getMagazine(stack).getAmountBeforeReload(stack) <= 0) Lego.playSound(entity, GUN_LEVER_COCK, 1F, 1F);
		}
		if(type == GunAnimation.JAMMED) {
			if(timer == 0) Lego.playSound(entity, GUN_MAG_SMALL_REMOVE, 1F, 0.9F);
			if(timer == 12) Lego.playSound(entity, GUN_LEVER_COCK, 1F, 1F);
			if(timer == 36) Lego.playSound(entity, GUN_LEVER_COCK, 1F, 1F);
			if(timer == 44) Lego.playSound(entity, GUN_LEVER_COCK, 1F, 1F);
		}
		if(type == GunAnimation.CYCLE) {
			if(timer == 0) sendMuzzleFlash(entity);
			if(timer == 14) {
				SpentCasing casing = ctx.config.getReceivers(stack)[0].getMagazine(stack).getCasing(stack, ctx.inventory);
				if(casing != null) CasingCreator.composeEffect(entity.level(), entity, 0.5, -0.125, aiming ? -0.125 : -0.375D, 0, 0.12, -0.12, 0.01, -7.5F + (float)entity.getRandom().nextGaussian() * 5F, (float)entity.getRandom().nextGaussian() * 1.5F, casing.getName(), true, 60, 0.5D, 20);
			}
			if(timer == 12) Lego.playSound(entity, GUN_LEVER_COCK, 1F, 1F);
		}
		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 2) Lego.playSound(entity, GUN_DRY_FIRE, 1F, 1F);
			if(timer == 12) Lego.playSound(entity, GUN_LEVER_COCK, 1F, 1F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_GREASEGUN = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		boolean aiming = ItemGunBaseNT.getIsAiming(stack);

		if(type == GunAnimation.EQUIP) {
			if(timer == 5) Lego.playSound(entity, GUN_LATCH_OPEN, 1F, 1F);
		}
		if(type == GunAnimation.CYCLE) {
			if(timer == 0) sendMuzzleFlash(entity);
			if(timer == 2) {
				SpentCasing casing = ctx.config.getReceivers(stack)[0].getMagazine(stack).getCasing(stack, ctx.inventory);
				if(casing != null) CasingCreator.composeEffect(entity.level(), entity, 0.55, aiming ? 0 : -0.125, aiming ? 0 : -0.25D, 0, 0.18, -0.12, 0.01, -7.5F + (float)entity.getRandom().nextGaussian() * 5F, 12F + (float)entity.getRandom().nextGaussian() * 5F, casing.getName());
			}
		}
		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 0) Lego.playSound(entity, GUN_DRY_FIRE, 1F, 0.8F);
			if(timer == 11) Lego.playSound(entity, GUN_PISTOL_COCK, 1F, 0.8F);

		}
		if(type == GunAnimation.RELOAD) {
			if(timer == 2) Lego.playSound(entity, GUN_MAG_REMOVE, 1F, 1F);
			if(timer == 24) Lego.playSound(entity, GUN_MAG_INSERT, 1F, 1F);
			if(timer == 36) Lego.playSound(entity, GUN_PISTOL_COCK, 1F, 0.8F);
		}
		if(type == GunAnimation.INSPECT) {
			if(timer == 5) Lego.playSound(entity, GUN_REVOLVER_COCK, 1F, 0.8F);
			if(timer == 26) Lego.playSound(entity, GUN_MAG_SMALL_INSERT, 1F, 1.25F);
		}
		if(type == GunAnimation.JAMMED) {
			if(timer == 11) Lego.playSound(entity, GUN_PISTOL_COCK, 1F, 0.8F);
			if(timer == 26) Lego.playSound(entity, GUN_PISTOL_COCK, 1F, 0.8F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_MARESLEG = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		boolean aiming = ItemGunBaseNT.getIsAiming(stack);

		if(type == GunAnimation.RELOAD) {
			if(timer == 8) Lego.playSound(entity, GUN_REVOLVER_COCK, 1F, 0.8F);
			if(timer == 16) Lego.playSound(entity, GUN_SHOTGUN_LOAD, 1F, 1F);
		}
		if(type == GunAnimation.RELOAD_CYCLE) {
			if(timer == 0) Lego.playSound(entity, GUN_SHOTGUN_LOAD, 1F, 1F);
		}
		if(type == GunAnimation.RELOAD_END) {
			if(timer == 2) Lego.playSound(entity, GUN_REVOLVER_COCK, 1F, 0.7F);
		}
		if(type == GunAnimation.JAMMED) {
			if(timer == 2) Lego.playSound(entity, GUN_REVOLVER_COCK, 1F, 0.7F);
			if(timer == 17) Lego.playSound(entity, GUN_LEVER_COCK, 1F, 0.8F);
			if(timer == 29) Lego.playSound(entity, GUN_LEVER_COCK, 1F, 0.8F);
		}
		if(type == GunAnimation.CYCLE) {
			if(timer == 0) sendMuzzleFlash(entity);
			if(timer == 14) {
				SpentCasing casing = ctx.config.getReceivers(stack)[0].getMagazine(stack).getCasing(stack, ctx.inventory);
				if(casing != null) CasingCreator.composeEffect(entity.level(), entity, 0.3125, -0.125, aiming ? -0.125 : -0.375D, 0, 0.18, -0.12, 0.01, -10F + (float)entity.getRandom().nextGaussian() * 5F, (float)entity.getRandom().nextGaussian() * 2.5F, casing.getName(), true, 60, 0.5D, 20);
			}
			if(timer == 8) Lego.playSound(entity, GUN_LEVER_COCK, 1F, 0.8F);
		}
		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 2) Lego.playSound(entity, GUN_DRY_FIRE, 1F, 1F);
			if(timer == 8) Lego.playSound(entity, GUN_LEVER_COCK, 1F, 0.8F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_MARESLEG_SHORT = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		boolean aiming = ItemGunBaseNT.getIsAiming(stack);

		if(type == GunAnimation.RELOAD) {
			if(timer == 8) Lego.playSound(entity, GUN_REVOLVER_COCK, 1F, 0.8F);
			if(timer == 16) Lego.playSound(entity, GUN_SHOTGUN_LOAD, 1F, 1F);
		}
		if(type == GunAnimation.RELOAD_CYCLE) {
			if(timer == 0) Lego.playSound(entity, GUN_SHOTGUN_LOAD, 1F, 1F);
		}
		if(type == GunAnimation.RELOAD_END) {
			if(timer == 2) Lego.playSound(entity, GUN_REVOLVER_COCK, 1F, 0.7F);
		}
		if(type == GunAnimation.JAMMED) {
			if(timer == 2) Lego.playSound(entity, GUN_REVOLVER_COCK, 1F, 0.7F);
			if(timer == 17) Lego.playSound(entity, GUN_LEVER_COCK, 1F, 0.8F);
			if(timer == 29) Lego.playSound(entity, GUN_LEVER_COCK, 1F, 0.8F);
		}
		if(type == GunAnimation.CYCLE) {
			if(timer == 0) sendMuzzleFlash(entity);
			if(timer == 14) {
				SpentCasing casing = ctx.config.getReceivers(stack)[0].getMagazine(stack).getCasing(stack, ctx.inventory);
				if(casing != null) CasingCreator.composeEffect(entity.level(), entity, 0.3125, -0.125, aiming ? -0.125 : -0.375D, 0, -0.08, 0, 0.01, -15F + (float)entity.getRandom().nextGaussian() * 5F, (float)entity.getRandom().nextGaussian() * 2.5F, casing.getName(), true, 60, 0.5D, 20);
			}
			if(timer == 8) Lego.playSound(entity, GUN_LEVER_COCK, 1F, 0.8F);
		}
		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 2) Lego.playSound(entity, GUN_DRY_FIRE, 1F, 1F);
			if(timer == 8) Lego.playSound(entity, GUN_LEVER_COCK, 1F, 0.8F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_MARESLEG_AKIMBO = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		boolean aiming = ItemGunBaseNT.getIsAiming(stack);

		if(type == GunAnimation.CYCLE) {
			if(timer == 0) sendMuzzleFlash(entity);
			if(timer == 14) {
				int offset = ctx.configIndex == 0 ? -1 : 1;
				SpentCasing casing = ctx.config.getReceivers(stack)[0].getMagazine(stack).getCasing(stack, ctx.inventory);
				if(casing != null) CasingCreator.composeEffect(entity.level(), entity, 0.3125, -0.125, aiming ? -0.125 * offset : -0.375D * offset, 0, -0.08, 0, 0.01, -15F + (float)entity.getRandom().nextGaussian() * 5F, (float)entity.getRandom().nextGaussian() * 2.5F, casing.getName(), true, 60, 0.5D, 20);
			}
			if(timer == 8) Lego.playSound(entity, GUN_LEVER_COCK, 1F, 0.8F);
			return;
		}

		ORCHESTRA_MARESLEG_SHORT.accept(stack, ctx);
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_FLAREGUN = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		boolean aiming = ItemGunBaseNT.getIsAiming(stack);

		if(type == GunAnimation.RELOAD) {
			if(timer == 0) Lego.playSound(entity, GUN_MAG_SMALL_REMOVE, 1F, 0.8F);
			if(timer == 4) {
				IMagazine<?> mag = ctx.config.getReceivers(stack)[0].getMagazine(stack);
				if(mag.getAmountAfterReload(stack) > 0) {
					SpentCasing casing = ctx.config.getReceivers(stack)[0].getMagazine(stack).getCasing(stack, ctx.inventory);
					if(casing != null) CasingCreator.composeEffect(entity.level(), entity, 0.625, -0.125, aiming ? -0.125 : -0.375D, -0.12, 0.18, 0, 0.01, -15F + (float)entity.getRandom().nextGaussian() * 7.5F, (float)entity.getRandom().nextGaussian() * 5F, casing.getName(), true, 60, 0.5D, 20);
					mag.setAmountBeforeReload(stack, 0);
				}
			}
			if(timer == 16) Lego.playSound(entity, GUN_CANISTER_INSERT, 1F, 1F);
			if(timer == 24) Lego.playSound(entity, GUN_MAG_SMALL_INSERT, 1F, 1F);
		}
		if(type == GunAnimation.JAMMED) {
			if(timer == 10) Lego.playSound(entity, GUN_MAG_SMALL_REMOVE, 1F, 0.8F);
			if(timer == 29) Lego.playSound(entity, GUN_MAG_SMALL_INSERT, 1F, 1F);
		}
		if(type == GunAnimation.CYCLE) {
			if(timer == 12) Lego.playSound(entity, GUN_REVOLVER_COCK, 1F, 1F);
		}
		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 2) Lego.playSound(entity, GUN_DRY_FIRE, 1F, 1F);
			if(timer == 12) Lego.playSound(entity, GUN_REVOLVER_COCK, 1F, 1F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_NOPIP = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);

		if(type == GunAnimation.RELOAD) {
			if(timer == 3) Lego.playSound(entity, GUN_REVOLVER_COCK, 1F, 1F);
			if(timer == 10) Lego.playSound(entity, GUN_MAG_SMALL_REMOVE, 1F, 1F);
			if(timer == 34) Lego.playSound(entity, GUN_MAG_SMALL_INSERT, 1F, 1F);
			if(timer == 40) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 1F);

			if(timer == 16) {
				Receiver rec = ctx.config.getReceivers(stack)[0];
				IMagazine<?> mag = rec.getMagazine(stack);
				SpentCasing casing = mag.getCasing(stack, ctx.inventory);
				if(casing != null) for(int i = 0; i < mag.getCapacity(stack); i++) CasingCreator.composeEffect(entity.level(), entity, 0.25, -0.125, -0.125, -0.05, 0, 0, 0.01, -6.5F + (float)entity.getRandom().nextGaussian() * 3F, (float)entity.getRandom().nextGaussian() * 5F, casing.getName());
			}
		}
		if(type == GunAnimation.CYCLE) {
			if(timer == 0) sendMuzzleFlash(entity);
			if(timer == 11) Lego.playSound(entity, GUN_REVOLVER_COCK, 1F, 1F);
		}
		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 2) Lego.playSound(entity, GUN_DRY_FIRE, 1F, 1F);
			if(timer == 11) Lego.playSound(entity, GUN_REVOLVER_COCK, 1F, 1F);
		}
		if(type == GunAnimation.INSPECT) {
			if(timer == 3) Lego.playSound(entity, GUN_REVOLVER_COCK, 1F, 1F);
			if(timer == 16) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 1F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_CARBINE = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		boolean aiming = ItemGunBaseNT.getIsAiming(stack);

		if(type == GunAnimation.CYCLE) {
			if(timer == 0) sendMuzzleFlash(entity);
			if(timer == 1) {
				SpentCasing casing = ctx.config.getReceivers(stack)[0].getMagazine(stack).getCasing(stack, ctx.inventory);
				if(casing != null) CasingCreator.composeEffect(entity.level(), entity, 0.3125, aiming ? 0 : -0.125, aiming ? 0 : -0.25D, 0, 0.21, -0.06, 0.01, -10F + (float)entity.getRandom().nextGaussian() * 2.5F, 2.5F + (float)entity.getRandom().nextGaussian() * 2F, casing.getName(), true, 60, 0.5D, 20);
			}
		}
		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 2) Lego.playSound(entity, GUN_DRY_FIRE, 1F, 1F);
			if(timer == 8) Lego.playSound(entity, GUN_PISTOL_COCK, 1F, 0.8F);
		}
		if(type == GunAnimation.RELOAD) {
			if(timer == 2) Lego.playSound(entity, GUN_MAG_REMOVE, 1F, 1F);
			if(timer == 26) Lego.playSound(entity, GUN_MAG_INSERT, 1F, 1F);
		}
		if(type == GunAnimation.RELOAD_END) {
			if(timer == 2) Lego.playSound(entity, GUN_PISTOL_COCK, 1F, 0.8F);
		}
		if(type == GunAnimation.JAMMED) {
			if(timer == 2) Lego.playSound(entity, GUN_PISTOL_COCK, 1F, 0.8F);
			if(timer == 31) Lego.playSound(entity, GUN_PISTOL_COCK, 1F, 0.8F);
		}
		if(type == GunAnimation.INSPECT) {
			if(timer == 6) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 1F);
			if(timer == 30) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 0.9F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_AM180 = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		boolean aiming = ItemGunBaseNT.getIsAiming(stack);

		if(ModClothConfig.get().gunAnimsLegacy) {
			if(type == GunAnimation.CYCLE) {
				if(timer == 0) {
					sendMuzzleFlash(entity);
					SpentCasing casing = ctx.config.getReceivers(stack)[0].getMagazine(stack).getCasing(stack, ctx.inventory);
					if(casing != null) CasingCreator.composeEffect(entity.level(), entity, 0.4375, aiming ? 0 : -0.125, aiming ? 0 : -0.25D, 0, -0.06, 0, 0.01, (float)entity.getRandom().nextGaussian() * 10F, (float)entity.getRandom().nextGaussian() * 10F, casing.getName());
				}
			}
			if(type == GunAnimation.CYCLE_DRY) {
				if(timer == 0) Lego.playSound(entity, GUN_DRY_FIRE, 1F, 1F);
				if(timer == 6) Lego.playSound(entity, GUN_PISTOL_COCK, 1F, 0.9F);
			}
			if(type == GunAnimation.RELOAD) {
				if(timer == 2) Lego.playSound(entity, GUN_MAG_REMOVE, 1F, 1F);
				if(timer == 20) Lego.playSound(entity, GUN_IMPACT, 0.25F, 1F);
				if(timer == 32) Lego.playSound(entity, GUN_MAG_INSERT, 1F, 1F);
				if(timer == 40) Lego.playSound(entity, GUN_PISTOL_COCK, 1F, 0.9F);
			}
			if(type == GunAnimation.JAMMED) {
				if(timer == 15) Lego.playSound(entity, GUN_PISTOL_COCK, 1F, 0.8F);
			}
			if(type == GunAnimation.INSPECT) {
				if(timer == 2) Lego.playSound(entity, GUN_MAG_REMOVE, 1F, 1F);
				if(timer == 35) Lego.playSound(entity, GUN_MAG_INSERT, 1F, 1F);
			}
		} else {
			if(type == GunAnimation.CYCLE) {
				if(timer == 0) {
					sendMuzzleFlash(entity);
					SpentCasing casing = ctx.config.getReceivers(stack)[0].getMagazine(stack).getCasing(stack, ctx.inventory);
					if(casing != null) CasingCreator.composeEffect(entity.level(), entity, 0.4375, aiming ? 0 : -0.125, aiming ? 0 : -0.25D, 0, -0.06, 0, 0.01, (float)entity.getRandom().nextGaussian() * 10F, (float)entity.getRandom().nextGaussian() * 10F, casing.getName());
				}
			}
			if(type == GunAnimation.CYCLE_DRY) {
				if(timer == 0) Lego.playSound(entity, GUN_DRY_FIRE, 1F, 1F);
				if(timer == 6) Lego.playSound(entity, GUN_PISTOL_COCK, 1F, 0.9F);
			}
			if(type == GunAnimation.RELOAD) {
				if(timer == 6) Lego.playSound(entity, GUN_MAG_REMOVE, 1F, 1F);
				if(timer == 26) Lego.playSound(entity, GUN_IMPACT, 0.25F, 1F);
				if(timer == 48) Lego.playSound(entity, GUN_MAG_INSERT, 1F, 1F);
				if(timer == 54) Lego.playSound(entity, GUN_PISTOL_COCK, 1F, 0.9F);
			}
			if(type == GunAnimation.JAMMED) {
				if(timer == 6) Lego.playSound(entity, GUN_PISTOL_COCK, 1F, 0.8F);
				if(timer == 20) Lego.playSound(entity, GUN_PISTOL_COCK, 1F, 1.0F);
			}
			if(type == GunAnimation.INSPECT) {
				if(timer == 6) Lego.playSound(entity, GUN_MAG_REMOVE, 1F, 1F);
				if(timer == 53) Lego.playSound(entity, GUN_MAG_INSERT, 1F, 1F);
			}
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_LIBERATOR = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		
		if(type == GunAnimation.CYCLE) {
			if(timer == 0) sendMuzzleFlash(entity);
		}
		if(type == GunAnimation.RELOAD) {
			if(timer == 0) Lego.playSound(entity, GUN_REVOLVER_COCK, 1F, 0.75F);
			if(timer == 4) {
				IMagazine<?> mag = ctx.config.getReceivers(stack)[0].getMagazine(stack);
				int toEject = mag.getAmountAfterReload(stack) - mag.getAmount(stack, ctx.inventory);
				SpentCasing casing = mag.getCasing(stack, ctx.inventory);
				if(casing != null) for(int i = 0; i < toEject; i++) CasingCreator.composeEffect(entity.level(), entity, 0.625, -0.1875, -0.375D, -0.12, 0.18, 0, 0.01, -15F + (float)entity.getRandom().nextGaussian() * 7.5F, (float)entity.getRandom().nextGaussian() * 5F, casing.getName(), true, 60, 0.5D, 20);
			}
			if(timer == 15) Lego.playSound(entity, GUN_MAG_SMALL_INSERT, 1F, 1F);
		}
		if(type == GunAnimation.RELOAD_CYCLE) {
			if(timer == 5) Lego.playSound(entity, GUN_MAG_SMALL_INSERT, 1F, 1F);
		}
		if(type == GunAnimation.RELOAD_END) {
			if(timer == 2) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 0.9F);
		}
		if(type == GunAnimation.JAMMED) {
			if(timer == 2) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 0.9F);
			if(timer == 12) Lego.playSound(entity, GUN_REVOLVER_COCK, 1F, 0.75F);
			if(timer == 26) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 0.9F);
		}
		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 0) Lego.playSound(entity, GUN_DRY_FIRE, 1F, 1F);
		}
		if(type == GunAnimation.INSPECT) {
			if(timer == 0) Lego.playSound(entity, GUN_REVOLVER_COCK, 1F, 0.75F);
			IMagazine<?> mag = ctx.config.getReceivers(stack)[0].getMagazine(stack);
			int toEject = mag.getAmountAfterReload(stack) - mag.getAmount(stack, ctx.inventory);
			if(timer == 4 && toEject > 0) {
				SpentCasing casing = mag.getCasing(stack, ctx.inventory);
				if(casing != null) for(int i = 0; i < toEject; i++) CasingCreator.composeEffect(entity.level(), entity, 0.625, -0.1875, -0.375D, -0.12, 0.18, 0, 0.01, -15F * (float)entity.getRandom().nextGaussian() * 7.5F, (float)entity.getRandom().nextGaussian() * 5F, casing.getName(), true, 60, 0.5D, 20);
				mag.setAmountAfterReload(stack, 0);
			}
			if(timer == 20) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 0.9F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_CONGOLAKE = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		boolean aiming = ItemGunBaseNT.getIsAiming(stack);

		if(type == GunAnimation.CYCLE) {
			if(timer == 0) sendMuzzleFlash(entity);
			if(timer == 15) {
				IMagazine<?> mag = ctx.config.getReceivers(stack)[0].getMagazine(stack);
				SpentCasing casing = mag.getCasing(stack, ctx.inventory);
				if(casing != null) CasingCreator.composeEffect(entity.level(), entity, 0.625, aiming ? -0.0625 : -0.25, aiming ? 0 : -0.375D, 0, 0.18, 0.12, 0.01, -5F + (float)entity.getRandom().nextGaussian() * 3.5F, -10F + entity.getRandom().nextFloat() * 5F, casing.getName(), true, 60, 0.5D, 20);
			}
		}
		if(type == GunAnimation.RELOAD || type == GunAnimation.RELOAD_CYCLE) {
			if(timer == 0) Lego.playSound(entity, GUN_GRENADE_RELOAD, 1F, 1F);
		}
		if(type == GunAnimation.INSPECT) {
			if(timer == 9) Lego.playSound(entity, GUN_GRENADE_OPEN, 1F, 1F);
			if(timer == 27) Lego.playSound(entity, GUN_GRENADE_CLOSE, 1F, 1F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_FLAMER = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);

		// Schleifenklang (Original AudioWrapper, clientseitig) - Ablauf 1:1 in OrchestrasClient
		if(entity.level().isClientSide) com.hbm_m.client.weapon.OrchestrasClient.flamer(entity, type, timer);
		if(entity.level().isClientSide) return;

		if(type == GunAnimation.RELOAD) {
			if(timer == 15) Lego.playSound(entity, GUN_LATCH_OPEN, 1F, 1F);
			if(timer == 35) Lego.playSound(entity, GUN_IMPACT, 0.5F, 1F);
			if(timer == 60) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 0.75F);
			if(timer == 70) Lego.playSound(entity, GUN_CANISTER_INSERT, 1F, 1F);
			if(timer == 85) Lego.playSound(entity, GUN_VALVE, 1F, 1F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_FLAMER_DAYBREAKER = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);

		if(type == GunAnimation.RELOAD) {
			if(timer == 15) Lego.playSound(entity, GUN_LATCH_OPEN, 1F, 1F);
			if(timer == 35) Lego.playSound(entity, GUN_IMPACT, 0.5F, 1F);
			if(timer == 60) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 0.75F);
			if(timer == 70) Lego.playSound(entity, GUN_CANISTER_INSERT, 1F, 1F);
			if(timer == 85) Lego.playSound(entity, GUN_VALVE, 1F, 1F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_LAG = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		boolean aiming = ItemGunBaseNT.getIsAiming(stack);

		if(type == GunAnimation.CYCLE) {
			if(timer == 0) sendMuzzleFlash(entity);
			if(timer == 1) {
				SpentCasing casing = ctx.config.getReceivers(stack)[0].getMagazine(stack).getCasing(stack, ctx.inventory);
				if(casing != null) CasingCreator.composeEffect(entity.level(), entity, 0.375, aiming ? 0 : -0.0625, aiming ? 0 : -0.25D, 0, 0.18, -0.12, 0.01, -10F + (float)entity.getRandom().nextGaussian() * 5F, 10F + entity.getRandom().nextFloat() * 10F, casing.getName());
			}
		}
		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 0) Lego.playSound(entity, GUN_DRY_FIRE, 1F, 1F);
			if(timer == 8) Lego.playSound(entity, GUN_REVOLVER_COCK, 1F, 1F);

		}
		if(type == GunAnimation.RELOAD) {
			if(timer == 8) Lego.playSound(entity, GUN_MAG_REMOVE, 1F, 1F);
			if(timer == 26) Lego.playSound(entity, GUN_MAG_INSERT, 1F, 1F);
			if(timer == 40) Lego.playSound(entity, GUN_PISTOL_COCK, 1F, 1F);
		}
		if(type == GunAnimation.JAMMED) {
			if(timer == 8) Lego.playSound(entity, GUN_MAG_REMOVE, 1F, 1F);
			if(timer == 20) Lego.playSound(entity, GUN_IMPACT, 0.5F, 1.6F);
			if(timer == 36) Lego.playSound(entity, GUN_MAG_INSERT, 1F, 1F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_UZI = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		boolean aiming = ItemGunBaseNT.getIsAiming(stack);

		if(type == GunAnimation.EQUIP) {
			if(timer == 8) Lego.playSound(entity, GUN_LATCH_OPEN, 1F, 1.25F);
		}
		if(type == GunAnimation.CYCLE) {
			if(timer == 0) sendMuzzleFlash(entity);
			if(timer == 1) {
				SpentCasing casing = ctx.config.getReceivers(stack)[0].getMagazine(stack).getCasing(stack, ctx.inventory);
				if(casing != null) CasingCreator.composeEffect(entity.level(), entity, 0.375, aiming ? 0 : -0.125, aiming ? 0 : -0.25D, 0, 0.18, -0.12, 0.01, -2.5F + (float)entity.getRandom().nextGaussian() * 5F, 10F + (float)entity.getRandom().nextFloat() * 15F, casing.getName());
			}
		}
		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 0) Lego.playSound(entity, GUN_DRY_FIRE, 1F, 1F);
			if(timer == 8) Lego.playSound(entity, GUN_PISTOL_COCK, 1F, 1F);

		}
		if(type == GunAnimation.RELOAD) {
			if(timer == 4) Lego.playSound(entity, GUN_MAG_REMOVE, 1F, 1F);
			if(timer == 26) Lego.playSound(entity, GUN_MAG_INSERT, 1F, 1F);
			if(timer == 36) Lego.playSound(entity, GUN_PISTOL_COCK, 1F, 1F);
		}
		if(type == GunAnimation.JAMMED) {
			if(timer == 17) Lego.playSound(entity, GUN_PISTOL_COCK, 1F, 1F);
			if(timer == 31) Lego.playSound(entity, GUN_PISTOL_COCK, 1F, 1F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_UZI_AKIMBO = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);

		if(type == GunAnimation.EQUIP) {
			if(timer == 8) Lego.playSound(entity, GUN_LATCH_OPEN, 1F, 1.25F);
		}
		if(type == GunAnimation.CYCLE) {
			if(timer == 0) sendMuzzleFlash(entity);
			if(timer == 1) {
				int mult = ctx.configIndex == 0 ? -1 : 1;
				SpentCasing casing = ctx.config.getReceivers(stack)[0].getMagazine(stack).getCasing(stack, ctx.inventory);
				if(casing != null) CasingCreator.composeEffect(entity.level(), entity, 0.375, -0.125, -0.375D * mult, 0, 0.18, -0.12 * mult, 0.01, -2.5F + (float)entity.getRandom().nextGaussian() * 5F, (10F + (float)entity.getRandom().nextFloat() * 15F) * mult, casing.getName());
			}
		}
		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 0) Lego.playSound(entity, GUN_DRY_FIRE, 1F, 1F);
			if(timer == 8) Lego.playSound(entity, GUN_PISTOL_COCK, 1F, 1F);

		}
		if(type == GunAnimation.RELOAD) {
			if(timer == 4) Lego.playSound(entity, GUN_MAG_REMOVE, 1F, 1F);
			if(timer == 26) Lego.playSound(entity, GUN_MAG_INSERT, 1F, 1F);
			if(timer == 36) Lego.playSound(entity, GUN_PISTOL_COCK, 1F, 1F);
		}
		if(type == GunAnimation.JAMMED) {
			if(timer == 17) Lego.playSound(entity, GUN_PISTOL_COCK, 1F, 1F);
			if(timer == 31) Lego.playSound(entity, GUN_PISTOL_COCK, 1F, 1F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_SPAS = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		boolean aiming = ItemGunBaseNT.getIsAiming(stack);

		if(type == GunAnimation.CYCLE || type == GunAnimation.ALT_CYCLE) {
			if(timer == 0) sendMuzzleFlash(entity);
			if(timer == 8) Lego.playSound(entity, GUN_SHOTGUN_COCK, 1F, 1F);
			if(timer == 10) {
				SpentCasing casing = ctx.config.getReceivers(stack)[0].getMagazine(stack).getCasing(stack, ctx.inventory); //turns out there's a reason why stovepipes look like that
				if(casing != null) CasingCreator.composeEffect(entity.level(), entity, 0.375, aiming ? 0 : -0.125, aiming ? 0 : -0.25D, 0, 0.18, -0.12, 0.01, -3F + (float)entity.getRandom().nextGaussian() * 2.5F, -15F + entity.getRandom().nextFloat() * -5F, casing.getName());
			}
		}
		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 0) Lego.playSound(entity, GUN_DRY_FIRE, 1F, 1F);
			if(timer == 8) Lego.playSound(entity, GUN_SHOTGUN_COCK, 1F, 1F);
		}
		if(type == GunAnimation.RELOAD) {
			IMagazine<?> mag = ctx.config.getReceivers(stack)[0].getMagazine(stack);
			if(mag.getAmount(stack, ctx.inventory) == 0) {
				if(timer == 0) Lego.playSound(entity, GUN_REVOLVER_COCK, 1F, 1F);
				if(timer == 7) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 1F);
			}
			if(timer == 5) Lego.playSound(entity, GUN_SHOTGUN_LOAD, 1F, 1F);
		}
		if(type == GunAnimation.RELOAD_CYCLE) {
			if(timer == 5) Lego.playSound(entity, GUN_SHOTGUN_LOAD, 1F, 1F);
		}
		if(type == GunAnimation.INSPECT) {
			if(timer == 5) Lego.playSound(entity, GUN_SHOTGUN_OPEN, 1F, 1F);
			if(timer == 18) Lego.playSound(entity, GUN_SHOTGUN_CLOSE, 1F, 1F);
		}
		if(type == GunAnimation.JAMMED) {
			if(timer == 18) Lego.playSound(entity, GUN_WHACK, 1F, 1F);
			if(timer == 25) Lego.playSound(entity, GUN_WHACK, 1F, 1F);
			if(timer == 29) Lego.playSound(entity, GUN_SHOTGUN_CLOSE, 1F, 1F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_PANERSCHRECK = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		if(type == GunAnimation.CYCLE) {
			if(timer == 0) sendMuzzleFlash(entity);
		}
		if(type == GunAnimation.RELOAD) {
			if(timer == 30) Lego.playSound(entity, GUN_CANISTER_INSERT, 1F, 1F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_STAR_F = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		boolean aiming = ItemGunBaseNT.getIsAiming(stack);

		if(type == GunAnimation.CYCLE) {
			if(timer == 0) {
				SpentCasing casing = ctx.config.getReceivers(stack)[0].getMagazine(stack).getCasing(stack, ctx.inventory);
				if(casing != null) CasingCreator.composeEffect(entity.level(), entity, 0.3125, aiming ? 0 : -0.125, aiming ? 0 : -0.1875D, 0, 0.18, -0.12, 0.01, (float)entity.getRandom().nextGaussian() * 5F, 12.5F + (float)entity.getRandom().nextFloat() * 5F, casing.getName());
				sendMuzzleFlash(entity);
			}
		}
		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 0) Lego.playSound(entity, GUN_DRY_FIRE, 1F, 0.9F);
			if(timer == 5) Lego.playSound(entity, GUN_PISTOL_COCK, 1F, 1.1F);

		}
		if(type == GunAnimation.RELOAD) {
			if(timer == 5) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 1F);
			if(timer == 5) Lego.playSound(entity, GUN_MAG_REMOVE, 1F, 1F);
			if(timer == 22) Lego.playSound(entity, GUN_MAG_INSERT, 1F, 1F);
			if(timer == 30) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 1.1F);
		}
		if(type == GunAnimation.JAMMED) {
			if(timer == 15) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 1F);
			if(timer == 19) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 1.1F);
			if(timer == 23) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 1F);
			if(timer == 27) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 1.1F);
		}
		if(type == GunAnimation.INSPECT) {
			if(timer == 7) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 1F);
			if(timer == 30) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 1.1F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_STAR_F_AKIMBO = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		boolean aiming = ItemGunBaseNT.getIsAiming(stack);

		if(type == GunAnimation.CYCLE) {
			if(timer == 0) {
				int side = ctx.configIndex == 0 ? -1 : 1;
				SpentCasing casing = ctx.config.getReceivers(stack)[0].getMagazine(stack).getCasing(stack, ctx.inventory);
				if(casing != null) CasingCreator.composeEffect(entity.level(), entity, 0.3125, aiming ? 0 : -0.125, aiming ? 0 : -0.1875D * side, 0, 0.18, -0.12 * side, 0.01, (float)entity.getRandom().nextGaussian() * 5F, 12.5F + (float)entity.getRandom().nextFloat() * 5F, casing.getName());
				sendMuzzleFlash(entity);
			}
		}
		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 0) Lego.playSound(entity, GUN_DRY_FIRE, 1F, 0.9F);
			if(timer == 5) Lego.playSound(entity, GUN_PISTOL_COCK, 1F, 1.1F);

		}
		if(type == GunAnimation.RELOAD) {
			if(timer == 5) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 1F);
			if(timer == 5) Lego.playSound(entity, GUN_MAG_REMOVE, 1F, 1F);
			if(timer == 22) Lego.playSound(entity, GUN_MAG_INSERT, 1F, 1F);
			if(timer == 30) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 1.1F);
		}
		if(type == GunAnimation.JAMMED) {
			if(timer == 15) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 1F);
			if(timer == 19) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 1.1F);
			if(timer == 23) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 1F);
			if(timer == 27) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 1.1F);
		}
		if(type == GunAnimation.INSPECT) {
			if(timer == 7) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 1F);
			if(timer == 30) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 1.1F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_G3 = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		boolean scoped = stack.getItem() == WeaponItems.gun("gun_g3_zebra") || XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_SCOPE);
		boolean aiming = ItemGunBaseNT.getIsAiming(stack) && !scoped;

		if(type == GunAnimation.CYCLE) {
			if(timer == 0) {
				SpentCasing casing = ctx.config.getReceivers(stack)[0].getMagazine(stack).getCasing(stack, ctx.inventory);
				if(casing != null) CasingCreator.composeEffect(entity.level(), entity, 0.5, aiming ? 0 : -0.125, aiming ? 0 : -0.25D, 0, 0.18, -0.12, 0.01, (float)entity.getRandom().nextGaussian() * 5F, 12.5F + (float)entity.getRandom().nextFloat() * 5F, casing.getName());
				sendMuzzleFlash(entity);
			}
		}
		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 0) Lego.playSound(entity, GUN_DRY_FIRE, 1F, 0.8F);
			if(timer == 5) Lego.playSound(entity, GUN_PISTOL_COCK, 1F, 0.9F);

		}
		if(type == GunAnimation.RELOAD) {
			if(timer == 2) Lego.playSound(entity, GUN_MAG_REMOVE, 1F, 1F);
			if(timer == 4) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 0.9F);
			if(timer == 32) Lego.playSound(entity, GUN_MAG_INSERT, 1F, 1F);
			if(timer == 36) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 1F);
		}
		if(type == GunAnimation.INSPECT) {
			if(timer == 2) Lego.playSound(entity, GUN_MAG_REMOVE, 1F, 1F);
			if(timer == 28) Lego.playSound(entity, GUN_MAG_INSERT, 1F, 1F);
		}
		if(type == GunAnimation.JAMMED) {
			if(timer == 16) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 0.9F);
			if(timer == 20) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 1F);
			if(timer == 26) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 0.9F);
			if(timer == 30) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 1F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_STINGER = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);

		if(entity.level().isClientSide) {
			// Aufschaltton (Original AudioWrapper) - Ablauf 1:1 in OrchestrasClient
			com.hbm_m.client.weapon.OrchestrasClient.stinger(stack, entity);
			return;
		}
		if(type == GunAnimation.CYCLE) {
			if(timer == 0) sendMuzzleFlash(entity);
		}
		if(type == GunAnimation.RELOAD) {
			if(timer == 30) Lego.playSound(entity, GUN_CANISTER_INSERT, 1F, 1F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_MK108 = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		boolean aiming = ItemGunBaseNT.getIsAiming(stack);

		if(type == GunAnimation.CYCLE) {
			if(timer == 0) {
				sendMuzzleFlash(entity);
			}

			if(timer == 2) {
				SpentCasing casing = ctx.config.getReceivers(stack)[0].getMagazine(stack).getCasing(stack, ctx.inventory);
				if(casing != null) CasingCreator.composeEffect(entity.level(), entity, 0.5, aiming ? -0.125 : -0.3125, aiming ? -0.375 : -0.3125D, 0, 0.18, -0.12, 0.01, -10F + (float)entity.getRandom().nextGaussian() * 2.5F, (float)entity.getRandom().nextGaussian() * -20F + 15F, casing.getName(), true, 60, 0.5D, 10);
			}
		}
		
		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 0) Lego.playSound(entity, GUN_DRY_FIRE, 1F, 0.75F);
		}
		
		if(type == GunAnimation.RELOAD) {
			if(timer == 0) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 0.65F);
			if(timer == 10) Lego.playSound(entity, GUN_MAG_SMALL_REMOVE, 1F, 0.75F);
			if(timer == 40) Lego.playSound(entity, GUN_MAG_REMOVE, 1F, 0.75F);
			if(timer == 60) Lego.playSound(entity, GUN_IMPACT, 0.5F, 1F);
			if(timer == 90) Lego.playSound(entity, GUN_MAG_INSERT, 1F, 0.75F);
			if(timer == 100) Lego.playSound(entity, GUN_MAG_SMALL_INSERT, 1F, 0.75F);
			if(timer == 125) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 0.65F);

			if(timer == 60) ctx.config.getReceivers(stack)[0].getMagazine(stack).reloadAction(stack, ctx.inventory);
		}
		
		if(type == GunAnimation.INSPECT) {
			int yeetHorizontal = 750;
			int untilImpact = yeetHorizontal * 9 / 15;
			int delay = 250;

			for(int i = 0; i < 3; i++) {
				if(timer == (untilImpact + delay * i) / 50) Lego.playSound(entity, GUN_IMPACT, 0.5F, 1.5F);
			}
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_CHEMTHROWER = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);

		// Schleifenklang (Original AudioWrapper, clientseitig) - Ablauf 1:1 in OrchestrasClient
		if(entity.level().isClientSide) com.hbm_m.client.weapon.OrchestrasClient.chemthrower(entity, type, timer);
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_AMAT = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		boolean aiming = ItemGunBaseNT.getIsAiming(stack);

		if(type == GunAnimation.EQUIP) {
			if(timer == 10) Lego.playSound(entity, GUN_REVOLVER_COCK, 0.5F, 1.25F);
			if(timer == 15) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 0.5F, 1.25F);
		}

		if(type == GunAnimation.CYCLE) {
			if(timer == 0) sendMuzzleFlash(entity);
			if(timer == 7) Lego.playSound(entity, GUN_BOLT_OPEN, 0.5F, 1F);
			if(timer == 16) Lego.playSound(entity, GUN_BOLT_CLOSE, 0.5F, 1F);
			if(timer == 12) {
				SpentCasing casing = ctx.config.getReceivers(stack)[0].getMagazine(stack).getCasing(stack, ctx.inventory);
				if(casing != null) CasingCreator.composeEffect(entity.level(), entity,
						0.375, aiming ? 0 : -0.125, -0.25D,
						-0.05, 0.2, -0.025,
						0.01, -10F + (float) entity.getRandom().nextGaussian() * 10F, (float) entity.getRandom().nextGaussian() * 12.5F, casing.getName(), true, 60, 0.5D, 10);
			}
		}

		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 0) Lego.playSound(entity, GUN_DRY_FIRE, 1F, 0.75F);
			if(timer == 7) Lego.playSound(entity, GUN_BOLT_OPEN, 0.5F, 1F);
			if(timer == 16) Lego.playSound(entity, GUN_BOLT_CLOSE, 0.5F, 1F);
		}

		if(type == GunAnimation.RELOAD) {
			if(timer == 2) Lego.playSound(entity, GUN_MAG_REMOVE, 1F, 1F);
			if(timer == 20) Lego.playSound(entity, GUN_MAG_INSERT, 1F, 1F);
			if(timer == 32) Lego.playSound(entity, GUN_BOLT_OPEN, 0.5F, 1F);
			if(timer == 41) Lego.playSound(entity, GUN_BOLT_CLOSE, 0.5F, 1F);
		}

		if(type == GunAnimation.JAMMED) {
			if(timer == 5) Lego.playSound(entity, GUN_BOLT_OPEN, 0.5F, 1F);
			if(timer == 12) Lego.playSound(entity, GUN_BOLT_CLOSE, 0.5F, 1F);
			if(timer == 16) Lego.playSound(entity, GUN_BOLT_OPEN, 0.5F, 1F);
			if(timer == 23) Lego.playSound(entity, GUN_BOLT_CLOSE, 0.5F, 1F);
		}

		if(type == GunAnimation.INSPECT) {
			if(timer == 0) Lego.playSound(entity, GUN_REVOLVER_COCK, 0.5F, 1F);
			if(timer == 45) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 0.5F, 1F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_M2 = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		boolean aiming = ItemGunBaseNT.getIsAiming(stack);

		if(type == GunAnimation.EQUIP) {
			if(timer == 0) Lego.playSound(entity, TURRET_CIWS_RELOAD, 1F, 1F);
		}

		if(type == GunAnimation.CYCLE) {
			if(timer == 0) {
				sendMuzzleFlash(entity);
				SpentCasing casing = ctx.config.getReceivers(stack)[0].getMagazine(stack).getCasing(stack, ctx.inventory);
				if(casing != null) CasingCreator.composeEffect(entity.level(), entity, 0.375, aiming ? 0 : -0.125, aiming ? 0 : -0.3125D, 0, 0.06, -0.18, 0.01, (float)entity.getRandom().nextGaussian() * 20F, 12.5F + (float)entity.getRandom().nextGaussian() * 7.5F, casing.getName());
			}
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_SHREDDER = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);

		if(type == GunAnimation.CYCLE) {
			if(timer == 0) sendMuzzleFlash(entity);
			if(timer == 2) Lego.playSound(entity, GUN_SHREDDER_CYCLE, 0.25F, 1.5F);
		}
		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 0) Lego.playSound(entity, GUN_DRY_FIRE, 1F, 1F);
			if(timer == 2) Lego.playSound(entity, GUN_SHREDDER_CYCLE, 0.25F, 1.5F);
		}
		if(type == GunAnimation.RELOAD) {
			if(timer == 2) Lego.playSound(entity, GUN_MAG_REMOVE, 1F, 1F);
			if(timer == 32) Lego.playSound(entity, GUN_MAG_INSERT, 1F, 1F);
		}
		if(type == GunAnimation.INSPECT) {
			if(timer == 2) Lego.playSound(entity, GUN_MAG_REMOVE, 1F, 1F);
			if(timer == 28) Lego.playSound(entity, GUN_MAG_INSERT, 1F, 1F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_SHREDDER_SEXY = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		boolean aiming = ItemGunBaseNT.getIsAiming(stack);

		if(type == GunAnimation.CYCLE) {
			if(timer == 0) {
				sendMuzzleFlash(entity);
				if(ctx.config.getReceivers(stack)[0].getMagazine(stack).getType(stack, null) == XFactory12ga.g12_equestrian_bj) {
					ItemGunBaseNT.setTimer(stack, 0, 20);
				}
			}

			if(timer == 2) {
				SpentCasing casing = ctx.config.getReceivers(stack)[0].getMagazine(stack).getCasing(stack, ctx.inventory);
				if(casing != null) CasingCreator.composeEffect(entity.level(), entity, 0.375, aiming ? -0.0625 : -0.125, aiming ? -0.125 : -0.25D, 0, 0.18, -0.12, 0.01, -10F + (float)entity.getRandom().nextGaussian() * 2.5F, (float)entity.getRandom().nextGaussian() * -20F + 15F, casing.getName(), false, 60, 0.5D, 20);
			}
		}

		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 0) Lego.playSound(entity, GUN_DRY_FIRE, 1F, 1F);
		}
		if(type == GunAnimation.RELOAD) {
			if(timer == 0) Lego.playSound(entity, GUN_REVOLVER_COCK, 1F, 1F);
			if(timer == 4) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 0.75F);
			if(timer == 16) Lego.playSound(entity, GUN_MAG_SMALL_REMOVE, 1F, 1F);
			if(timer == 30) Lego.playSound(entity, GUN_MAG_REMOVE, 1F, 1F);
			if(timer == 55) Lego.playSound(entity, GUN_IMPACT, 0.5F, 1F);
			if(timer == 65) Lego.playSound(entity, GUN_MAG_INSERT, 1F, 1F);
			if(timer == 74) Lego.playSound(entity, GUN_MAG_SMALL_INSERT, 1F, 1F);
			if(timer == 88) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 0.75F);
			if(timer == 100) Lego.playSound(entity, GUN_REVOLVER_COCK, 1F, 1F);

			if(timer == 55) ctx.config.getReceivers(stack)[0].getMagazine(stack).reloadAction(stack, ctx.inventory);
		}

		if(type == GunAnimation.INSPECT) {
			if(timer == 20) Lego.playSound(entity, PLAYER_GULP, 1F, 1F);
			if(timer == 25) Lego.playSound(entity, PLAYER_GULP, 1F, 1F);
			if(timer == 30) Lego.playSound(entity, PLAYER_GULP, 1F, 1F);
			if(timer == 35) Lego.playSound(entity, PLAYER_GULP, 1F, 1F);
			if(timer == 50) Lego.playSound(entity, PLAYER_GROAN, 1F, 1F);
			if(timer == 60) {
				entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 30 * 20, 2));
				entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30 * 20, 2));
				entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 10 * 20, 0));
			}
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_QUADRO = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		
		if(type == GunAnimation.CYCLE) {
			if(timer == 0) sendMuzzleFlash(entity);
		}
		if(type == GunAnimation.RELOAD) {
			if(timer == 30) Lego.playSound(entity, GUN_CANISTER_INSERT, 1F, 1F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_MINIGUN = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		boolean aiming = ItemGunBaseNT.getIsAiming(stack);

		if(type == GunAnimation.CYCLE) {
			if(timer == 0) {
				sendMuzzleFlash(entity);
				int rounds = XWeaponModManager.hasUpgrade(stack, ctx.configIndex, XWeaponModManager.ID_MINIGUN_SPEED) ? 3 : 1;
				for(int i = 0; i < rounds; i++) {
					SpentCasing casing = ctx.config.getReceivers(stack)[0].getMagazine(stack).getCasing(stack, ctx.inventory);
					if(casing != null) CasingCreator.composeEffect(entity.level(), entity, aiming ? 0.125 : 0.5, aiming ? -0.125 : -0.25, aiming ? -0.25 : -0.5D, 0, 0.18, -0.12, 0.01, (float)entity.getRandom().nextGaussian() * 15F, (float)entity.getRandom().nextGaussian() * 15F, casing.getName());
				}
			}
			if(timer == (XWeaponModManager.hasUpgrade(stack, 0, 207) ? 3 : 1)) Lego.playSound(entity, GUN_REVOLVER_SPIN, 1F, 0.75F);
		}
		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 0) Lego.playSound(entity, GUN_DRY_FIRE, 1F, 0.75F);
			if(timer == 1) Lego.playSound(entity, GUN_REVOLVER_SPIN, 1F, 0.75F);
		}
		if(type == GunAnimation.RELOAD) {
			if(timer == 0) Lego.playSound(entity, GUN_REVOLVER_SPIN, 1F, 0.75F);
		}
		if(type == GunAnimation.INSPECT) {
			if(timer == 0) Lego.playSound(entity, GUN_REVOLVER_SPIN, 1F, 0.75F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_MINIGUN_DUAL = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);

		if(type == GunAnimation.CYCLE) {
			if(timer == 0) {
				sendMuzzleFlash(entity);
				int index = ctx.configIndex == 0 ? -1 : 1;
				int rounds = XWeaponModManager.hasUpgrade(stack, ctx.configIndex, XWeaponModManager.ID_MINIGUN_SPEED) ? 3 : 1;
				for(int i = 0; i < rounds; i++) {
					SpentCasing casing = ctx.config.getReceivers(stack)[0].getMagazine(stack).getCasing(stack, ctx.inventory);
					if(casing != null) CasingCreator.composeEffect(entity.level(), entity, 0.25, -0.25, -0.5D * index, 0, 0.18, -0.12 * index, 0.01, (float)entity.getRandom().nextGaussian() * 15F, (float)entity.getRandom().nextGaussian() * 15F, casing.getName());
				}
			}
			if(timer == (XWeaponModManager.hasUpgrade(stack, 0, 207) ? 3 : 1)) Lego.playSound(entity, GUN_REVOLVER_SPIN, 1F, 0.75F);
		}
		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 0) Lego.playSound(entity, GUN_DRY_FIRE, 1F, 0.75F);
			if(timer == 1) Lego.playSound(entity, GUN_REVOLVER_SPIN, 1F, 0.75F);
		}
		if(type == GunAnimation.RELOAD) {
			if(timer == 0) Lego.playSound(entity, GUN_REVOLVER_SPIN, 1F, 0.75F);
		}
		if(type == GunAnimation.INSPECT) {
			if(timer == 0) Lego.playSound(entity, GUN_REVOLVER_SPIN, 1F, 0.75F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_MISSILE_LAUNCHER = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		
		if(type == GunAnimation.CYCLE) {
			if(timer == 0) sendMuzzleFlash(entity);
		}
		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 0) Lego.playSound(entity, GUN_DRY_FIRE, 1F, 1.25F);
		}
		if(type == GunAnimation.RELOAD) {
			if(timer == 0) Lego.playSound(entity, GUN_BOLT_OPEN, 1F, 0.9F);
			if(timer == 30) Lego.playSound(entity, GUN_CANISTER_INSERT, 1F, 1F);
			if(timer == 42) Lego.playSound(entity, GUN_BOLT_CLOSE, 1F, 0.9F);
		}

		if(type == GunAnimation.JAMMED || type == GunAnimation.INSPECT) {
			if(timer == 0) Lego.playSound(entity, GUN_BOLT_OPEN, 1F, 0.9F);
			if(timer == 27) Lego.playSound(entity, GUN_BOLT_CLOSE, 1F, 0.9F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_TESLA = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);

		if(type == GunAnimation.CYCLE) {
			if(timer == 2) Lego.playSound(entity, GUN_SHREDDER_CYCLE, 0.25F, 1.25F);
		}
		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 0) Lego.playSound(entity, GUN_DRY_FIRE, 1F, 1F);
			if(timer == 2) Lego.playSound(entity, GUN_SHREDDER_CYCLE, 0.25F, 1.25F);
		}
		if(type == GunAnimation.INSPECT) {
			if(timer == 12) Lego.playSound(entity, BLOCK_PLUSHY, 0.25F, 1F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_LASER_PISTOL = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		
		if(type == GunAnimation.CYCLE) {
			if(timer == 0) sendMuzzleFlash(entity);
		}
		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 0) Lego.playSound(entity, GUN_DRY_FIRE, 1F, 1.5F);
		}

		if(type == GunAnimation.RELOAD) {
			if(timer == 0) Lego.playSound(entity, GUN_REVOLVER_COCK, 1F, 1F);
			if(timer == 10) Lego.playSound(entity, GUN_MAG_SMALL_REMOVE, 1F, 1.25F);
			if(timer == 34) Lego.playSound(entity, GUN_MAG_SMALL_INSERT, 1F, 1.25F);
			if(timer == 40) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 1.25F);
		}

		if(type == GunAnimation.JAMMED) {
			if(timer == 10) Lego.playSound(entity, GUN_REVOLVER_COCK, 1F, 1F);
			if(timer == 15) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 1.25F);
			if(timer == 30) Lego.playSound(entity, GUN_IMPACT, 0.25F, 1.5F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_STG77 = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		boolean aiming = ItemGunBaseNT.getIsAiming(stack);

		if(ModClothConfig.get().gunAnimsLegacy) {
			if(type == GunAnimation.CYCLE) {
				if(timer == 0) {
					sendMuzzleFlash(entity);
					SpentCasing casing = ctx.config.getReceivers(stack)[0].getMagazine(stack).getCasing(stack, ctx.inventory);
					if(casing != null) CasingCreator.composeEffect(entity.level(), entity, aiming ? 0.125 : 0.125, aiming ? -0.125 : -0.25, aiming ? -0.125 : -0.25D, 0, 0.18, -0.12, 0.01, (float)entity.getRandom().nextGaussian() * 5F, 7.5F + entity.getRandom().nextFloat() * 5F, casing.getName());
				}
				if(timer == 40) Lego.playSound(entity, GUN_DRY_FIRE, 0.25F, 1.25F);
			}
			if(type == GunAnimation.CYCLE_DRY) {
				if(timer == 0) Lego.playSound(entity, GUN_DRY_FIRE, 1F, 0.8F);
				if(timer == 5) Lego.playSound(entity, GUN_PISTOL_COCK, 1F, 0.9F);
				if(timer == 40) Lego.playSound(entity, GUN_DRY_FIRE, 0.25F, 1.25F);
			}
			if(type == GunAnimation.RELOAD) {
				if(timer == 0) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 0.9F);
				if(timer == 10) Lego.playSound(entity, GUN_MAG_REMOVE, 1F, 1F);
				if(timer == 24) Lego.playSound(entity, GUN_MAG_INSERT, 1F, 1F);
				if(timer == 34) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 1F);
			}
			if(type == GunAnimation.INSPECT) {
				if(timer == 0) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 0.9F);
				if(timer == 10) Lego.playSound(entity, GUN_MAG_SMALL_REMOVE, 1F, 1F);

				if(timer == 114) Lego.playSound(entity, GUN_MAG_SMALL_INSERT, 1F, 1F);
				if(timer == 124) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 1F);
			}
		} else {
			if(type == GunAnimation.CYCLE) {
				if(timer == 0) {
					sendMuzzleFlash(entity);
					SpentCasing casing = ctx.config.getReceivers(stack)[0].getMagazine(stack).getCasing(stack, ctx.inventory);
					if(casing != null) CasingCreator.composeEffect(entity.level(), entity, aiming ? 0.125 : 0.25, aiming ? -0.125 : -0.25, aiming ? -0.125 : -0.25D, 0, 0.18, -0.12, 0.01, (float)entity.getRandom().nextGaussian() * 5F, 7.5F + entity.getRandom().nextFloat() * 5F, casing.getName());
				}
				if(timer == 40) Lego.playSound(entity, GUN_DRY_FIRE, 0.25F, 1.25F);
			}
			if(type == GunAnimation.CYCLE_DRY) {
				if(timer == 0) Lego.playSound(entity, GUN_DRY_FIRE, 1F, 0.8F);
				if(timer == 5) Lego.playSound(entity, GUN_PISTOL_COCK, 1F, 0.9F);
				if(timer == 40) Lego.playSound(entity, GUN_DRY_FIRE, 0.25F, 1.25F);
			}
			if(type == GunAnimation.RELOAD) {
				if(timer == 0) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 0.9F);
				if(timer == 16) Lego.playSound(entity, GUN_MAG_REMOVE, 1F, 1F);
				if(timer == 32) Lego.playSound(entity, GUN_IMPACT, 0.25F, 1.25F);
				if(timer == 38) Lego.playSound(entity, GUN_MAG_INSERT, 1F, 1F);
				if(timer == 43) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 1F);
			}
			if(type == GunAnimation.INSPECT) {
				if(timer == 0) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 0.9F);
				if(timer == 11) Lego.playSound(entity, GUN_MAG_SMALL_REMOVE, 1F, 1F);

				if(timer == 72) Lego.playSound(entity, GUN_MAG_SMALL_INSERT, 1F, 1F);
				if(timer == 84) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 1F);
			}
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_TAU = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);

		// Aufladeklang (Original AudioWrapper, clientseitig) - Ablauf 1:1 in OrchestrasClient
		if(entity.level().isClientSide) com.hbm_m.client.weapon.OrchestrasClient.tau(entity, type, timer);
		if(entity.level().isClientSide) return;

		if(type == GunAnimation.CYCLE) {
			if(timer == 0) Lego.playSound(entity, GUN_TAU_FIRE, 0.5F, 0.9F + entity.getRandom().nextFloat() * 0.2F);
		}

		if(type == GunAnimation.ALT_CYCLE) {
			if(timer == 0) Lego.playSound(entity, GUN_TAU_FIRE, 0.5F, 0.7F + entity.getRandom().nextFloat() * 0.2F);
		}

		if(type == GunAnimation.SPINUP) {
			if(timer % 10 == 0 && timer < 130) {
				IMagazine<?> mag = ctx.config.getReceivers(stack)[0].getMagazine(stack);
				if(mag.getAmount(stack, ctx.inventory) <= 0) {
					ItemGunBaseNT.playAnimation(ctx.getPlayer(), stack, GunAnimation.CYCLE_DRY, ctx.configIndex);
					return;
				}
				mag.useUpAmmo(stack, ctx.inventory, 1);
			}

			if(timer > 200) {
				ItemGunBaseNT.playAnimation(ctx.getPlayer(), stack, GunAnimation.CYCLE_DRY, ctx.configIndex);

				entity.hurt(ModDamageSources.create(entity.level(), ModDamageTypes.TAU_BLAST), 1_000F);

				ItemGunBaseNT.setWear(stack, ctx.configIndex, Math.min(ItemGunBaseNT.getWear(stack, ctx.configIndex) + 10_000F, ctx.config.getDurability(stack)));

				playSoundEffect(entity.level(), entity.getX(), entity.getY() + entity.getEyeHeight(), entity.getZ(), GUN_TESLA_BLAST, 5.0F, 0.9F);
				playSoundEffect(entity.level(), entity.getX(), entity.getY() + entity.getEyeHeight(), entity.getZ(), VANILLA_FIREWORKS_BANG, 5.0F, 0.5F);

				float yaw = entity.level().random.nextFloat() * 180F;
				for(int i = 0; i < 3; i++) {
					CompoundTag data = new CompoundTag();
					data.putString("type", "plasmablast");
					data.putFloat("r", 1.0F);
					data.putFloat("g", 0.8F);
					data.putFloat("b", 0.5F);
					data.putFloat("pitch", -60F + 60F * i);
					data.putFloat("yaw", yaw);
					data.putFloat("scale", 2F);
					if(entity.level() instanceof ServerLevel server) IParticleCreator.sendPacket(server, entity.getX(), entity.getY() + entity.getEyeHeight(), entity.getZ(), 100, data);
				}
			}
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_FATMAN = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);

		if(type == GunAnimation.RELOAD) {
			if(timer == 0) Lego.playSound(entity, GUN_FATMAN_RELOAD, 1F, 1F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_LASRIFLE = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		
		if(type == GunAnimation.CYCLE) {
			if(timer == 0) sendMuzzleFlash(entity);
		}
		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 0) Lego.playSound(entity, GUN_DRY_FIRE, 1F, 1.5F);
		}

		if(type == GunAnimation.RELOAD) {
			if(timer == 2) Lego.playSound(entity, GUN_MAG_SMALL_REMOVE, 1F, 1F);
			if(timer == 18) Lego.playSound(entity, GUN_IMPACT, 0.25F, 1F);
			if(timer == 30) Lego.playSound(entity, GUN_MAG_INSERT, 1F, 1F);
			if(timer == 38) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 1F);
		}

		if(type == GunAnimation.INSPECT) {
			if(timer == 2) Lego.playSound(entity, GUN_MAG_SMALL_REMOVE, 1F, 1F);
			if(timer == 12) Lego.playSound(entity, GUN_MAG_INSERT, 1F, 1F);
			if(timer == 20) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 1F);
		}

		if(type == GunAnimation.JAMMED) {
			if(timer == 2) Lego.playSound(entity, GUN_MAG_SMALL_REMOVE, 1F, 1F);
			if(timer == 22) Lego.playSound(entity, GUN_MAG_INSERT, 1F, 1F);
			if(timer == 30) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 1F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_COILGUN = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		
		if(type == GunAnimation.CYCLE && stack.getItem() == WeaponItems.gun("gun_n_i_4_n_i")) {
			if(timer == 0) sendMuzzleFlash(entity);
		}
		if(type == GunAnimation.RELOAD) {
			if(timer == 0) Lego.playSound(entity, GUN_COIL_RELOAD, 1F, 1F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_HANGMAN = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		
		if(type == GunAnimation.CYCLE) {
			if(timer == 0) sendMuzzleFlash(entity);
		}
		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 0) Lego.playSound(entity, GUN_DRY_FIRE, 1F, 1F);
		}

		if(type == GunAnimation.RELOAD) {

			if(timer == 0) Lego.playSound(entity, GUN_REVOLVER_COCK, 1F, 0.8F);
			if(timer == 5) Lego.playSound(entity, GUN_MAG_SMALL_REMOVE, 1F, 0.8F);
			if(timer == 25) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 1F);
			if(timer == 35) Lego.playSound(entity, GUN_REVOLVER_COCK, 1F, 0.75F);

			if(timer == 10) {
				Receiver rec = ctx.config.getReceivers(stack)[0];
				IMagazine<?> mag = rec.getMagazine(stack);
				SpentCasing casing = mag.getCasing(stack, ctx.inventory);
				if(casing != null) for(int i = 0; i < mag.getCapacity(stack); i++) CasingCreator.composeEffect(entity.level(), entity, 0.25, -0.25, -0.125, -0.05, 0, 0, 0.01, -6.5F + (float)entity.getRandom().nextGaussian() * 3F, (float)entity.getRandom().nextGaussian() * 5F, casing.getName());
			}
		}

		if(type == GunAnimation.INSPECT) {
			if(timer == 16 && ctx.getPlayer() != null) {
				MovingObjectPosition mop = getMouseOver(ctx.getPlayer(), 3.0D);
				if(mop != null) {
					if(mop.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
						float damage = 10F;
						mop.entityHit.hurt(ctx.getPlayer().damageSources().playerAttack(ctx.getPlayer()), damage);
						Vec3 m = mop.entityHit.getDeltaMovement();
						mop.entityHit.setDeltaMovement(m.x * 2, m.y, m.z * 2);
						Lego.playSound(mop.entityHit, GUN_SMACK, 1F, 0.9F + entity.getRandom().nextFloat() * 0.2F);
					}
					if(mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
						BlockState b = entity.level().getBlockState(mop.getBlockPos());
						entity.level().playSound(null, mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord, b.getSoundType().getStepSound(), SoundSource.BLOCKS, 2F, 0.9F + entity.getRandom().nextFloat() * 0.2F);
					}
				}
			}
		}

		if(type == GunAnimation.JAMMED) {
			if(timer == 10) Lego.playSound(entity, GUN_REVOLVER_COCK, 1F, 0.8F);
			if(timer == 15) Lego.playSound(entity, GUN_MAG_SMALL_REMOVE, 1F, 0.8F);
			if(timer == 20) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 1F);
			if(timer == 25) Lego.playSound(entity, GUN_REVOLVER_COCK, 1F, 0.75F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_BOLTER = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		boolean aiming = ItemGunBaseNT.getIsAiming(stack);

		if(type == GunAnimation.CYCLE) {
			if(timer == 1) {
				SpentCasing casing = ctx.config.getReceivers(stack)[0].getMagazine(stack).getCasing(stack, ctx.inventory);
				if(casing != null) CasingCreator.composeEffect(entity.level(), entity, 0.5, aiming ? 0 : -0.125, aiming ? -0.0625 : -0.25D, 0, 0.18, -0.12, 0.01, -10F + (float)entity.getRandom().nextGaussian() * 5F, 10F + entity.getRandom().nextFloat() * 10F, casing.getName());
			}
		}

		if(type == GunAnimation.RELOAD) {
			if(timer == 5) Lego.playSound(entity, GUN_MAG_REMOVE, 1F, 1F);
			if(timer == 26) Lego.playSound(entity, GUN_MAG_INSERT, 1F, 1F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_FOLLY = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);

		if(type == GunAnimation.RELOAD) {
			if(timer == 20) Lego.playSound(entity, GUN_SCREW, 1F, 1F);
			if(timer == 80) Lego.playSound(entity, GUN_ROCKET_INSERT, 1F, 1F);
			if(timer == 120) Lego.playSound(entity, GUN_SCREW, 1F, 1F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_DOUBLE_BARREL = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);

		if(type == GunAnimation.RELOAD) {
			if(timer == 5) Lego.playSound(entity, GUN_REVOLVER_COCK, 1F, 0.75F);
			if(timer == 19) Lego.playSound(entity, GUN_MAG_SMALL_INSERT, 1F, 0.9F);
			if(timer == 29) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 0.8F);

			if(timer == 12) {
				IMagazine<?> mag = ctx.config.getReceivers(stack)[0].getMagazine(stack);
				int toEject = mag.getAmountAfterReload(stack) - mag.getAmount(stack, ctx.inventory);
				SpentCasing casing = mag.getCasing(stack, ctx.inventory);
				if(casing != null) for(int i = 0; i < toEject; i++) CasingCreator.composeEffect(entity.level(), entity, 0, -0.1875, -0.375D, -0.24, 0.18, 0, 0.01, -20F + (float)entity.getRandom().nextGaussian() * 5F, (float)entity.getRandom().nextGaussian() * 2.5F, casing.getName(), true, 60, 0.5D, 20);
			}
		}

		if(type == GunAnimation.INSPECT) {
			if(timer == 5) Lego.playSound(entity, GUN_REVOLVER_COCK, 1F, 0.75F);
			if(timer == 19) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 0.8F);
		}
		if(type == GunAnimation.CYCLE) {
			if(timer == 0) sendMuzzleFlash(entity);
		}
		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 2) Lego.playSound(entity, GUN_DRY_FIRE, 1F, 1F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_ABERRATOR = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		boolean aiming = ItemGunBaseNT.getIsAiming(stack);

		if(type == GunAnimation.RELOAD) {
			if(timer == 5) Lego.playSound(entity, GUN_MAG_SMALL_REMOVE, 1F, 0.75F);
			if(timer == 32) Lego.playSound(entity, GUN_MAG_SMALL_INSERT, 1F, 0.75F);
			if(timer == 42) Lego.playSound(entity, GUN_PISTOL_COCK, 1F, 0.75F);
		}

		if(type == GunAnimation.CYCLE) {
			if(timer == 0) sendMuzzleFlash(entity);
			if(timer == 1) {
				int cba = (stack.getItem() == WeaponItems.gun("gun_aberrator_eott") && ctx.configIndex == 0) ? -1 : 1;
				SpentCasing casing = ctx.config.getReceivers(stack)[0].getMagazine(stack).getCasing(stack, ctx.inventory);
				if(casing != null) CasingCreator.composeEffect(entity.level(), entity, 0.5, aiming ? 0 : -0.125, aiming ? -0.0625 : -0.25D * cba, -0.05, 0.25, -0.05 * cba, 0.01, -10F + (float)entity.getRandom().nextGaussian() * 10F, (float)entity.getRandom().nextGaussian() * 12.5F, casing.getName());
			}
		}

		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 1) Lego.playSound(entity, GUN_DRY_FIRE, 1F, 0.75F);
			if(timer == 9) Lego.playSound(entity, GUN_PISTOL_COCK, 1F, 0.75F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_MAS36 = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);
		boolean aiming = ItemGunBaseNT.getIsAiming(stack) && !XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_SCOPE);

		if(type == GunAnimation.EQUIP) {
			if(timer == 10) Lego.playSound(entity, GUN_LATCH_OPEN, 1F, 1F);
			if(timer == 18) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 1F);
		}

		if(type == GunAnimation.CYCLE) {
			if(timer == 0) sendMuzzleFlash(entity);
			if(timer == 7) Lego.playSound(entity, GUN_BOLT_OPEN, 0.5F, 1F);
			if(timer == 16) Lego.playSound(entity, GUN_BOLT_CLOSE, 0.5F, 1F);
			if(timer == 12) {
				SpentCasing casing = ctx.config.getReceivers(stack)[0].getMagazine(stack).getCasing(stack, ctx.inventory);
				if(casing != null) CasingCreator.composeEffect(entity.level(), entity,
						0.375, aiming ? 0 : -0.125, aiming ? 0 : -0.25D,
						-0.05, 0.2, -0.025,
						0.01, -10F + (float) entity.getRandom().nextGaussian() * 10F, (float) entity.getRandom().nextGaussian() * 12.5F, casing.getName(), true, 60, 0.5D, 10);
			}
		}

		if(type == GunAnimation.CYCLE_DRY) {
			if(timer == 0) Lego.playSound(entity, GUN_DRY_FIRE, 1F, 0.75F);
			if(timer == 7) Lego.playSound(entity, GUN_BOLT_OPEN, 0.5F, 1F);
			if(timer == 16) Lego.playSound(entity, GUN_BOLT_CLOSE, 0.5F, 1F);
		}

		if(type == GunAnimation.RELOAD) {
			if(timer == 0) Lego.playSound(entity, GUN_BOLT_OPEN, 1F, 1F);
			if(timer == 20) Lego.playSound(entity, GUN_RIFLE_COCK, 1F, 1F);
			if(timer == 36) Lego.playSound(entity, GUN_BOLT_CLOSE, 1F, 1F);
		}

		if(type == GunAnimation.JAMMED) {
			if(timer == 5) Lego.playSound(entity, GUN_BOLT_OPEN, 0.5F, 1F);
			if(timer == 12) Lego.playSound(entity, GUN_BOLT_CLOSE, 0.5F, 1F);
			if(timer == 16) Lego.playSound(entity, GUN_BOLT_OPEN, 0.5F, 1F);
			if(timer == 23) Lego.playSound(entity, GUN_BOLT_CLOSE, 0.5F, 1F);
		}

		if(type == GunAnimation.INSPECT) {
			if(timer == 0) Lego.playSound(entity, GUN_BOLT_OPEN, 0.5F, 1F);
			if(timer == 17) Lego.playSound(entity, GUN_BOLT_CLOSE, 0.5F, 1F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_FIREEXT = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);

		if(type == GunAnimation.RELOAD) {
			if(timer == 0) Lego.playSound(entity, GUN_VALVE, 1F, 1F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_CHARGE_THROWER = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);

		if(type == GunAnimation.CYCLE_DRY) {
			Entity e = entity.level().getEntity(com.hbm_m.item.weapon.sedna.impl.ItemGunChargeThrower.getLastHook(stack));
			if(timer == 0 && e == null) Lego.playSound(entity, GUN_DRY_FIRE, 1F, 0.75F);
		}

		if(type == GunAnimation.RELOAD) {
			if(timer == 30) Lego.playSound(entity, GUN_ROCKET_INSERT, 1F, 1F);
			if(timer == 40) Lego.playSound(entity, GUN_BOLT_CLOSE, 1F, 1F);
		}
	};

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_DRILL = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);

		// Motorklang (Original AudioWrapper + HbmAnimations "SPEED", clientseitig) - Ablauf 1:1 in OrchestrasClient
		if(entity.level().isClientSide) com.hbm_m.client.weapon.OrchestrasClient.drill(stack, ctx.configIndex, entity, type);
		if(entity.level().isClientSide) return;

		if(type == GunAnimation.RELOAD) {
			if(timer == 15) Lego.playSound(entity, GUN_LATCH_OPEN, 1F, 1F);
			if(timer == 35) Lego.playSound(entity, GUN_IMPACT, 0.5F, 1F);
			if(timer == 60) Lego.playSound(entity, GUN_REVOLVER_CLOSE, 1F, 0.75F);
			if(timer == 70) Lego.playSound(entity, GUN_CANISTER_INSERT, 1F, 1F);
			if(timer == 85) Lego.playSound(entity, GUN_VALVE, 1F, 1F);
		}
	};
}
