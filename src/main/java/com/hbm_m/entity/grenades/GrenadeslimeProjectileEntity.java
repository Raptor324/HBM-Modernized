package com.hbm_m.entity.grenades;

import com.hbm_m.entity.ModEntities;
import com.hbm_m.item.ModItems;
import com.hbm_m.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;


public class GrenadeslimeProjectileEntity extends ThrowableItemProjectile {

    // --- НОВЫЕ ПОЛЯ КЛАССА ---
    private static final int MAX_BOUNCES = 4; // Максимальное количество отскоков перед взрывом
    private int bounceCount = 0; // Текущий счетчик отскоков
    private float bounceMultiplier = 0.51f; // Коэффициент сохранения скорости после отскока (0.0 - 1.0)
    // -------------------------

    public GrenadeslimeProjectileEntity(EntityType<? extends ThrowableItemProjectile> p_37442_pEntityType, Level p_37443_pLevel) {
        super(p_37442_pEntityType, p_37443_pLevel);
    }

    private static final SoundEvent[] BOUNCE_SOUNDS = new SoundEvent[]{
            ModSounds.BOUNCE1.get(), // Замените на ваш первый звук
            ModSounds.BOUNCE2.get(), // Замените на ваш второй звук
            ModSounds.BOUNCE3.get()  // Замените на ваш третий звук
    };

    public GrenadeslimeProjectileEntity(Level pLevel) {
        super(ModEntities.GRENADESLIME_PROJECTILE.get(), pLevel);
    }

    public GrenadeslimeProjectileEntity(Level pLevel, LivingEntity livingEntity) {
        super(ModEntities.GRENADESLIME_PROJECTILE.get(), livingEntity, pLevel);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.GRENADESLIME.get();
    }

    @Override
    protected void onHitBlock(BlockHitResult pResult) {
        // Проверяем, что код выполняется на серверной стороне, чтобы избежать дублирования на клиенте
        if (!this.level().isClientSide) {

            if (!this.level().isClientSide()) {
                BlockPos blockPos = pResult.getBlockPos(); // Получаем позицию блока, в который попала граната

                // --- Добавленная логика для проигрывания случайного звука ---
                RandomSource random = this.level().random; // Используем RandomSource из мира для рандома
                SoundEvent bounceSound = BOUNCE_SOUNDS[random.nextInt(BOUNCE_SOUNDS.length)]; // Выбираем случайный звук

                // Проигрываем звук в позиции гранаты
                this.level().playSound(
                        null,         // Игрок, проигрывающий звук (null для звука мира)
                        blockPos,     // Позиция, где проигрывается звук
                        bounceSound,  // Выбранный случайный звук
                        SoundSource.NEUTRAL, // Категория звука (например, NEUTRAL, BLOCKS, PLAYERS)
                        2.1F,         // Громкость (1.0F - полная громкость)
                        1.0F          // Высота тона (1.0F - обычная высота тона)
                );

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

                blockPos = pResult.getBlockPos();
                float power = 6.5F; // Мощность взрыва. Можешь изменить это значение по своему усмотрению.
                boolean causesFire = false; // Будет ли взрыв вызывать огонь. Установлено в 'false', если огонь не нужен.

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
}
}
