package com.hbm_m.client.loader;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hbm_m.client.model.DoorBakedModel;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}
//? if < 1.21.1 {
import net.minecraftforge.client.model.obj.ObjModel;
//?} else {
/*import net.neoforged.neoforge.client.model.obj.ObjModel;
*///?}

/**
 * Загрузчик дверных OBJ-моделей с ДЕДУПЛИКАЦИЕЙ текстурных скинов.
 * <p>
 * Почти все скины двери ссылаются на ОДИН И ТОТ ЖЕ OBJ и отличаются только
 * текстурами ({"textures": {"default": ...}}). Раньше каждый скин запускал
 * полный {@code ObjModel.bake} на каждую часть — ~35 полных копий квадов.
 * Теперь первая запечённая версия части становится canonical (кэш по
 * геометрической идентичности: ObjModel-инстанс + часть + root-transform),
 * остальные скины получают {@link RemappedPartModel} — ленивый аффинный
 * ремап UV из области canonical-спрайта в область спрайта скина. Квады при
 * этом хранятся в одном экземпляре.
 * <p>
 * Кэш чистится на resource reload (см. {@link #clearCanonicalCache}).
 */
@OnlyIn(Dist.CLIENT)
public class DoorModelLoader extends AbstractObjPartModelLoader<DoorBakedModel> {

    private static final Set<String> DEFAULT_PART_NAMES = Set.of(
        "frame", "doorLeft", "doorRight"
    );

    /** Canonical-запекание части: модель + спрайт, которым она запечена, + текстура RL. */
    private record CanonicalEntry(BakedModel model, TextureAtlasSprite sprite, ResourceLocation texture) {}

    private static final ConcurrentHashMap<String, CanonicalEntry> CANONICAL_PARTS = new ConcurrentHashMap<>();

    /** Инвалидация на resource reload — canonical-модели держат спрайты прошлого стича. */
    public static void clearCanonicalCache() {
        CANONICAL_PARTS.clear();
    }

    @Override
    protected Set<String> getPartNames(JsonObject jsonObject) {
        if (jsonObject.has("parts")) {
            return jsonObject.getAsJsonArray("parts")
                .asList()
                .stream()
                .map(JsonElement::getAsString)
                .collect(Collectors.toSet());
        }
        return DEFAULT_PART_NAMES;
    }

    @Override
    protected BakedModel bakePart(ObjModel model, AbstractObjPartModelLoader.SinglePartBakingContext partContext,
                                  ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter,
                                  ModelState identityState, ItemOverrides overrides, ResourceLocation modelName) {
        BakedModel fresh = super.bakePart(model, partContext, baker, spriteGetter, identityState, overrides, modelName);
        if (fresh == null) {
            return null;
        }

        String partName = partContext.visiblePartName();
        Material mat = partContext.getMaterial(partName);
        ResourceLocation texture = mat != null ? mat.texture() : null;
        if (partContext.textureOverride() != null) {
            texture = partContext.textureOverride();
        }
        TextureAtlasSprite sprite = null;
        if (texture != null) {
            try {
                sprite = spriteGetter.apply(new Material(TextureAtlas.LOCATION_BLOCKS, texture));
            } catch (Exception ignored) {
                // Спрайт не застичен — ремап невозможен, вернём собственный бейк.
            }
        }
        if (sprite == null) {
            return fresh;
        }

        // Геометрическая идентичность: один ObjModel-инстанс (= modelLocation + flipV,
        // LoaderHooks.OBJ_CACHE) + та же группа + тот же root-transform.
        String key = System.identityHashCode(model)
                + "|" + partName
                + "|" + partContext.getRootTransform().hashCode();

        CanonicalEntry prev = CANONICAL_PARTS.putIfAbsent(
                key, new CanonicalEntry(fresh, sprite, texture));
        if (prev == null) {
            return fresh; // мы и есть canonical
        }
        if (java.util.Objects.equals(prev.texture(), texture)) {
            return prev.model; // идентичный вид — шарим модель напрямую
        }
        return new RemappedPartModel(prev.model, prev.sprite, sprite);
    }

    @Override
    protected DoorBakedModel createBakedModel(HashMap<String, BakedModel> bakedParts,
                                               ItemTransforms transforms,
                                               ResourceLocation modelLocation) {
        return new DoorBakedModel(bakedParts, transforms, modelLocation);
    }
}
