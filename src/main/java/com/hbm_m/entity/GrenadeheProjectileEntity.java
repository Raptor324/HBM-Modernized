package com.hbm_m.entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import com.hbm_m.item.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
public class GrenadeheProjectileEntity extends ThrowableItemProjectile {
    public GrenadeheProjectileEntity(EntityType<? extends ThrowableItemProjectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }
    public GrenadeheProjectileEntity(Level pLevel) {
        super(ModEntities.GRENADEHE_PROJECTILE.get(), pLevel);
    }
    public GrenadeheProjectileEntity(Level pLevel, LivingEntity livingEntity) {
        super(ModEntities.GRENADEHE_PROJECTILE.get(), livingEntity, pLevel);
    }
    @Override
    protected Item getDefaultItem() {
        return ModItems.GRENADEHE.get();
    }
    @Override
    protected void onHitBlock(BlockHitResult pResult) {
        // Проверяем, что код выполняется только на серверной стороне, чтобы избежать дублирования на клиенте
        if (!this.level().isClientSide()) {
            BlockPos blockPos = pResult.getBlockPos(); // Получаем позицию блока, в который попала граната

            float power = 7.0F; // Мощность взрыва. Можешь изменить это значение по своему усмотрению.
            boolean causesFire = false; // Будет ли взрыв вызывать огонь. Установи в 'false', если огонь не нужен.

            // Используем метод level.explode() для создания взрыва.
            // Это более прямой и надежный способ, который обрабатывает все аспекты взрыва:
            // разрушение блоков, нанесение урона сущностям и создание огня.
            this.level().explode(
                    this, // Сущность, которая является источником взрыва (сама граната).
                    null, // Пользовательский DamageSource. Если null, будет использован стандартный источник урона для взрыва.
                    null, // Пользовательский ExplosionDamageCalculator. Если null, будет использован стандартный.
                    blockPos.getX() + 0.5, // Координата X центра взрыва (добавляем 0.5 для центрирования)
                    blockPos.getY() + 0.5, // Координата Y центра взрыва (добавляем 0.5 для центрирования)
                    blockPos.getZ() + 0.5, // Координата Z центра взрыва (добавляем 0.5 для центрирования)
                    power, // Радиус или мощность взрыва.
                    causesFire, // Будет ли взрыв создавать огонь.
                    Level.ExplosionInteraction.BLOCK // Тип взаимодействия взрыва с блоками. BLOCK означает разрушение блоков и нанесение урона сущностям.
            );
            this.discard(); // Удаляем сущность гранаты из мира после взрыва.
        }
    }
}
