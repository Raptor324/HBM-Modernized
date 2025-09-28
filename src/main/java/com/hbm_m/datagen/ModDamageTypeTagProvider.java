package com.hbm_m.datagen;

// Провайдер генерации тегов типов урона для мода.
// Здесь мы добавляем наши пользовательские типы урона в ванильные теги,
// используя стандартный API Minecraft.

import com.hbm_m.lib.RefStrings;
import com.hbm_m.damagesource.ModDamageTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.DamageTypeTagsProvider;
import net.minecraft.tags.DamageTypeTags;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;

public class ModDamageTypeTagProvider extends DamageTypeTagsProvider {

    public ModDamageTypeTagProvider(PackOutput pOutput, CompletableFuture<HolderLookup.Provider> pLookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(pOutput, pLookupProvider, RefStrings.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(@Nonnull HolderLookup.Provider pProvider) {
        // Здесь мы добавляем наши типы урона в ванильные теги, используя стандартный API.

        // Тег: Урон, который игнорирует броню
        tag(DamageTypeTags.BYPASSES_ARMOR).add(
                ModDamageTypes.RADIATION,
                ModDamageTypes.MUD_POISONING,
                ModDamageTypes.EUTHANIZED_SELF,
                ModDamageTypes.EUTHANIZED_SELF_2,
                ModDamageTypes.TAU_BLAST,
                ModDamageTypes.CHEATER,
                ModDamageTypes.BLACK_HOLE,
                ModDamageTypes.BLENDER,
                ModDamageTypes.METEORITE,
                ModDamageTypes.TAINT,
                ModDamageTypes.CLOUD,
                ModDamageTypes.LEAD,
                ModDamageTypes.ENERVATION,
                ModDamageTypes.ELECTRICITY,
                ModDamageTypes.EXHAUST,
                ModDamageTypes.LUNAR,
                ModDamageTypes.MONOXIDE,
                ModDamageTypes.ASBESTOS,
                ModDamageTypes.BLACKLUNG,
                ModDamageTypes.VACUUM,
                ModDamageTypes.OVERDOSE,
                ModDamageTypes.MICROWAVE,
                ModDamageTypes.NITAN,
                ModDamageTypes.TAU,
                ModDamageTypes.CMB,
                ModDamageTypes.SUB_ATOMIC,
                ModDamageTypes.EUTHANIZED,
                ModDamageTypes.LASER
        );

        // Тег: Урон, считающийся взрывом
        tag(DamageTypeTags.IS_EXPLOSION).add(
                ModDamageTypes.NUCLEAR_BLAST,
                ModDamageTypes.BLAST,
                ModDamageTypes.BANG
        );
        
        // Тег: Урон, который является снарядом
        tag(DamageTypeTags.IS_PROJECTILE).add(
                ModDamageTypes.REVOLVER_BULLET,
                ModDamageTypes.CHOPPER_BULLET,
                ModDamageTypes.SHRAPNEL,
                ModDamageTypes.RUBBLE
        );

        // Тег: Урон, который обходит неуязвимость (например, в креативе или после получения урона)
        tag(DamageTypeTags.BYPASSES_INVULNERABILITY).add(
                ModDamageTypes.CHEATER,
                ModDamageTypes.EUTHANIZED_SELF,
                ModDamageTypes.DIGAMMA,
                ModDamageTypes.EUTHANIZED_SELF_2
        );
        
        // И так далее для других тегов...
        // DamageTypeTags.BYPASSES_EFFECTS
        // DamageTypeTags.BYPASSES_ENCHANTMENTS
        // DamageTypeTags.BYPASSES_RESISTANCE
    }
}