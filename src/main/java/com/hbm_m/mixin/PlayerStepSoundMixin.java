package com.hbm_m.mixin;

import com.hbm_m.main.MainRegistry;
import com.hbm_m.powerarmor.ModPowerArmorItem;
import com.hbm_m.powerarmor.PowerArmorSpecs;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerStepSoundMixin {

    @Inject(method = "playStepSound", at = @At("HEAD"), cancellable = true)
    private void hbm_m$replaceStepSound(BlockPos pos, BlockState state, CallbackInfo ci) {
        Player player = (Player) (Object) this;

        // Шаги должны считаться ванилью, а мы только подменяем звук
        if (!ModPowerArmorItem.hasFSBArmor(player)) return;

        var chest = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        if (!(chest.getItem() instanceof ModPowerArmorItem armorItem)) return;

        PowerArmorSpecs specs = armorItem.getSpecs();
        if (specs.stepSound == null || specs.stepSound.isBlank()) return;

        // Резолвим SoundEvent по ResourceLocation (поддерживает модовые звуки тоже)
        SoundEvent ev = getSoundEvent(player.level(), specs.stepSound);
        if (ev == null) return;

        // Играем шаг вместо ванильного и отменяем ванильный вызов
        // Важно: используем ту же категорию, что и ванильные шаги
        player.level().playSound(player, player.getX(), player.getY(), player.getZ(), ev, SoundSource.PLAYERS, 0.15F, 1.0F);
        ci.cancel();
    }

    private static SoundEvent getSoundEvent(Level level, String id) {
        // Поддержка двух форматов:
        // 1) "hbm_m:step.powered"
        // 2) "step.powered" (будет считаться как hbm_m:step.powered)
        ResourceLocation rl;
        if (id.contains(":")) rl = ResourceLocation.tryParse(id);
        else rl = ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, id);

        if (rl == null) return null;

        return level.registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.SOUND_EVENT)
                .get(rl);
    }
}
