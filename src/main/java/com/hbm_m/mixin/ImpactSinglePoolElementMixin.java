package com.hbm_m.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.hbm_m.world.gen.ImpactWorldGen;
import com.mojang.datafixers.util.Either;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * Original {@code ModEventHandlerImpact.modifyVillageGen} ({@code BiomeEvent.GetVillageBlockID}): Dorfteile
 * ({@code village/...}) bekommen nach dem Tom-Einschlag den Ersetzungsprozessor {@link ImpactWorldGen#VILLAGE}.
 */
@Mixin(SinglePoolElement.class)
public abstract class ImpactSinglePoolElementMixin {

    @Shadow @Final protected Either<ResourceLocation, StructureTemplate> template;

    @Inject(method = "getSettings", at = @At("RETURN"))
    private void hbm_m$impactVillage(Rotation rotation, BoundingBox box, boolean keepJigsaws, CallbackInfoReturnable<StructurePlaceSettings> cir) {
        ResourceLocation id = this.template.left().orElse(null);
        if (id == null || !id.getPath().startsWith("village/")) return;
        cir.getReturnValue().addProcessor(ImpactWorldGen.VILLAGE);
    }
}
