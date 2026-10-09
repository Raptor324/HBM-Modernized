package com.hbm_m.blockentity.decorations;

import com.hbm_m.blockentity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code TileEntityLantern}: jede Sekunde bekommen alle Glyphiden im Umkreis 7.5 um die Laterne (5.5 ueber dem
 * Fuss) 5 s Blindheit. Glyphiden = alle Wesen der Glyphiden-Familie ({@code EntityGlyphid} und Unterarten, im Port an
 * der ID {@code hbm_m:glyphid*} erkannt - die Glyphiden folgen mit der Entity-Runde).
 */
public class LanternBlockEntity extends BlockEntity implements com.hbm_m.api.render.RenderBoundsProvider {

    public LanternBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LANTERN.get(), pos, state);
    }

    public static boolean isGlyphid(LivingEntity e) {
        var key = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
        return key != null && "hbm_m".equals(key.getNamespace()) && key.getPath().startsWith("glyphid");
    }

    public static void serverTick(Level world, BlockPos pos, BlockState state, LanternBlockEntity be) {
        if (world.getGameTime() % 20 != 0) return;
        double x = pos.getX() + 0.5, y = pos.getY() + 5.5, z = pos.getZ() + 0.5;
        for (LivingEntity glyphid : world.getEntitiesOfClass(LivingEntity.class, new AABB(x, y, z, x, y, z).inflate(7.5, 7.5, 7.5), LanternBlockEntity::isGlyphid)) {
            glyphid.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
        }
    }

    @Override
    public AABB getRenderBoundingBox() {
        return com.hbm_m.platform.BlockHooks.aabb(worldPosition, worldPosition.offset(1, 6, 1));
    }
}
