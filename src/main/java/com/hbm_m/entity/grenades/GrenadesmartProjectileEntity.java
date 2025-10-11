package com.hbm_m.entity.grenades;

import com.hbm_m.entity.ModEntities;
import com.hbm_m.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;


public class GrenadesmartProjectileEntity extends ThrowableItemProjectile {

    // --- НОВЫЕ ПОЛЯ КЛАССА ---
    private static final int MAX_BOUNCES = 3; // Максимальное количество отскоков перед взрывом
    private int bounceCount = 0; // Текущий счетчик отскоков
    private float bounceMultiplier = 0.3f; // Коэффициент сохранения скорости после отскока (0.0 - 1.0)
    // -------------------------

    public GrenadesmartProjectileEntity(EntityType<? extends ThrowableItemProjectile> p_37442_pEntityType, Level p_37443_pLevel) {
        super(p_37442_pEntityType, p_37443_pLevel);
    }

    public GrenadesmartProjectileEntity(Level pLevel) {
        super(ModEntities.GRENADESMART_PROJECTILE.get(), pLevel);
    }

    public GrenadesmartProjectileEntity(Level pLevel, LivingEntity livingEntity) {
        super(ModEntities.GRENADESMART_PROJECTILE.get(), livingEntity, pLevel);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.GRENADESMART.get();
    }

    @Override
    protected void onHitBlock(BlockHitResult pResult) {
        // Проверяем, что код выполняется на серверной стороне, чтобы избежать дублирования на клиенте
        if (!this.level().isClientSide) {
            this.bounceCount++; // Увеличиваем счетчик отскоков



            if (this.bounceCount < MAX_BOUNCES) {
                // Если количество отскоков меньше максимального, граната отскакивает

                // Получаем текущую скорость гранаты
                Vec3 currentVelocity = this.getDeltaMovement();
                // Получаем нормаль поверхности, в которую ударилась граната
                Vec3 hitNormal = Vec3.atLowerCornerOf(pResult.getDirection().getNormal());

                // Вычисляем отраженную скорость: V_reflected = V - 2 * (V . N) * N
                // Где V - текущая скорость, N - нормаль поверхности
                Vec3 reflectedVelocity = currentVelocity.subtract(hitNormal.scale(2 * currentVelocity.dot(hitNormal)));

                // Применяем множитель отскока (для имитации потери энергии) и устанавливаем новую скорость
                this.setDeltaMovement(reflectedVelocity.scale(bounceMultiplier));

                // Опционально: добавьте небольшой импульс вверх, чтобы граната не "прилипала" к земле
                // this.setDeltaMovement(this.getDeltaMovement().add(0, 0.1, 0));


                // Важно: не вызываем super.onHitBlock(pResult); чтобы не вызывать стандартное поведение ThrowableItemProjectile
                // и не даем гранате взорваться до достижения MAX_BOUNCES.
                return; // Завершаем метод, чтобы избежать взрыва
            } else {
                // Если достигнуто максимальное количество отскоков, граната взрывается

                BlockPos blockPos = pResult.getBlockPos(); // Получаем позицию блока, в который попала граната
                float power = 6.5F; // Мощность взрыва. Можешь изменить это значение по своему усмотрению.
                boolean causesFire = true; // Будет ли взрыв вызывать огонь. Установлено в 'false', если огонь не нужен.

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
                this.discard(); // Удаляем сущность гранаты после взрыва
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult pResult) {
        // Если граната попадает в сущность, она должна взорваться немедленно
        if (!this.level().isClientSide) {
            BlockPos blockPos = pResult.getEntity().blockPosition(); // Позиция сущности, в которую попали
            float power = 8.0F;
            boolean causesFire = false;

            this.level().explode(
                    this,
                    null,
                    null,
                    blockPos.getX() + 0.5,
                    blockPos.getY() + 0.5,
                    blockPos.getZ() + 0.5,
                    power,
                    causesFire,
                    Level.ExplosionInteraction.BLOCK
            );
            this.discard(); // Удаляем сущность гранаты после взрыва
        }
    }

    // Вы можете также переопределить метод tick() для дополнительных эффектов или логики,
    // но для отскоков и взрыва основная логика находится в onHitBlock и onHitEntity.
}
