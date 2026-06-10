package com.hbm_m.powerarmor.render;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import com.hbm_m.main.MainRegistry;

/**
 * Общий (multiloader) holder для id/ModelResourceLocation силовой брони.
 *
 * Регистрация geometry loaders / additional models / render layers делается в loader-specific коде.
 * (см. {@code ClientPowerArmorRenderForge}).
 */
public final class ClientPowerArmorRender {

    //? if fabric && < 1.21.1 {
    /*public static final ResourceLocation T51_MODEL_ID = new ResourceLocation(MainRegistry.MOD_ID, "t51_armor");
    *///?} else {
        public static final ResourceLocation T51_MODEL_ID = ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "t51_armor");
    //?}

    public static final ModelResourceLocation T51_MODEL_BAKED = new ModelResourceLocation(T51_MODEL_ID, "inventory");

    //? if fabric && < 1.21.1 {
    /*public static final ResourceLocation AJR_MODEL_ID = new ResourceLocation(MainRegistry.MOD_ID, "ajr_armor");
    *///?} else {
        public static final ResourceLocation AJR_MODEL_ID = ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "ajr_armor");
    //?}

    public static final ModelResourceLocation AJR_MODEL_BAKED = new ModelResourceLocation(AJR_MODEL_ID, "inventory");

    //? if fabric && < 1.21.1 {
    /*public static final ResourceLocation AJRO_MODEL_ID = new ResourceLocation(MainRegistry.MOD_ID, "ajro_armor");
    *///?} else {
        public static final ResourceLocation AJRO_MODEL_ID = ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "ajro_armor");
    //?}

    public static final ModelResourceLocation AJRO_MODEL_BAKED = new ModelResourceLocation(AJRO_MODEL_ID, "inventory");

    //? if fabric && < 1.21.1 {
    /*public static final ResourceLocation BISMUTH_MODEL_ID = new ResourceLocation(MainRegistry.MOD_ID, "bismuth_armor");
    *///?} else {
        public static final ResourceLocation BISMUTH_MODEL_ID = ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "bismuth_armor");
    //?}

    public static final ModelResourceLocation BISMUTH_MODEL_BAKED = new ModelResourceLocation(BISMUTH_MODEL_ID, "inventory");

    //? if fabric && < 1.21.1 {
    /*public static final ResourceLocation DNT_MODEL_ID = new ResourceLocation(MainRegistry.MOD_ID, "dnt_armor");
    *///?} else {
        public static final ResourceLocation DNT_MODEL_ID = ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "dnt_armor");
    //?}

    public static final ModelResourceLocation DNT_MODEL_BAKED = new ModelResourceLocation(DNT_MODEL_ID, "inventory");

    private ClientPowerArmorRender() {}
}

