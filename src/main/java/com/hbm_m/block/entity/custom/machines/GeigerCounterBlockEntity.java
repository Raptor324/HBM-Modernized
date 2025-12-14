package com.hbm_m.block.entity.custom.machines;

// Блок-энтити для Гейгера, который измеряет радиацию в чанке и издает щелчки в зависимости от уровня радиации.
// Логика звуков адаптирована из GeigerCounterItem, но с учетом того, что это блок, а не предмет.
// Радиоактивность измеряется с помощью ChunkRadiationManager, который управляет радиацией на уровне чанков.
// Звук издается в мире, а не игроку, и громкость/частота зависят от измеренного уровня радиации.
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.radiation.ChunkRadiationManager;
import com.hbm_m.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import javax.annotation.Nonnull;

public class GeigerCounterBlockEntity extends BlockEntity {

    private static final Random RANDOM = new Random();
    private int timer = 0;
    private float lastMeasuredRads = 0;

    public GeigerCounterBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.GEIGER_COUNTER_BE.get(), pPos, pBlockState);
    }

    // Основная логика, вызывается каждый тик на сервере благодаря тикеру, который мы настроили в классе блока
    public void tick(Level level, BlockPos pos, BlockState state) {
        timer++;

        // Измеряем радиацию раз в полсекунды (10 тиков)
        if (timer >= 10) {
            timer = 0;
            // Используем менеджер радиации
            this.lastMeasuredRads = ChunkRadiationManager.getRadiation(level, pos.getX(), pos.getY(), pos.getZ());
            
            // Важно! Уведомляем соседние блоки об изменении, чтобы компаратор обновился.
            level.updateNeighbourForOutputSignal(pos, this.getBlockState().getBlock());
        }

        // Проигрываем звуки каждые 5 тиков, как в старом моде
        if (timer % 5 == 0) {
            playGeigerTickSound(level, pos, this.lastMeasuredRads);
        }
    }
    
    // Логика проигрывания звука, адаптированная из GeigerCounterItem
    private void playGeigerTickSound(Level level, BlockPos pos, float radiationLevel) {
        int soundIndex = 0;
        List<Integer> soundOptions = new ArrayList<>();

        if (radiationLevel > 0) {
            if (radiationLevel < 10) soundOptions.add(1);
            if (radiationLevel > 5 && radiationLevel < 15) soundOptions.add(2);
            if (radiationLevel > 10 && radiationLevel < 20) soundOptions.add(3);
            if (radiationLevel > 15 && radiationLevel < 25) soundOptions.add(4);
            if (radiationLevel > 20 && radiationLevel < 30) soundOptions.add(5);
            if (radiationLevel > 25) soundOptions.add(6);

            if (!soundOptions.isEmpty()) {
                soundIndex = soundOptions.get(RANDOM.nextInt(soundOptions.size()));
            }
        } else if (RANDOM.nextInt(50) == 0) {
            soundIndex = 1; // Редкий фоновый щелчок
        }

        Optional<RegistryObject<SoundEvent>> soundRegistryObject = switch (soundIndex) {
            case 1 -> Optional.of(ModSounds.GEIGER_1);
            case 2 -> Optional.of(ModSounds.GEIGER_2);
            case 3 -> Optional.of(ModSounds.GEIGER_3);
            case 4 -> Optional.of(ModSounds.GEIGER_4);
            case 5 -> Optional.of(ModSounds.GEIGER_5);
            case 6 -> Optional.of(ModSounds.GEIGER_6);
            default -> Optional.empty();
        };

        // Проигрываем звук в мире
        soundRegistryObject.ifPresent(regObject -> {
            level.playSound(null, pos, regObject.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        });
    }

    public float getLastMeasuredRads() {
        return lastMeasuredRads;
    }
    
    // Сохранение данных при выходе из мира
    @Override
    protected void saveAdditional(@Nonnull CompoundTag pTag) {
        pTag.putFloat("lastMeasuredRads", this.lastMeasuredRads);
        super.saveAdditional(pTag);
    }

    // Загрузка данных при входе в мир
    @Override
    public void load(@Nonnull CompoundTag pTag) {
        super.load(pTag);
        this.lastMeasuredRads = pTag.getFloat("lastMeasuredRads");
    }
}