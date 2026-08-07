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
 * SCOPE-Vereinfachung: Interdimensionale Teleportation ist im Original ueber Reflection-Hacks auf
 * private Netty/EntityTracker-Felder gelost (1.7.10-spezifisch) - hier per moderner Forge-API
 * ({@code ServerPlayer#teleportTo}) fuer Spieler; nicht-Spieler-Entities werden nur innerhalb
 * derselben Dimension teleportiert (kein Dimensionswechsel fuer Mobs/Items - selten genutzter
 * Randfall, der Kernmechanismus fuer Spieler bleibt vollstaendig erhalten).
 */
public class MachineTeleporterBlockEntity extends BaseMachineBlockEntity {

    private static final long MAX_POWER = 1_500_000L;
    private static final long CONSUMPTION = 1_000_000L;

    public int targetX = 0;
    public int targetY = -1;
    public int targetZ = 0;
    public String targetDim = "minecraft:overworld";

    public MachineTeleporterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_TELEPORTER_BE.get(), pos, state, 0, MAX_POWER, MAX_POWER, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineTeleporterBlockEntity be) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return;
        if (be.targetY == -1) return;
        if (be.getEnergyStored() < CONSUMPTION) return;

        AABB box = new AABB(pos.getX() + 0.25, pos.getY(), pos.getZ() + 0.25,
                pos.getX() + 0.75, pos.getY() + 2, pos.getZ() + 0.75);
        List<Entity> entities = serverLevel.getEntitiesOfClass(Entity.class, box);
        for (Entity e : entities) {
            be.teleport(serverLevel, e);
        }
    }

    private void teleport(ServerLevel level, Entity entity) {
        if (getEnergyStored() < CONSUMPTION) return;

        level.playSound(null, worldPosition, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1.0F, 1.0F);

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
        }
        // Cross-dimension teleport for non-player entities is intentionally not ported (see class javadoc).

        level.playSound(null, BlockPos.containing(tx, ty, tz), SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1.0F, 1.0F);

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
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("targetX", targetX);
        tag.putInt("targetY", targetY);
        tag.putInt("targetZ", targetZ);
        tag.putString("targetDim", targetDim);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        targetX = tag.getInt("targetX");
        targetY = tag.contains("targetY") ? tag.getInt("targetY") : -1;
        targetZ = tag.getInt("targetZ");
        targetDim = tag.contains("targetDim") ? tag.getString("targetDim") : "minecraft:overworld";
    }
}
