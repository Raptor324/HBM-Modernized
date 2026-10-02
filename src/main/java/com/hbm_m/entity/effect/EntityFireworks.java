package com.hbm_m.entity.effect;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.entity.ModEntities;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** 1:1 {@code EntityFireworks}: steigt 30 Ticks mit 3 Bloecken/Tick und zerplatzt zu einem Buchstaben in Farbe. */
public class EntityFireworks extends Entity {

    int color;
    int character;
    private int age;

    public EntityFireworks(EntityType<? extends EntityFireworks> type, Level world) {
        super(type, world);
    }

    public EntityFireworks(Level world, double x, double y, double z, int color, int character) {
        this(ModEntities.FIREWORKS.get(), world);
        this.moveTo(x, y, z, 0.0F, 0.0F);
        this.color = color;
        this.character = character;
    }

    @Override
    protected void defineSynchedData() { }

    @Override
    public void tick() {
        this.move(MoverType.SELF, new Vec3(0.0, 3.0D, 0.0));
        this.level().addParticle(ParticleTypes.FLAME, getX(), getY(), getZ(), 0.0, -0.3, 0.0);
        this.level().addParticle(ParticleTypes.SMOKE, getX(), getY(), getZ(), 0.0, -0.2, 0.0);

        if (!this.level().isClientSide) {
            age++;
            if (age > 30) {
                this.level().playSound(null, getX(), getY(), getZ(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.AMBIENT, 20, 1F + this.random.nextFloat() * 0.2F);
                this.discard();
                CompoundTag data = new CompoundTag();
                data.putString("type", "fireworks");
                data.putInt("color", color);
                data.putInt("char", character);
                com.hbm_m.particle.helper.IParticleCreator.sendPacket((ServerLevel) this.level(), getX(), getY(), getZ(), 300, data);
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag nbt) {
        this.character = nbt.getInt("char");
        this.color = nbt.getInt("color");
        this.age = nbt.getInt("ticksExisted");
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag nbt) {
        nbt.putInt("char", character);
        nbt.putInt("color", color);
        nbt.putInt("ticksExisted", age);
    }

    //? if < 1.21.1 {
    @NotNull
    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new net.minecraft.network.protocol.game.ClientboundAddEntityPacket(this);
    }
    //?} else {
    /*@NotNull
    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(net.minecraft.server.level.ServerEntity serverEntity) {
        return new net.minecraft.network.protocol.game.ClientboundAddEntityPacket(this, serverEntity);
    }
    *///?}
}
