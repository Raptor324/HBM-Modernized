//? if neoforge {
/*package com.hbm_m.recipe;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

// Forge-1.20.1-Zutaten forge:nbt (StrictNBTIngredient) und forge:partial_nbt (PartialNBTIngredient) fuer NeoForge 1.21.1.
// Die Datagen-Rezepte (1.20.1-Format) benutzen sie unveraendert. Verglichen wird gegen die "alte NBT-Sicht" des
// Stacks: CUSTOM_DATA + Damage (bei beschaedigbaren Items, wie 1.20.1) + BlockStateTag aus BLOCK_STATE.
public final class LegacyNbtIngredients {
    private LegacyNbtIngredients() {}

    public static final DeferredRegister<IngredientType<?>> FORGE_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.INGREDIENT_TYPES, "forge");

    // "nbt": SNBT-String oder Objekt (beides in Forge 1.20.1 erlaubt)
    private static final Codec<CompoundTag> NBT = TagParser.LENIENT_CODEC;

    public static final MapCodec<Strict> STRICT_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(s -> s.item),
            NBT.optionalFieldOf("nbt", new CompoundTag()).forGetter(s -> s.nbt)
    ).apply(i, Strict::new));

    public static final MapCodec<Partial> PARTIAL_CODEC = RecordCodecBuilder.<Partial>mapCodec(i -> i.group(
            BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("item").forGetter(p -> p.items.size() == 1 ? Optional.of(p.items.get(0)) : Optional.empty()),
            BuiltInRegistries.ITEM.byNameCodec().listOf().optionalFieldOf("items").forGetter(p -> p.items.size() == 1 ? Optional.empty() : Optional.of(p.items)),
            NBT.fieldOf("nbt").forGetter(p -> p.nbt)
    ).apply(i, (one, many, nbt) -> new Partial(one.map(List::of).orElseGet(() -> many.orElse(List.of())), nbt)))
            .validate(p -> p.items.isEmpty() ? DataResult.error(() -> "forge:partial_nbt braucht item oder items") : DataResult.success(p));

    public static final java.util.function.Supplier<IngredientType<Strict>> STRICT =
            FORGE_TYPES.register("nbt", () -> new IngredientType<>(STRICT_CODEC));
    public static final java.util.function.Supplier<IngredientType<Partial>> PARTIAL =
            FORGE_TYPES.register("partial_nbt", () -> new IngredientType<>(PARTIAL_CODEC));

    public static void register(IEventBus modBus) {
        FORGE_TYPES.register(modBus);
    }

    // Item-NBT, wie ihn 1.20.1 (getShareTag) gesehen haette.
    public static CompoundTag legacyView(ItemStack stack) {
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag view = cd != null ? cd.copyTag() : new CompoundTag();
        if (stack.isDamageableItem()) view.putInt("Damage", stack.getDamageValue());
        BlockItemStateProperties bs = stack.get(DataComponents.BLOCK_STATE);
        if (bs != null && !bs.isEmpty() && !view.contains("BlockStateTag", Tag.TAG_COMPOUND)) {
            CompoundTag t = new CompoundTag();
            bs.properties().forEach(t::putString);
            view.put("BlockStateTag", t);
        }
        return view;
    }

    // Anzeige-/Ergebnis-Stack aus altem Item-NBT (Damage -> Komponente, BlockStateTag zusaetzlich als BLOCK_STATE).
    public static ItemStack fromLegacy(Item item, CompoundTag nbt) {
        ItemStack s = new ItemStack(item);
        CompoundTag rest = nbt.copy();
        if (rest.contains("Damage", Tag.TAG_ANY_NUMERIC)) {
            if (s.isDamageableItem()) s.setDamageValue(rest.getInt("Damage"));
            rest.remove("Damage");
        }
        if (rest.contains("BlockStateTag", Tag.TAG_COMPOUND)) {
            CompoundTag bst = rest.getCompound("BlockStateTag");
            java.util.Map<String, String> props = new java.util.LinkedHashMap<>();
            for (String k : bst.getAllKeys()) props.put(k, bst.get(k).getAsString());
            s.set(DataComponents.BLOCK_STATE, new BlockItemStateProperties(props));
        }
        if (!rest.isEmpty()) s.set(DataComponents.CUSTOM_DATA, CustomData.of(rest));
        return s;
    }

    public static final class Strict implements ICustomIngredient {
        final Item item;
        final CompoundTag nbt;
        private final ItemStack display;

        Strict(Item item, CompoundTag nbt) {
            this.item = item;
            this.nbt = nbt;
            this.display = fromLegacy(item, nbt);
        }

        @Override
        public boolean test(ItemStack input) {
            if (input == null) return false;
            return input.getItem() == item && input.getDamageValue() == display.getDamageValue() && legacyView(input).equals(legacyView(display));
        }

        @Override
        public Stream<ItemStack> getItems() {
            return Stream.of(display);
        }

        @Override
        public boolean isSimple() {
            return false;
        }

        @Override
        public IngredientType<?> getType() {
            return STRICT.get();
        }
    }

    public static final class Partial implements ICustomIngredient {
        final List<Item> items;
        final CompoundTag nbt;

        Partial(List<Item> items, CompoundTag nbt) {
            this.items = items;
            this.nbt = nbt;
        }

        @Override
        public boolean test(ItemStack input) {
            if (input == null) return false;
            return items.contains(input.getItem()) && NbtUtils.compareNbt(nbt, legacyView(input), true);
        }

        @Override
        public Stream<ItemStack> getItems() {
            return items.stream().map(i -> fromLegacy(i, nbt));
        }

        @Override
        public boolean isSimple() {
            return false;
        }

        @Override
        public IngredientType<?> getType() {
            return PARTIAL.get();
        }
    }
}
*///?}
