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

    private static boolean dedicatedServer() {
        //? if forge {
        return net.minecraftforge.fml.loading.FMLEnvironment.dist.isDedicatedServer();
        //?} elif neoforge {
        /*return net.neoforged.fml.loading.FMLEnvironment.dist.isDedicatedServer();
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
        return !(mixinClassName.startsWith(CLIENT_PACKAGE) && dedicatedServer());
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
