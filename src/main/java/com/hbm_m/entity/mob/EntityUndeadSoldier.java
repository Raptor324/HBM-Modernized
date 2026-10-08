package com.hbm_m.entity.mob;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.entity.ModEntities;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.weapon.sedna.WeaponItems;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code com.hbm.entity.mob.EntityUndeadSoldier} ("entity_ntm_undead_soldier"): Zombie- oder Skelettsoldat in
 * voller Taurun-Ruestung mit einer von fuenf Waffen. Wie im Original ohne Angriffsaufgabe (er sucht sich Ziele, greift
 * aber nicht an) und ohne jegliche Drops.
 */
public class EntityUndeadSoldier extends Monster {

    /** Original DataWatcher-Slot 12. */
    public static final EntityDataAccessor<Byte> DW_TYPE = SynchedEntityData.defineId(EntityUndeadSoldier.class, EntityDataSerializers.BYTE);
    public static final byte TYPE_ZOMBIE = 0;
    public static final byte TYPE_SKELETON = 1;

    public EntityUndeadSoldier(EntityType<? extends EntityUndeadSoldier> type, Level world) {
        super(type, world);
    }

    public EntityUndeadSoldier(Level world) {
        this(ModEntities.UNDEAD_SOLDIER.get(), world);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(4, new RandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Villager.class, true));
    }

    @Override
    //? if < 1.21.1 {
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DW_TYPE, (byte) 0);
    }
    //?} else {
    /*protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DW_TYPE, (byte) 0);
    }
    *///?}

    public byte getSoldierType() {
        return this.entityData.get(DW_TYPE);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor level, @NotNull DifficultyInstance difficulty, @NotNull MobSpawnType reason, @Nullable SpawnGroupData data, @Nullable net.minecraft.nbt.CompoundTag tag) {
        this.addRandomArmor();
        this.entityData.set(DW_TYPE, random.nextBoolean() ? TYPE_ZOMBIE : TYPE_SKELETON);
        return super.finalizeSpawn(level, difficulty, reason, data, tag);
    }

    /** Original {@code addRandomArmor}: feste Taurun-Ruestung und eine zufaellige Waffe. */
    protected void addRandomArmor() {
        this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.TAURUN_HELMET.get()));
        this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.TAURUN_PLATE.get()));
        this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.TAURUN_LEGS.get()));
        this.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.TAURUN_BOOTS.get()));

        int gun = random.nextInt(5);
        if (gun == 0) this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(WeaponItems.gun("gun_heavy_revolver")));
        if (gun == 1) this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(WeaponItems.gun("gun_light_revolver")));
        if (gun == 2) this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(WeaponItems.gun("gun_carbine")));
        if (gun == 3) this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(WeaponItems.gun("gun_maresleg")));
        if (gun == 4) this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(WeaponItems.gun("gun_greasegun")));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        byte type = getSoldierType();
        if (type == TYPE_ZOMBIE) return SoundEvents.ZOMBIE_AMBIENT;
        if (type == TYPE_SKELETON) return SoundEvents.SKELETON_AMBIENT;
        return super.getAmbientSound();
    }

    @Override
    protected SoundEvent getHurtSound(@NotNull DamageSource source) {
        byte type = getSoldierType();
        if (type == TYPE_ZOMBIE) return SoundEvents.ZOMBIE_HURT;
        if (type == TYPE_SKELETON) return SoundEvents.SKELETON_HURT;
        return super.getHurtSound(source);
    }

    @Override
    protected SoundEvent getDeathSound() {
        byte type = getSoldierType();
        if (type == TYPE_ZOMBIE) return SoundEvents.ZOMBIE_DEATH;
        if (type == TYPE_SKELETON) return SoundEvents.SKELETON_DEATH;
        return super.getDeathSound();
    }

    @Override
    protected void playStepSound(@NotNull BlockPos pos, @NotNull BlockState block) {
        byte type = getSoldierType();
        if (type == TYPE_ZOMBIE) this.playSound(SoundEvents.ZOMBIE_STEP, 0.15F, 1.0F);
        if (type == TYPE_SKELETON) this.playSound(SoundEvents.SKELETON_STEP, 0.15F, 1.0F);
    }

    @Override
    public @NotNull MobType getMobType() {
        return MobType.UNDEAD;
    }

    /** Original {@code getCanSpawnHere}: nicht friedlich, frei von Entities/Bloecken/Fluessigkeit - ohne Lichtpruefung. */
    @Override
    public boolean checkSpawnRules(@NotNull LevelAccessor level, @NotNull MobSpawnType reason) {
        return level.getDifficulty() != Difficulty.PEACEFUL;
    }

    @Override
    public boolean checkSpawnObstruction(@NotNull net.minecraft.world.level.LevelReader level) {
        return level.isUnobstructed(this) && level.noCollision(this) && !level.containsAnyLiquid(this.getBoundingBox());
    }

    /** Original {@code dropFewItems}/{@code dropEquipment}: leer. */
    @Override
    protected void dropCustomDeathLoot(@NotNull DamageSource source, int looting, boolean player) { }
}
