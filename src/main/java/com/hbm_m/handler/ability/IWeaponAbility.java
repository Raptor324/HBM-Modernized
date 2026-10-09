package com.hbm_m.handler.ability;

import com.hbm_m.platform.PlatformHooks;

import com.hbm_m.platform.EffectHooks;

import com.hbm_m.platform.StackNbt;

import com.hbm_m.effect.ModEffects;
import com.hbm_m.item.ModItems;
import com.hbm_m.sound.HbmSoundsNT;
import com.hbm_m.util.ContaminationUtil;
import com.hbm_m.util.ContaminationUtil.ContaminationType;
import com.hbm_m.util.ContaminationUtil.HazardType;
import com.hbm_m.util.confetti.ConfettiUtil;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** 1:1 {@code com.hbm.handler.ability.IWeaponAbility}. */
public interface IWeaponAbility extends IBaseAbility {
    // Note: tool is currently unused in weapon abilities
    void onHit(int level, Level world, Player player, Entity victim, Item tool);

    int SORT_ORDER_BASE = 200;

    // region handlers
    IWeaponAbility NONE = new IWeaponAbility() {
        @Override public String getName() { return ""; }
        @Override public int sortOrder() { return SORT_ORDER_BASE + 0; }
        @Override public void onHit(int level, Level world, Player player, Entity victim, Item tool) { }
    };

    IWeaponAbility RADIATION = new IWeaponAbility() {
        @Override public String getName() { return "weapon.ability.radiation"; }

        public final float[] radAtLevel = { 15F, 50F, 500F };

        @Override public int levels() { return radAtLevel.length; }
        @Override public String getExtension(int level) { return " (" + radAtLevel[level] + ")"; }
        @Override public int sortOrder() { return SORT_ORDER_BASE + 1; }

        @Override
        public void onHit(int level, Level world, Player player, Entity victim, Item tool) {
            if (victim instanceof LivingEntity living)
                ContaminationUtil.contaminate(living, HazardType.RADIATION, ContaminationType.CREATIVE, radAtLevel[level]);
        }
    };

    IWeaponAbility VAMPIRE = new IWeaponAbility() {
        @Override public String getName() { return "weapon.ability.vampire"; }

        public final float[] amountAtLevel = { 2F, 3F, 5F, 10F, 50F };

        @Override public int levels() { return amountAtLevel.length; }
        @Override public String getExtension(int level) { return " (" + amountAtLevel[level] + ")"; }
        @Override public int sortOrder() { return SORT_ORDER_BASE + 2; }

        @Override
        public void onHit(int level, Level world, Player player, Entity victim, Item tool) {
            float amount = amountAtLevel[level];

            if (victim instanceof LivingEntity living) {
                if (living.getHealth() <= 0)
                    return;
                living.setHealth(living.getHealth() - amount);
                if (living.getHealth() <= 0)
                    living.die(world.damageSources().magic());
                player.heal(amount);
            }
        }
    };

    IWeaponAbility STUN = new IWeaponAbility() {
        @Override public String getName() { return "weapon.ability.stun"; }

        public final int[] durationAtLevel = { 2, 3, 5, 10, 15 };

        @Override public int levels() { return durationAtLevel.length; }
        @Override public String getExtension(int level) { return " (" + durationAtLevel[level] + ")"; }
        @Override public int sortOrder() { return SORT_ORDER_BASE + 3; }

        @Override
        public void onHit(int level, Level world, Player player, Entity victim, Item tool) {
            int duration = durationAtLevel[level];

            if (victim instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration * 20, 4));
                living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration * 20, 4));
            }
        }
    };

    IWeaponAbility PHOSPHORUS = new IWeaponAbility() {
        @Override public String getName() { return "weapon.ability.phosphorus"; }

        public final int[] durationAtLevel = { 60, 90 };

        @Override public int levels() { return durationAtLevel.length; }
        @Override public String getExtension(int level) { return " (" + durationAtLevel[level] + ")"; }
        @Override public int sortOrder() { return SORT_ORDER_BASE + 4; }

        @Override
        public void onHit(int level, Level world, Player player, Entity victim, Item tool) {
            int duration = durationAtLevel[level];

            if (victim instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(EffectHooks.of(ModEffects.PHOSPHORUS), duration * 20, 4));
            }
        }
    };

    IWeaponAbility FIRE = new IWeaponAbility() {
        @Override public String getName() { return "weapon.ability.fire"; }

        public final int[] durationAtLevel = { 5, 10 };

        @Override public int levels() { return durationAtLevel.length; }
        @Override public String getExtension(int level) { return " (" + durationAtLevel[level] + ")"; }
        @Override public int sortOrder() { return SORT_ORDER_BASE + 6; }

        @Override
        public void onHit(int level, Level world, Player player, Entity victim, Item tool) {
            if (victim instanceof LivingEntity) {
                PlatformHooks.setSecondsOnFire(victim, durationAtLevel[level]);
            }
        }
    };

    IWeaponAbility CHAINSAW = new IWeaponAbility() {
        @Override public String getName() { return "weapon.ability.chainsaw"; }

        public final int[] dividerAtLevel = { 15, 10 };

        @Override public int levels() { return dividerAtLevel.length; }
        @Override public String getExtension(int level) { return " (1:" + dividerAtLevel[level] + ")"; }
        @Override public int sortOrder() { return SORT_ORDER_BASE + 7; }

        @Override
        public void onHit(int level, Level world, Player player, Entity victim, Item tool) {
            int divider = dividerAtLevel[level];

            if (victim instanceof LivingEntity living) {
                if (living.getHealth() <= 0.0F) {
                    int count = Math.min((int) Math.ceil(living.getMaxHealth() / divider), 250); // safeguard to prevent funnies from bosses with obscene  health

                    for (int i = 0; i < count; i++) {
                        living.spawnAtLocation(new ItemStack(ModItems.NITRA_SMALL.get()), 1);
                        world.addFreshEntity(new ExperienceOrb(world, living.getX(), living.getY(), living.getZ(), 1));
                    }

                    ConfettiUtil.gib(living);
                    world.playSound(null, living.getX(), living.getY() + living.getBbHeight() * 0.5, living.getZ(),
                            HbmSoundsNT.get("weapon.chainsaw"), SoundSource.PLAYERS, 0.5F, 1.0F);
                }
            }
        }
    };

    IWeaponAbility BEHEADER = new IWeaponAbility() {
        @Override public String getName() { return "weapon.ability.beheader"; }
        @Override public int sortOrder() { return SORT_ORDER_BASE + 8; }

        @Override
        public void onHit(int level, Level world, Player player, Entity victim, Item tool) {
            if (victim instanceof LivingEntity living && living.getHealth() <= 0.0F) {

                if (living instanceof AbstractSkeleton) {
                    if (!(living instanceof WitherSkeleton)) {
                        living.spawnAtLocation(new ItemStack(Items.SKELETON_SKULL), 0.0F);
                    } else {
                        if (world.random.nextInt(20) == 0)
                            living.spawnAtLocation(new ItemStack(Items.WITHER_SKELETON_SKULL), 0.0F);
                        else
                            living.spawnAtLocation(new ItemStack(Items.COAL, 3), 0.0F);
                    }
                } else if (living instanceof Zombie) {
                    living.spawnAtLocation(new ItemStack(Items.ZOMBIE_HEAD), 0.0F);
                } else if (living instanceof Creeper) {
                    living.spawnAtLocation(new ItemStack(Items.CREEPER_HEAD), 0.0F);
                } else if (living instanceof MagmaCube) {
                    living.spawnAtLocation(new ItemStack(Items.MAGMA_CREAM, 3), 0.0F);
                } else if (living instanceof Slime) {
                    living.spawnAtLocation(new ItemStack(Items.SLIME_BALL, 3), 0.0F);
                } else if (living instanceof Player p) {
                    ItemStack head = new ItemStack(Items.PLAYER_HEAD);
                    //? if < 1.21.1 {
                    CompoundTag tag = StackNbt.orCreate(head);
                    tag.put("SkullOwner", NbtUtils.writeGameProfile(new CompoundTag(), p.getGameProfile()));
                    //?} else {
                    /*head.set(net.minecraft.core.component.DataComponents.PROFILE, new net.minecraft.world.item.component.ResolvableProfile(p.getGameProfile()));
                    *///?}
                    living.spawnAtLocation(head, 0.0F);
                } else {
                    living.spawnAtLocation(new ItemStack(Items.ROTTEN_FLESH, 3), 0.0F);
                    living.spawnAtLocation(new ItemStack(Items.BONE, 2), 0.0F);
                }
            }
        }
    };

    IWeaponAbility BOBBLE = new IWeaponAbility() {
        @Override public String getName() { return "weapon.ability.bobble"; }
        @Override public int sortOrder() { return SORT_ORDER_BASE + 9; }

        @Override
        public void onHit(int level, Level world, Player player, Entity victim, Item tool) {
            if (victim instanceof Monster mob && mob.getHealth() <= 0.0F) {

                int chance = 1000;

                if (mob.getMaxHealth() > 20) {
                    chance = 750;
                }

                if (world.random.nextInt(chance) == 0) {
                    ItemStack bobble = BobbleDrop.randomBobblehead(world);
                    if (!bobble.isEmpty()) mob.spawnAtLocation(bobble, 0.0F);
                }
            }
        }
    };
    // endregion handlers

    IWeaponAbility[] abilities = { NONE, RADIATION, VAMPIRE, STUN, PHOSPHORUS, FIRE, CHAINSAW, BEHEADER, BOBBLE };

    static IWeaponAbility getByName(String name) {
        for (IWeaponAbility ability : abilities) {
            if (ability.getName().equals(name))
                return ability;
        }

        return NONE;
    }
}
