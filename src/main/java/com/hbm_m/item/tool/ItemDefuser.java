package com.hbm_m.item.tool;

import com.hbm_m.api.block.IToolable;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code ItemDefuser} ({@code defuser}): {@code ItemTooling(ToolType.DEFUSER, 100)}; Rechtsklick auf einen Creeper
 * entschaerft ihn ({@code ItemModDefuser.castrateCreeper}); ein sterbender {@code EntityGlyphidNuclear} wird
 * gesprengt und laesst eine Demolier-Mini-Nuke ({@code ammo_standard NUKE_DEMO}) fallen.
 */
public class ItemDefuser extends ItemTooling {

    public ItemDefuser(int durability, Properties properties) {
        super(IToolable.ToolType.DEFUSER, durability, properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity entity, InteractionHand hand) {
        if (entity instanceof com.hbm_m.entity.mob.glyphid.EntityGlyphidNuclear john) {

            if (!player.level().isClientSide && john.deathTicks > 0) {
                john.discard();

                com.hbm_m.explosion.vanillant.ExplosionVNT vnt = new com.hbm_m.explosion.vanillant.ExplosionVNT(john.level(), john.getX(), john.getY(), john.getZ(), 5F, john);
                vnt.setEntityProcessor(new com.hbm_m.explosion.vanillant.standard.EntityProcessorCrossSmooth(1, 20).setupPiercing(10F, 0.2F));
                vnt.setPlayerProcessor(new com.hbm_m.explosion.vanillant.standard.PlayerProcessorStandard());
                vnt.setSFX(new com.hbm_m.explosion.vanillant.standard.ExplosionEffectWeapon(10, 2.5F, 1F));
                vnt.explode();

                com.hbm_m.util.confetti.ConfettiUtil.gib(john);

                john.spawnAtLocation(new ItemStack(com.hbm_m.item.weapon.sedna.WeaponItems.ammo(com.hbm_m.item.weapon.sedna.factory.GunFactory.EnumAmmo.NUKE_DEMO)), 1.5F);
            }

            return InteractionResult.sidedSuccess(player.level().isClientSide);
        }

        if (entity instanceof Creeper creeper) {
            return com.hbm_m.armormod.item.ItemModDefuser.castrateCreeper(creeper, player, true)
                    ? InteractionResult.sidedSuccess(player.level().isClientSide) : InteractionResult.PASS;
        }
        return InteractionResult.PASS;
    }
}
