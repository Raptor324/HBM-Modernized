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

    private static boolean dedicatedServer() {
        //? if forge {
        return net.minecraftforge.fml.loading.FMLEnvironment.dist.isDedicatedServer();
        //?} elif neoforge {
        /*return net.neoforged.fml.loading.FMLEnvironment.dist.isDedicatedServer();
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
        // Мод опционален: без него миксин, добавляющий интерфейсы мода в наш класс, сломал бы загрузку BE.
        if (mixinClassName.startsWith(SABLE_COMPAT_PACKAGE)) {
            return sableLoaded();
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
