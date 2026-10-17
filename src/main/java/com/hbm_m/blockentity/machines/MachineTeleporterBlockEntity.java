package com.hbm_m.blockentity.machines;

import java.util.List;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Port of {@code TileEntityMachineTeleporter} (1.7.10 Original) - a fixed-destination area
 * teleporter. Has no GUI in the original (confirmed: {@code ContainerMachineTeleporter} exists but
 * is never wired to a screen anywhere) - the destination is set entirely via {@code ItemTeleLink}
 * (see {@link com.hbm_m.item.ItemTeleLink}), which records a position by sneak-right-clicking any
 * block, then applies it by right-clicking a teleporter.
 * <p>
 * Interdimensional wie im Original fuer Spieler und alle anderen Entities (dort per Reflection auf
 * EntityTracker/Netty geloest, hier ueber {@code Entity#teleportTo(ServerLevel, ...)}).
 */
public class MachineTeleporterBlockEntity extends BaseMachineBlockEntity implements com.hbm_m.api.fluids.IFluidStandardReceiverMK2 {

    private static final long MAX_POWER = 1_500_000L;
    private static final long CONSUMPTION = 1_000_000L;

    public int targetX = 0;
    public int targetY = -1;
    public int targetZ = 0;
    public String targetDim = "minecraft:overworld";

    /** E16 (Fork, nur mit Space-Addon): N-Masse fuer Dimensionswechsel (Space {@code flucu = 100}, Tank 16 000 mB). */
    public static final int FLUCU = 100;
    @org.jetbrains.annotations.Nullable
    public final com.hbm_m.inventory.fluid.tank.FluidTank tank;

    public MachineTeleporterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_TELEPORTER_BE.get(), pos, state, 0, MAX_POWER, MAX_POWER, 0L);
        net.minecraft.world.level.material.Fluid nmass = com.hbm_m.api.space.SpaceApi.forkActive() ? com.hbm_m.api.space.SpaceApi.fork().teleporterFluid() : null;
        this.tank = nmass == null ? null : new com.hbm_m.inventory.fluid.tank.FluidTank(nmass, 16000);
    }

    @Override
    public com.hbm_m.inventory.fluid.tank.FluidTank[] getAllTanks() {
        return tank == null ? new com.hbm_m.inventory.fluid.tank.FluidTank[0] : new com.hbm_m.inventory.fluid.tank.FluidTank[] { tank };
    }

    @Override
    public com.hbm_m.inventory.fluid.tank.FluidTank[] getReceivingTanks() {
        return getAllTanks();
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    private boolean crossDimension(Level level) {
        return !level.dimension().location().toString().equals(targetDim);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineTeleporterBlockEntity be) {
        if (level.isClientSide()) {
            // Original: blaue reddust-Partikel, solange ein Ziel gesetzt und genug Strom da ist
            if (be.targetY != -1 && be.getEnergyStored() >= CONSUMPTION && (be.tank == null || be.tank.getFill() >= FLUCU)) {
                double x = pos.getX() + 0.5 + level.random.nextGaussian() * 0.25D;
                double y = pos.getY() + 1 + level.random.nextDouble() * 2D;
                double z = pos.getZ() + 0.5 + level.random.nextGaussian() * 0.25D;
                level.addParticle(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0.4F, 0.8F, 1F), 1.0F), x, y, z, 0, 0, 0);
            }
            return;
        }
        if (!(level instanceof ServerLevel serverLevel)) return;
        be.ensureNetworkInitialized();
        if (be.tank != null) {
            for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) be.trySubscribe(be.tank.getTankType(), level, pos.relative(d), d);
        }
        if (level.getGameTime() % 15 == 0) be.sendUpdateToClient();
        if (be.targetY == -1) return;

        AABB box = new AABB(pos.getX() + 0.25, pos.getY(), pos.getZ() + 0.25,
                pos.getX() + 0.75, pos.getY() + 2, pos.getZ() + 0.75);
        List<Entity> entities = serverLevel.getEntitiesOfClass(Entity.class, box);
        for (Entity e : entities) {
            be.teleport(serverLevel, e);
        }
    }

    private void teleport(ServerLevel level, Entity entity) {
        if (getEnergyStored() < CONSUMPTION) return;
        // E16: N-Masse ist fuer jeden Dimensionswechsel noetig
        boolean cross = tank != null && crossDimension(level);
        if (cross && tank.getFill() < FLUCU) return;
        if (cross) tank.setFill(tank.getFill() - FLUCU);

        level.playSound(null, worldPosition.getX() + 0.5, worldPosition.getY() + 1.5, worldPosition.getZ() + 0.5, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1.0F, 1.0F);

        double tx = targetX + 0.5D, ty = targetY + 1.5D, tz = targetZ + 0.5D;
        ResourceKey<Level> destKey = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                net.minecraft.resources.ResourceLocation.tryParse(targetDim));
        ServerLevel destLevel = level.getServer().getLevel(destKey);
        if (destLevel == null) destLevel = level;

        if (entity instanceof ServerPlayer player) {
            if (destLevel == level) {
                player.teleportTo(tx, ty, tz);
            } else {
                player.teleportTo(destLevel, tx, ty, tz, entity.getYRot(), entity.getXRot());
            }
        } else if (destLevel == level) {
            entity.teleportTo(tx, ty, tz);
        } else {
            entity.teleportTo(destLevel, tx, ty, tz, java.util.Set.of(), entity.getYRot(), entity.getXRot());
        }

        // E16 (Fork, nur mit Space-Addon): "I have done nothing but teleport bread for three days" - Brot wird zum Schleim
        if (tank != null && entity instanceof net.minecraft.world.entity.item.ItemEntity item && item.getItem().is(net.minecraft.world.item.Items.BREAD)) {
            net.minecraft.world.entity.monster.Slime slime = net.minecraft.world.entity.EntityType.SLIME.create(destLevel);
            if (slime != null) {
                slime.moveTo(tx, ty, tz, entity.getYRot(), entity.getXRot());
                //? if < 1.21.1 {
                slime.finalizeSpawn(destLevel, destLevel.getCurrentDifficultyAt(slime.blockPosition()), net.minecraft.world.entity.MobSpawnType.SPAWN_EGG, null, null);
                //?} else {
                /*slime.finalizeSpawn(destLevel, destLevel.getCurrentDifficultyAt(slime.blockPosition()), net.minecraft.world.entity.MobSpawnType.SPAWN_EGG, null);
                *///?}
                destLevel.addFreshEntity(slime);
            }
        }

        // Original: zweiter Ton an der Entity-Position nach dem Sprung
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1.0F, 1.0F);

        setEnergyStored(getEnergyStored() - CONSUMPTION);
        setChanged();
    }

    public void setTarget(int x, int y, int z, String dimensionId) {
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
        this.targetDim = dimensionId;
        setChanged();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.machine_teleporter");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return null;
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putInt("targetX", targetX);
        tag.putInt("targetY", targetY);
        tag.putInt("targetZ", targetZ);
        tag.putString("targetDim", targetDim);
        if (tank != null) tank.writeToNBT(tag, "tt");
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        targetX = tag.getInt("targetX");
        targetY = tag.contains("targetY") ? tag.getInt("targetY") : -1;
        targetZ = tag.getInt("targetZ");
        targetDim = tag.contains("targetDim") ? tag.getString("targetDim") : "minecraft:overworld";
        if (tank != null) tank.readFromNBT(tag, "tt");
    }
}
