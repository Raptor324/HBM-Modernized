package com.hbm_m.inventory.fluid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.fluid.trait.FT_Corrosive;
import com.hbm_m.inventory.fluid.trait.FluidTrait;
import com.hbm_m.inventory.fluid.trait.FluidTraitSimple.FT_Amat;
import com.hbm_m.inventory.fluid.trait.FluidTraitSimple.FT_LeadContainer;
import com.hbm_m.inventory.fluid.trait.FluidTraitSimple.FT_NoContainer;
import com.hbm_m.inventory.fluid.trait.FluidTraitSimple.FT_NoID;
import com.hbm_m.inventory.fluid.trait.FluidTraitSimple.FT_Viscous;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Порт 1.7.10 {@code com.hbm.inventory.fluid.FluidType}.
 *
 * <p>Хранит per-fluid метаданные: температуру и набор {@link FluidTrait}.
 * Имя/тинт-цвет вычисляются из {@link Fluid} и {@link ModFluids}.
 *
 * <p>Статическая часть класса заменяет роль 1.7.10 {@code Fluids} — мапа {@link Fluid} → {@link FluidType}.
 *
 * <p>Соответствие методов с оригиналом 1.7.10:
 * <ul>
 *   <li>{@link #setTemp(int)}, {@link #getTemperature()}, {@link #isHot()}</li>
 *   <li>{@link #addTraits(FluidTrait...)}, {@link #hasTrait(Class)}, {@link #getTrait(Class)}</li>
 *   <li>{@link #getName()}, {@link #getColor()}, {@link #getTint()},
 *       {@link #getUnlocalizedName()}, {@link #getLocalizedName()}, {@link #getConditionalName()},
 *       {@link #getDict(int)}</li>
 *   <li>{@link #isCorrosive()}, {@link #isAntimatter()}, {@link #hasNoContainer()},
 *       {@link #hasNoID()}, {@link #needsLeadContainer()}, {@link #isDispersable()}</li>
 *   <li>{@link #onTankBroken(BlockEntity, FluidTank)},
 *       {@link #onTankUpdate(BlockEntity, FluidTank)},
 *       {@link #onFluidRelease(BlockEntity, FluidTank, int)},
 *       {@link #onFluidRelease(Level, BlockPos, FluidTank, int)}</li>
 *   <li>{@link #addInfo(boolean, List)} — порт 1.7.10 {@code FluidType.addInfo(List)}</li>
 * </ul>
 */
public class FluidType {

    /** Same numerical value as 1.7.10 {@code FluidType.ROOM_TEMPERATURE} (20°C). */
    public static final int ROOM_TEMPERATURE = 20;

    // ═══════════════════════════════════════════════════════════════════════
    //                          Реестр Fluid → FluidType
    // ═══════════════════════════════════════════════════════════════════════

    private static final Map<Fluid, FluidType> TYPES = new HashMap<>();

    /** Возвращает {@link FluidType} для жидкости (создаёт пустой при первом обращении). */
    public static FluidType forFluid(Fluid fluid) {
        return TYPES.computeIfAbsent(fluid, FluidType::new);
    }

    public static void setTemperatureCelsius(Fluid fluid, int tempC) {
        forFluid(fluid).setTemp(tempC);
    }

    /** Комнатная температура (20°C), если для жидкости ещё не задавали температуру. */
    public static int getTemperatureCelsius(Fluid fluid) {
        FluidType t = TYPES.get(fluid);
        return t != null ? t.getTemperature() : ROOM_TEMPERATURE;
    }

    public static void addTrait(Fluid fluid, FluidTrait trait) {
        forFluid(fluid).addTraits(trait);
    }

    public static <T extends FluidTrait> T getTrait(Fluid fluid, Class<T> traitClass) {
        FluidType t = TYPES.get(fluid);
        return t != null ? t.getTrait(traitClass) : null;
    }

    public static boolean hasTrait(Fluid fluid, Class<? extends FluidTrait> traitClass) {
        FluidType t = TYPES.get(fluid);
        return t != null && t.hasTrait(traitClass);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //                              Состояние
    // ═══════════════════════════════════════════════════════════════════════

    private final Fluid fluid;
    /** В 1.7.10 поле {@code public int temperature}. */
    public int temperature = ROOM_TEMPERATURE;
    /** В 1.7.10 поле {@code public HashMap<Class<? extends FluidTrait>, FluidTrait> traits}. */
    public final HashMap<Class<? extends FluidTrait>, FluidTrait> traits = new HashMap<>();

    public FluidType(Fluid fluid) {
        this.fluid = fluid;
    }

    public Fluid getFluid() {
        return fluid;
    }

    public int getTemperature() {
        return temperature;
    }

    public FluidType setTemp(int temperatureCelsius) {
        this.temperature = temperatureCelsius;
        return this;
    }

    public FluidType addTraits(FluidTrait... toAdd) {
        for (FluidTrait t : toAdd) {
            this.traits.put(t.getClass(), t);
        }
        return this;
    }

    public boolean hasTrait(Class<? extends FluidTrait> clazz) {
        return this.traits.containsKey(clazz);
    }

    @SuppressWarnings("unchecked")
    public <T extends FluidTrait> T getTrait(Class<? extends T> clazz) {
        return (T) this.traits.get(clazz);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //                          Идентификация / имена
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Возвращает HBM-имя жидкости (без суффикса {@code _source}). 1.7.10 {@code getName()} → {@code stringId}.
     *
     * <p>Здесь же реализована логика, ранее жившая в {@code HbmFluidRegistry.getFluidName}.
     */
    public String getName() {
        if (fluid == null || fluid == Fluids.EMPTY) return "none";
        ResourceLocation loc = BuiltInRegistries.FLUID.getKey(fluid);
        if (loc == null) return "none";
        String path = loc.getPath();
        return path.endsWith("_source") ? path.substring(0, path.length() - "_source".length()) : path;
    }

    /** 1.7.10 {@code getColor()} — цвет, использовавшийся для рендера труб. В 1.20.1 это тот же tint, что и {@link #getTint()}. */
    public int getColor() {
        return ModFluids.getTintColor(getName());
    }

    /** 1.7.10 {@code getTint()} — GUI-тинт. В 1.20.1 хранится в {@code ModFluids.TINT_COLORS}. */
    public int getTint() {
        return ModFluids.getTintColor(getName());
    }

    /**
     * 1.7.10 {@code getUnlocalizedName()} = {@code "hbmfluid." + name}.
     * Возвращаем тот же префикс для парности; для реальной локализации см. {@link #getLocalizedName()}.
     */
    public String getUnlocalizedName() {
        return "hbmfluid." + getName();
    }

    /**
     * 1.7.10 {@code getLocalizedName()}: {@code I18nUtil.resolveKey(unlocalized)}.
     * В 1.20.1 используем translation-key из {@link Fluid}-реестра через Architectury, плюс
     * специальные имена для {@link Fluids#EMPTY} и {@link ModFluids#NONE}.
     */
    public Component getLocalizedName() {
        if (fluid == null || fluid == Fluids.EMPTY) {
            return Component.translatable("gui.hbm_m.fluid_tank.empty");
        }
        if (fluid == ModFluids.NONE.getSource()) {
            return Component.translatable("fluid.hbm_m.none");
        }
        return dev.architectury.fluid.FluidStack.create(fluid, 1L).getName();
    }

    /** 1.7.10 {@code getConditionalName()}: для серверного кода без I18n. */
    public String getConditionalName() {
        return getUnlocalizedName();
    }

    /** 1.7.10 {@code getDict(int)}: префикс из {@code GeneralConfig.enableFluidContainerCompat ? "container" : "ntmcontainer"} + объём + имя без {@code _}. */
    public String getDict(int quantity) {
        return "ntmcontainer" + quantity + getName().replace("_", "").toLowerCase(Locale.US);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //                       Convenience trait-проверки
    // ═══════════════════════════════════════════════════════════════════════

    public boolean isHot() {
        return this.temperature >= 100;
    }

    public boolean isCorrosive() {
        return hasTrait(FT_Corrosive.class);
    }

    public boolean isAntimatter() {
        return hasTrait(FT_Amat.class);
    }

    public boolean hasNoContainer() {
        return hasTrait(FT_NoContainer.class);
    }

    public boolean hasNoID() {
        return hasTrait(FT_NoID.class);
    }

    public boolean needsLeadContainer() {
        return hasTrait(FT_LeadContainer.class);
    }

    public boolean isDispersable() {
        return !(hasTrait(FT_Amat.class) || hasTrait(FT_NoContainer.class) || hasTrait(FT_Viscous.class));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //                          Lifecycle hooks
    // ═══════════════════════════════════════════════════════════════════════

    /** 1.7.10 {@code onTankBroken(TileEntity, FluidTank)}: вызывается при разрушении хранилища. */
    public void onTankBroken(BlockEntity be, FluidTank tank) { }

    /** 1.7.10 {@code onTankUpdate(TileEntity, FluidTank)}: вызывается каждый тик из update-метода. */
    public void onTankUpdate(BlockEntity be, FluidTank tank) { }

    /**
     * 1.7.10 {@code onFluidRelease(TileEntity, FluidTank, int)}: делегирует в координатную форму, как в оригинале.
     */
    public void onFluidRelease(BlockEntity be, FluidTank tank, int overflowAmount) {
        if (be == null || be.getLevel() == null) return;
        onFluidRelease(be.getLevel(), be.getBlockPos(), tank, overflowAmount);
    }

    /** 1.7.10 {@code onFluidRelease(World, x, y, z, FluidTank, int)} — пустая реализация по умолчанию. */
    public void onFluidRelease(Level level, BlockPos pos, FluidTank tank, int overflowAmount) { }

    // ═══════════════════════════════════════════════════════════════════════
    //                              Tooltip
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Прямой порт 1.7.10 {@code FluidType.addInfo(List<String> info)}.
     *
     * <p>Порядок строк (1:1 с оригиналом):
     * <ol>
     *   <li>температурная строка, если {@code temperature != ROOM_TEMPERATURE} (синяя если &lt;0, красная если &gt;0)</li>
     *   <li>для каждого {@code clazz} из {@link FluidTrait#traitList}: {@code trait.addInfo(info)},
     *       и если зажат шифт — также {@code trait.addInfoHidden(info)}</li>
     *   <li>если хотя бы у одного трейта есть скрытые строки и шифт не зажат — подсказка
     *       «hold LSHIFT for more info»</li>
     * </ol>
     */
    public void addInfo(boolean shiftDown, List<Component> info) {
        if (temperature != ROOM_TEMPERATURE) {
            // 1:1 с 1.7.10: число печатается как есть, без сокращений (k/M/...).
            if (temperature < 0) {
                info.add(Component.literal(temperature + "°C").withStyle(ChatFormatting.BLUE));
            }
            if (temperature > 0) {
                info.add(Component.literal(temperature + "°C").withStyle(ChatFormatting.RED));
            }
        }

        List<Component> hidden = new ArrayList<>();
        for (Class<? extends FluidTrait> clazz : FluidTrait.traitList) {
            FluidTrait trait = getTrait(clazz);
            if (trait != null) {
                trait.addInfo(info);
                if (shiftDown) {
                    trait.addInfoHidden(info);
                }
                trait.addInfoHidden(hidden);
            }
        }

        if (!hidden.isEmpty() && !shiftDown) {
            info.add(Component.translatable("gui.hbm_m.fluid_tank.hold_shift_more")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}
