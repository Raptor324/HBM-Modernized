package com.hbm_m.entity.missile;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.bomb.BlockTaint;
import com.hbm_m.explosion.ExplosionChaos;
import com.hbm_m.explosion.ExplosionLarge;
import com.hbm_m.item.missile.ItemCustomMissilePart;
import com.hbm_m.item.missile.ItemCustomMissilePart.FuelType;
import com.hbm_m.item.missile.ItemCustomMissilePart.PartSize;
import com.hbm_m.item.missile.ItemCustomMissilePart.WarheadType;
import com.hbm_m.item.missile.MissileStruct;

import api.hbm_m.entity.IRadarDetectable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code EntityMissileCustom}: Baukasten-Rakete. Sprengkopf, Rumpf, Leitwerk und Triebwerk werden synchronisiert
 * (Renderer, Abgasart); der Treibstoff des Rumpfs wird pro Tick um den Verbrauch des Triebwerks gesenkt, danach faellt
 * die Rakete antriebslos. Der Aufschlag richtet sich nach dem Sprengkopftyp.
 */
public class EntityMissileCustom extends MissileBaseEntity {

    private static final EntityDataAccessor<String> WARHEAD = SynchedEntityData.defineId(EntityMissileCustom.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> FUSELAGE = SynchedEntityData.defineId(EntityMissileCustom.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> FINS = SynchedEntityData.defineId(EntityMissileCustom.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> THRUSTER = SynchedEntityData.defineId(EntityMissileCustom.class, EntityDataSerializers.STRING);

    public float fuel;
    public float consumption;

    public EntityMissileCustom(EntityType<? extends EntityMissileCustom> type, Level level) {
        super(type, level);
    }

    public void setup(double x, double y, double z, int a, int b, MissileStruct template) {
        this.initLaunch(x, y, z, a, b);

        this.entityData.set(WARHEAD, id(template.warhead));
        this.entityData.set(FUSELAGE, id(template.fuselage));
        this.entityData.set(THRUSTER, id(template.thruster));
        this.entityData.set(FINS, template.fins != null ? id(template.fins) : "");

        this.fuel = (Float) template.fuselage.attributes[1];
        this.consumption = (Float) template.thruster.attributes[1];
    }

    private static String id(@Nullable Item item) {
        if (item == null) return "";
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
        return key == null ? "" : key.toString();
    }

    @Nullable
    private static ItemCustomMissilePart part(String id) {
        if (id == null || id.isEmpty()) return null;
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) return null;
        return BuiltInRegistries.ITEM.get(rl) instanceof ItemCustomMissilePart p ? p : null;
    }

    /** Der Verbund fuer Renderer und Radar. */
    public MissileStruct getStruct() {
        return new MissileStruct(part(entityData.get(WARHEAD)), part(entityData.get(FUSELAGE)), part(entityData.get(FINS)), part(entityData.get(THRUSTER)));
    }

    //? if < 1.21.1 {
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(WARHEAD, "");
        this.entityData.define(FUSELAGE, "");
        this.entityData.define(FINS, "");
        this.entityData.define(THRUSTER, "");
    }
    //?} else {
    /*@Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(WARHEAD, "");
        builder.define(FUSELAGE, "");
        builder.define(FINS, "");
        builder.define(THRUSTER, "");
    }
    *///?}

    @Override
    protected void killMissile() {
        if (!this.isRemoved()) {
            releaseChunkTicket();
            this.discard();
            Vec3 m = getDeltaMovement();
            ExplosionLarge.explode(level(), getX(), getY(), getZ(), 5, true, false, true);
            ExplosionLarge.spawnShrapnelShower(level(), getX(), getY(), getZ(), m.x, m.y, m.z, 15, 0.075);
        }
    }

    @Override
    public void tick() {
        ItemCustomMissilePart part = part(entityData.get(WARHEAD));
        WarheadType type = part != null ? (WarheadType) part.attributes[0] : null;
        if (type != null && type.updateCustom != null) {
            type.updateCustom.accept(this);
        }

        if (!level().isClientSide) {
            if (this.hasPropulsion()) this.fuel -= this.consumption;
        }

        super.tick();
    }

    @Override
    protected boolean hasPropulsion() {
        return this.fuel > 0;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        fuel = nbt.getFloat("fuel");
        consumption = nbt.getFloat("consumption");
        this.entityData.set(WARHEAD, nbt.getString("warhead"));
        this.entityData.set(FUSELAGE, nbt.getString("fuselage"));
        this.entityData.set(FINS, nbt.getString("fins"));
        this.entityData.set(THRUSTER, nbt.getString("thruster"));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putFloat("fuel", fuel);
        nbt.putFloat("consumption", consumption);
        nbt.putString("warhead", entityData.get(WARHEAD));
        nbt.putString("fuselage", entityData.get(FUSELAGE));
        nbt.putString("fins", entityData.get(FINS));
        nbt.putString("thruster", entityData.get(THRUSTER));
    }

    /** {@code spawnContrail}: Abgasfahne je Treibstoff des Rumpfs (Xenon ohne). */
    @Override
    protected void spawnContrail() {
        ItemCustomMissilePart part = part(entityData.get(FUSELAGE));
        if (part == null) return;
        String smoke = switch ((FuelType) part.attributes[0]) {
            case BALEFIRE -> "exBalefire";
            case HYDROGEN -> "exHydrogen";
            case KEROSENE -> "exKerosene";
            case SOLID -> "exSolid";
            case XENON -> "";
        };
        if (smoke.isEmpty()) return;

        Vec3 v = getDeltaMovement().normalize();
        for (int i = 0; i < velocity; i++) {
            CompoundTag data = new CompoundTag();
            data.putDouble("posX", getX() - v.x * i);
            data.putDouble("posY", getY() - v.y * i);
            data.putDouble("posZ", getZ() - v.z * i);
            data.putString("type", smoke);
            com.hbm_m.particle.helper.ParticleEffectClient.effectNT(data);
        }
    }

    @Override
    protected void onMissileImpact(BlockPos pos) { //TODO: demolish this steaming pile of shit
        if (!(level() instanceof ServerLevel server)) return;

        ItemCustomMissilePart part = part(entityData.get(WARHEAD));
        if (part == null) return;

        WarheadType type = (WarheadType) part.attributes[0];
        float strength = (Float) part.attributes[1];

        if (type.impactCustom != null) {
            type.impactCustom.accept(this);
            return;
        }

        double x = getX(), y = getY(), z = getZ();
        Vec3 m = getDeltaMovement();

        switch (type) {
            case HE -> {
                ExplosionLarge.explode(server, x, y, z, strength, true, false, true);
                ExplosionLarge.jolt(server, x, y, z, strength, (int) (strength * 50), 0.25);
            }
            case INC -> {
                ExplosionLarge.explodeFire(server, x, y, z, strength, true, false, true);
                ExplosionLarge.jolt(server, x, y, z, strength * 1.5, (int) (strength * 50), 0.25);
            }
            case CLUSTER -> { }
            case BUSTER -> ExplosionLarge.buster(server, x, y, z, m, strength, strength * 4);
            case NUCLEAR, TX -> com.hbm_m.explosion.NuclearExplosionAPI.start(server, x, y, z,
                    com.hbm_m.explosion.NuclearExplosionConfig.builder((int) strength).fallout(true).radiation(true).mushroomType(0).build());
            case BALEFIRE -> {
                var bf = com.hbm_m.entity.logic.EntityBalefireExplosion.statFac(server, x, y, z, (int) strength);
                server.addFreshEntity(bf);
            }
            case N2 -> com.hbm_m.explosion.NuclearExplosionAPI.start(server, x, y, z,
                    com.hbm_m.explosion.NuclearExplosionConfig.builder((int) strength).fallout(false).radiation(false).mushroomType(0).build());
            case TAINT -> {
                int r = (int) strength;
                for (int i = 0; i < r * 10; i++) {
                    int a = random.nextInt(r) + (int) x - (r / 2 - 1);
                    int b = random.nextInt(r) + (int) y - (r / 2 - 1);
                    int c = random.nextInt(r) + (int) z - (r / 2 - 1);
                    BlockPos p = new BlockPos(a, b, c);
                    var state = server.getBlockState(p);
                    if (state.isRedstoneConductor(server, p) && !state.isAir()) {
                        server.setBlock(p, ModBlocks.TAINT.get().defaultBlockState().setValue(BlockTaint.AGE, random.nextInt(3) + 4), 2);
                    }
                }
            }
            case CLOUD -> {
                server.levelEvent(2002, BlockPos.containing(Math.round(x), Math.round(y), Math.round(z)), 0);
                ExplosionChaos.spawnPoisonCloud(server, x - m.x, y - m.y, z - m.z, 750, 2.5, 2);
            }
            case TURBINE -> {
                ExplosionLarge.explode(server, x, y, z, 10, true, false, true);
                // 1:1: strength Turbinenblaetter (EntityBulletBaseNT, TURBINE) im Kreis
                int count = (int) strength;
                net.minecraft.world.phys.Vec3 vec = new net.minecraft.world.phys.Vec3(0.5, 0, 0);
                for (int i = 0; i < count; i++) {
                    com.hbm_m.entity.projectile.EntityBulletBaseNT blade = com.hbm_m.entity.projectile.EntityBulletBaseNT.create(server,
                            com.hbm_m.handler.BulletConfigSyncingUtil.TURBINE);
                    blade.moveTo(x - m.x, y - m.y + random.nextGaussian(), z - m.z, 0, 0);
                    blade.setDeltaMovement(vec.x, blade.getDeltaMovement().y, vec.z);
                    server.addFreshEntity(blade);
                    vec = vec.yRot((float) (Math.PI * 2F / (float) count));
                }
            }
            default -> { }
        }
    }

    @Override
    public IRadarDetectable.RadarTargetType getTargetType() {
        ItemCustomMissilePart part = part(entityData.get(FUSELAGE));
        if (part == null) return IRadarDetectable.RadarTargetType.MISSILE_TIER1;
        PartSize top = part.top, bottom = part.bottom;
        if (top == PartSize.SIZE_10 && bottom == PartSize.SIZE_10) return IRadarDetectable.RadarTargetType.MISSILE_10;
        if (top == PartSize.SIZE_10 && bottom == PartSize.SIZE_15) return IRadarDetectable.RadarTargetType.MISSILE_10_15;
        if (top == PartSize.SIZE_15 && bottom == PartSize.SIZE_15) return IRadarDetectable.RadarTargetType.MISSILE_15;
        if (top == PartSize.SIZE_15 && bottom == PartSize.SIZE_20) return IRadarDetectable.RadarTargetType.MISSILE_15_20;
        if (top == PartSize.SIZE_20 && bottom == PartSize.SIZE_20) return IRadarDetectable.RadarTargetType.MISSILE_20;
        return IRadarDetectable.RadarTargetType.MISSILE_TIER1;
    }

    @Override protected java.util.List<net.minecraft.world.item.ItemStack> getDebris() { return new java.util.ArrayList<>(); }
    @Override protected net.minecraft.world.item.ItemStack getDebrisRareDrop() { return net.minecraft.world.item.ItemStack.EMPTY; }
}
