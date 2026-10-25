package com.hbm_m.item.weapon.sedna.mods;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import com.hbm_m.item.weapon.sedna.GunConfig;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.hbm_m.item.weapon.sedna.factory.Lego;
import com.hbm_m.item.weapon.sedna.factory.Orchestras;
import com.hbm_m.item.weapon.sedna.factory.XFactory44;
import com.hbm_m.item.weapon.sedna.factory.XFactory762mm;
import com.hbm_m.render.anim.AnimationEnums.GunAnimation;
import com.hbm_m.render.anim.BusAnimation;
import com.hbm_m.render.anim.BusAnimationSequence;
import com.hbm_m.render.anim.BusAnimationKeyframe.IType;
import com.hbm_m.util.MovingObjectPosition;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

@SuppressWarnings("unchecked")
public class WeaponModMASBayonet extends WeaponModBase {

	public WeaponModMASBayonet(int id) {
		super(id, "BAYONET");
	}

	@Override
	public <T> T eval(T base, ItemStack gun, String key, Object parent) {
		if(key == GunConfig.FUN_ANIMNATIONS) return (T) LAMBDA_MAS36_ANIMS;
		if(key == GunConfig.I_INSPECTDURATION) return cast(30, base);
		if(key == GunConfig.CON_ONPRESSSECONDARY) return (T) XFactory44.SMACK_A_FUCKER;
		if(key == GunConfig.CON_ORCHESTRA) return (T) ORCHESTRA_MAS36;
		if(key == GunConfig.I_INSPECTCANCEL) return cast(false, base);
		return base;
	}

	public static BiConsumer<ItemStack, LambdaContext> ORCHESTRA_MAS36 = (stack, ctx) -> {
		LivingEntity entity = ctx.entity;
		if(entity.level().isClientSide) return;
		GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
		int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);

		if(type == GunAnimation.INSPECT) {

			if(timer == 15 && ctx.getPlayer() != null) {
				MovingObjectPosition mop = Orchestras.getMouseOver(ctx.getPlayer(), 3.0D);
				if(mop != null) {
					if(mop.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
						float damage = 15F;
						mop.entityHit.hurt(ctx.getPlayer().damageSources().playerAttack(ctx.getPlayer()), damage);
						Vec3 m = mop.entityHit.getDeltaMovement();
						mop.entityHit.setDeltaMovement(m.x * 2, m.y, m.z * 2);
						Lego.playSound(mop.entityHit, Orchestras.GUN_STAB_A_FUCKER, 1F, 0.9F + entity.getRandom().nextFloat() * 0.2F);
					}
					if(mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
						BlockState b = entity.level().getBlockState(mop.getBlockPos());
						entity.level().playSound(null, mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord, b.getSoundType().getStepSound(), SoundSource.BLOCKS, 2F, 0.9F + entity.getRandom().nextFloat() * 0.2F);
					}
				}
			}
			return;
		}

		Orchestras.ORCHESTRA_MAS36.accept(stack, ctx);
	};

	@SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_MAS36_ANIMS = (stack, type) -> {
		switch(type) {
		case INSPECT: return new BusAnimation()
				.addBus("STAB", new BusAnimationSequence().addPos(0, 1, -2, 250, IType.SIN_DOWN).hold(250).addPos(0, 1, 5, 250, IType.SIN_UP).hold(250).addPos(0, 0, 0, 500, IType.SIN_FULL));
		}

		return XFactory762mm.LAMBDA_MAS36_ANIMS.apply(stack, type);
	};
}
