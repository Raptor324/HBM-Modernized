package com.hbm_m.entity.mob;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.config.MobConfig;
import com.hbm_m.entity.mob.ai.BreakingGoal;
import com.hbm_m.entity.pathfinder.PathFinderUtils;
import com.hbm_m.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code EntityFBI}: FBI-Agent der Razzia. Bricht Tueren und Bloecke im Weg, zerstoert Maschinen in Reichweite
 * ({@code raidAttackReach}, alle {@code raidAttackDelay} Ticks), zuendet herumliegende Gegenstaende an, haelt sich
 * immer den naechsten Spieler (128) als Ziel und laeuft mit Teilpfaden an. Ruestungswert 20, feuerfest, keine
 * Trankwirkung (setzt sich stattdessen eine M65 auf), verschwindet nie.
 * Bewaffnung {@code gun_heavy_revolver}/{@code gun_spas12} (SEDNA); der Fernkampf des Originals ist leer.
 */
public class EntityFBI extends Monster implements RangedAttackMob {

    public EntityFBI(EntityType<? extends EntityFBI> type, Level world) {
        super(type, world);
        if (this.getNavigation() instanceof GroundPathNavigation nav) nav.setCanOpenDoors(true);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new BreakingGoal(this));
        this.goalSelector.addGoal(2, new RangedAttackGoal(this, 1D, 20, 25, 15.0F));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new RandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 0, false, false, null));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {

        if (source.getDirectEntity() != source.getEntity() && source.getEntity() instanceof EntityFBI) {
            return false;
        }

        if (this.getItemBySlot(EquipmentSlot.HEAD).is(Blocks.GLASS.asItem())) {
            if ("oxygenSuffocation".equals(source.getMsgId()))
                return false;
            if ("thermal".equals(source.getMsgId()))
                return false;
        }

        return super.hurt(source, amount);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    /** Original {@code addRandomArmor}: 1/5 Sicherheitsruestung, ausserhalb der Oberwelt Glashelm + PAA. */
    protected void addRandomArmor() {

        int equip = random.nextInt(2);

        switch (equip) {
            case 0 -> this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(com.hbm_m.item.weapon.sedna.WeaponItems.gun("gun_heavy_revolver")));
            case 1 -> this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(com.hbm_m.item.weapon.sedna.WeaponItems.gun("gun_spas12")));
        }

        if (random.nextInt(5) == 0) {
            this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.SECURITY_HELMET.get()));
            this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.SECURITY_PLATE.get()));
            this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.SECURITY_LEGS.get()));
            this.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.SECURITY_BOOTS.get()));
        }

        if (level().dimension() != Level.OVERWORLD) {
            this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Blocks.GLASS));
            this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.PAA_PLATE.get()));
            this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.PAA_LEGS.get()));
            this.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.PAA_BOOTS.get()));
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();

        if (this.getTarget() == null) {
            Player p = level().getNearestPlayer(this, 128.0D);
            if (p != null && !p.isCreative() && !p.isSpectator()) this.setTarget(p);
        }

        // hell yeah!!
        if (this.getTarget() != null) {
            var path = PathFinderUtils.getPathEntityToEntityPartial(level(), this, this.getTarget(), 16F);
            if (path != null) this.getNavigation().moveTo(path, 1);
        }
    }

    //combat vest = full diamond set
    @Override
    public int getArmorValue() {
        return 20;
    }

    @Override
    public void performRangedAttack(@NotNull LivingEntity entity, float f) { }

    private static Set<Block> canDestroy;

    private static Set<Block> canDestroy() {
        if (canDestroy != null) return canDestroy;
        Set<Block> s = new HashSet<>();
        s.add(Blocks.OAK_DOOR);
        s.add(Blocks.IRON_DOOR);
        s.add(Blocks.OAK_TRAPDOOR);
        for (String n : new String[] { "press", "epress", "chemical_plant", "chemical_factory", "crystallizer", "steam_turbine", "industrial_turbine",
                "machine_chungus", "purex", "crate_iron", "crate_steel", "dieselgen", "machine_rtg", "cyclotron" }) {
            Block b = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.hbm_m.lib.RefStrings.MODID, n));
            if (b != Blocks.AIR) s.add(b);
        }
        s.add(Blocks.CHEST);
        s.add(Blocks.TRAPPED_CHEST);
        canDestroy = s;
        return s;
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor level, @NotNull DifficultyInstance difficulty, @NotNull MobSpawnType reason, @Nullable SpawnGroupData data, @Nullable net.minecraft.nbt.CompoundTag tag) {
        this.addRandomArmor();
        return super.finalizeSpawn(level, difficulty, reason, data, tag);
    }

    /** Original {@code isPotionApplicable}: nie; ohne Helm setzt er stattdessen eine M65 auf. */
    @Override
    public boolean canBeAffected(@NotNull MobEffectInstance potion) {
        if (this.getItemBySlot(EquipmentSlot.HEAD).isEmpty())
            this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.GAS_MASK_M65.get()));

        return false;
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (level().isClientSide || this.getHealth() <= 0)
            return;

        if (this.tickCount % MobConfig.raidAttackDelay() == 0) {
            Vec3 vec = new Vec3(MobConfig.raidAttackReach(), 0, 0).yRot((float) (Math.PI * 2) * random.nextFloat());

            Vec3 vec3 = new Vec3(getX(), getY() + 0.5 + random.nextFloat(), getZ());
            Vec3 vec31 = vec3.add(vec);
            BlockHitResult mop = level().clip(new ClipContext(vec3, vec31, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));

            if (mop.getType() == HitResult.Type.BLOCK) {
                BlockPos p = mop.getBlockPos();
                if (canDestroy().contains(level().getBlockState(p).getBlock()))
                    level().destroyBlock(p, false);
            }
        }

        double range = 1.5;

        List<ItemEntity> items = level().getEntitiesOfClass(ItemEntity.class, new AABB(getX(), getY(), getZ(), getX(), getY(), getZ()).inflate(range, range, range));

        for (ItemEntity item : items)
            item.setSecondsOnFire(10);
    }
}
