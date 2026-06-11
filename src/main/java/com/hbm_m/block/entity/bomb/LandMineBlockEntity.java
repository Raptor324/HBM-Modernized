package com.hbm_m.block.entity.bomb;

import java.util.List;

import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.block.bomb.LandmineBlock;
import com.hbm_m.sound.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class LandMineBlockEntity extends BlockEntity {

    private boolean isPrimed = false;
    public boolean waitingForPlayer = false;

    public LandMineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LANDMINE_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, LandMineBlockEntity be) {
        if (level.isClientSide) return;

        if (!(state.getBlock() instanceof LandmineBlock landmine)) return;

        double range = landmine.range;
        double height = landmine.height;

        if (be.waitingForPlayer) {
            range = 25;
            height = 25;
        } else if (!be.isPrimed) {
            range *= 2;
            height *= 2;
        }

        if (!level.isEmptyBlock(pos.above())) return;

        AABB searchBox = new AABB(
                pos.getX() - range, pos.getY() - height, pos.getZ() - range,
                pos.getX() + range + 1, pos.getY() + height, pos.getZ() + range + 1
        );
        List<Entity> entities = level.getEntities(null, searchBox);

        for (Entity entity : entities) {
            if (entity.getType().getCategory() == MobCategory.WATER_CREATURE) continue;
            if (entity.getType().getCategory() == MobCategory.AMBIENT) continue;

            if (be.waitingForPlayer) {
                if (entity instanceof Player) {
                    be.waitingForPlayer = false;
                    be.setChanged();
                    return;
                }
            } else if (entity instanceof LivingEntity living) {
                if (living instanceof Player player && player.isCreative()) continue;

                if (be.isPrimed) {
                    landmine.explode(level, pos);
                }
                return;
            }
        }

        if (!be.isPrimed && !be.waitingForPlayer) {
            ModSounds.GRENADE_TRIGGER.ifPresent(sound -> level.playSound(
                    null,
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    sound,
                    SoundSource.BLOCKS,
                    3.0F,
                    1.0F
            ));
            be.isPrimed = true;
            be.setChanged();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("primed", isPrimed);
        tag.putBoolean("waiting", waitingForPlayer);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        isPrimed = tag.getBoolean("primed");
        waitingForPlayer = tag.getBoolean("waiting");
    }
}
