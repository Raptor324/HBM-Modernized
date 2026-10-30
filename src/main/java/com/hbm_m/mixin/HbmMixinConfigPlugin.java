package com.hbm_m.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Keeps {@code com.hbm_m.mixin.client.*} off a dedicated server. The "client" list of the mixin
 * config is not enough: the loader's mixin environment is not side-aware, and a client mixin
 * whose target survives dist cleaning (NeoForge's own ClientPayloadHandler does) is applied on
 * the server, where frame recomputation then loads client-only classes and aborts startup.
 */
public final class HbmMixinConfigPlugin implements IMixinConfigPlugin {

    private static final String CLIENT_PACKAGE = "com.hbm_m.mixin.client.";
    /** Compat-миксины, применяемые только при наличии целевого мода в рантайме. */
    private static final String SABLE_COMPAT_PACKAGE = "com.hbm_m.mixin.compat.sable.";
    private static final String CREATE_COMPAT_PACKAGE = "com.hbm_m.mixin.compat.create.";
    /**
     * Create-миксины вне compat-пакета: клиентский оутлайн-хелпер в client-пакете
     * (там его держит серверный гейт) тоже требует Create в рантайме.
     */
    private static final String SUPER_GLUE_OUTLINE_MIXIN =
            "com.hbm_m.mixin.client.SuperGlueSelectionOutlineMixin";
    /**
     * Iris-миксины: применяются только когда Iris/Oculus реально загружен.
     * Iris/Oculus - опциональная зависимость (ноль Iris-классов на compile
     * classpath): без такой загрузки цель @Pseudo-миксина отсутствует, а мы не
     * хотим даже попыток применения. Имена классов перечисляем явно.
     */
    private static final String IRIS_PIPELINE_MIXIN =
            "com.hbm_m.mixin.client.MixinIrisRenderingPipelinePreComposite";
    private static final String IRIS_SHADOW_MIXIN =
            "com.hbm_m.mixin.client.MixinIrisShadowRenderer";
    /**
     * NeoForge-специфичные стабы: на 1.20.1 (Forge) таргет-класса
     * {@code ClientPayloadHandler} не существует, а стабы - пустые
     * {@code @Mixin(AbstractContainerMenu.class)}-заглушки, которым нечего
     * применять. Отключаем их на Forge, чтобы они не висели на ванильном классе.
     */
    private static final String NEOFORGE_PAYLOAD_STUBS_PREFIX =
            "com.hbm_m.mixin.client.ClientPayloadHandler";

    private static boolean dedicatedServer() {
        //? if forge {
        return net.minecraftforge.fml.loading.FMLEnvironment.dist.isDedicatedServer();
        //?} elif neoforge {
        /*return net.minecraftforge.fml.loading.FMLEnvironment.dist.isDedicatedServer();
        *///?} else {
        /*return false;
        *///?}
    }

    private static boolean sableLoaded() {
        // ВАЖНО: shouldApplyMixin вызывается при загрузке конфига миксинов, КОГДА ModList ещё
        // не построен (решение принимается до конструирования модов) - ModList.get() тут null
        // и гейт молча скипал миксин навсегда. LoadingModList доступен сразу после discovery.
        //? if forge {
        var loading = net.minecraftforge.fml.loading.FMLLoader.getLoadingModList();
        return loading != null && loading.getModFileById("sable") != null;
        //?} elif neoforge {
        /*var loading = net.neoforged.fml.loading.FMLLoader.getLoadingModList();
        return loading != null && loading.getModFileById("sable") != null;
        *///?} else {
        /*return false;
        *///?}
    }

    private static boolean createLoaded() {
        //? if forge {
        var loading = net.minecraftforge.fml.loading.FMLLoader.getLoadingModList();
        return loading != null && loading.getModFileById("create") != null;
        //?} elif neoforge {
        /*var loading = net.neoforged.fml.loading.FMLLoader.getLoadingModList();
        return loading != null && loading.getModFileById("create") != null;
        *///?} else {
        /*return false;
        *///?}
    }

    private static boolean irisOrOculusLoaded() {
        // Как в sableLoaded(): shouldApplyMixin вызывается до построения ModList,
        // LoadingModList доступен сразу после discovery. Iris на NeoForge/Fabric
        // и Oculus на Forge - разные modid, проверяем оба.
        //? if forge {
        var loading = net.minecraftforge.fml.loading.FMLLoader.getLoadingModList();
        return loading != null && (loading.getModFileById("iris") != null || loading.getModFileById("oculus") != null);
        //?} elif neoforge {
        /*var loading = net.neoforged.fml.loading.FMLLoader.getLoadingModList();
        return loading != null && (loading.getModFileById("iris") != null || loading.getModFileById("oculus") != null);
        *///?} else {
        /*return false;
        *///?}
    }

    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.startsWith(CLIENT_PACKAGE) && dedicatedServer()) {
            return false;
        }
        // NeoForge-стабы ClientPayloadHandler*: на Forge (1.20.1) им нечего
        // применять - отключаем всегда.
        if (mixinClassName.startsWith(NEOFORGE_PAYLOAD_STUBS_PREFIX)) {
            //? if forge {
            return false;
            //?} elif neoforge {
            /*return true;
            *///?}
        }
        // Мод опционален: без него миксин, добавляющий интерфейсы мода в наш класс, сломал бы загрузку BE.
        if (mixinClassName.startsWith(SABLE_COMPAT_PACKAGE)) {
            return sableLoaded();
        }
        if (mixinClassName.startsWith(CREATE_COMPAT_PACKAGE) || SUPER_GLUE_OUTLINE_MIXIN.equals(mixinClassName)) {
            return createLoaded();
        }
        // Iris/Oculus опциональны: миксин в их пайплайн применяется только при
        // реальном наличии мода (без него @Pseudo-цель всё равно отсутствует).
        if (IRIS_PIPELINE_MIXIN.equals(mixinClassName) || IRIS_SHADOW_MIXIN.equals(mixinClassName)) {
            return irisOrOculusLoaded();
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
