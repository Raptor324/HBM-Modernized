package com.hbm_m.effect;

import com.hbm_m.block.bomb.BlockTaint;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.damagesource.ModDamageSources;
import com.hbm_m.entity.mob.EntityCreeperTainted;
import com.hbm_m.effect.render.TaintEffectRenderer;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

//? if forge {
import net.minecraftforge.client.extensions.common.IClientMobEffectExtensions;
//?}

/**
 * Эффект порчи — периодический урон и следы блока taint под сущностью.
 * Порт {@link com.hbm.potion.HbmPotion#taint} (1.7.10).
 */
public class TaintEffect extends MobEffect {

    public TaintEffect() {
        super(MobEffectCategory.HARMFUL, 0x800080);
    }

    @Override
    public void applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
        Level level = entity.level();
        if (level.isClientSide) {
            return;
        }

        if (entity instanceof EntityCreeperTainted) {
            return;
        }

        if (level.random.nextInt(40) == 0) {
            entity.hurt(ModDamageSources.taint(level), amplifier + 1);
        }

        if (ModClothConfig.get().taintTrails) {
            BlockPos below = BlockPos.containing(entity.getX(), entity.getY() - 1.0, entity.getZ());
            if (below.getY() > level.getMinBuildHeight()) {
                BlockState ground = level.getBlockState(below);
                if (BlockTaint.canBeReplacedByTaint(level, below, ground)) {
                    level.setBlock(below, BlockTaint.stateWithAge(14), 2);
                }
            }
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 2 == 0;
    }

    //? if forge {
    @Override
    public void initializeClient(@NotNull Consumer<IClientMobEffectExtensions> consumer) {
        consumer.accept(new IClientMobEffectExtensions() {

            @Override
            public boolean renderInventoryIcon(MobEffectInstance instance,
                                               EffectRenderingInventoryScreen<?> screen,
                                               GuiGraphics gfx, int x, int y, int blitOffset) {
                TaintEffectRenderer.renderInventory(gfx, x, y, blitOffset);
                return true;
            }

            @Override
            public boolean renderGuiIcon(MobEffectInstance instance,
                    net.minecraft.client.gui.Gui gui,
                    GuiGraphics gfx, int x, int y, float z, float alpha) {
                TaintEffectRenderer.renderHud(gfx, x, y, (int) z, alpha);
                return true;
            }
        });
    }
    //?}
}
