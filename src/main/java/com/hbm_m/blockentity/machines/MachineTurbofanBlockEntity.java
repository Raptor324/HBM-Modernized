package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.machines.MachineTurbofanBlock;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.MachinePollutingBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.compat.sable.TurbofanVehiclePhysics;
import com.hbm_m.damagesource.ModDamageSources;
import com.hbm_m.interfaces.IItemFluidIdentifier;
import com.hbm_m.interfaces.TurbofanAirflowFrame;
import com.hbm_m.inventory.UpgradeManager;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.fluid.trait.FT_Combustible;
import com.hbm_m.inventory.fluid.trait.FT_Combustible.FuelGrade;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.industrial.ItemMachineUpgrade;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.platform.PlatformHooks;
import com.hbm_m.particle.helper.IParticleCreator;
import com.hbm_m.sound.ClientSoundBootstrap;
import com.hbm_m.sound.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Турбовентилятор - 1:1-порт {@code TileEntityMachineTurbofan} (1.7.10).
 * Сжигает топливо AERO-грейда (керосин): {@code amountToBurn = min(1 + afterburner, fill)} mB,
 * {@code output = burnValue * amountToBurn * (1 + min(afterburner/3, 4))},
 * {@code burnValue = combustionEnergy / 1000}. Буфер 1 000 000 HE (оригинальный maxPower).
 * <p>
 * Красный камень останавливает горение по внешним соседям четырёх портов (оригинальный
 * {@code getConPos()}). Behlтер-слоты: идентификатор жидкости задаёт тип бака
 * ({@code tank.setType}), контейнер в слоте 0 сливается в бак, пустой уходит в слот 1
 * ({@code tank.loadTank(0, 1)}). Кровь: 50 mB за существо, убитое лопастями; гиблетсы и
 * звук сломанной двери при смерти. Форсаж-частицы - gasfire (scale 8/4), при форсаже >90
 * - звук перегрузки 1/30 тиков. Дым - заглушка {@link #emitBurnPollution()} (TODO, раз в 20 тиков).
 * <p>
 * Анимация ротора: поля spin/lastSpin/momentum продвигаются ТОЛЬКО на клиенте
 * (по флагу wasOn), идемпотентный тик безопасен и для BER. Толчок/застревание локального
 * игрока повторяются на клиенте, как в оригинале. На движущихся конструкциях зоны
 * эффектов пересчитываются через {@link TurbofanAirflowFrame} (compat-mixin, без него -
 * мировые координаты). См. также {@link TurbofanVehiclePhysics} - общие константы тяги.
 */
public class MachineTurbofanBlockEntity extends MachinePollutingBlockEntity
        implements com.hbm_m.api.fluids.IFluidStandardTransceiverMK2 {

    public static final int SLOT_FUEL_CONTAINER = 0;
    public static final int SLOT_EMPTY_CONTAINER = 1;
    public static final int SLOT_BATTERY = 2;
    public static final int SLOT_FLUID_IDENTIFIER = 3;
    /**
     * Слот форсажного улучшения. В оригинале это слот 2; здесь висит в конце, чтобы не
     * менять уже сохранённые инвентари по прежним индексам.
     */
    public static final int SLOT_UPGRADE = 4;
    private static final int INVENTORY_SIZE = 5;

    private static final long MAX_POWER = 1_000_000L;
    private static final int TANK_CAPACITY_MB = 24_000;
    private static final int BASE_BURN_MB_PER_TICK = 1;
    /** Оригинал: {@code blood = new FluidTank(Fluids.BLOOD, 24000)}. */
    private static final int BLOOD_CAPACITY_MB = 24_000;
    /** Оригинал: {@code blood.setFill(blood.getFill() + 50)} за каждое разобранное существо. */
    private static final int BLOOD_PER_KILL_MB = 50;
    /** Базовая дальность слышимости цикла (запас над слышимыми ~128 при attenuation 64). */
    public static final double SOUND_RANGE_BASE = 192.0D;
    /** Прибавка дальности звука и зон воздействия за уровень форсажа (клэмп 3, как сам форсаж). */
    public static final double SOUND_RANGE_PER_AFTERBURN_LEVEL = 64.0D;
    /**
     * Зоны выхлопа/забора и пламя форсажа: +20% НАРУЖНОЙ дальности за уровень форсажа.
     * Внутренние границы зон прижаты к машине (лопасти физически не двигаются) - иначе
     * перед лопастями возникает безопасный карман.
     */
    public static final double ZONE_REACH_PER_AFTERBURN_LEVEL = 0.20D;

    private final FluidTank tank = new FluidTank(ModFluids.KEROSENE.getSource(), TANK_CAPACITY_MB);
    private final FluidTank blood = new FluidTank(ModFluids.BLOOD.getSource(), BLOOD_CAPACITY_MB);

    private final UpgradeManager upgradeManager = new UpgradeManager();
    /** Оригинал: {@code afterburner} - уровень форсажа, 100 с Фламенпони. */
    private int afterburner = 0;
    /** Оригинал: {@code showBlood} - включает датчик крови в GUI. */
    private boolean showBlood = false;
    private boolean wasOn = false;
    private int output = 0;
    private int consumption = 0;
    /** wasOn на момент последнего отправленного синка - для пакета при переходе в выключение. */
    private boolean lastSyncWasOn = false;

    // Клиентская анимация ротора (в NBT не пишется - восстанавливается с нуля, как в оригинале).
    private float spin;
    private float lastSpin;
    private int momentum;
    private long lastClientAnimationTick = Long.MIN_VALUE;

    /**
     * Клиентский накал сопла 0..1 (в NBT не пишется): НЕ накопление от времени работы,
     * а плавное приближение к цели из синкнутой мощности ({@link #output} - она кодирует
     * и калорийность топлива, и уровень форсажа). Рендер сопла читает его для тинта/света.
     */
    private float glowHeat;
    private float lastGlowHeat;
    /** База накала: керосин без форсажа (burnValue 3850 * 1 mB) свечения не даёт. */
    private static final float GLOW_OUTPUT_BASE = 4000.0F;
    /** Верхняя реперная точка лог-шкалы: ~реформатор на полном форсаже даёт 1.0. */
    private static final float GLOW_OUTPUT_MAX = 50_000.0F;
    /** Скорость прогрева/остывания (доли шкалы за тик): разогрев быстрее остывания. */
    private static final float GLOW_HEAT_RATE_UP = 1.0F / 40.0F;
    private static final float GLOW_HEAT_RATE_DOWN = 1.0F / 70.0F;

    public MachineTurbofanBlockEntity(BlockPos pos, BlockState state) {
        // Оригинал: super(5, 150) - 5 слотов и 150 mB дымового буфера на сорт.
        super(ModBlockEntities.TURBOFAN_BE.get(), pos, state, INVENTORY_SIZE, MAX_POWER, 0L, 80_000L, 150);
    }

    // ═══════════════════════════ Тик ═══════════════════════════

    public static void tick(Level level, BlockPos pos, BlockState state, MachineTurbofanBlockEntity be) {
        if (level.isClientSide()) {
            be.clientTick();
            return;
        }
        be.serverTick(level, pos, state);
    }

    private void serverTick(Level level, BlockPos pos, BlockState state) {
        ensureNetworkInitialized();

        // Behlтер-слоты: тип бака по идентификатору, слив контейнера слот 0 -> бак, пустой -> слот 1.
        ItemStack[] slots = getSlotsArray();
        boolean slotsChanged = false;
        if (tank.setType(SLOT_FLUID_IDENTIFIER, slots)) slotsChanged = true;
        if (tank.loadTank(SLOT_FUEL_CONTAINER, SLOT_EMPTY_CONTAINER, slots)) slotsChanged = true;
        if (slotsChanged) {
            applySlotsArray(slots);
            // Оба метода выше срабатывают только на изменение (edge-triggered), так что
            // лишних синков нет. Без этого при простаивающей турбине смена типа/заливка
            // бочки не доходили до клиента: GUI показывал протухший бак до первого
            // синка горения - "бочка пропала, бак пуст, турбина не стартует".
            setChanged();
            sendUpdateToClient();
        }

        // Подписка бака на трубу за внешней гранью каждого порта; кровь - только НА ВЫДАЧУ
        // (оригинальный трансивер: getReceivingTanks = только топливо, getSendingTanks = кровь),
        // так что труба забирает её сама, без типизации бака идентификатором.
        if (level.getGameTime() % 20 == 0) {
            for (PortInfo port : getPorts()) {
                trySubscribe(tank, level, port.target(), port.outward());
                tryProvide(blood, level, port.target(), port.outward());
            }
        }

        output = 0;
        consumption = 0;
        wasOn = false;
        chargeItemInSlot(SLOT_BATTERY);
        updateAfterburner();

        Fluid fuel = tank.getStoredFluid();
        FT_Combustible combustible = FluidType.getTrait(fuel, FT_Combustible.class);

        // Оригинал: блокировка по красному камню на четырёх точках подключения.
        boolean redstone = hasPortRedstone(level);

        int amount = BASE_BURN_MB_PER_TICK + afterburner;
        int amountToBurn = Math.min(amount, tank.getFill());
        long burnValue = 0L;
        // Оригинал: burnValue вычисляется ВНУТРИ проверки красного камня, поэтому глушение
        // выключает сразу всё - горение, эффекты и зоны (wasOn=false, ротор и звук докручиваются).
        if (!redstone && combustible != null && combustible.getGrade() == FuelGrade.AERO) {
            burnValue = combustible.getCombustionEnergy() / 1_000L;
        }

        if (!redstone && burnValue > 0 && amountToBurn > 0) {
            wasOn = true;
            tank.drainMb(amountToBurn);
            // Оригинал: output = burnValue * amountToBurn * (1 + min(afterburner / 3, 4)).
            output = (int) (burnValue * amountToBurn * (1 + Math.min(afterburner / 3D, 4)));
            energy += output;
            consumption = amountToBurn;

            // Оригинал: раз в 20 тиков pollute(BURN, amountToBurn * 5) - аргумент amount
            // в оригинале игнорируется, дым считается от карты FT_Polluting (см. заглушку).
            if (level.getGameTime() % 20 == 0) {
                emitBurnPollution();
            }
        }

        sendSmokeAllDirections();

        if (burnValue > 0 && amountToBurn > 0) {
            runCombustionEffects(level, pos, state);
        }

        // Зажим - последним: батарея и сеть сначала разбирают избыток.
        if (energy > MAX_POWER) energy = MAX_POWER;
        if (energy < 0L) energy = 0L;

        // Оригинал синкается безусловно каждый тик; здесь - по деятельности ИЛИ при переходе
        // вкл/выкл: без последнего клиент залипает с wasOn=true, и глушёная машина
        // продолжает крутить ротор и играть звук.
        boolean active = wasOn || output > 0 || consumption > 0;
        if (active || lastSyncWasOn != wasOn) {
            lastSyncWasOn = wasOn;
            setChanged();
            sendUpdateToClient();
        }
    }

    private void clientTick() {
        ensureClientAnimationTick();
        repeatLocalPlayerMotion();
        // Радиус чуть шире слышимости (attenuation 64 * volume ~2..3 = 128..192), чтобы звук
        // не пересоздавался на границе; каждый уровень форсажа добавляет дальности.
        // BE на корабле в plot-координатах - сравниваем в мировых, иначе дистанция
        // до игрока гигантская и звук молчит/чурнится.
        ClientSoundBootstrap.updateSound(this, momentum > 0 && isLoopAudible(getSoundRange()),
                this::createLoopSoundReflect, getSoundRange());
    }

    /** Дальность слышимости цикла: база + прибавка за уровень форсажа (клиент читает синкнутый afterburner). */
    public double getSoundRange() {
        return SOUND_RANGE_BASE + Math.min(afterburner, 3) * SOUND_RANGE_PER_AFTERBURN_LEVEL;
    }

    /** Масштаб дальности зон воздействия (выхлоп/забор/лопасти) и пламени форсажа. */
    public double getZoneReachScale() {
        return 1.0D + Math.min(afterburner, 3) * ZONE_REACH_PER_AFTERBURN_LEVEL;
    }

    /** Клиентская проверка слышимости цикла в мировых координатах (мост Sable - тождество вне кораблей). */
    private boolean isLoopAudible(double range) {
        Level level = getLevel();
        if (level == null) return false;
        net.minecraft.world.phys.Vec3 world = com.hbm_m.compat.sable.SableCompat.toWorld(level,
                getBlockPos().getX() + 0.5D, getBlockPos().getY() + 1.5D, getBlockPos().getZ() + 0.5D);
        for (Player player : level.players()) {
            if (player.isLocalPlayer()
                    && player.distanceToSqr(world.x, world.y, world.z) <= range * range) {
                return true;
            }
        }
        return false;
    }

    /** Оригинал: цикл звука TURBOFAN_LOOP с миксом по momentum/форсажу. Клиентский класс — через звуковой мост. */
    private Object createLoopSoundReflect() {
        return com.hbm_m.sound.ClientSoundBootstrap.createTurbofanLoop(this);
    }

    // ═══════════════════════════ Анимация ротора ═══════════════════════════

    /**
     * Один тик ротора на игровой тик, идемпотентно: вызывается и тикером блока, и BER
     * (который может отработать раньше тикера, например на саб-уровне движущейся конструкции).
     */
    public void ensureClientAnimationTick() {
        Level level = getLevel();
        if (level == null || !level.isClientSide()) return;
        long gameTime = level.getGameTime();
        if (lastClientAnimationTick == gameTime) return;
        if (lastClientAnimationTick == Long.MIN_VALUE || gameTime < lastClientAnimationTick) {
            lastClientAnimationTick = gameTime - 1L;
        }
        // Догоняем чуть-чуть, не до тепловой смерти вселенной.
        long elapsed = Math.min(gameTime - lastClientAnimationTick, 200L);
        for (long tick = 0; tick < elapsed; tick++) advanceClientAnimationOneTick();
        lastClientAnimationTick = gameTime;
    }

    private void advanceClientAnimationOneTick() {
        lastSpin = spin;
        if (wasOn) {
            if (momentum < 100) momentum++;
        } else if (momentum > 0) {
            momentum--;
        }
        // Целочисленное деление даёт ротору его фирменную механическую медлительность.
        spin += momentum / 2;
        if (spin >= 360.0F) {
            spin -= 360.0F;
            lastSpin -= 360.0F;
        }

        // Накал сопла: монотонное приближение к цели без перелёта (сходимость точная,
        // чтобы skip-write тинта в рендере не «дрожал» младшими битами).
        lastGlowHeat = glowHeat;
        float target = glowTarget();
        if (glowHeat < target) {
            glowHeat = Math.min(glowHeat + GLOW_HEAT_RATE_UP, target);
        } else if (glowHeat > target) {
            glowHeat = Math.max(glowHeat - GLOW_HEAT_RATE_DOWN, target);
        }
        // Смена кванта накала = событие для рендера: статичный Body иначе обновляет
        // тинт/эмиссию только по TTL-пересбору (лесенка вместо плавного хода).
        if (Math.round(glowHeat * 64.0F) != Math.round(lastGlowHeat * 64.0F)) {
            markRenderDirty();
        }
    }

    /**
     * Цель накала из синкнутой мощности: лог-шкала output между базой (без свечения)
     * и репером ~50k HE/тик. Отдельного бонуса за форсаж нет - он уже внутри output
     * (количество сжигаемого топлива и множитель), а пламя позади даёт
     * {@code afterburner > 0} переключение сопла на горячую текстуру.
     */
    private float glowTarget() {
        if (!wasOn || output <= 0) return 0.0F;
        if (output <= GLOW_OUTPUT_BASE) return 0.0F;
        float t = (float) (Math.log((double) output / GLOW_OUTPUT_BASE)
                / Math.log(GLOW_OUTPUT_MAX / (double) GLOW_OUTPUT_BASE));
        return net.minecraft.util.Mth.clamp(t, 0.0F, 1.0F);
    }

    public float getSpin() { return spin; }
    public float getLastSpin() { return lastSpin; }
    public int getMomentum() { return momentum; }
    public boolean wasOn() { return wasOn; }
    public int getOutput() { return output; }
    public int getConsumption() { return consumption; }
    public float getGlowHeat() { return glowHeat; }
    public float getLastGlowHeat() { return lastGlowHeat; }

    // ═══════════════════════════ Порты мультиблока ═══════════════════════════

    private record PortInfo(BlockPos portPos, BlockPos target, Direction outward) { }

    /** Раскладка фиксирована после установки, кэшируем как оригинальный cachedPorts. */
    private java.util.List<PortInfo> cachedPorts;

    /** Четыре порта: позиция порта в мире, внешняя клетка и наружное направление. */
    private java.util.List<PortInfo> getPorts() {
        java.util.List<PortInfo> cached = cachedPorts;
        if (cached != null) return cached;

        java.util.List<PortInfo> ports = new java.util.ArrayList<>();
        if (getBlockState().getBlock() instanceof MachineTurbofanBlock block) {
            MultiblockStructureHelper helper = block.getStructureHelper();
            Direction facing = getBlockState().getValue(MachineTurbofanBlock.FACING);
            BlockPos core = getBlockPos();
            for (BlockPos localPos : helper.getStructureMap().keySet()) {
                if (!helper.resolvePartRole(localPos, block).canReceiveEnergy()) continue;
                BlockPos portPos = helper.getRotatedPos(core, localPos, facing);
                // Ряд порта относительно ядра по оси FACING: передний ряд наружу по FACING,
                // задний - против (оригинальный getConPos: ±rot). Проекция мирового смещения
                // на ось FACING; сравнение по мировой Z-компоненте работало только при
                // ориентации на север.
                BlockPos offset = portPos.subtract(core);
                int axisOffset = offset.getX() * facing.getStepX() + offset.getZ() * facing.getStepZ();
                Direction outward = axisOffset > 0 ? facing : facing.getOpposite();
                ports.add(new PortInfo(portPos, portPos.relative(outward), outward));
            }
        }
        return cachedPorts = java.util.List.copyOf(ports);
    }

    /** Оригинал: isBlockIndirectlyGettingPowered на точках подключения (не на ядре). */
    private boolean hasPortRedstone(Level level) {
        for (PortInfo port : getPorts()) {
            if (level.hasNeighborSignal(port.target())) return true;
        }
        return false;
    }

    /** Энергопорты мультиблока для сети (образец - промышленная турбина). */
    @Override
    protected BlockPos[] getExtraEnergyPorts() {
        java.util.List<BlockPos> ports = new java.util.ArrayList<>();
        for (PortInfo port : getPorts()) {
            ports.add(port.portPos());
        }
        return ports.toArray(new BlockPos[0]);
    }

    // ═══════════════════════════ Форсаж и слоты ═══════════════════════════

    /** Оригинал: getValidUpgrades() -> AFTERBURN -> 3 (UpgradeManagerNT клэмпит сумму тиров). */
    private static final java.util.Map<ItemMachineUpgrade.UpgradeType, Integer> UPGRADE_CAPS =
            java.util.Map.of(ItemMachineUpgrade.UpgradeType.AFTERBURN, 3);

    /** Оригинал: уровень AFTERBURN из слота улучшений (клэмп 3); Фламенпони ставит уровень 100. */
    private static final int FLAME_PONY_AFTERBURNER = 100;

    private void updateAfterburner() {
        upgradeManager.checkSlots(getInventory(), SLOT_UPGRADE, SLOT_UPGRADE, UPGRADE_CAPS);
        this.afterburner = upgradeManager.getLevel(ItemMachineUpgrade.UpgradeType.AFTERBURN);

        if (getInventory().getStackInSlot(SLOT_UPGRADE).is(ModItems.FLAME_PONY.get())) {
            this.afterburner = 100;
        }
    }

    // ═══════════════════════════ Зоны воздействия ═══════════════════════════

    /**
     * Три зоны оригинала вдоль оси забора воздуха ({@code airflow = FACING.getClockWise()},
     * как в оригинальной развёртке модели). Форсаж ({@link #getZoneReachScale()}) растягивает
     * зоны наружу, внутренние границы всегда прижаты к машине:
     * <ul>
     *   <li><b>Выхлоп</b> (-3.5..-19.5×reach позади): сдувает всё прочь; с форсажом поджигает
     *       на 5 с и наносит 5 урона.</li>
     *   <li><b>Забор</b> (+3.5..+8.5×reach впереди): затягивает всё к машине.</li>
     *   <li><b>Плоскость лопастей</b> (+3.5..+3.75, не масштабируется): 1000 абсолютного урона,
     *       застревание в паутине, гиблетсы и 50 mB крови за существо.</li>
     * </ul>
     */
    private void runCombustionEffects(Level level, BlockPos pos, BlockState state) {
        // Модель развёрнута вбок относительно установки - ось эффектов тоже.
        Direction airflow = state.getValue(MachineTurbofanBlock.FACING).getClockWise();
        Direction width = airflow.getClockWise();
        Vec3 airflowPush = localVectorToWorld(
                Vec3.atLowerCornerOf(airflow.getNormal()).scale(-0.2D));

        // Уровень форсажа растягивает зоны НАРУЖУ (дальше забор и выхлоп), но внутренние
        // границы прижаты к машине: физические лопасти не двигаются, и безопасного кармана
        // перед ними возникать не должно (иначе сущность, подошедшая вплотную, выживает).
        double reach = getZoneReachScale();

        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel && afterburner > 0) {
            emitAfterburnerParticles(serverLevel, pos, airflow, width, reach);
        }

        // Выхлоп позади машины (дальняя граница растягивается с форсажем).
        AABB exhaustLocal = directionalBounds(pos, airflow, width, -3.5D, -19.5D * reach);
        // Забор перед машиной (дальняя граница растягивается с форсажем).
        AABB intakeLocal = directionalBounds(pos, airflow, width, 3.5D, 8.5D * reach);
        // Плоскость лопастей - всегда вплотную к грани, физические лопасти на месте.
        AABB bladesLocal = directionalBounds(pos, airflow, width, 3.5D, 3.75D);

        // На движущейся конструкции BE лежит в plot-чанках родительского уровня (за 20 млн),
        // а сущности - в том же уровне, но в мировых координатах: зоны переводятся мостом
        // конструкции в мир (вне конструкции мост - тождество, боксы остаются локальными).
        AABB exhaust = worldBoundsFromLocal(exhaustLocal);
        AABB intake = worldBoundsFromLocal(intakeLocal);
        AABB blades = worldBoundsFromLocal(bladesLocal);
        processAirflowZones(level, exhaust, intake, blades, airflowPush);

        // Flame pony (afterburner = 100): турбина "плюётся" пламенем во все стороны -
        // поджигает всех в радиусе 3 блоков, кроме огнестойких (иммунитет к огню,
        // зелье огнестойкости, асбестовая броня - аналог ArmorAsbestos из оригинала).
        if (afterburner >= FLAME_PONY_AFTERBURNER) {
            AABB hotZone = new AABB(pos).inflate(3.0D);
            for (Entity entity : level.getEntitiesOfClass(Entity.class, hotZone)) {
                if (!entity.isAlive() || isProtectedFromFire(entity)) continue;
                PlatformHooks.setSecondsOnFire(entity, 5);
            }
        }
    }

    /** Огнестойкость сущности: иммунитет к огню, зелье или асбестовая броня (хотя бы одна деталь). */
    private static boolean isProtectedFromFire(Entity entity) {
        if (entity.fireImmune()) return true;
        if (entity instanceof LivingEntity living
                && living.hasEffect(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE)) {
            return true;
        }
        if (entity instanceof LivingEntity living2) {
            for (var slot : living2.getArmorSlots()) {
                if (slot.is(ModItems.ASBESTOS_HELMET.get()) || slot.is(ModItems.ASBESTOS_CHESTPLATE.get())
                        || slot.is(ModItems.ASBESTOS_LEGGINGS.get()) || slot.is(ModItems.ASBESTOS_BOOTS.get())) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Три зоны оригинала: выхлоп (сдув/поджог), забор (затягивание), лопасти (блендер). */
    private void processAirflowZones(Level level, AABB exhaust, AABB intake, AABB blades, Vec3 airflowPush) {
        // Сущности и зоны в одном пространстве (локальном или мировом) - прямая проверка;
        // обратный transformInverse здесь портил бы интерсекцию.
        for (Entity entity : level.getEntitiesOfClass(Entity.class, exhaust)) {
            if (!entity.getBoundingBox().intersects(exhaust)) continue;
            if (afterburner > 0) {
                PlatformHooks.setSecondsOnFire(entity, 5);
                entity.hurt(level.damageSources().onFire(), 5F);
            }
            push(entity, airflowPush);
        }

        for (Entity entity : level.getEntitiesOfClass(Entity.class, intake)) {
            if (!entity.getBoundingBox().intersects(intake)) continue;
            push(entity, airflowPush);
        }

        for (Entity entity : level.getEntitiesOfClass(Entity.class, blades)) {
            if (!entity.getBoundingBox().intersects(blades)) continue;
            entity.hurt(ModDamageSources.blender(level), 1000F);
            entity.makeStuckInBlock(Blocks.COBWEB.defaultBlockState(), new Vec3(0.25D, 0.05D, 0.25D));

            if (!entity.isAlive() && entity instanceof LivingEntity) {
                CompoundTag gib = new CompoundTag();
                gib.putString("type", "giblets");
                gib.putInt("ent", entity.getId());
                IParticleCreator.sendPacket((net.minecraft.server.level.ServerLevel) level,
                        entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ(), 150, gib);
                level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                        SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.BLOCKS,
                        2.0F, 0.95F + level.random.nextFloat() * 0.2F);

                blood.setFill(Math.min(blood.getFill() + BLOOD_PER_KILL_MB, blood.getMaxFill()));
                showBlood = true;
                setChanged();
            }
        }
    }

    /** Мировой AABB локальной зоны: углы через мост конструкции, обёртка по min/max. */
    private AABB worldBoundsFromLocal(AABB localBounds) {
        Vec3 first = hbm$localPosToWorld(new Vec3(localBounds.minX, localBounds.minY, localBounds.minZ));
        Vec3 second = hbm$localPosToWorld(new Vec3(localBounds.maxX, localBounds.maxY, localBounds.maxZ));
        return new AABB(
                Math.min(first.x, second.x), Math.min(first.y, second.y), Math.min(first.z, second.z),
                Math.max(first.x, second.x), Math.max(first.y, second.y), Math.max(first.z, second.z));
    }

    /**
     * TODO(parité-turbofan): реальная логика дымообразования при горении. Вызывается раз в 20
     * тиков при активном горении (в оригинале - {@code super.pollute(tankType, BURN, amountToBurn * 5)},
     * где аргумент amount ИГНОРИРУЕТСЯ). Алгоритм 1.7.10 ({@code TileEntityMachinePolluting.pollute}):
     * <ol>
     *   <li>Взять {@code FT_Polluting}-трейт типа бака; при горении используется его {@code burnMap}.</li>
     *   <li>Для каждого {@code (PollutionType, value)}: дым в бак {@code ceil(value * 100)} mB
     *       (для керосина/P_FUEL: SOOT 0.001 -> 1 mB, POISON 0.00005 -> 1 mB за вызов, т.е. в секунду).</li>
     *   <li>Излишек сверх ёмкости смок-бака (150 mB) уходит в мировое загрязнение
     *       ({@code PollutionHandler}) с шипением (оригинальный FIRE_EXTINGUISH-звук, как в NTM-Reworked).</li>
     * </ol>
     * Пока заглушка: дым не выделяется (прежний вызов с amountToBurn * 5 завышал выброс и был удалён).
     */
    private void emitBurnPollution() {
        // TODO(turbofan-parity): implement per algorithm above; remove this stub body then.
    }

    /** Оригинал: газовое пламя (AuxParticle gasfire) позади и над машиной при форсаже. */
    private void emitAfterburnerParticles(net.minecraft.server.level.ServerLevel level, BlockPos pos,
                                          Direction airflow, Direction width, double reach) {
        for (int index = 0; index < 2; index++) {
            double speed = 2.0D + level.random.nextDouble() * 3.0D;
            double deviation = level.random.nextGaussian() * 0.2D;
            double x = pos.getX() + 0.5D - airflow.getStepX() * (3 - index) * reach;
            double y = pos.getY() + 1.5D;
            double z = pos.getZ() + 0.5D - airflow.getStepZ() * (3 - index);

            CompoundTag data = new CompoundTag();
            data.putString("type", "gasfire");
            data.putDouble("mX", -airflow.getStepX() * speed + deviation);
            data.putDouble("mY", 0.0D);
            data.putDouble("mZ", -airflow.getStepZ() * speed + deviation);
            data.putFloat("scale", 8.0F);
            IParticleCreator.sendPacket(level, x, y, z, 100, data);
        }

        if (afterburner <= 90) return;
        if (level.random.nextInt(30) == 0) {
            PlatformHooks.playSound(level, pos, ModSounds.MACHINE_DAMAGE.get(),
                    SoundSource.BLOCKS, 3.0F, 0.95F + level.random.nextFloat() * 0.2F);
        }

        double along = level.random.nextDouble() * 4.0D - 2.0D;
        double across = level.random.nextDouble() * 2.0D - 1.0D;
        double x = pos.getX() + 0.5D + airflow.getStepX() * along + width.getStepX() * across;
        double y = pos.getY() + 1.0D + level.random.nextDouble() * 2.0D;
        double z = pos.getZ() + 0.5D + airflow.getStepZ() * along + width.getStepZ() * across;

        CompoundTag data = new CompoundTag();
        data.putString("type", "gasfire");
        data.putDouble("mX", 0.0D);
        data.putDouble("mY", level.random.nextDouble() * 0.1D);
        data.putDouble("mZ", 0.0D);
        data.putFloat("scale", 4.0F);
        IParticleCreator.sendPacket(level, x, y, z, 100, data);
    }

    /** Повтор толчка и застревания для локального игрока (клиент): оригинальный клиентский дубль зон. */
    private void repeatLocalPlayerMotion() {
        if (!wasOn || getLevel() == null) return;
        Player localPlayer = null;
        for (Player player : getLevel().players()) {
            if (player.isLocalPlayer()) {
                localPlayer = player;
                break;
            }
        }
        if (localPlayer == null || localPlayer.getAbilities().instabuild) return;

        BlockPos pos = getBlockPos();
        Direction airflow = getBlockState().getValue(MachineTurbofanBlock.FACING).getClockWise();
        Direction width = airflow.getClockWise();
        AABB playerBounds = worldBoundsToAirflowFrame(localPlayer.getBoundingBox());
        Vec3 airflowPush = localVectorToWorld(
                Vec3.atLowerCornerOf(airflow.getNormal()).scale(-0.2D));
        double reach = getZoneReachScale();

        if (playerBounds.intersects(directionalBounds(pos, airflow, width, -3.5D, -19.5D * reach))
                || playerBounds.intersects(directionalBounds(pos, airflow, width, 3.5D, 8.5D * reach))) {
            push(localPlayer, airflowPush);
        }
        if (playerBounds.intersects(directionalBounds(pos, airflow, width, 3.5D, 3.75D))) {
            localPlayer.makeStuckInBlock(Blocks.COBWEB.defaultBlockState(), new Vec3(0.25D, 0.05D, 0.25D));
        }
    }

    private static void push(Entity entity, Vec3 impulse) {
        entity.setDeltaMovement(entity.getDeltaMovement().add(impulse));
        entity.hurtMarked = true;
    }

    private static AABB directionalBounds(BlockPos pos, Direction axis, Direction width, double first, double second) {
        double centerX = pos.getX() + 0.5D;
        double centerZ = pos.getZ() + 0.5D;
        double firstX = centerX + axis.getStepX() * first - width.getStepX() * 1.5D;
        double firstZ = centerZ + axis.getStepZ() * first - width.getStepZ() * 1.5D;
        double secondX = centerX + axis.getStepX() * second + width.getStepX() * 1.5D;
        double secondZ = centerZ + axis.getStepZ() * second + width.getStepZ() * 1.5D;
        return new AABB(Math.min(firstX, secondX), pos.getY(), Math.min(firstZ, secondZ),
                Math.max(firstX, secondX), pos.getY() + 3.0D, Math.max(firstZ, secondZ));
    }

    // ═══════════════════════════ Мост системы координат ═══════════════════════════

    private Vec3 localVectorToWorld(Vec3 localVector) {
        if ((Object) this instanceof TurbofanAirflowFrame frame) {
            return frame.hbm$localVectorToWorld(localVector);
        }
        return localVector;
    }

    private AABB worldBoundsToAirflowFrame(AABB worldBounds) {
        if ((Object) this instanceof TurbofanAirflowFrame frame) {
            return frame.hbm$worldBoundsToLocal(worldBounds);
        }
        return worldBounds;
    }

    private Vec3 hbm$localPosToWorld(Vec3 localPos) {
        if ((Object) this instanceof TurbofanAirflowFrame frame) {
            return frame.hbm$localPosToWorld(localPos);
        }
        return localPos;
    }

    // ═══════════════════════════ Баки и инвентарь ═══════════════════════════

    private ItemStack[] getSlotsArray() {
        ItemStack[] slots = new ItemStack[INVENTORY_SIZE];
        for (int i = 0; i < INVENTORY_SIZE; i++) slots[i] = getInventory().getStackInSlot(i);
        return slots;
    }

    private void applySlotsArray(ItemStack[] slots) {
        for (int i = 0; i < INVENTORY_SIZE; i++) getInventory().setStackInSlot(i, slots[i]);
    }

    @Override
    public FluidTank[] getAllTanks() {
        return new FluidTank[] { tank, blood, smoke, smokeLeaded, smokePoison };
    }

    @Override
    public FluidTank[] getReceivingTanks() {
        return new FluidTank[] { tank };
    }

    @Override
    public FluidTank[] getSendingTanks() {
        // Оригинал: кроме дыма наружу уходит и кровь.
        FluidTank[] smokeTanks = getSmokeTanks();
        return new FluidTank[] { blood, smokeTanks[0], smokeTanks[1], smokeTanks[2] };
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved();
    }

    public FluidTank getTank() { return tank; }
    public FluidTank getBloodTank() { return blood; }
    public boolean isShowingBlood() { return showBlood; }
    public int getAfterburner() { return afterburner; }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) return isEnergyReceiverItem(stack);
        if (slot == SLOT_FLUID_IDENTIFIER) return stack.getItem() instanceof IItemFluidIdentifier;
        if (slot == SLOT_FUEL_CONTAINER) return true;
        if (slot == SLOT_UPGRADE) {
            return stack.getItem() instanceof ItemMachineUpgrade || stack.is(ModItems.FLAME_PONY.get());
        }
        return false;
    }

    // ═══════════════════════════ NBT и синхронизация ═══════════════════════════

    @Override
    protected void writeNbtData(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tank.writeToNBT(tag, "fuel");
        blood.writeToNBT(tag, "blood");
        tag.putBoolean("showBlood", showBlood);
        tag.putInt("afterburner", afterburner);
        tag.putBoolean("wasOn", wasOn);
        tag.putInt("output", output);
        tag.putInt("consumption", consumption);
    }

    @Override
    protected void readNbtData(CompoundTag tag, HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        tank.readFromNBT(tag, "fuel");
        blood.readFromNBT(tag, "blood");
        showBlood = tag.getBoolean("showBlood");
        afterburner = Math.max(tag.getInt("afterburner"), 0);
        wasOn = tag.getBoolean("wasOn");
        output = Math.max(tag.getInt("output"), 0);
        consumption = Math.max(tag.getInt("consumption"), 0);
    }

    // ═══════════════════════════ GUI / меню ═══════════════════════════

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.hbm_m.turbofan");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new com.hbm_m.inventory.menu.MachineTurbofanMenu(id, inv, this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.turbofan");
    }

    /** Границы рендера = весь габарит 7x3x3 (образец - промышленная турбина). */
    public net.minecraft.world.phys.AABB getRenderBoundingBox() {
        if (!(getBlockState().getBlock() instanceof MachineTurbofanBlock block)) {
            return new AABB(getBlockPos());
        }
        MultiblockStructureHelper helper = block.getStructureHelper();
        Direction facing = getBlockState().getValue(MachineTurbofanBlock.FACING);
        AABB bounds = new AABB(getBlockPos());
        for (BlockPos localPos : helper.getStructureMap().keySet()) {
            bounds = bounds.minmax(new AABB(helper.getRotatedPos(getBlockPos(), localPos, facing)));
        }
        return bounds.inflate(0.25D);
    }
}
