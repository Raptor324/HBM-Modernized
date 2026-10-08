package com.hbm_m.platform;

/**
 * Versionsfassade fuer Item-Eigenschaften, die 1.21.1 als Default-Datenkomponenten fuehrt (Phase B, Agent C).
 *
 * <p>1.20.1: {@code Item.getRarity(ItemStack)} wird von den Port-Items ueberschrieben.<br>
 * 1.21.1: Die Seltenheit ist die Komponente {@code DataComponents.RARITY}; sie wird hier vorgemerkt und bei
 * {@code ModifyDefaultComponentsEvent} (nach der Registrierung, alle {@code ModItems} sind dann aufloesbar)
 * als Default-Komponente gesetzt. Abweichung zu 1.20.1: Verzauberungen heben die Seltenheit wie bei Vanilla
 * eine Stufe an (1.21.1 hat dafuer keinen Item-Hook).</p>
 */
public final class ItemComponentHooks {
    private ItemComponentHooks() {}

    //? if >= 1.21.1 {
    /*private static final java.util.Map<net.minecraft.world.item.Item, java.util.function.Supplier<net.minecraft.world.item.Rarity>> RARITIES =
            new java.util.LinkedHashMap<>();

    /^* Merkt die Seltenheit eines Items vor (Lieferant darf {@code null} liefern = Vanilla-Wert behalten). ^/
    public static void deferRarity(net.minecraft.world.item.Item item, java.util.function.Supplier<net.minecraft.world.item.Rarity> rarity) {
        synchronized (RARITIES) {
            RARITIES.put(item, rarity);
        }
    }

    private static final java.util.List<net.minecraft.world.item.Item> JUKEBOX = new java.util.ArrayList<>();

    /^*
     * 1.21.1: {@code RecordItem} entfaellt; die Platte spielt den Jukebox-Song {@code <modid>:<item-id>}
     * ({@code data/<modid>/jukebox_song/<item-id>.json}), gesetzt als Default-Komponente {@code JUKEBOX_PLAYABLE}.
     ^/
    public static void deferJukeboxSong(net.minecraft.world.item.Item item) {
        synchronized (JUKEBOX) {
            JUKEBOX.add(item);
        }
    }

    private static final java.util.List<net.minecraft.world.item.Item> UNBREAKABLE = new java.util.ArrayList<>();

    /^*
     * 1.20.1 {@code canBeDepleted() == false}: das Item nutzt sich nie ab. 1.21.1: die Default-Komponenten
     * {@code MAX_DAMAGE}/{@code DAMAGE} werden entfernt (ohne den Tooltip von {@code UNBREAKABLE}).
     ^/
    public static void deferUnbreakable(net.minecraft.world.item.Item item) {
        synchronized (UNBREAKABLE) {
            UNBREAKABLE.add(item);
        }
    }
    *///?}

    //? if neoforge {
    /*@net.neoforged.fml.common.EventBusSubscriber(modid = com.hbm_m.lib.RefStrings.MODID, bus = net.neoforged.fml.common.EventBusSubscriber.Bus.MOD)
    public static final class Events {
        private Events() {}

        @net.neoforged.bus.api.SubscribeEvent
        public static void onModifyDefaultComponents(net.neoforged.neoforge.event.ModifyDefaultComponentsEvent event) {
            synchronized (RARITIES) {
                RARITIES.forEach((item, supplier) -> {
                    net.minecraft.world.item.Rarity r = supplier.get();
                    if (r != null) event.modify(item, b -> b.set(net.minecraft.core.component.DataComponents.RARITY, r));
                });
            }
            synchronized (UNBREAKABLE) {
                for (net.minecraft.world.item.Item item : UNBREAKABLE) {
                    event.modify(item, b -> b.remove(net.minecraft.core.component.DataComponents.MAX_DAMAGE).remove(net.minecraft.core.component.DataComponents.DAMAGE));
                }
            }
            // 1.20.1: Haltbarkeit 0 (z.B. TieredItem mit Material-Haltbarkeit 0) = unzerstoerbar. 1.21.1 setzt dann
            // MAX_DAMAGE 0 und das Item zerbraeche beim ersten Schaden -> wie oben entfernen (nur Items dieses Mods).
            event.modifyMatching(item -> com.hbm_m.lib.RefStrings.MODID.equals(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).getNamespace())
                            && Integer.valueOf(0).equals(item.components().get(net.minecraft.core.component.DataComponents.MAX_DAMAGE)),
                    b -> b.remove(net.minecraft.core.component.DataComponents.MAX_DAMAGE).remove(net.minecraft.core.component.DataComponents.DAMAGE));
            synchronized (JUKEBOX) {
                for (net.minecraft.world.item.Item item : JUKEBOX) {
                    net.minecraft.resources.ResourceKey<net.minecraft.world.item.JukeboxSong> song = net.minecraft.resources.ResourceKey.create(
                            net.minecraft.core.registries.Registries.JUKEBOX_SONG, net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item));
                    event.modify(item, b -> b.set(net.minecraft.core.component.DataComponents.JUKEBOX_PLAYABLE,
                            new net.minecraft.world.item.JukeboxPlayable(new net.minecraft.world.item.EitherHolder<>(song), true)));
                }
            }
        }
    }
    *///?}
}
