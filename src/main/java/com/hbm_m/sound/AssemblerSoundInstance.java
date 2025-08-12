package com.hbm_m.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

// AssemblerSoundInstance.java

@OnlyIn(Dist.CLIENT)
public class AssemblerSoundInstance extends AbstractTickableSoundInstance {
    
    private final BlockPos pos;

    public AssemblerSoundInstance(BlockPos pos) {
        // ДИАГНОСТИЧЕСКОЕ ИЗМЕНЕНИЕ 
        // Временно используем ванильный звук вагонетки вместо вашего.
        // super(SoundEvents.MINECART_RIDING, SoundSource.BLOCKS, Minecraft.getInstance().level.getRandom());
        super(ModSounds.ASSEMBLER_OPERATE.get(), SoundSource.BLOCKS, Minecraft.getInstance().level.getRandom()); // <-- Ваша строка закомментирована
        this.pos = pos;
        this.x = pos.getX() + 0.5D;
        this.y = pos.getY() + 0.5D;
        this.z = pos.getZ() + 0.5D;
        this.attenuation = Attenuation.LINEAR;
        this.relative = false;
        this.looping = true;
        this.delay = 0;
    }

    @Override
    public void tick() {
        // УБИРАЕМ ВСЕ ПРОВЕРКИ isCrafting()!
        // Единственная проверка - на случай, если блок физически уничтожен
        // или игрок вышел из мира.
        if (Minecraft.getInstance().level == null || !Minecraft.getInstance().level.isLoaded(this.pos)) {
            this.stop();
            // ВАЖНО: Сообщаем менеджеру, что мы остановились
            ClientSoundManager.onSoundStopped(this.pos); 
        }
    }
    
    public void stopSound() {
        this.stop();
        // ВАЖНО: Также сообщаем менеджеру, когда нас останавливают извне
        ClientSoundManager.onSoundStopped(this.pos);
    }
}