package com.hbm_m.test;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.FluidDuctBlockEntity;
import com.hbm_m.blockentity.machines.MachineFluidTankBlockEntity;
import com.hbm_m.inventory.FluidContainerRegistry;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.liquids.FluidIdentifierItem;
import com.hbm_m.item.liquids.ItemFluidTank;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
//? if forge {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} elif neoforge {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
 *///?}

/**
 * Faesser und Fluidtanks (Nutzer-Meldung 2026-10-13 "Barrels/Tanks funktionieren nicht mehr"):
 * Identifier-Slot, Behaelter laden/entladen, Rohrtransfer, Plattform-Fluid-Capability je Modus,
 * Abbau mit Inhalt im Gegenstand und Wiedersetzen, unendliches Fass.
 */
@GameTestHolder("hbm_m")
@PrefixGameTestTemplate(false)
public final class BarrelTankGameTest {

    private BarrelTankGameTest() {}

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    private static Fluid water() { return ModFluids.WATER.getSource(); }

    private static Player makePlayer(GameTestHelper helper) {
        //? if < 1.21.1 {
        return helper.makeMockPlayer();
        //?} else {
        /*return helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        *///?}
    }

    private static MachineFluidTankBlockEntity place(GameTestHelper helper, Block block, BlockPos rel) {
        BlockPos pos = helper.absolutePos(rel);
        helper.getLevel().setBlock(pos, block.defaultBlockState(), Block.UPDATE_ALL);
        var be = helper.getLevel().getBlockEntity(pos);
        check(be instanceof MachineFluidTankBlockEntity, block + " hat keine Tank-Blockentitaet: " + be);
        return (MachineFluidTankBlockEntity) be;
    }

    private static ItemStack identifier(Fluid f) {
        ItemStack id = new ItemStack(ModItems.FLUID_IDENTIFIER.get());
        FluidIdentifierItem.setType(id, f, true);
        return id;
    }

    /** Identifier setzt den Typ, volle Behaelter fuellen, leere Behaelter entleeren. */
    @GameTest(template = "empty3x3x3", batch = "barrel_tank", timeoutTicks = 200)
    public static void barrelLoadsAndUnloadsItems(GameTestHelper helper) {
        MachineFluidTankBlockEntity be = place(helper, ModBlocks.BARREL_STEEL.get(), new BlockPos(1, 1, 1));
        be.getItemHandler().setStackInSlot(MachineFluidTankBlockEntity.SLOT_ID_IN, identifier(water()));
        ItemStack full = ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), water(), 2);
        int per = FluidContainerRegistry.getFluidContent(full, water());
        check(per > 0, "fluid_tank_full (Wasser) nicht in der FluidContainerRegistry");
        be.getItemHandler().setStackInSlot(MachineFluidTankBlockEntity.SLOT_LOAD_IN, full);

        helper.startSequence()
                .thenWaitUntil(() -> {
                    check(be.getFluidTank().getTankType() == water(), "Typ nicht Wasser: " + be.getFluidTank().getTankType());
                    check(be.getFluidTank().getFill() == 2 * per, "Fuellstand " + be.getFluidTank().getFill() + " statt " + (2 * per));
                })
                .thenExecute(() -> be.getItemHandler().setStackInSlot(MachineFluidTankBlockEntity.SLOT_UNLOAD_IN, new ItemStack(ModItems.FLUID_TANK_EMPTY.get(), 1)))
                .thenWaitUntil(() -> {
                    check(be.getFluidTank().getFill() == per, "Entladen: Fuellstand " + be.getFluidTank().getFill());
                    ItemStack out = be.getItemHandler().getStackInSlot(MachineFluidTankBlockEntity.SLOT_UNLOAD_OUT);
                    check(FluidContainerRegistry.getFluidContent(out, water()) == per, "Entladen: kein voller Behaelter im Ausgang: " + out);
                })
                .thenSucceed();
    }

    /** Volles Fluessigkeitsfass (fluid_barrel_full) und unendliches Fass fuellen den Tank. */
    @GameTest(template = "empty3x3x3", batch = "barrel_tank", timeoutTicks = 200)
    public static void fluidBarrelAndInfiniteFillTank(GameTestHelper helper) {
        MachineFluidTankBlockEntity a = place(helper, ModBlocks.BARREL_TCALLOY.get(), new BlockPos(0, 1, 1));
        MachineFluidTankBlockEntity b = place(helper, ModBlocks.BARREL_PLASTIC.get(), new BlockPos(2, 1, 1));
        a.getItemHandler().setStackInSlot(MachineFluidTankBlockEntity.SLOT_ID_IN, identifier(water()));
        b.getItemHandler().setStackInSlot(MachineFluidTankBlockEntity.SLOT_ID_IN, identifier(water()));
        ItemStack barrel = ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), water(), 1);
        int content = FluidContainerRegistry.getFluidContent(barrel, water());
        check(content > 0, "fluid_barrel_full (Wasser) nicht in der FluidContainerRegistry");
        a.getItemHandler().setStackInSlot(MachineFluidTankBlockEntity.SLOT_LOAD_IN, barrel);
        b.getItemHandler().setStackInSlot(MachineFluidTankBlockEntity.SLOT_LOAD_IN, new ItemStack(ModItems.FLUID_BARREL_INFINITE.get()));

        helper.succeedWhen(() -> {
            check(a.getFluidTank().getFill() == content, "fluid_barrel_full: Fuellstand " + a.getFluidTank().getFill() + " statt " + content);
            check(b.getFluidTank().getFill() == b.getFluidTank().getMaxFill(), "Unendliches Fass: Fuellstand " + b.getFluidTank().getFill());
            check(!b.getItemHandler().getStackInSlot(MachineFluidTankBlockEntity.SLOT_LOAD_IN).isEmpty()
                    || !b.getItemHandler().getStackInSlot(MachineFluidTankBlockEntity.SLOT_LOAD_OUT).isEmpty(), "Unendliches Fass verbraucht");
        });
    }

    /** Rohr: Fass A (Modus 2 = nur abgeben) -> Rohr -> Fass B (Modus 0 = nur annehmen). */
    @GameTest(template = "empty3x3x3", batch = "barrel_tank", timeoutTicks = 200)
    public static void pipeMovesFluidBetweenBarrels(GameTestHelper helper) {
        MachineFluidTankBlockEntity a = place(helper, ModBlocks.BARREL_IRON.get(), new BlockPos(0, 1, 1));
        MachineFluidTankBlockEntity b = place(helper, ModBlocks.BARREL_STEEL.get(), new BlockPos(2, 1, 1));
        a.getFluidTank().conform(water());
        a.getFluidTank().setFill(4000);
        a.handleModeButton();
        a.handleModeButton();
        check(a.getMode() == 2 && b.getMode() == 0, "Modi " + a.getMode() + "/" + b.getMode());
        b.getFluidTank().conform(water());

        BlockPos duct = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlock(duct, ModBlocks.FLUID_DUCT.get().defaultBlockState(), Block.UPDATE_ALL);
        check(helper.getLevel().getBlockEntity(duct) instanceof FluidDuctBlockEntity, "kein Rohr");
        ((FluidDuctBlockEntity) helper.getLevel().getBlockEntity(duct)).setFluidType(water());

        helper.succeedWhen(() -> {
            check(b.getFluidTank().getFill() == 4000 && a.getFluidTank().getFill() == 0,
                    "Rohr: A=" + a.getFluidTank().getFill() + " B=" + b.getFluidTank().getFill());
        });
    }

    /**
     * Plattform-Fluid-Capability (Fremdmods, Eimer am Block): Modus 0 nimmt an, Modus 2 gibt ab, Modus 3 sperrt
     * (Original-Nummerierung wie getReceivingTanks/getSendingTanks).
     */
    @GameTest(template = "empty3x3x3", batch = "barrel_tank", timeoutTicks = 100)
    public static void platformCapabilityFollowsMode(GameTestHelper helper) {
        MachineFluidTankBlockEntity be = place(helper, ModBlocks.BARREL_STEEL.get(), new BlockPos(1, 1, 1));
        be.getFluidTank().conform(water());
        Level level = helper.getLevel();
        BlockPos pos = be.getBlockPos();
        //? if forge {
        var h = com.hbm_m.api.fluids.FluidBlockAccess.getFluidHandler(level, pos, Direction.UP);
        check(h != null, "keine Fluid-Capability");
        var stack = new net.minecraftforge.fluids.FluidStack(water(), 1000);
        var exec = net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;
        //?} elif neoforge {
        /*var h = com.hbm_m.api.fluids.FluidBlockAccess.getFluidHandler(level, pos, Direction.UP);
        check(h != null, "keine Fluid-Capability");
        var stack = new net.neoforged.neoforge.fluids.FluidStack(water(), 1000);
        var exec = net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;
        *///?}
        // Modus 0: nur annehmen
        check(h.fill(stack.copy(), exec) == 1000, "Modus 0 (annehmen) verweigert fill");
        check(h.drain(500, exec).isEmpty(), "Modus 0 gibt ab");
        be.handleModeButton(); // 1 Puffer
        check(h.fill(stack.copy(), exec) == 1000, "Modus 1 (Puffer) verweigert fill");
        check(h.drain(500, exec).getAmount() == 500, "Modus 1 (Puffer) verweigert drain");
        be.handleModeButton(); // 2 nur abgeben
        check(h.fill(stack.copy(), exec) == 0, "Modus 2 nimmt an");
        check(h.drain(500, exec).getAmount() == 500, "Modus 2 (abgeben) verweigert drain");
        be.handleModeButton(); // 3 aus
        check(h.fill(stack.copy(), exec) == 0 && h.drain(500, exec).isEmpty(), "Modus 3 nicht gesperrt");
        check(be.getFluidTank().getFill() == 1000, "Endstand " + be.getFluidTank().getFill());
        helper.succeed();
    }

    /** Abbau: Inhalt + Modus im Gegenstand, Wiedersetzen stellt beides her. */
    @GameTest(template = "empty3x3x3", batch = "barrel_tank", timeoutTicks = 100)
    public static void breakKeepsContentsAndReplaceRestores(GameTestHelper helper) {
        MachineFluidTankBlockEntity be = place(helper, ModBlocks.BARREL_STEEL.get(), new BlockPos(0, 1, 0));
        be.getFluidTank().conform(water());
        be.getFluidTank().setFill(5000);
        be.handleModeButton();
        ServerLevel level = helper.getLevel();
        BlockPos pos = be.getBlockPos();
        BlockState state = level.getBlockState(pos);
        var drops = Block.getDrops(state, level, pos, be);
        ItemStack item = drops.stream().filter(s -> s.is(ModBlocks.BARREL_STEEL.get().asItem())).findFirst().orElse(ItemStack.EMPTY);
        check(!item.isEmpty(), "kein Fass-Drop: " + drops);
        check(com.hbm_m.platform.BlockEntityItemData.has(item), "Fass-Drop ohne Inhalt");
        level.removeBlockEntity(pos);
        level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);

        BlockPos target = helper.absolutePos(new BlockPos(2, 1, 2));
        Player player = makePlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, item);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(target.below()).add(0, 0.5, 0), Direction.UP, target.below(), false);
        BlockPlaceContext ctx = new BlockPlaceContext(level, player, InteractionHand.MAIN_HAND, item, hit);
        target = ctx.getClickedPos();
        var result = ((BlockItem) item.getItem()).place(ctx);
        check(result.consumesAction(), "Wiedersetzen fehlgeschlagen: " + result);
        var placed = level.getBlockEntity(target);
        check(placed instanceof MachineFluidTankBlockEntity, "kein Fass gesetzt");
        MachineFluidTankBlockEntity p = (MachineFluidTankBlockEntity) placed;
        check(p.getFluidTank().getTankType() == water() && p.getFluidTank().getFill() == 5000,
                "Inhalt verloren: " + p.getFluidTank().getTankType() + " " + p.getFluidTank().getFill());
        check(p.getMode() == 1, "Modus verloren: " + p.getMode());
        helper.succeed();
    }

    /** Grosser Tank (Kern) laedt Behaelter genauso. */
    @GameTest(template = "empty3x3x3", batch = "barrel_tank", timeoutTicks = 200)
    public static void bigTankCoreLoadsItems(GameTestHelper helper) {
        MachineFluidTankBlockEntity be = place(helper, ModBlocks.FLUID_TANK.get(), new BlockPos(1, 1, 1));
        be.getItemHandler().setStackInSlot(MachineFluidTankBlockEntity.SLOT_ID_IN, identifier(water()));
        ItemStack full = ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), water(), 1);
        int per = FluidContainerRegistry.getFluidContent(full, water());
        be.getItemHandler().setStackInSlot(MachineFluidTankBlockEntity.SLOT_LOAD_IN, full);
        helper.succeedWhen(() -> check(be.getFluidTank().getFill() == per, "Tank: Fuellstand " + be.getFluidTank().getFill()));
    }

    /**
     * GUI-Slots: die HBM-Behaelter (ohne Plattform-Capability) muessen sich in Fass-, Tank- und BAT9000-Menue legen
     * lassen - voll in den Lade-, leer in den Entlade-Slot (Nutzer-Meldung "laesst sich nicht einfuegen").
     */
    @GameTest(template = "empty3x3x3", batch = "barrel_tank", timeoutTicks = 100)
    public static void menuSlotsAcceptHbmContainers(GameTestHelper helper) {
        Player player = makePlayer(helper);
        MachineFluidTankBlockEntity be = place(helper, ModBlocks.BARREL_STEEL.get(), new BlockPos(1, 1, 1));
        Fluid h2 = ModFluids.HYDROGEN.getSource();
        ItemStack[] full = {
                ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), h2, 1),
                ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), water(), 1),
                ((ItemFluidTank) ModItems.CANISTER_FULL.get()).getSubItems().get(0),
                ((ItemFluidTank) ModItems.GAS_FULL.get()).getSubItems().get(0),
                new ItemStack(ModItems.FLUID_BARREL_INFINITE.get()),
        };
        ItemStack[] empty = { new ItemStack(ModItems.FLUID_BARREL_EMPTY.get()), new ItemStack(ModItems.FLUID_TANK_EMPTY.get()),
                new ItemStack(ModItems.CANISTER_EMPTY.get()), new ItemStack(ModItems.GAS_EMPTY.get()) };
        // Jedes Fass/jeder Tank ueber sein eigenes createMenu (wie beim Oeffnen der GUI). Frueher fehlte hier das
        // FluidTankMenu (Plastik/Tcalloy/Korrodiert/Antimaterie/Orbus/Grosstank), dessen Slots nur die Capability prueften
        // (Nutzer-Meldung "kann nichts in Faesser fuellen", im Client-Test BarrelFillClientTestHook nachgestellt).
        java.util.List<net.minecraft.world.inventory.AbstractContainerMenu> menus = new java.util.ArrayList<>();
        menus.add(be.createMenu(0, player.getInventory(), player));
        Block[] blocks = { ModBlocks.BARREL_IRON.get(), ModBlocks.BARREL_PLASTIC.get(), ModBlocks.BARREL_TCALLOY.get(),
                ModBlocks.BARREL_CORRODED.get(), ModBlocks.BARREL_ANTIMATTER.get(), ModBlocks.FLUID_TANK.get() };
        for (int i = 0; i < blocks.length; i++) {
            // Menues halten nur die Referenz; spaeteres Ueberbauen durch den Grosstank stoert die Slot-Pruefung nicht
            menus.add(place(helper, blocks[i], new BlockPos(i % 3, 2, i / 3 * 2)).createMenu(0, player.getInventory(), player));
        }
        for (var menu : menus) {
            var load = menu.slots.get(MachineFluidTankBlockEntity.SLOT_LOAD_IN);
            var unload = menu.slots.get(MachineFluidTankBlockEntity.SLOT_UNLOAD_IN);
            check(load.getContainerSlot() == MachineFluidTankBlockEntity.SLOT_LOAD_IN && unload.getContainerSlot() == MachineFluidTankBlockEntity.SLOT_UNLOAD_IN, "Slotreihenfolge " + menu);
            for (ItemStack s : full) check(load.mayPlace(s), menu.getClass().getSimpleName() + ": Lade-Slot verweigert " + s);
            for (ItemStack s : empty) check(unload.mayPlace(s), menu.getClass().getSimpleName() + ": Entlade-Slot verweigert " + s);
            check(!load.mayPlace(new ItemStack(net.minecraft.world.item.Items.DIRT)), "Erde im Lade-Slot");
            // Shift-Klick (Hotbar 0 = Menueslot 33): Identifier in Slot 0, voller Behaelter in den Ladeslot
            var inv = player.getInventory();
            var idSlot = menu.slots.get(MachineFluidTankBlockEntity.SLOT_ID_IN);
            inv.clearContent();
            idSlot.set(ItemStack.EMPTY);
            load.set(ItemStack.EMPTY);
            inv.setItem(0, identifier(water()));
            menu.quickMoveStack(player, 33);
            check(FluidIdentifierItem.resolvePrimaryForTank(idSlot.getItem()) == water(),
                    menu.getClass().getSimpleName() + ": Shift-Klick legt den Identifier nicht in Slot 0");
            inv.setItem(0, ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), water(), 1));
            menu.quickMoveStack(player, 33);
            check(!load.getItem().isEmpty(), menu.getClass().getSimpleName() + ": Shift-Klick legt den vollen Tank nicht in den Ladeslot");
            idSlot.set(ItemStack.EMPTY);
            load.set(ItemStack.EMPTY);
        }
        helper.succeed();
    }

    /** Raketenstartrampen-Fall: Wasserstoff-Fluessigkeitsfass fuellt einen Wasserstofftank (Raketenbedarf 64.000 mB). */
    @GameTest(template = "empty3x3x3", batch = "barrel_tank", timeoutTicks = 100)
    public static void hydrogenFluidBarrelLoadsTank(GameTestHelper helper) {
        Fluid h2 = ModFluids.HYDROGEN.getSource();
        com.hbm_m.inventory.fluid.tank.FluidTank tank = new com.hbm_m.inventory.fluid.tank.FluidTank(h2, 64_000);
        ItemStack[] slots = { ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), h2, 2), ItemStack.EMPTY };
        check(FluidContainerRegistry.getFluidContent(slots[3], h2) == 16_000, "Wasserstoff-Fass nicht registriert");
        check(tank.loadTank(3, 4, slots), "loadTank Wasserstoff-Fass");
        check(tank.loadTank(3, 4, slots), "loadTank Wasserstoff-Fass 2");
        check(tank.getFill() == 32_000 && tank.getTankType() == h2, "Fuellstand " + tank.getFill());
        check(slots[4].is(ModItems.FLUID_BARREL_EMPTY.get()) && slots[4].getCount() == 2 && slots[3].isEmpty(), "leere Faesser " + slots[4]);
        // Alle Fluessigkeiten mit Fass/Tank: Eintrag vorhanden
        int missing = 0;
        StringBuilder sb = new StringBuilder();
        for (ItemStack s : ((ItemFluidTank) ModItems.FLUID_BARREL_FULL.get()).getSubItems()) {
            Fluid f = ItemFluidTank.getFluid(s);
            if (FluidContainerRegistry.getFluidContent(s, f) != 16_000) { missing++; sb.append(' ').append(f); }
        }
        check(missing == 0, missing + " Fass-Fluessigkeiten ohne Registry-Eintrag:" + sb);
        helper.succeed();
    }
}
