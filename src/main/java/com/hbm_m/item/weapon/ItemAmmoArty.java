package com.hbm_m.item.weapon;

import java.util.List;
import java.util.Random;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.effect.ModEffects;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.effect.EntityMist;
import com.hbm_m.entity.logic.EntityNukeExplosionMK5;
import com.hbm_m.entity.projectile.EntityArtilleryShell;
import com.hbm_m.explosion.ExplosionChaos;
import com.hbm_m.explosion.ExplosionLarge;
import com.hbm_m.explosion.ExplosionNukeSmall;
import com.hbm_m.explosion.vanillant.ExplosionVNT;
import com.hbm_m.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm_m.explosion.vanillant.standard.BlockMutatorDebris;
import com.hbm_m.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm_m.explosion.vanillant.standard.EntityProcessorCross;
import com.hbm_m.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm_m.handler.pollution.PollutionHandler;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.trait.PollutionType;
import com.hbm_m.item.ITooltipProvider;
import com.hbm_m.particle.SpentCasing;
import com.hbm_m.particle.SpentCasing.CasingType;
import com.hbm_m.particle.helper.ExplosionCreator;
import com.hbm_m.particle.helper.IParticleCreator;
import com.hbm_m.platform.PlatformHooks;
import com.hbm_m.sound.HbmSoundsNT;
import com.hbm_m.util.MovingObjectPosition;
import com.hbm_m.util.MovingObjectPosition.MovingObjectType;
import com.hbm_m.util.Vec3NT;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code ItemAmmoArty} (ammo_arty, Meta = Granatentyp). Im Port ist jede Meta ein eigener Gegenstand mit dem
 * Originalnamen des Typs ({@code ammo_arty}, {@code ammo_arty_classic} ...); {@link #type} ist die Original-Meta,
 * die auch die {@link EntityArtilleryShell} als Typ fuehrt. Die Typen ({@link #itemTypes}) sind statisch.
 */
public class ItemAmmoArty extends Item implements ITooltipProvider {

    public static Random rand = new Random();
    public static ArtilleryShell[] itemTypes = new ArtilleryShell[ /* >>> */ 12 /* <<< */ ];
    /* item types */
    public static final int NORMAL = 0;
    public static final int CLASSIC = 1;
    public static final int EXPLOSIVE = 2;
    public static final int MINI_NUKE = 3;
    public static final int NUKE = 4;
    public static final int PHOSPHORUS = 5;
    public static final int MINI_NUKE_MULTI = 6;
    public static final int PHOSPHORUS_MULTI = 7;
    public static final int CARGO = 8;
    public static final int CHLORINE = 9;
    public static final int PHOSGENE = 10;
    public static final int MUSTARD = 11;

    /** Original-Meta dieses Gegenstands. */
    public final int type;

    public ItemAmmoArty(int type, Properties properties) {
        super(properties);
        this.type = type;
    }

    /** Original {@code getItemDamage()} eines Granatenstapels, -1 fuer Fremdgegenstaende. */
    public static int typeOf(ItemStack stack) {
        return stack.getItem() instanceof ItemAmmoArty a ? a.type : -1;
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {

        ChatFormatting r = ChatFormatting.RED;
        ChatFormatting y = ChatFormatting.YELLOW;
        ChatFormatting b = ChatFormatting.BLUE;

        switch (type) {
            case NORMAL -> {
                list.add(Component.literal("Strength: 10").withStyle(y));
                list.add(Component.literal("Damage modifier: 3x").withStyle(y));
                list.add(Component.literal("Does not destroy blocks").withStyle(b));
            }
            case CLASSIC -> {
                list.add(Component.literal("Strength: 15").withStyle(y));
                list.add(Component.literal("Damage modifier: 5x").withStyle(y));
                list.add(Component.literal("Does not destroy blocks").withStyle(b));
            }
            case EXPLOSIVE -> {
                list.add(Component.literal("Strength: 15").withStyle(y));
                list.add(Component.literal("Damage modifier: 3x").withStyle(y));
                list.add(Component.literal("Destroys blocks").withStyle(r));
            }
            case PHOSPHORUS -> {
                list.add(Component.literal("Strength: 10").withStyle(y));
                list.add(Component.literal("Damage modifier: 3x").withStyle(y));
                list.add(Component.literal("Phosphorus splash").withStyle(r));
                list.add(Component.literal("Does not destroy blocks").withStyle(b));
            }
            case PHOSPHORUS_MULTI -> list.add(Component.literal("Splits x10").withStyle(r));
            case MINI_NUKE -> {
                list.add(Component.literal("Strength: 20").withStyle(y));
                list.add(Component.literal("Deals nuclear damage").withStyle(r));
                list.add(Component.literal("Destroys blocks").withStyle(r));
            }
            case MINI_NUKE_MULTI -> list.add(Component.literal("Splits x5").withStyle(r));
            case NUKE -> {
                list.add(Component.literal("☠").withStyle(r));
                list.add(Component.literal("(that is the best skull and crossbones").withStyle(r));
                list.add(Component.literal("minecraft's unicode has to offer)").withStyle(r));
            }
            case CARGO -> {
                CompoundTag tag = PlatformHooks.getItemTag(stack);
                if (tag != null && tag.contains("cargo")) {
                    ItemStack cargo = PlatformHooks.itemStackOf(tag.getCompound("cargo"), PlatformHooks.bestEffortProvider());
                    list.add(Component.empty().append(cargo.getHoverName()).withStyle(y));
                } else {
                    list.add(Component.literal("Empty").withStyle(r));
                }
            }
            default -> { }
        }
    }

    /** Original {@code getIconIndex}: Frachtgranate mit Ladung zeigt {@code ammo_arty_cargo_full}. */
    public static boolean hasCargo(ItemStack stack) {
        if (typeOf(stack) != CARGO) return false;
        CompoundTag tag = PlatformHooks.getItemTag(stack);
        return tag != null && tag.contains("cargo");
    }

    protected static SpentCasing SIXTEEN_INCH_CASE = new SpentCasing(CasingType.STRAIGHT).setScale(15F, 15F, 10F).setupSmoke(1F, 1D, 200, 60).setMaxAge(300).setBounceMotion(1F, 0.5F);

    public abstract static class ArtilleryShell {

        public final String name;
        public SpentCasing casing;

        public ArtilleryShell(String name, int casingColor) {
            this.name = name;
            this.casing = SIXTEEN_INCH_CASE.clone().register(name).setColor(casingColor);
        }

        public abstract void onImpact(EntityArtilleryShell shell, MovingObjectPosition mop);
        public void onUpdate(EntityArtilleryShell shell) { }
    }

    public static void standardExplosion(EntityArtilleryShell shell, MovingObjectPosition mop, float size, float rangeMod, boolean breaksBlocks) {
        Vec3NT vec = new Vec3NT(shell.getDeltaMovement()).normalize();
        ExplosionVNT xnt = new ExplosionVNT(shell.level(), mop.hitVec.xCoord - vec.xCoord, mop.hitVec.yCoord - vec.yCoord, mop.hitVec.zCoord - vec.zCoord, size);
        if (breaksBlocks) {
            xnt.setBlockAllocator(new BlockAllocatorStandard(48));
            // Original: BlockMutatorDebris(block_slag, 1) - Metadaten gibt es im Port nicht
            xnt.setBlockProcessor(new BlockProcessorStandard().setNoDrop().withBlockEffect(new BlockMutatorDebris(ModBlocks.BLOCK_SLAG.get())));
        }
        xnt.setEntityProcessor(new EntityProcessorCross(7.5D).withRangeMod(rangeMod));
        xnt.setPlayerProcessor(new PlayerProcessorStandard());
        xnt.explode();
        shell.killAndClear();
    }

    public static void standardCluster(EntityArtilleryShell shell, int clusterType, int amount, double splitHeight, double deviation) {
        if (!shell.getWhistle() || shell.getDeltaMovement().y > 0) return;
        if (shell.getTargetHeight() + splitHeight < shell.getY()) return;

        shell.killAndClear();

        CompoundTag data = new CompoundTag();
        data.putString("type", "plasmablast");
        data.putFloat("r", 1.0F);
        data.putFloat("g", 1.0F);
        data.putFloat("b", 1.0F);
        data.putFloat("scale", 50F);
        if (shell.level() instanceof ServerLevel server) IParticleCreator.sendPacket(server, shell.getX(), shell.getY(), shell.getZ(), 500, data);

        for (int i = 0; i < amount; i++) {
            EntityArtilleryShell cluster = new EntityArtilleryShell(ModEntities.ARTILLERY_SHELL.get(), shell.level());
            cluster.setType(clusterType);
            double mx = i == 0 ? shell.getDeltaMovement().x : (shell.getDeltaMovement().x + rand.nextGaussian() * deviation);
            double my = shell.getDeltaMovement().y;
            double mz = i == 0 ? shell.getDeltaMovement().z : (shell.getDeltaMovement().z + rand.nextGaussian() * deviation);
            cluster.setDeltaMovement(mx, my, mz);
            cluster.moveTo(shell.getX(), shell.getY(), shell.getZ(), shell.getYRot(), shell.getXRot());
            double[] target = shell.getTarget();
            cluster.setTarget(target[0], target[1], target[2]);
            cluster.setWhistle(shell.getWhistle() && !shell.didWhistle());
            shell.level().addFreshEntity(cluster);
        }
    }

    private static void phosphorusSplash(Entity source, double cx, double cy, double cz, int radius) {
        List<Entity> hit = source.level().getEntities(source, new AABB(cx - radius, cy - radius, cz - radius, cx + radius, cy + radius, cz + radius));
        for (Entity e : hit) {
            PlatformHooks.setSecondsOnFire(e, 5);
            if (e instanceof LivingEntity living) {
                MobEffectInstance eff = new MobEffectInstance(ModEffects.PHOSPHORUS.get(), 30 * 20, 0, true, true);
                //? if forge {
                eff.setCurativeItems(new java.util.ArrayList<>());
                //?}
                living.addEffect(eff);
            }
        }
    }

    /** Original: Haze-/RBMK-Pilz-Pakete nach dem Phosphoraufschlag. */
    static void sendFx(Entity e, String type, double x, double y, double z, int range, float scale) {
        if (!(e.level() instanceof ServerLevel server)) return;
        CompoundTag data = new CompoundTag();
        data.putString("type", type);
        if (scale > 0) data.putFloat("scale", scale);
        IParticleCreator.sendPacket(server, x, y, z, range, data);
    }

    private static void composeEffect(EntityArtilleryShell shell, MovingObjectPosition mop, int cloudCount, float cloudScale, float cloudSpeedMult, float waveScale,
            int debrisCount, int debrisSize, int debrisRetry, float debrisVelocity, float debrisHorizontalDeviation, float debrisVerticalOffset, float soundRange) {
        if (shell.level() instanceof ServerLevel server) {
            ExplosionCreator.composeEffect(server, mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord, cloudCount, cloudScale, cloudSpeedMult, waveScale,
                    debrisCount, debrisSize, debrisRetry, debrisVelocity, debrisHorizontalDeviation, debrisVerticalOffset, soundRange);
        }
    }

    private static void gas(EntityArtilleryShell shell, MovingObjectPosition mop, FluidType fluid, int count, double spread, double yOff, float width, float height) {
        Vec3NT vec = new Vec3NT(shell.getDeltaMovement()).normalize();
        for (int i = 0; i < count; i++) {
            EntityMist mist = new EntityMist(ModEntities.ENTITY_MIST.get(), shell.level());
            mist.setFluidType(fluid);
            double x = mop.hitVec.xCoord - vec.xCoord;
            double z = mop.hitVec.zCoord - vec.zCoord;
            if (i > 0) {
                x += rand.nextGaussian() * spread;
                z += rand.nextGaussian() * spread;
            }
            mist.setPos(x, mop.hitVec.yCoord - vec.yCoord - yOff, z);
            mist.setArea(width, height);
            shell.level().addFreshEntity(mist);
        }
    }

    private static void gasBlast(EntityArtilleryShell shell, MovingObjectPosition mop) {
        shell.killAndClear();
        Vec3NT vec = new Vec3NT(shell.getDeltaMovement()).normalize();
        shell.level().explode(shell, mop.hitVec.xCoord - vec.xCoord, mop.hitVec.yCoord - vec.yCoord, mop.hitVec.zCoord - vec.zCoord, 5F, Level.ExplosionInteraction.NONE);
    }

    static {
        /* STANDARD SHELLS */
        itemTypes[NORMAL] = new ArtilleryShell("ammo_arty", SpentCasing.COLOR_CASE_16INCH) { public void onImpact(EntityArtilleryShell shell, MovingObjectPosition mop) { standardExplosion(shell, mop, 10F, 3F, false); composeEffect(shell, mop, 10, 2F, 0.5F, 25F, 5, 0, 20, 0.75F, 1F, -2F, 150); }};
        itemTypes[CLASSIC] = new ArtilleryShell("ammo_arty_classic", SpentCasing.COLOR_CASE_16INCH) { public void onImpact(EntityArtilleryShell shell, MovingObjectPosition mop) { standardExplosion(shell, mop, 15F, 5F, false); composeEffect(shell, mop, 15, 5F, 1F, 45F, 10, 0, 50, 1F, 3F, -2F, 200); }};
        itemTypes[EXPLOSIVE] = new ArtilleryShell("ammo_arty_he", SpentCasing.COLOR_CASE_16INCH) { public void onImpact(EntityArtilleryShell shell, MovingObjectPosition mop) { standardExplosion(shell, mop, 15F, 3F, true); composeEffect(shell, mop, 15, 5F, 1F, 45F, 10, 16, 50, 1F, 3F, -2F, 200); }};

        /* MINI NUKE */
        itemTypes[MINI_NUKE] = new ArtilleryShell("ammo_arty_mini_nuke", SpentCasing.COLOR_CASE_16INCH_NUKE) {
            public void onImpact(EntityArtilleryShell shell, MovingObjectPosition mop) {
                shell.killAndClear();
                Vec3NT vec = new Vec3NT(shell.getDeltaMovement()).normalize();
                ExplosionNukeSmall.explode(shell.level(), mop.hitVec.xCoord - vec.xCoord, mop.hitVec.yCoord - vec.yCoord, mop.hitVec.zCoord - vec.zCoord, ExplosionNukeSmall.PARAMS_MEDIUM);
            }
        };

        /* FULL NUKE */
        itemTypes[NUKE] = new ArtilleryShell("ammo_arty_nuke", SpentCasing.COLOR_CASE_16INCH_NUKE) {
            public void onImpact(EntityArtilleryShell shell, MovingObjectPosition mop) {
                int radius = com.hbm_m.config.ModClothConfig.get().missileRadius;
                // Original: spawnEntityInWorld(EntityNukeExplosionMK5.statFac(...)) - start() erzeugt und spawnt in einem Schritt
                EntityNukeExplosionMK5.start(shell.level(), radius, mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord);
                com.hbm_m.particle.helper.NukeTorexCreator.statFacStandard(shell.level(), mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord, radius);
                shell.discard();
            }
        };

        /* PHOSPHORUS */
        itemTypes[PHOSPHORUS] = new ArtilleryShell("ammo_arty_phosphorus", SpentCasing.COLOR_CASE_16INCH_PHOS) {
            public void onImpact(EntityArtilleryShell shell, MovingObjectPosition mop) {
                shell.level().playSound(null, shell.getX(), shell.getY(), shell.getZ(), HbmSoundsNT.get("hbm:weapon.explosionMedium"), SoundSource.NEUTRAL, 20.0F, 0.9F + rand.nextFloat() * 0.2F);
                standardExplosion(shell, mop, 10F, 3F, false);
                ExplosionLarge.spawnShrapnels(shell.level(), (int) mop.hitVec.xCoord, (int) mop.hitVec.yCoord, (int) mop.hitVec.zCoord, 15);
                ExplosionChaos.igniteAllBlocks(shell.level(), (int) mop.hitVec.xCoord, (int) mop.hitVec.yCoord, (int) mop.hitVec.zCoord, 12);
                phosphorusSplash(shell, shell.getX(), shell.getY(), shell.getZ(), 15);
                for (int i = 0; i < 5; i++) {
                    sendFx(shell, "haze", mop.hitVec.xCoord + shell.level().random.nextGaussian() * 10, mop.hitVec.yCoord, mop.hitVec.zCoord + shell.level().random.nextGaussian() * 10, 150, 0);
                }
                sendFx(shell, "rbmkmush", mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord, 250, 10);
            }
        };

        /* THIS DOOFUS */
        itemTypes[CARGO] = new ArtilleryShell("ammo_arty_cargo", SpentCasing.COLOR_CASE_16INCH) { public void onImpact(EntityArtilleryShell shell, MovingObjectPosition mop) {
            if (mop.typeOfHit == MovingObjectType.BLOCK) {
                shell.setPos(mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord);
                shell.getStuck(mop.getBlockPos(), mop.sideHit);
            }
        }};

        /* GAS */
        itemTypes[CHLORINE] = new ArtilleryShell("ammo_arty_chlorine", SpentCasing.COLOR_CASE_16INCH) {
            public void onImpact(EntityArtilleryShell shell, MovingObjectPosition mop) {
                gasBlast(shell, mop);
                gas(shell, mop, FluidType.forFluid(ModFluids.CHLORINE.getSource()), 1, 0, 3, 15, 7.5F);
                PollutionHandler.incrementPollution(shell.level(), mop.blockX, mop.blockY, mop.blockZ, PollutionType.HEAVYMETAL, 5F);
            }
        };
        itemTypes[PHOSGENE] = new ArtilleryShell("ammo_arty_phosgene", SpentCasing.COLOR_CASE_16INCH_NUKE) {
            public void onImpact(EntityArtilleryShell shell, MovingObjectPosition mop) {
                gasBlast(shell, mop);
                gas(shell, mop, FluidType.forFluid(ModFluids.PHOSGENE.getSource()), 3, 15, 5, 15, 10);
                PollutionHandler.incrementPollution(shell.level(), mop.blockX, mop.blockY, mop.blockZ, PollutionType.HEAVYMETAL, 10F);
                PollutionHandler.incrementPollution(shell.level(), mop.blockX, mop.blockY, mop.blockZ, PollutionType.POISON, 15F);
            }
        };
        itemTypes[MUSTARD] = new ArtilleryShell("ammo_arty_mustard_gas", SpentCasing.COLOR_CASE_16INCH_NUKE) {
            public void onImpact(EntityArtilleryShell shell, MovingObjectPosition mop) {
                gasBlast(shell, mop);
                gas(shell, mop, FluidType.forFluid(ModFluids.MUSTARDGAS.getSource()), 5, 25, 5, 20, 10);
                PollutionHandler.incrementPollution(shell.level(), mop.blockX, mop.blockY, mop.blockZ, PollutionType.HEAVYMETAL, 15F);
                PollutionHandler.incrementPollution(shell.level(), mop.blockX, mop.blockY, mop.blockZ, PollutionType.POISON, 30F);
            }
        };

        /* CLUSTER SHELLS */
        itemTypes[PHOSPHORUS_MULTI] = new ArtilleryShell("ammo_arty_phosphorus_multi", SpentCasing.COLOR_CASE_16INCH_PHOS) {
            public void onImpact(EntityArtilleryShell shell, MovingObjectPosition mop) { itemTypes[PHOSPHORUS].onImpact(shell, mop); }
            public void onUpdate(EntityArtilleryShell shell) { standardCluster(shell, PHOSPHORUS, 10, 300, 5); }
        };
        itemTypes[MINI_NUKE_MULTI] = new ArtilleryShell("ammo_arty_mini_nuke_multi", SpentCasing.COLOR_CASE_16INCH_NUKE) {
            public void onImpact(EntityArtilleryShell shell, MovingObjectPosition mop) { itemTypes[MINI_NUKE].onImpact(shell, mop); }
            public void onUpdate(EntityArtilleryShell shell) { standardCluster(shell, MINI_NUKE, 5, 300, 5); }
        };
    }
}
