package com.hbm_m.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.hbm_m.world.gen.ImpactWorldGen;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

/**
 * Original {@code ModEventHandlerImpact.postImpactDecoration} ({@code DecorateBiomeEvent.Decorate}): nach dem
 * Tom-Einschlag werden Baeume/Riesenpilze/Pflanzen bei der Weltgenerierung verweigert, siehe
 * {@link ImpactWorldGen#allowDecoration}.
 */
@Mixin(ConfiguredFeature.class)
public abstract class ImpactConfiguredFeatureMixin {

    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void hbm_m$impactDecoration(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        ConfiguredFeature<?, ?> self = (ConfiguredFeature<?, ?>) (Object) this;
        if (!ImpactWorldGen.allowDecoration(level, self.feature(), self.config(), pos, random)) {
            cir.setReturnValue(false);
        }
    }
}
