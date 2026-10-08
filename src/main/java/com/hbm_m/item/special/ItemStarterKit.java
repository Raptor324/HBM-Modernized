package com.hbm_m.item.special;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.item.ITooltipProvider;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code com.hbm.items.special.ItemStarterKit}. Die Inhalte stehen als Registry-IDs in der Reihenfolge
 * des Originals ({@code "id"} oder {@code "id*anzahl"}, ohne Namensraum = hbm_m). IDs, die der Port
 * nicht kennt, werden beim Oeffnen uebersprungen und geloggt. Original-IDs sind auf Port-IDs umgesetzt
 * (machine_excavator=mining_drill, machine_gascent=gas_centrifuge, early_explosive_lenses=fat_man_explosive usw.). Gefuellte Behaelter stehen als "behaelter_fluessigkeit" (z. B. canister_full_diesel).
 */
public class ItemStarterKit extends Item implements ITooltipProvider {

    private static final Map<String, String[]> KITS = Map.ofEntries(
            Map.entry("nuke_starter_kit", new String[] {
                    "uranium_ingot*32", "yellowcake_powder*32", "press", "machine_blast_furnace", "gas_centrifuge",
                    "breeder", "machine_assembler", "chemical_plant", "reactor_research", "steam_turbine*2",
                    "radaway*8", "radx*2", "stamp_titanium_flat", "stamp_titanium_flat", "stamp_titanium_flat",
                    "steel_ingot*64", "lead_ingot*64", "minecraft:copper_ingot*64", "gas_mask_m65", "geiger_counter", "#haz1"}),
            Map.entry("nuke_advanced_kit", new String[] {
                    "yellowcake_powder*64", "plutonium_powder*64", "steel_ingot*64", "minecraft:copper_ingot*64", "tungsten_ingot*64",
                    "lead_ingot*64", "polymer_ingot*64", "machine_blast_furnace*3", "gas_centrifuge*3", "centrifuge*2",
                    "machine_uf6_tank*2", "machine_puf6_tank*2", "breeder*2", "reactor_research*4", "steam_turbine*4",
                    "machine_radgen", "machine_rtg", "machine_assembler*3", "chemical_plant*2", "fluid_tank",
                    "pellet_rtg", "pellet_rtg", "pellet_rtg", "pellet_rtg_weak", "pellet_rtg_weak", "pellet_rtg_weak",
                    "cell_empty*32", "rod_empty*32", "fluid_barrel_full_coolant*4", "radaway_strong*4", "radx*4", "pill_iodine",
                    "geiger_counter", "survey_scanner", "gas_mask_m65", "#haz2"}),
            Map.entry("nuke_commercially_kit", new String[] {
                    "reactor_research*8", "breeder*8", "fluid_tank*8", "billet_pu238be*40", "u233_ingot*40",
                    "uranium_fuel_ingot*32", "plutonium_fuel_ingot*16", "mox_fuel_ingot*8", "inf_water_mk2", "inf_water_mk2", "inf_water_mk2",
                    "rod_empty*64", "rod_dual_empty*64", "rod_quad_empty*64", "fluid_tank_lead_empty*64", "fluid_barrel_empty*64",
                    "barrel_steel*16", "plate_iron*64", "minecraft:ink_sac*64", "radaway_flush*8", "iv_blood*8", "pill_iodine*8",
                    "gas_mask_filter_combo", "gas_mask_filter_combo", "gas_mask_filter_combo", "#haz2"}),
            Map.entry("nuke_electric_kit", new String[] {
                    "coil_copper*16", "coil_gold*8", "coil_tungsten*8", "motor*4", "vacuum_tube*16", "capacitor*16", "integrated_circuit*16",
                    "wiring_red_copper", "magnetron*5", "piston_selenium", "piston_selenium", "piston_selenium",
                    "canister_full_diesel*16", "canister_full_biofuel*16", "battery_potato", "screwdriver", "mining_drill",
                    "dieselgen*2", "red_cable*64", "red_wire_coated*16", "red_pylon*8", "machine_battery_socket*4",
                    "battery_pack_battery_lead*4", "machine_converter_he_rf", "machine_converter_rf_he"}),
            Map.entry("gadget_kit", new String[] {
                    "nuke_gadget", "fat_man_explosive", "fat_man_explosive", "fat_man_explosive", "fat_man_explosive",
                    "gadget_wireing", "gadget_core", "#haz0"}),
            Map.entry("boy_kit", new String[] {
                    "nuke_boy", "boy_shielding", "boy_target", "boy_bullet", "boy_propellant", "boy_igniter", "#haz0"}),
            Map.entry("man_kit", new String[] {
                    "nuke_fat_man", "fat_man_explosive", "fat_man_explosive", "fat_man_explosive", "fat_man_explosive",
                    "fat_man_igniter", "fat_man_core", "#haz0"}),
            Map.entry("mike_kit", new String[] {
                    "nuke_mike", "explosive_lenses", "explosive_lenses", "explosive_lenses", "explosive_lenses", "fat_man_core",
                    "mike_core", "mike_deut", "mike_cooling_unit", "#haz0"}),
            Map.entry("tsar_kit", new String[] {
                    "nuke_tsar", "explosive_lenses", "explosive_lenses", "explosive_lenses", "explosive_lenses", "fat_man_core", "tsar_core", "#haz0"}),
            Map.entry("multi_kit", new String[] {
                    "bomb_multi*6", "minecraft:tnt*26", "minecraft:gunpowder*2", "pellet_cluster*2", "fire_powder*2", "powder_poison*2", "pellet_gas*2"}),
            Map.entry("custom_kit", new String[] {
                    "nuke_custom", "custom_tnt", "custom_tnt", "custom_tnt", "custom_tnt", "custom_tnt", "custom_tnt",
                    "custom_nuke", "custom_nuke", "custom_nuke", "custom_nuke", "custom_hydro", "custom_hydro", "custom_amat", "custom_amat",
                    "custom_dirty", "custom_dirty", "custom_dirty", "custom_schrab", "custom_fall"}),
            Map.entry("fleija_kit", new String[] {
                    "nuke_fleija", "fleija_igniter", "fleija_igniter", "fleija_propellant", "fleija_propellant", "fleija_propellant",
                    "fleija_core", "fleija_core", "fleija_core", "fleija_core", "fleija_core", "fleija_core", "#haz2"}),
            Map.entry("solinium_kit", new String[] {
                    "nuke_solinium", "solinium_igniter", "solinium_igniter", "solinium_igniter", "solinium_igniter",
                    "solinium_propellant", "solinium_propellant", "solinium_propellant", "solinium_propellant", "solinium_core", "#haz1"}),
            Map.entry("prototype_kit", new String[] {
                    "nuke_prototype", "igniter", "cell_sas3*4", "rod_quad_uranium*4", "rod_quad_lead*4", "rod_quad_np237*2", "#haz2"}),
            Map.entry("missile_kit", new String[] {
                    "launch_pad", "designator", "designator_range", "designator_manual", "missile_generic", "missile_strong", "missile_burst",
                    "missile_incendiary", "missile_incendiary_strong", "missile_inferno", "missile_cluster", "missile_cluster_strong",
                    "missile_rain", "missile_buster", "missile_buster_strong", "missile_drill", "missile_nuclear", "missile_nuclear_cluster",
                    "missile_volcano", "missile_doomsday", "missile_taint", "missile_micro", "missile_bhole", "missile_schrabidium", "missile_emp"}),
            Map.entry("euphemium_kit", new String[] {
                    "euphemium_helmet", "euphemium_plate", "euphemium_legs", "euphemium_boots", "statue_elb_f"}),
            Map.entry("hazmat_kit", new String[] {"#haz0"}),
            Map.entry("hazmat_red_kit", new String[] {"#haz1"}),
            Map.entry("hazmat_grey_kit", new String[] {"#haz2"})
    );

    private final String kit;

    public ItemStarterKit(String kit, Properties properties) {
        super(properties);
        this.kit = kit;
    }

    private static Item item(String id) {
        ResourceLocation rl = id.contains(":") ? ResourceLocation.tryParse(id) : ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, id);
        return rl == null ? Items.AIR : BuiltInRegistries.ITEM.get(rl);
    }

    /** Gefuellte Behaelter des Originals ({@code canister_full}, Meta = Fluessigkeit) als "behaelter_fluessigkeit". */
    private static ItemStack filledContainer(String id, int count) {
        String[][] bases = {{"fluid_barrel_full_", "fluid_barrel_full"}, {"fluid_tank_lead_full_", "fluid_tank_lead_full"},
                {"fluid_tank_full_", "fluid_tank_full"}, {"canister_full_", "canister_full"}, {"gas_full_", "gas_full"}};
        for (String[] b : bases) {
            if (!id.startsWith(b[0])) continue;
            var fluid = com.hbm_m.inventory.fluid.ModFluids.getEntry(id.substring(b[0].length()));
            if (fluid == null) return ItemStack.EMPTY;
            return com.hbm_m.item.liquids.ItemFluidTank.make(item(b[1]), fluid.getSource(), count);
        }
        return ItemStack.EMPTY;
    }

    private static void give(Player player, String id, int count) {
        ItemStack filled = filledContainer(id, count);
        if (!filled.isEmpty()) {
            player.getInventory().add(filled);
            return;
        }
        Item it = item(id);
        if (it == Items.AIR) {
            MainRegistry.LOGGER.warn("[ItemStarterKit] {} fehlt im Port, uebersprungen", id);
            return;
        }
        player.getInventory().add(new ItemStack(it, count));
    }

    private static final String[][] HAZ = {
            {"hazmat_helmet", "hazmat_plate", "hazmat_legs", "hazmat_boots"},
            {"hazmat_helmet_red", "hazmat_plate_red", "hazmat_legs_red", "hazmat_boots_red"},
            {"hazmat_helmet_grey", "hazmat_plate_grey", "hazmat_legs_grey", "hazmat_boots_grey"}
    };

    /** Original giveHaz: vorhandene Ruestung fallen lassen, Schutzanzug anlegen. */
    private static void giveHaz(Level world, Player p, int tier) {
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (EquipmentSlot slot : slots) {
            ItemStack worn = p.getItemBySlot(slot);
            if (!worn.isEmpty() && !world.isClientSide) {
                world.addFreshEntity(new ItemEntity(world, p.getX(), p.getEyeY(), p.getZ(), worn));
            }
        }
        for (int i = 0; i < 4; i++) {
            Item it = item(HAZ[tier][i]);
            p.setItemSlot(slots[i], it == Items.AIR ? ItemStack.EMPTY : new ItemStack(it));
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (kit.equals("stealth_boy")) {
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 30 * 20, 1, true, true));
        } else {
            String[] content = KITS.get(kit);
            if (content != null && !world.isClientSide) {
                for (String entry : content) {
                    if (entry.startsWith("#haz")) {
                        giveHaz(world, player, entry.charAt(4) - '0');
                        continue;
                    }
                    int star = entry.indexOf('*');
                    String id = star < 0 ? entry : entry.substring(0, star);
                    int count = star < 0 ? 1 : Integer.parseInt(entry.substring(star + 1));
                    give(player, id, count);
                }
                player.inventoryMenu.broadcastChanges();
            }
        }

        world.playSound(null, player.getX(), player.getY(), player.getZ(), HbmSoundsNT.get("hbm:item.unpack"), SoundSource.PLAYERS, 1.0F, 1.0F);
        stack.shrink(1);
        return InteractionResultHolder.sidedSuccess(stack, world.isClientSide);
    }

    private static final java.util.Set<String> EMPTY_INV = java.util.Set.of("nuke_starter_kit", "nuke_advanced_kit", "nuke_commercially_kit",
            "nuke_electric_kit", "gadget_kit", "boy_kit", "man_kit", "mike_kit", "tsar_kit", "prototype_kit", "fleija_kit", "solinium_kit",
            "missile_kit", "multi_kit");
    private static final java.util.Set<String> DISPLACE = java.util.Set.of("nuke_starter_kit", "nuke_advanced_kit", "nuke_commercially_kit",
            "gadget_kit", "boy_kit", "man_kit", "mike_kit", "tsar_kit", "prototype_kit", "fleija_kit", "solinium_kit", "hazmat_kit");

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        if (EMPTY_INV.contains(kit)) list.add(Component.literal("Please empty inventory before opening!"));
        if (DISPLACE.contains(kit)) list.add(Component.literal("Armor will be displaced by hazmat suit."));
    }
}
