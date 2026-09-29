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

        public static final ResourceLocation T51_MODEL_ID = ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "t51_armor");

    public static final ModelResourceLocation T51_MODEL_BAKED = new ModelResourceLocation(T51_MODEL_ID, "inventory");

        public static final ResourceLocation AJR_MODEL_ID = ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "ajr_armor");

    public static final ModelResourceLocation AJR_MODEL_BAKED = new ModelResourceLocation(AJR_MODEL_ID, "inventory");

        public static final ResourceLocation AJRO_MODEL_ID = ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "ajro_armor");

    public static final ModelResourceLocation AJRO_MODEL_BAKED = new ModelResourceLocation(AJRO_MODEL_ID, "inventory");

        public static final ResourceLocation BISMUTH_MODEL_ID = ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "bismuth_armor");

    public static final ModelResourceLocation BISMUTH_MODEL_BAKED = new ModelResourceLocation(BISMUTH_MODEL_ID, "inventory");

        public static final ResourceLocation DNT_MODEL_ID = ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "dnt_armor");

    public static final ModelResourceLocation DNT_MODEL_BAKED = new ModelResourceLocation(DNT_MODEL_ID, "inventory");

    /** Общая OBJ-геометрия костюма химзащиты (все окрасы шарят меш). По модели на окрас:
     * UV в запечённых квадах привязаны к атлас-позиции спрайта конкретной текстуры,
     * поэтому подмена материала в слое цвет не меняет - нужна своя запечённая модель. */
    public static final ResourceLocation HAZMAT_MODEL_ID = ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "hazmat_armor");

    public static final ModelResourceLocation HAZMAT_MODEL_BAKED = new ModelResourceLocation(HAZMAT_MODEL_ID, "inventory");

    public static final ResourceLocation HAZMAT_RED_MODEL_ID = ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "hazmat_red_armor");

    public static final ModelResourceLocation HAZMAT_RED_MODEL_BAKED = new ModelResourceLocation(HAZMAT_RED_MODEL_ID, "inventory");

    public static final ResourceLocation HAZMAT_GREY_MODEL_ID = ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "hazmat_grey_armor");

    public static final ModelResourceLocation HAZMAT_GREY_MODEL_BAKED = new ModelResourceLocation(HAZMAT_GREY_MODEL_ID, "inventory");

    private ClientPowerArmorRender() {}
}

