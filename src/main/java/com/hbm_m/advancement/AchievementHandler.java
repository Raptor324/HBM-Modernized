package com.hbm_m.advancement;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;
import com.hbm_m.lib.RefStrings;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.PlayerEvent;

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
        item(ModMaterialItems.get(ModMaterials.POLYMER, MaterialShape.PLATE), ModAdvancements.POLYMER);
        item(ModMaterialItems.get(ModMaterials.DESH, MaterialShape.NUGGET),  ModAdvancements.DESH);
        item(ModItems.GEM_TANTALIUM,           ModAdvancements.TANTALUM);
        block(ModBlocks.MACHINE_GASCENT,       ModAdvancements.GAS_CENT);
        block(ModBlocks.MACHINE_CENTRIFUGE,    ModAdvancements.CENTRIFUGE);
        item(ModMaterialItems.get(ModMaterials.SCHRABIDIUM, MaterialShape.NUGGET), ModAdvancements.SCHRAB);
        block(ModBlocks.MACHINE_CRYSTALLIZER,  ModAdvancements.ACIDIZER);
        block(ModBlocks.SILEX,                 ModAdvancements.SILEX);
        item(ModMaterialItems.get(ModMaterials.TECHNETIUM, MaterialShape.NUGGET),     ModAdvancements.TECHNETIUM);
        block(ModBlocks.STRUCT_WATZ_CORE,      ModAdvancements.WATZ);
        item(ModMaterialItems.get(ModMaterials.BISMUTH, MaterialShape.NUGGET), ModAdvancements.BISMUTH);
        item(ModMaterialItems.get(ModMaterials.AM241, MaterialShape.NUGGET),   ModAdvancements.BREEDING);
        item(ModMaterialItems.get(ModMaterials.AM242, MaterialShape.NUGGET),   ModAdvancements.BREEDING);
        item(ModItems.MISSILE_NUCLEAR,         ModAdvancements.RED_BALLOONS);
        item(ModItems.MISSILE_NUCLEAR_CLUSTER, ModAdvancements.RED_BALLOONS);
        item(ModItems.MISSILE_DOOMSDAY,        ModAdvancements.RED_BALLOONS);
        block(ModBlocks.STRUCT_TORUS_CORE,     ModAdvancements.FUSION);
        block(ModBlocks.MACHINE_BLAST_FURNACE,  ModAdvancements.BLAST_FURNACE);
        item(ModItems.MACHINE_ASSEMBLER,       ModAdvancements.ASSEMBLY);
        item(ModMaterialItems.get(ModMaterials.PU_MIX, MaterialShape.BILLET),  ModAdvancements.CHICAGO_PILE);
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

    /** Registriert die Handler auf den plattformneutralen Architectury-Events. */
    public static void init() {
        PlayerEvent.CRAFT_ITEM.register((player, constructed, inventory) -> fire(player, constructed));
        PlayerEvent.SMELT_ITEM.register(AchievementHandler::fire);
        PlayerEvent.PICKUP_ITEM_POST.register((player, entity, stack) -> onPickup(player, stack));
        EntityEvent.LIVING_DEATH.register(AchievementHandler::onDeath);
        BlockEvent.BREAK.register(AchievementHandler::onBlockBreak);
    }

    private static void onPickup(Player player, ItemStack stack) {
        if (stack.is(Items.SLIME_BALL)) {
            ModAdvancements.grant(player, ModAdvancements.SLIMEBALL);
        }
        // Machines drop their output into the world rather than into a crafting slot, so a pickup
        // is the only moment the port can see some of these items reach the player.
        fire(player, stack);
    }

    /**
     * {@code ModEventHandler.onEntityDeath}: killing a tainted creeper specifically with boxcar
     * damage. The original hides this one behind an obscure interaction, hence the name.
     */
    private static EventResult onDeath(net.minecraft.world.entity.LivingEntity entity,
                                       net.minecraft.world.damagesource.DamageSource source) {
        if (!(entity instanceof com.hbm_m.entity.mob.EntityCreeperTainted creeper)) return EventResult.pass();
        if (!source.is(com.hbm_m.damagesource.ModDamageTypes.BOXCAR)) return EventResult.pass();
        ModAdvancements.grantNearby(creeper, 50D, ModAdvancements.HIDDEN);
        return EventResult.pass();
    }

    private static EventResult onBlockBreak(net.minecraft.world.level.Level level,
                                            net.minecraft.core.BlockPos pos,
                                            net.minecraft.world.level.block.state.BlockState state,
                                            net.minecraft.server.level.ServerPlayer player,
                                            dev.architectury.utils.value.IntValue exp) {
        if (state.is(ModBlocks.STONE_GNEISS.get())) {
            ModAdvancements.grant(player, ModAdvancements.STRATUM);
        }
        return EventResult.pass();
    }
}
