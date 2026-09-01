package com.hbm_m.advancement;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 1:1 port of {@code com.hbm.util.AchievementHandler} plus the handful of pickup and block-break
 * triggers the original keeps in {@code ModEventHandler}.
 *
 * <p>The 1.7.10 version listens for crafting only. Forge 1.20 splits that into crafting and
 * smelting, and several of these items come out of a machine rather than off a bench in this
 * port, so crafting, smelting and pickup all feed the same table - otherwise a player who
 * smelted their first desh nugget would silently never get the advancement.</p>
 */
@Mod.EventBusSubscriber(modid = RefStrings.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AchievementHandler {

    /**
     * Item supplier -> advancement. Suppliers rather than items because this class is loaded
     * before the registries are populated.
     */
    private static final Map<Supplier<? extends Item>, String> CRAFTING = new HashMap<>();

    /** Resolved on first use, once the registries are guaranteed to be frozen. */
    private static Map<Item, String> resolved;

    static {
        item(ModItems.PISTON_SELENIUM,         ModAdvancements.SELENIUM);
        item(ModItems.GUN_B92,                 ModAdvancements.SELENIUM);
        item(ModItems.BATTERY_POTATO,          ModAdvancements.POTATO);
        block(ModBlocks.MACHINE_PRESS,         ModAdvancements.BURNER_PRESS);
        item(ModItems.RBMK_FUEL_EMPTY,         ModAdvancements.RBMK);
        block(ModBlocks.CHEMICAL_PLANT,        ModAdvancements.CHEMPLANT);
        block(ModBlocks.CONCRETE,              ModAdvancements.CONCRETE);
        block(ModBlocks.CONCRETE_ASBESTOS,     ModAdvancements.CONCRETE);
        item(ModItems.PLATE_POLYMER,           ModAdvancements.POLYMER);
        item(ModItems.NUGGET_DESH,             ModAdvancements.DESH);
        item(ModItems.GEM_TANTALIUM,           ModAdvancements.TANTALUM);
        block(ModBlocks.MACHINE_GASCENT,       ModAdvancements.GAS_CENT);
        block(ModBlocks.MACHINE_CENTRIFUGE,    ModAdvancements.CENTRIFUGE);
        item(ModItems.NUGGET_SCHRABIDIUM,      ModAdvancements.SCHRAB);
        block(ModBlocks.MACHINE_CRYSTALLIZER,  ModAdvancements.ACIDIZER);
        block(ModBlocks.SILEX,                 ModAdvancements.SILEX);
        item(ModItems.NUGGET_TECHNETIUM,       ModAdvancements.TECHNETIUM);
        block(ModBlocks.STRUCT_WATZ_CORE,      ModAdvancements.WATZ);
        item(ModItems.NUGGET_BISMUTH,          ModAdvancements.BISMUTH);
        item(ModItems.NUGGET_AM241,            ModAdvancements.BREEDING);
        item(ModItems.NUGGET_AM242,            ModAdvancements.BREEDING);
        item(ModItems.MISSILE_NUCLEAR,         ModAdvancements.RED_BALLOONS);
        item(ModItems.MISSILE_NUCLEAR_CLUSTER, ModAdvancements.RED_BALLOONS);
        item(ModItems.MISSILE_DOOMSDAY,        ModAdvancements.RED_BALLOONS);
        block(ModBlocks.STRUCT_TORUS_CORE,     ModAdvancements.FUSION);
        block(ModBlocks.MACHINE_BLAST_FURNACE,  ModAdvancements.BLAST_FURNACE);
        item(ModItems.MACHINE_ASSEMBLER,       ModAdvancements.ASSEMBLY);
        item(ModItems.BILLET_PU_MIX,           ModAdvancements.CHICAGO_PILE);
        item(ModItems.PARTICLE_DIGAMMA,        ModAdvancements.OMEGA12);
    }

    private static void item(Supplier<? extends Item> supplier, String advancement) {
        CRAFTING.put(supplier, advancement);
    }

    private static void block(Supplier<? extends Block> supplier, String advancement) {
        CRAFTING.put(() -> supplier.get().asItem(), advancement);
    }

    private static Map<Item, String> table() {
        if (resolved == null) {
            resolved = new HashMap<>();
            for (Map.Entry<Supplier<? extends Item>, String> e : CRAFTING.entrySet()) {
                Item item = e.getKey().get();
                if (item != null && item != Items.AIR) resolved.put(item, e.getValue());
            }
        }
        return resolved;
    }

    /** {@code AchievementHandler.fire}. */
    public static void fire(Player player, ItemStack stack) {
        if (player == null || player.level().isClientSide || stack.isEmpty()) return;
        String advancement = table().get(stack.getItem());
        if (advancement != null) ModAdvancements.grant(player, advancement);
    }

    @SubscribeEvent
    public static void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        fire(event.getEntity(), event.getCrafting());
    }

    @SubscribeEvent
    public static void onSmelted(PlayerEvent.ItemSmeltedEvent event) {
        fire(event.getEntity(), event.getSmelting());
    }

    @SubscribeEvent
    public static void onPickup(PlayerEvent.ItemPickupEvent event) {
        ItemStack stack = event.getStack();
        if (stack.is(Items.SLIME_BALL)) {
            ModAdvancements.grant(event.getEntity(), ModAdvancements.SLIMEBALL);
        }
        // Machines drop their output into the world rather than into a crafting slot, so a pickup
        // is the only moment the port can see some of these items reach the player.
        fire(event.getEntity(), stack);
    }

    /**
     * {@code ModEventHandler.onEntityDeath}: killing a tainted creeper specifically with boxcar
     * damage. The original hides this one behind an obscure interaction, hence the name.
     */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof com.hbm_m.entity.mob.EntityCreeperTainted creeper)) return;
        if (!event.getSource().is(com.hbm_m.damagesource.ModDamageTypes.BOXCAR)) return;
        ModAdvancements.grantNearby(creeper, 50D, ModAdvancements.HIDDEN);
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getState().is(ModBlocks.STONE_GNEISS.get())) {
            ModAdvancements.grant(event.getPlayer(), ModAdvancements.STRATUM);
        }
    }
}
