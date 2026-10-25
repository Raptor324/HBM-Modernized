package com.hbm_m.powerarmor;

import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.hbm_m.item.weapon.sedna.factory.Lego;
import com.hbm_m.item.weapon.sedna.factory.XFactoryPA;
import com.hbm_m.item.weapon.sedna.impl.IPAMelee;
import com.hbm_m.render.anim.AnimationEnums.GunAnimation;
import com.hbm_m.render.anim.BusAnimation;
import com.hbm_m.render.anim.BusAnimationKeyframe.IType;
import com.hbm_m.render.anim.BusAnimationSequence;
import com.hbm_m.util.EntityDamageUtil;
import com.hbm_m.util.MovingObjectPosition;
import com.hbm_m.util.confetti.ConfettiUtil;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** 1:1 {@code com.hbm.items.armor.ArmorNCRPAMelee}: Nahkampf der NCR-Powerarmor (Doppelschlag, Rundumhieb). */
public class ArmorNCRPAMelee implements IPAMelee {

    @Override public void clickPrimary(ItemStack stack, LambdaContext ctx) { XFactoryPA.doSwing(stack, ctx, GunAnimation.CYCLE, 25); }
    @Override public void clickSecondary(ItemStack stack, LambdaContext ctx) { XFactoryPA.doSwing(stack, ctx, GunAnimation.ALT_CYCLE, 30); }

    @Override
    public void orchestra(ItemStack stack, LambdaContext ctx) {
        LivingEntity entity = ctx.entity;
        if (entity.level().isClientSide) return;
        GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex);
        int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex);

        boolean swings = type == GunAnimation.CYCLE && (timer == 5 || timer == 15);
        boolean sweep = type == GunAnimation.ALT_CYCLE && timer == 5;

        if ((swings || sweep) && ctx.getPlayer() != null) {
            MovingObjectPosition mop = EntityDamageUtil.getMouseOver(ctx.getPlayer(), 3.0D, 0.5D);

            if (mop != null) {
                if (mop.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && mop.entityHit.isAlive()) {
                    float damage = swings ? 15F : 35F;
                    float knockback = swings ? 0F : 1.5F;
                    float dt = swings ? 5F : 15F;
                    float pierce = swings ? 0.1F : 0.25F;

                    if (mop.entityHit instanceof LivingEntity living) {
                        if (living.getMaxHealth() >= 100) damage *= 2.5;
                        EntityDamageUtil.attackEntityFromNT(living, ctx.getPlayer().damageSources().playerAttack(ctx.getPlayer()), damage, true, false, knockback, dt, pierce);
                        if (!living.isAlive()) ConfettiUtil.gib(living);
                    } else {
                        mop.entityHit.hurt(ctx.getPlayer().damageSources().playerAttack(ctx.getPlayer()), damage);
                    }

                    Lego.playSound(mop.entityHit, "hbm:weapon.fire.stab", 1F, 0.9F + entity.getRandom().nextFloat() * 0.2F);
                }
                if (mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                    BlockState b = entity.level().getBlockState(mop.getBlockPos());
                    entity.level().playSound(null, mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord, b.getSoundType().getStepSound(), SoundSource.BLOCKS, 2F, 0.9F + entity.getRandom().nextFloat() * 0.2F);
                }
            }
        }
    }

    @Override
    public BusAnimation playAnim(ItemStack stack, GunAnimation type) {
        if (type == GunAnimation.EQUIP) return new BusAnimation()
                .addBus("EQUIP", new BusAnimationSequence().setPos(-1, 0, 0).addPos(0, 0, 0, 750, IType.SIN_DOWN));
        if (type == GunAnimation.CYCLE) return new BusAnimation()
                .addBus("SWINGRIGHT", new BusAnimationSequence().addPos(1, 0, 0, 250, IType.SIN_DOWN).addPos(0, 0, 0, 500, IType.SIN_FULL))
                .addBus("SWINGLEFT", new BusAnimationSequence().addPos(0, 0, 0, 500).addPos(1, 0, 0, 250, IType.SIN_DOWN).addPos(0, 0, 0, 500, IType.SIN_FULL));
        if (type == GunAnimation.ALT_CYCLE) return new BusAnimation()
                .addBus("SWEEPTURN", new BusAnimationSequence().addPos(1, 0, 0, 100, IType.LINEAR).hold(350).addPos(0, 0, 0, 500, IType.LINEAR))
                .addBus("SWEEPCUT", new BusAnimationSequence().hold(100).addPos(1, 0, 0, 250, IType.SIN_DOWN).hold(100).addPos(0, 0, 0, 500, IType.SIN_FULL));

        return null;
    }

    @Override public void setupFirstPerson(ItemStack stack) { }

    /** Nur clientseitig (aus ItemRenderPAMelee); GL-Code liegt in {@code PAMeleeRenders}. */
    @Override
    public void renderFirstPerson(ItemStack stack) {
        com.hbm_m.client.weapon.render.PAMeleeRenders.renderNCRPA(stack);
    }
}
