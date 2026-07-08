package com.hbm_m.blockentity.machines;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.radiation.ChunkRadiationManager;
import com.hbm_m.sound.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Порт {@link com.hbm.tileentity.machine.TileEntityGeiger} (1.7.10).
 */
public class GeigerCounterBlockEntity extends BlockEntity {

    private static final Random RANDOM = new Random();

    private int timer = 0;
    private float ticker = 0;

    public GeigerCounterBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.GEIGER_COUNTER_BE.get(), pos, blockState);
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        timer++;

        if (timer == 10) {
            timer = 0;
            ticker = check(level, pos);
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
        }

        if (timer % 5 == 0) {
            if (ticker > 0) {
                List<Integer> list = new ArrayList<>();

                if (ticker < 1) {
                    list.add(0);
                }
                if (ticker < 5) {
                    list.add(0);
                }
                if (ticker < 10) {
                    list.add(1);
                }
                if (ticker > 5 && ticker < 15) {
                    list.add(2);
                }
                if (ticker > 10 && ticker < 20) {
                    list.add(3);
                }
                if (ticker > 15 && ticker < 25) {
                    list.add(4);
                }
                if (ticker > 20 && ticker < 30) {
                    list.add(5);
                }
                if (ticker > 25) {
                    list.add(6);
                }

                int r = list.get(RANDOM.nextInt(list.size()));

                if (r > 0) {
                    playGeigerSound(level, pos, r);
                }
            } else if (RANDOM.nextInt(50) == 0) {
                playGeigerSound(level, pos, 1);
            }
        }
    }

    public float check(Level level, BlockPos pos) {
        return ChunkRadiationManager.getRadiation(level, pos.getX(), pos.getY(), pos.getZ());
    }

    public float getTicker() {
        return ticker;
    }

    private static void playGeigerSound(Level level, BlockPos pos, int index) {
        SoundEvent sound = switch (index) {
            case 1 -> ModSounds.GEIGER_1.orElse(null);
            case 2 -> ModSounds.GEIGER_2.orElse(null);
            case 3 -> ModSounds.GEIGER_3.orElse(null);
            case 4 -> ModSounds.GEIGER_4.orElse(null);
            case 5 -> ModSounds.GEIGER_5.orElse(null);
            case 6 -> ModSounds.GEIGER_6.orElse(null);
            default -> null;
        };
        if (sound != null) {
            level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }
}
