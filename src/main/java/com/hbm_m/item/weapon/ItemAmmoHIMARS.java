package com.hbm_m.item.weapon;

import com.hbm_m.platform.EffectHooks;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.effect.ModEffects;
import com.hbm_m.entity.projectile.EntityArtilleryRocket;
import com.hbm_m.explosion.ExplosionChaos;
import com.hbm_m.explosion.ExplosionLarge;
import com.hbm_m.explosion.ExplosionNukeSmall;
import com.hbm_m.explosion.vanillant.ExplosionVNT;
import com.hbm_m.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm_m.explosion.vanillant.standard.BlockMutatorDebris;
import com.hbm_m.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm_m.explosion.vanillant.standard.EntityProcessorCross;
import com.hbm_m.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm_m.item.ITooltipProvider;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.particle.helper.ExplosionCreator;
import com.hbm_m.particle.helper.IParticleCreator;
import com.hbm_m.platform.PlatformHooks;
import com.hbm_m.sound.HbmSoundsNT;
import com.hbm_m.util.MovingObjectPosition;
import com.hbm_m.util.Vec3NT;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code ItemAmmoHIMARS} (ammo_himars, Meta = Raketentyp, Stapel 1). Im Port je Meta ein Gegenstand
 * ({@code rocket_himars_*}); {@link #type} ist die Original-Meta, die auch {@link EntityArtilleryRocket} fuehrt.
 */
public class ItemAmmoHIMARS extends Item implements ITooltipProvider {

    public static HIMARSRocket[] itemTypes = new HIMARSRocket[ /* >>> */ 8 /* <<< */ ];

    public static final int SMALL = 0;
    public static final int LARGE = 1;
    public static final int SMALL_HE = 2;
    public static final int SMALL_WP = 3;
    public static final int SMALL_TB = 4;
    public static final int LARGE_TB = 5;
    public static final int SMALL_MINI_NUKE = 6;
    public static final int SMALL_LAVA = 7;

    /** Original-Meta dieses Gegenstands. */
    public final int type;

    public ItemAmmoHIMARS(int type, Properties properties) {
        super(properties.stacksTo(1));
        this.type = type;
    }

    /** Original {@code getItemDamage()} eines Raketenstapels, -1 fuer Fremdgegenstaende. */
    public static int typeOf(ItemStack stack) {
        return stack.getItem() instanceof ItemAmmoHIMARS a ? a.type : -1;
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {

        ChatFormatting r = ChatFormatting.RED;
        ChatFormatting y = ChatFormatting.YELLOW;
        ChatFormatting b = ChatFormatting.BLUE;

        switch (type) {
            case SMALL -> {
                list.add(Component.literal("Strength: 20").withStyle(y));
                list.add(Component.literal("Damage modifier: 3x").withStyle(y));
                list.add(Component.literal("Does not destroy blocks").withStyle(b));
            }
            case SMALL_HE -> {
                list.add(Component.literal("Strength: 20").withStyle(y));
                list.add(Component.literal("Damage modifier: 3x").withStyle(y));
                list.add(Component.literal("Destroys blocks").withStyle(r));
            }
            case SMALL_WP -> {
                list.add(Component.literal("Strength: 20").withStyle(y));
                list.add(Component.literal("Damage modifier: 3x").withStyle(y));
                list.add(Component.literal("Phosphorus splash").withStyle(r));
                list.add(Component.literal("Does not destroy blocks").withStyle(b));
            }
            case SMALL_TB -> {
                list.add(Component.literal("Strength: 20").withStyle(y));
                list.add(Component.literal("Damage modifier: 10x").withStyle(y));
                list.add(Component.literal("Destroys blocks").withStyle(r));
            }
            case SMALL_MINI_NUKE -> {
                list.add(Component.literal("Strength: 20").withStyle(y));
                list.add(Component.literal("Deals nuclear damage").withStyle(r));
                list.add(Component.literal("Destroys blocks").withStyle(r));
            }
            case SMALL_LAVA -> {
                list.add(Component.literal("Strength: 20").withStyle(y));
                list.add(Component.literal("Creates volcanic lava").withStyle(r));
                list.add(Component.literal("Destroys blocks").withStyle(r));
            }
            case LARGE -> {
                list.add(Component.literal("Strength: 50").withStyle(y));
                list.add(Component.literal("Damage modifier: 5x").withStyle(y));
                list.add(Component.literal("Destroys blocks").withStyle(r));
            }
            case LARGE_TB -> {
                list.add(Component.literal("Strength: 50").withStyle(y));
                list.add(Component.literal("Damage modifier: 12x").withStyle(y));
                list.add(Component.literal("Destroys blocks").withStyle(r));
            }
            default -> { }
        }
    }

    public abstract static class HIMARSRocket {

        public final String name;
        public final ResourceLocation texture;
        public final int amount;
        public final int modelType; /* 0 = sixfold/standard ; 1 = single */

        public HIMARSRocket(String name, String texture, int type) {
            this.name = name;
            this.texture = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/projectiles/" + texture + ".png");
            this.amount = type == 0 ? 6 : 1;
            this.modelType = type;
        }

        public abstract void onImpact(EntityArtilleryRocket rocket, MovingObjectPosition mop);
        public void onUpdate(EntityArtilleryRocket rocket) { }
    }

    /** Original {@code standardExplosion(..., Block slag, int slagMeta)} - Metadaten gibt es im Port nicht. */
    public static void standardExplosion(EntityArtilleryRocket rocket, MovingObjectPosition mop, float size, float rangeMod, boolean breaksBlocks, BlockState slag) {
        Vec3NT vec = new Vec3NT(rocket.getDeltaMovement()).normalize();
        ExplosionVNT xnt = new ExplosionVNT(rocket.level(), mop.hitVec.xCoord - vec.xCoord, mop.hitVec.yCoord - vec.yCoord, mop.hitVec.zCoord - vec.zCoord, size);
        if (breaksBlocks) {
            xnt.setBlockAllocator(new BlockAllocatorStandard(48));
            xnt.setBlockProcessor(new BlockProcessorStandard().setNoDrop().withBlockEffect(new BlockMutatorDebris(slag)));
        }
        xnt.setEntityProcessor(new EntityProcessorCross(7.5).withRangeMod(rangeMod));
        xnt.setPlayerProcessor(new PlayerProcessorStandard());
        xnt.explode();
        rocket.killAndClear();
    }

    public static void standardMush(EntityArtilleryRocket rocket, MovingObjectPosition mop, float size) {
        if (!(rocket.level() instanceof ServerLevel server)) return;
        CompoundTag data = new CompoundTag();
        data.putString("type", "rbmkmush");
        data.putFloat("scale", size);
        IParticleCreator.sendPacket(server, mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord, 250, data);
    }

    private static BlockState slag() { return ModBlocks.BLOCK_SLAG.get().defaultBlockState(); }

    /** Original: {@code composeEffect} am Mittelpunkt des getroffenen Blocks. */
    private static void composeEffect(EntityArtilleryRocket rocket, MovingObjectPosition mop, int cloudCount, float cloudScale, float cloudSpeedMult, float waveScale,
            int debrisCount, int debrisSize, int debrisRetry, float debrisVelocity, float debrisHorizontalDeviation, float debrisVerticalOffset, float soundRange) {
        if (rocket.level() instanceof ServerLevel server) {
            ExplosionCreator.composeEffect(server, mop.blockX + 0.5, mop.blockY + 0.5, mop.blockZ + 0.5, cloudCount, cloudScale, cloudSpeedMult, waveScale,
                    debrisCount, debrisSize, debrisRetry, debrisVelocity, debrisHorizontalDeviation, debrisVerticalOffset, soundRange);
        }
    }

    private static void explosionMedium(EntityArtilleryRocket rocket) {
        rocket.level().playSound(null, rocket.getX(), rocket.getY(), rocket.getZ(), HbmSoundsNT.get("hbm:weapon.explosionMedium"), SoundSource.NEUTRAL, 20.0F, 0.9F + rocket.level().random.nextFloat() * 0.2F);
    }

    static {
        /* STANDARD ROCKETS */
        itemTypes[SMALL] = new HIMARSRocket("standard", "himars_standard", 0) { public void onImpact(EntityArtilleryRocket rocket, MovingObjectPosition mop) { standardExplosion(rocket, mop, 20F, 3F, false, slag()); composeEffect(rocket, mop, 15, 5F, 1F, 45F, 10, 0, 50, 1F, 3F, -2F, 200); }};
        itemTypes[SMALL_HE] = new HIMARSRocket("standard_he", "himars_standard_he", 0) { public void onImpact(EntityArtilleryRocket rocket, MovingObjectPosition mop) { standardExplosion(rocket, mop, 20F, 3F, true, slag()); composeEffect(rocket, mop, 15, 5F, 1F, 45F, 10, 16, 50, 1F, 3F, -2F, 200); }};
        itemTypes[SMALL_LAVA] = new HIMARSRocket("standard_lava", "himars_standard_lava", 0) { public void onImpact(EntityArtilleryRocket rocket, MovingObjectPosition mop) { standardExplosion(rocket, mop, 20F, 3F, true, ModBlocks.VOLCANIC_LAVA_BLOCK.get().defaultBlockState()); }};
        itemTypes[LARGE] = new HIMARSRocket("single", "himars_single", 1) { public void onImpact(EntityArtilleryRocket rocket, MovingObjectPosition mop) { standardExplosion(rocket, mop, 50F, 5F, true, slag()); composeEffect(rocket, mop, 30, 6.5F, 2F, 65F, 25, 16, 50, 1.25F, 3F, -2F, 350); }};

        itemTypes[SMALL_MINI_NUKE] = new HIMARSRocket("standard_mini_nuke", "himars_standard_mini_nuke", 0) {
            public void onImpact(EntityArtilleryRocket rocket, MovingObjectPosition mop) {
                rocket.killAndClear();
                Vec3NT vec = new Vec3NT(rocket.getDeltaMovement()).normalize();
                ExplosionNukeSmall.explode(rocket.level(), mop.hitVec.xCoord - vec.xCoord, mop.hitVec.yCoord - vec.yCoord, mop.hitVec.zCoord - vec.zCoord, ExplosionNukeSmall.PARAMS_MEDIUM);
            }
        };

        itemTypes[SMALL_WP] = new HIMARSRocket("standard_wp", "himars_standard_wp", 0) {
            public void onImpact(EntityArtilleryRocket rocket, MovingObjectPosition mop) {
                explosionMedium(rocket);
                standardExplosion(rocket, mop, 20F, 3F, false, slag());
                ExplosionLarge.spawnShrapnels(rocket.level(), (int) mop.hitVec.xCoord, (int) mop.hitVec.yCoord, (int) mop.hitVec.zCoord, 30);
                ExplosionChaos.igniteAllBlocks(rocket.level(), (int) mop.hitVec.xCoord, (int) mop.hitVec.yCoord, (int) mop.hitVec.zCoord, 20);
                int radius = 30;
                List<Entity> hit = rocket.level().getEntities(rocket, new AABB(rocket.getX() - radius, rocket.getY() - radius, rocket.getZ() - radius, rocket.getX() + radius, rocket.getY() + radius, rocket.getZ() + radius));
                for (Entity e : hit) {
                    PlatformHooks.setSecondsOnFire(e, 5);
                    if (e instanceof LivingEntity living) {
                        MobEffectInstance eff = new MobEffectInstance(EffectHooks.of(ModEffects.PHOSPHORUS), 30 * 20, 0, true, true);
                        //? if forge {
                        eff.setCurativeItems(new java.util.ArrayList<>());
                        //?} elif neoforge {
                        /*eff.getCures().clear();
                        *///?}
                        living.addEffect(eff);
                    }
                }
                for (int i = 0; i < 10; i++) {
                    ItemAmmoArty.sendFx(rocket, "haze", mop.hitVec.xCoord + rocket.level().random.nextGaussian() * 15, mop.hitVec.yCoord, mop.hitVec.zCoord + rocket.level().random.nextGaussian() * 15, 150, 0);
                }
                standardMush(rocket, mop, 15);
            }};

        itemTypes[SMALL_TB] = new HIMARSRocket("standard_tb", "himars_standard_tb", 0) {
            public void onImpact(EntityArtilleryRocket rocket, MovingObjectPosition mop) {
                explosionMedium(rocket);
                standardExplosion(rocket, mop, 20F, 10F, true, slag());
                ExplosionLarge.spawnShrapnels(rocket.level(), (int) mop.hitVec.xCoord, (int) mop.hitVec.yCoord, (int) mop.hitVec.zCoord, 30);
                standardMush(rocket, mop, 20);
            }};

        itemTypes[LARGE_TB] = new HIMARSRocket("single_tb", "himars_single_tb", 1) {
            public void onImpact(EntityArtilleryRocket rocket, MovingObjectPosition mop) {
                explosionMedium(rocket);
                standardExplosion(rocket, mop, 50F, 12F, true, slag());
                ExplosionLarge.spawnShrapnels(rocket.level(), (int) mop.hitVec.xCoord, (int) mop.hitVec.yCoord, (int) mop.hitVec.zCoord, 30);
                standardMush(rocket, mop, 35);
            }};
    }
}
