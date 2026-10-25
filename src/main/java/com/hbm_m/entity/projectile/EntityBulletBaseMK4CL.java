package com.hbm_m.entity.projectile;

import java.util.Comparator;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.entity.ModEntities;
import com.hbm_m.item.weapon.sedna.BulletConfig;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code EntityBulletBaseMK4CL}: Geschoss, das den Chunk unter sich geladen haelt (Original: ForgeChunkManager-
 * Ticket, jeden Tick auf den aktuellen Chunk umgesetzt) - fuer weit fliegende Munition wie Raketen und Granaten.
 */
public class EntityBulletBaseMK4CL extends EntityBulletBaseMK4 {

    private static final TicketType<UUID> TICKET = TicketType.create("hbm_m_bullet", Comparator.comparing(UUID::toString));

    @Nullable
    private ChunkPos loadedChunk;

    public EntityBulletBaseMK4CL(EntityType<? extends EntityBulletBaseMK4CL> type, Level world) {
        super(type, world);
    }

    public EntityBulletBaseMK4CL(LivingEntity entity, BulletConfig config, float damage, float spread, double sideOffset, double heightOffset, double forwardOffset) {
        super(ModEntities.BULLET_MK4_CL.get(), entity, config, damage, spread, sideOffset, heightOffset, forwardOffset);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && !isRemoved()) loadNeighboringChunks(new ChunkPos(this.blockPosition()));
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        clearChunkLoader();
    }

    public void clearChunkLoader() {
        if (level() instanceof ServerLevel server && loadedChunk != null) {
            server.getChunkSource().removeRegionTicket(TICKET, loadedChunk, 2, this.getUUID());
            loadedChunk = null;
        }
    }

    public void loadNeighboringChunks(ChunkPos pos) {
        if (!(level() instanceof ServerLevel server)) return;
        if (pos.equals(loadedChunk)) return;
        if (loadedChunk != null) server.getChunkSource().removeRegionTicket(TICKET, loadedChunk, 2, this.getUUID());
        loadedChunk = pos;
        server.getChunkSource().addRegionTicket(TICKET, loadedChunk, 2, this.getUUID());
    }
}
