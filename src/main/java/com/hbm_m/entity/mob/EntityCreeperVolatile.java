package com.hbm_m.entity.mob;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.generic.BlockSlag;
import com.hbm_m.explosion.vanillant.ExplosionVNT;
import com.hbm_m.explosion.vanillant.standard.BlockAllocatorBulkie;
import com.hbm_m.explosion.vanillant.standard.BlockMutatorBulkie;
import com.hbm_m.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm_m.explosion.vanillant.standard.EntityProcessorStandard;
import com.hbm_m.explosion.vanillant.standard.ExplosionEffectStandard;
import com.hbm_m.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Возгораемый крипер — усиленный взрыв с «шлаковым» краем (оригинал ExplosionVNT).
 */
public class EntityCreeperVolatile extends Creeper {

    public EntityCreeperVolatile(EntityType<? extends Creeper> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Creeper.createAttributes();
    }

    @SuppressWarnings("unchecked")
    public static boolean checkVolatileSpawnRules(
            EntityType<EntityCreeperVolatile> entityType,
            ServerLevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random) {
        if (!Creeper.checkMonsterSpawnRules(
                (EntityType<Creeper>) (EntityType<?>) entityType, level, spawnType, pos, random)) {
            return false;
        }
        if (pos.getY() > 40) {
            return false;
        }
        return level.getLevel().dimension() == Level.OVERWORLD;
    }

    /** Взрыв (оригинал {@code func_146077_cc}). Вызывается из {@link com.hbm_m.mixin.CreeperMixin}. */
    public void volatileExplode() {
        if (this.level().isClientSide) {
            return;
        }

        this.discard();

        float power = this.isPowered() ? 14.0F : 7.0F;
        BlockState slagShell = ModBlocks.BLOCK_SLAG.get().defaultBlockState().setValue(BlockSlag.BROKEN, true);

        ExplosionVNT vnt = new ExplosionVNT(this.level(), this.getX(), this.getY(), this.getZ(), power, this);
        vnt.setBlockAllocator(new BlockAllocatorBulkie(60.0D, this.isPowered() ? 32 : 16));
        vnt.setBlockProcessor(new BlockProcessorStandard().withBlockEffect(new BlockMutatorBulkie(slagShell)));
        vnt.setEntityProcessor(new EntityProcessorStandard().withRangeMod(0.5F));
        vnt.setSFX(new ExplosionEffectStandard());
        vnt.explode();
    }

    @Override
    protected void dropCustomDeathLoot(net.minecraft.world.damagesource.DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        this.spawnAtLocation(new ItemStack(ModItems.SULFUR.get(), 2 + this.random.nextInt(3)));
        // stick_tnt — после порта предмета в ModItems
    }
}
