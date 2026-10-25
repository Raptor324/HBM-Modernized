package com.hbm_m.armormod.item;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.armormod.util.ArmorModificationHelper;
import com.hbm_m.extprop.HbmLivingProps;
import com.hbm_m.extprop.HbmPlayerProps;
import com.hbm_m.item.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** 1:1 {@code ItemModDefuser} (defuser_gold): entschaerft Creeper im Umkreis von 5. */
public class ItemModDefuser extends ItemArmorMod {

    public ItemModDefuser() {
        super(new Properties().stacksTo(1), ArmorModificationHelper.extra, true, true, true, true);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable Level level, List<Component> list) {
        list.add(line("Defuses nearby creepers", ChatFormatting.YELLOW));
        list.add(Component.empty());
    }

    @Override
    public void addDesc(List<Component> list, ItemStack stack, ItemStack armor) {
        list.add(descLine(ChatFormatting.YELLOW, stack, " (Defuses creepers)"));
    }

    @Override
    public void modUpdate(LivingEntity entity, ItemStack armor) {
        if (entity.level().isClientSide || entity.level().getGameTime() % 20 != 0) return;
        List<net.minecraft.world.entity.monster.Creeper> creepers = entity.level().getEntitiesOfClass(net.minecraft.world.entity.monster.Creeper.class, entity.getBoundingBox().inflate(5, 5, 5));
        for (net.minecraft.world.entity.monster.Creeper creeper : creepers) castrateCreeper(creeper, entity, true);
    }

    /** my bualls */
    public static boolean castrateCreeper(net.minecraft.world.entity.monster.Creeper creeper, @Nullable LivingEntity entity, boolean dropItem) {
        creeper.setSwellDir(-1);
        creeper.getEntityData().set(com.hbm_m.mixin.CreeperAccessor.hbm_m$getDataIsIgnited(), false);

        if (!creeper.level().isClientSide) {
            net.minecraft.world.entity.ai.goal.SwellGoal toRem = null;
            for (net.minecraft.world.entity.ai.goal.WrappedGoal entry : ((com.hbm_m.mixin.MobAccessor) creeper).hbm_m$getGoalSelector().getAvailableGoals()) {
                if (entry.getGoal() instanceof net.minecraft.world.entity.ai.goal.SwellGoal swell) {
                    toRem = swell;
                    break;
                }
            }

            if (toRem != null) {
                ((com.hbm_m.mixin.MobAccessor) creeper).hbm_m$getGoalSelector().removeGoal(toRem);

                if (dropItem) {
                    creeper.level().playSound(null, creeper.getX(), creeper.getY(), creeper.getZ(), com.hbm_m.sound.HbmSoundsNT.get("hbm:item.pinBreak"), net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 1.0F);
                    creeper.spawnAtLocation(ModItems.SAFETY_FUSE.get(), 0);
                    creeper.hurt(entity != null ? creeper.damageSources().mobAttack(entity) : creeper.damageSources().magic(), 1.0F);
                    creeper.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 0, 200));
                }
                creeper.getPersistentData().putBoolean("hfr_defused", true);
                return true;
            }
        }
        return false;
    }
}
