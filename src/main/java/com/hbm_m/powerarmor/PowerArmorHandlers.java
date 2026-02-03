package com.hbm_m.powerarmor;

import java.util.List;

import com.hbm_m.main.MainRegistry;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.network.packets.PowerArmorDashPacket;
import com.hbm_m.powerarmor.resist.DamageResistanceHandler;
import com.hbm_m.sound.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/**
 * Combined handler for Power Armor events.
 * Includes: Damage (DT/DR), Movement (Step/Dash), and Hard Landing logic.
 * 
 * УНИФИЦИРОВАННАЯ СИСТЕМА РЕЗИСТОВ:
 * - Используется ТОЛЬКО DamageResistanceHandler для всех расчетов
 * - Ванильная защита отключена через getDefense() = 0
 * - Все расчеты через централизованную систему DT+DR
 */
@Mod.EventBusSubscriber(modid = MainRegistry.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PowerArmorHandlers {

    private static final String TAG_DASH_COOLDOWN = "hbm_power_armor_dash_cooldown";
    private static final int DASH_COOLDOWN_TICKS = 20; // 1 секунда
    private static final long DASH_ENERGY_COST = 5000; // 5к энергии за деш

    private PowerArmorHandlers() {}
    private static final ThreadLocal<Float> DAMAGE_CACHE = ThreadLocal.withInitial(() -> null);

    // ========== DAMAGE HANDLING ==========
    
    /**
     * HIGHEST priority - перехватываем урон ДО ванильной обработки
     * и рассчитываем урон через DamageResistanceHandler
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;
        if (player.getAbilities().instabuild || player.isSpectator()) return;
        
        // ========== ARROW DEFLECTION CHECK ==========
        if (DamageResistanceHandler.shouldDeflectProjectile(player, event.getSource())) {
            event.setCanceled(true);
            Entity projectile = event.getSource().getDirectEntity();
            if (projectile instanceof Projectile proj) {
                Vec3 velocity = proj.getDeltaMovement();
                proj.setDeltaMovement(velocity.scale(-0.5));
                proj.hurtMarked = true;
            }
            return;
        }
        
        // ========== NORMAL DAMAGE HANDLING ==========
        if (!ModArmorFSB.hasFSBArmor(player)) return;
        ItemStack chestStack = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!(chestStack.getItem() instanceof ModPowerArmorItem)) return;
        
        // Рассчитываем урон через систему DT/DR
        float calculatedDamage = DamageResistanceHandler.calculateDamage(
            player,
            event.getSource(),
            event.getAmount(),
            0F, // pierceDT (временно 0, система подготовлена для будущего использования)
            0F  // pierceDR
        );
        
        // Кэшируем результат в ThreadLocal (безопасно для многопоточности)
        if (calculatedDamage < event.getAmount()) {
            DAMAGE_CACHE.set(calculatedDamage);
        }
    }

    /**
     * LOWEST priority - применяем наш урон ПОСЛЕ всей ванильной обработки
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;
        if (player.getAbilities().instabuild || player.isSpectator()) return;
        
        CompoundTag data = player.getPersistentData();
        if (data.contains("hbm_power_armor_damage")) {
            float calculated = data.getFloat("hbm_power_armor_damage");
            event.setAmount(calculated);
            data.remove("hbm_power_armor_damage");
        }
    }

    // ========== MOVEMENT HANDLING ==========
    
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        Player player = event.player;
        
        // Обработка кулдауна даша
        CompoundTag tag = player.getPersistentData();
        int cooldown = tag.getInt(TAG_DASH_COOLDOWN);
        if (cooldown > 0) {
            tag.putInt(TAG_DASH_COOLDOWN, cooldown - 1);
        }
        
        if (player instanceof ServerPlayer sp) {
            handleHardLanding(sp);
        }
        
        if (!ModPowerArmorItem.hasFSBArmor(player)) {
            if (player.maxUpStep() > 0.6F) {
                player.setMaxUpStep(0.6F);
            }
            return;
        }
        
        var chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!(chestplate.getItem() instanceof ModPowerArmorItem armorItem)) return;
        
        var specs = armorItem.getSpecs();
        float stepHeight = specs.stepHeight;
        if (stepHeight > 0) {
            player.setMaxUpStep(Math.max(0.6F, stepHeight));
        }
    }

    public static void performDash(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (!ModPowerArmorItem.hasFSBArmor(player)) return;
        
        ItemStack chestStack = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!(chestStack.getItem() instanceof ModPowerArmorItem armorItem)) return;
        
        PowerArmorSpecs specs = armorItem.getSpecs();
        if (specs.dashCount <= 0) return;
        
        // Проверка кулдауна
        CompoundTag tag = player.getPersistentData();
        int cooldown = tag.getInt(TAG_DASH_COOLDOWN);
        if (cooldown > 0) return;
        
        // Проверка энергии
        long currentEnergy = ((ModArmorFSBPowered) armorItem).getCharge(chestStack);
        if (currentEnergy < DASH_ENERGY_COST) return;
        
        // Расход энергии
        ((ModArmorFSBPowered) armorItem).dischargeBattery(chestStack, DASH_ENERGY_COST);
        
        // Применение даша
        Vec3 lookDirection = player.getLookAngle();
        double dashSpeed = 1.5 + (specs.dashCount * 0.5);
        Vec3 dashVelocity = lookDirection.scale(dashSpeed);
        player.setDeltaMovement(dashVelocity.x, Math.max(dashVelocity.y, 0.2), dashVelocity.z);
        
        // Установка кулдауна
        tag.putInt(TAG_DASH_COOLDOWN, DASH_COOLDOWN_TICKS);
        
        // Отправка визуального эффекта другим игрокам
        ModPacketHandler.INSTANCE.send(
            PacketDistributor.TRACKING_ENTITY.with(() -> player),
            new PowerArmorDashPacket(player.getId(), dashVelocity)
        );
        
        // Синхронизация энергии клиенту
        ((ModArmorFSBPowered) armorItem).syncEnergyToClient(player, chestStack, player.level(), EquipmentSlot.CHEST);
    }

    // ========== HARD LANDING ==========
    
    private static final float MACE_MIN_FALL = 2.5F;
    private static final float MACE_HEAVY_FALL = 8.0F;
    private static final float HBM_AOE_MIN_FALL = 10.0F;
    private static final double HBM_RADIUS = 3.0D;
    private static final String TAG_WAS_IN_AIR = "hbm_was_in_air";
    private static final String TAG_MAX_FALL = "hbm_max_fall";
    private static final String TAG_SMASHING_SOFT = "hbm_smashing_soft";
    private static final String TAG_SMASH_FALLDIST = "hbm_smash_falldist";
    
    private static final TagKey<net.minecraft.world.level.block.Block> HBM_HARDLANDING_BREAKABLE =
            TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "hardlanding_breakable"));

    private static void handleHardLanding(ServerPlayer player) {
        if (player.isSpectator()) return;
        if (!ModArmorFSB.hasFSBArmorIgnoreCharge(player)) return;
        
        ItemStack chestStack = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!(chestStack.getItem() instanceof ModPowerArmorItem armorItem)) return;
        
        PowerArmorSpecs specs = armorItem.getSpecs();
        if (!specs.hasHardLanding) return;
        
        CompoundTag tag = player.getPersistentData();
        boolean onGround = player.onGround();
        boolean wasInAir = tag.getBoolean(TAG_WAS_IN_AIR);
        boolean smashingSoft = tag.getBoolean(TAG_SMASHING_SOFT);
        
        // Любая растекающаяся жидкость отменяет эффект (в т.ч. модовая)
        if (player.isInFluidType() || !player.level().getFluidState(player.blockPosition()).isEmpty()) {
            tag.putBoolean(TAG_WAS_IN_AIR, false);
            tag.putFloat(TAG_MAX_FALL, 0.0F);
            tag.putBoolean(TAG_SMASHING_SOFT, false);
            tag.putFloat(TAG_SMASH_FALLDIST, 0.0F);
            return;
        }
        
        // Карабканье (лестница/лиана/и т.д.) сбрасывает высоту и отменяет эффект
        if (player.onClimbable()) {
            tag.putBoolean(TAG_WAS_IN_AIR, false);
            tag.putFloat(TAG_MAX_FALL, 0.0F);
            tag.putBoolean(TAG_SMASHING_SOFT, false);
            tag.putFloat(TAG_SMASH_FALLDIST, 0.0F);
            return;
        }
        
        ServerLevel level = player.serverLevel();
        
        // === РЕЖИМ "ПРОВАЛИВАНИЯ": каждый тик ломаем мягкие блоки под собой, пока не упрёмся в твёрдый ===
        if (smashingSoft) {
            boolean broke = breakSoftBlocksUnderPlayer(level, player);
            if (broke) return; // пока ломаем — ни звука, ни AOE
            
            // Если уже нечего ломать, но игрок ещё не стоит на земле — ждём следующего тика
            if (!player.onGround()) return;
            
            // Уперлись в твёрдый блок: финализируем звук/частицы/AOE один раз
            float fallDist = tag.getFloat(TAG_SMASH_FALLDIST);
            tag.putBoolean(TAG_SMASHING_SOFT, false);
            tag.putFloat(TAG_SMASH_FALLDIST, 0.0F);
            
            if (fallDist <= MACE_MIN_FALL) return;
            if (player.isFallFlying()) return;
            
            // Блок под игроком для частиц (должен быть твёрдым)
            BlockPos belowPos = player.blockPosition().below();
            BlockState belowState = level.getBlockState(belowPos);
            if (belowState.isAir() || !belowState.getFluidState().isEmpty()) return;
            
            var sound = fallDist > MACE_HEAVY_FALL
                    ? ModSounds.MACE_SMASH_GROUND_HEAVY.get()
                    : ModSounds.MACE_SMASH_GROUND.get();
            level.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, 1.0F, 1.0F);
            
            Vec3 centerPos = belowPos.getCenter();
            int count = (int) Mth.clamp(50.0F * fallDist, 0.0F, 200.0F);
            for (int i = 0; i < 5; i++) {
                double offsetX = (level.random.nextDouble() - 0.5) * 2.5;
                double offsetY = level.random.nextDouble() * 0.5;
                double offsetZ = (level.random.nextDouble() - 0.5) * 2.5;
                level.sendParticles(
                        new BlockParticleOption(ParticleTypes.BLOCK, belowState),
                        centerPos.x + offsetX,
                        centerPos.y + offsetY + 0.5,
                        centerPos.z + offsetZ,
                        count / 5,
                        0.5F, 0.8F, 0.5F, 0.3F
                );
            }
            
            if (fallDist < HBM_AOE_MIN_FALL) return;
            applyHardLandingAOE(level, player);
            return;
        }
        
        // === Обычный трекинг падения ===
        if (!onGround) {
            float prevMax = tag.getFloat(TAG_MAX_FALL);
            float cur = player.fallDistance;
            if (cur > prevMax) tag.putFloat(TAG_MAX_FALL, cur);
            tag.putBoolean(TAG_WAS_IN_AIR, true);
            return;
        }
        
        // На земле, но не падали => просто идём по листве/траве и т.п., ничего не ломаем
        if (!wasInAir) return;
        
        // Приземлились после падения
        float fallDist = tag.getFloat(TAG_MAX_FALL);
        tag.putBoolean(TAG_WAS_IN_AIR, false);
        tag.putFloat(TAG_MAX_FALL, 0.0F);
        
        if (fallDist <= MACE_MIN_FALL) return;
        if (player.isFallFlying()) return;
        
        // Старт "проваливания" только если именно упали НА мягкий блок (а не просто зашли на него пешком)
        BlockPos belowPos = player.blockPosition().below();
        BlockState belowState = level.getBlockState(belowPos);
        
        if (isSoftBreakableForHardLanding(level, belowPos, belowState)) {
            tag.putBoolean(TAG_SMASHING_SOFT, true);
            tag.putFloat(TAG_SMASH_FALLDIST, fallDist);
            // Сразу ломаем первый слой (звук/АОЕ не играем, пока не дойдём до твёрдого)
            breakSoftBlocksUnderPlayer(level, player);
            return;
        }
        
        // Обычная "жёсткая посадка" на твёрдый блок
        if (belowState.isAir() || !belowState.getFluidState().isEmpty()) return;
        
        var sound = fallDist > MACE_HEAVY_FALL
                ? ModSounds.MACE_SMASH_GROUND_HEAVY.get()
                : ModSounds.MACE_SMASH_GROUND.get();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, 1.0F, 1.0F);
        
        Vec3 centerPos = belowPos.getCenter();
        int count = (int) Mth.clamp(50.0F * fallDist, 0.0F, 200.0F);
        for (int i = 0; i < 5; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 2.5;
            double offsetY = level.random.nextDouble() * 0.5;
            double offsetZ = (level.random.nextDouble() - 0.5) * 2.5;
            level.sendParticles(
                    new BlockParticleOption(ParticleTypes.BLOCK, belowState),
                    centerPos.x + offsetX,
                    centerPos.y + offsetY + 0.5,
                    centerPos.z + offsetZ,
                    count / 5,
                    0.5F, 0.8F, 0.5F, 0.3F
            );
        }
        
        if (fallDist < HBM_AOE_MIN_FALL) return;
        applyHardLandingAOE(level, player);
        player.getPersistentData().putBoolean("hbmhardlandingoccured", true);
    }

    private static void applyHardLandingAOE(ServerLevel level, Player player) {
        AABB box = player.getBoundingBox().inflate(HBM_RADIUS, 0.0D, HBM_RADIUS);
        List<Entity> entities = level.getEntities(player, box);
        
        for (Entity e : entities) {
            if (e == player || e instanceof ItemEntity || !(e instanceof LivingEntity le)) continue;
            
            Vec3 delta = e.position().subtract(player.position());
            double d = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
            if (d >= HBM_RADIUS || d <= 1.0E-6) continue;
            
            double intensity = (HBM_RADIUS - d);
            double nx = delta.x / d;
            double nz = delta.z / d;
            
            e.setDeltaMovement(
                    e.getDeltaMovement().x + nx * intensity * 2.0,
                    e.getDeltaMovement().y + 0.1D * intensity,
                    e.getDeltaMovement().z + nz * intensity * 2.0
            );
            e.hurtMarked = true;
            
            float damage = (float) (intensity * 10.0);
            le.hurt(com.hbm_m.damagesource.ModDamageSources.hardlandingSmash(player), damage);
        }
    }

    private static boolean isSoftBreakableForHardLanding(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.isAir()) return false;
        if (!state.getFluidState().isEmpty()) return false;
        
        // Базовые ванильные теги
        if (state.is(BlockTags.LEAVES)) return true;
        if (state.is(BlockTags.FLOWERS)) return true;
        if (state.is(BlockTags.SAPLINGS)) return true;
        if (state.is(BlockTags.REPLACEABLE)) return true;
        
        // Ваш datapack-тег (сюда добавляете cake, flower_pot, lily_pad, и т.д.)
        if (state.is(HBM_HARDLANDING_BREAKABLE)) return true;
        
        return false;
    }

    private static boolean breakSoftBlocksUnderPlayer(ServerLevel level, ServerPlayer player) {
        AABB bb = player.getBoundingBox();
        final double eps = 1.0E-6;
        int minX = Mth.floor(bb.minX + eps);
        int maxX = Mth.floor(bb.maxX - eps);
        int minZ = Mth.floor(bb.minZ + eps);
        int maxZ = Mth.floor(bb.maxZ - eps);
        
        // ВАЖНО: целимся в блок прямо под ногами, а не "minY-1"
        BlockPos baseBelow = player.blockPosition().below();
        int y0 = baseBelow.getY(); // блок под игроком (листва/трава/и т.п.)
        int y1 = y0 - 1; // ещё слой ниже (чтобы не застревал в густой растительности)
        
        boolean brokeAny = false;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                BlockPos p0 = new BlockPos(x, y0, z);
                BlockState s0 = level.getBlockState(p0);
                if (isSoftBreakableForHardLanding(level, p0, s0)) {
                    level.destroyBlock(p0, true);
                    brokeAny = true;
                }
                
                BlockPos p1 = new BlockPos(x, y1, z);
                BlockState s1 = level.getBlockState(p1);
                if (isSoftBreakableForHardLanding(level, p1, s1)) {
                    level.destroyBlock(p1, true);
                    brokeAny = true;
                }
            }
        }
        
        if (brokeAny) {
            Vec3 v = player.getDeltaMovement();
            player.setDeltaMovement(v.x, Math.min(v.y, -0.55D), v.z);
            player.hurtMarked = true;
        }
        
        return brokeAny;
    }

    // @SubscribeEvent
    // public static void onLivingFall(LivingFallEvent event) {
    //     if (!(event.getEntity() instanceof Player player)) return;
    //     if (player.level().isClientSide) return;
    //     if (!ModArmorFSB.hasFSBArmor(player)) return;
        
    //     ItemStack chestStack = player.getItemBySlot(EquipmentSlot.CHEST);
    //     if (!(chestStack.getItem() instanceof ModPowerArmorItem armorItem)) return;
        
    //     PowerArmorSpecs specs = armorItem.getSpecs();
    //     float fallDist = event.getDistance();
        
    //     // ЗАЩИТА ОТ УРОНА (В самом конце)
    //     if (fallDist > 2.0F) {
    //         // Если полная защита от падения через DT/DR
    //         if (specs.drFall >= 1.0F || specs.dtFall > 1000) {
    //             event.setDistance(0);
    //             event.setCanceled(true);
    //         }
    //     }
    // }
}