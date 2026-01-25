package com.hbm_m.block.entity.custom.explosives;

import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class MineNukeBlockEntity extends BlockEntity {

    private static final double DETECTION_RADIUS = 5.0;
    private static final int SOUND_COOLDOWN = 60; // 3 секунды (20 тиков = 1 сек)
    private int soundCooldown = 0;
    private boolean hasPlayedWarning = false;

    public MineNukeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MINE_NUKE_BLOCK_ENTITY.get(), pos, state);
    }

    public static void tick(BlockEntity entity) {
        if (!(entity instanceof MineNukeBlockEntity mine)) return;
        if (mine.getLevel() == null || mine.getLevel().isClientSide) return;

        // Проверяем мобов в радиусе обнаружения
        AABB searchArea = new AABB(mine.getBlockPos()).inflate(DETECTION_RADIUS);
        List<LivingEntity> entities = mine.getLevel().getEntitiesOfClass(
                LivingEntity.class,
                searchArea,
                e -> !(e instanceof Player player && player.isCreative())
        );

        // Оповещение
        if (!entities.isEmpty()) {
            if (mine.soundCooldown == 0) {
                mine.playWarningSound();
                mine.soundCooldown = SOUND_COOLDOWN;
                mine.hasPlayedWarning = true;
            }
        } else {
            mine.hasPlayedWarning = false;
        }

        if (mine.soundCooldown > 0) mine.soundCooldown--;

        // Взрыв, если кто-то оказался непосредственно на мине
        for (LivingEntity entityInRange : entities) {
            double distance = entityInRange.distanceToSqr(
                    mine.getBlockPos().getX() + 0.5,
                    mine.getBlockPos().getY() + 0.5,
                    mine.getBlockPos().getZ() + 0.5
            );
            if (distance < 1.2) { // 1 блок
                Level level = mine.getLevel();
                BlockPos pos = mine.getBlockPos();
                // Взрыв
                level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8F, true, Level.ExplosionInteraction.TNT);
                level.removeBlock(pos, false);
                break;
            }
        }
    }

    private void playWarningSound() {
        if (this.level != null && ModSounds.GRENADE_TRIGGER.isPresent()) {
            this.level.playSound(
                    null,
                    this.worldPosition.getX() + 0.5,
                    this.worldPosition.getY() + 0.5,
                    this.worldPosition.getZ() + 0.5,
                    ModSounds.GRENADE_TRIGGER.get(),
                    net.minecraft.sounds.SoundSource.BLOCKS,
                    3.0F,
                    3.0F
            );
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("SoundCooldown", this.soundCooldown);
        tag.putBoolean("HasPlayedWarning", this.hasPlayedWarning);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.soundCooldown = tag.getInt("SoundCooldown");
        this.hasPlayedWarning = tag.getBoolean("HasPlayedWarning");
    }
}