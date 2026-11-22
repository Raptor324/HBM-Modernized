package com.hbm_m.compat.jade;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.BaseHbmBlockEntity;

import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Jade (WAILA) integration for HBM machines.
 *
 * <p>This class is only ever instantiated by Jade's {@code @WailaPlugin} classpath scan, so with
 * Jade absent it is never loaded and the missing API classes cannot break the game (the mod
 * depends on Jade compile-only).
 *
 * <p>IMPORTANT: the annotation value is the MODID the plugin belongs to - Jade keeps the plugin
 * only if {@code ModList.isLoaded(value)} is true. It must be {@code "hbm_m"} (a plugin id like
 * {@code "hbm_m:compat"} silently drops the plugin: not a modid, so the check fails).
 *
 * <p>Two features:
 * <ol>
 *   <li>Multiblock phantoms ({@code Universal Machine Part}) pick-block to the machine they
 *       belong to ({@code getCloneItemStack} delegates to the controller). Registering the part
 *       block with {@code usePickedResult} makes Jade show the machine's name - and keep the
 *       machine's icon - instead of the casing block's own name.</li>
 *   <li>{@link HbmFluidStorageProvider} feeds Jade the machine's full tank list server-side, so
 *       the fluid section renders no matter which cell of the multiblock is in the crosshair,
 *       not only on the connector/controller cells that happen to expose the fluid capability.</li>
 * </ol>
 */
@WailaPlugin("hbm_m")
public class HbmJadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        //? if < 1.21.1 {
        registration.registerFluidStorage(HbmFluidStorageProvider.V11.INSTANCE, BaseHbmBlockEntity.class);
        //?} else {
        /*registration.registerFluidStorage(HbmFluidStorageProvider.V15.INSTANCE, BaseHbmBlockEntity.class);
        *///?}
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.usePickedResult(ModBlocks.UNIVERSAL_MACHINE_PART.get());
    }
}
