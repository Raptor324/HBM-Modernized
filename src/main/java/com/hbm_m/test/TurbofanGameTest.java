package com.hbm_m.test;

import com.hbm_m.block.machines.MachineTurbofanBlock;
import com.hbm_m.blockentity.machines.MachineTurbofanBlockEntity;
import com.hbm_m.compat.sable.TurbofanVehiclePhysics;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.trait.FT_Combustible;
import com.hbm_m.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
//? if forge {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} elif neoforge {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

/**
 * Тесты турбовентилятора: математика тяги, сгорание AERO-топлива, отказ не-AERO,
 * множитель форсажа, краснокаменная блокировка (порты).
 */
@GameTestHolder("hbm_m")
@PrefixGameTestTemplate(false)
public class TurbofanGameTest {

    private static void check(boolean condition, String message) {
        if (!condition) throw new GameTestAssertException(message);
    }

    private static MachineTurbofanBlockEntity makeBe(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockState state = com.hbm_m.block.ModBlocks.TURBOFAN.get().defaultBlockState();
        MachineTurbofanBlockEntity be = new MachineTurbofanBlockEntity(pos, state);
        be.setLevel(helper.getLevel());
        return be;
    }

    @GameTest(template = "empty3x3x3", batch = "turbofan", timeoutTicks = 100)
    public static void thrustMath(GameTestHelper helper) {
        check(TurbofanVehiclePhysics.thrust(3_850) == 256.0D, "base thrust must be 256 pN");
        check(TurbofanVehiclePhysics.airflow(3_850) == 25.6D, "base airflow must be 25.6 m/s");
        check(TurbofanVehiclePhysics.thrust(0) == 0.0D, "no output - no thrust");
        check(TurbofanVehiclePhysics.isActive(true, 100, 1), "active state");
        check(!TurbofanVehiclePhysics.isActive(false, 100, 1), "off machine is not active");
        // Выхлоп - против оси забора (забор = ClockWise от направления установки).
        check(TurbofanVehiclePhysics.exhaustDirection(Direction.NORTH).getOpposite() == Direction.NORTH.getClockWise(),
                "exhaust axis must oppose intake axis");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "turbofan", timeoutTicks = 100)
    public static void burnAeroFuel(GameTestHelper helper) {
        helper.runAfterDelay(2, () -> {
            MachineTurbofanBlockEntity be = makeBe(helper);
            check(be.getTank().fillMb(ModFluids.KEROSENE.getSource(), 20) == 20, "tank must accept kerosene");

            FT_Combustible trait = FluidType.getTrait(ModFluids.KEROSENE.getSource(), FT_Combustible.class);
            check(trait != null && trait.getGrade() == FT_Combustible.FuelGrade.AERO, "kerosene must be AERO grade");
            long burnValue = trait.getCombustionEnergy() / 1_000L;
            // Керосин = расчётное топливо (оригинал ~3 850 000 HE/ведро): проверяем масштаб.
            check(burnValue >= 3_000L && burnValue <= 4_000L, "kerosene burn value out of range: " + burnValue);

            MachineTurbofanBlockEntity.tick(helper.getLevel(), be.getBlockPos(), be.getBlockState(), be);
            // Один тик сжигает 1 + afterburner mB: без форсажа - 1 mB -> burnValue HE.
            check(be.getEnergyStored() == burnValue, "one tick must burn 1 mB, got " + be.getEnergyStored());
            check(be.getTank().getFill() == 19, "fuel must be drained 1:1");
            check(be.wasOn(), "machine must be on");
            helper.succeed();
        });
    }

    @GameTest(template = "empty3x3x3", batch = "turbofan", timeoutTicks = 100)
    public static void rejectsNonAeroFuel(GameTestHelper helper) {
        helper.runAfterDelay(2, () -> {
            MachineTurbofanBlockEntity be = makeBe(helper);
            be.getTank().setTankType(ModFluids.DIESEL.getSource());
            be.getTank().fillMb(ModFluids.DIESEL.getSource(), 20);

            MachineTurbofanBlockEntity.tick(helper.getLevel(), be.getBlockPos(), be.getBlockState(), be);
            check(be.getEnergyStored() == 0L, "non-AERO fuel must not burn");
            check(be.getTank().getFill() == 20, "non-AERO fuel must not be consumed");
            helper.succeed();
        });
    }

    @GameTest(template = "empty3x3x3", batch = "turbofan", timeoutTicks = 100)
    public static void flamePonyMultiplier(GameTestHelper helper) {
        helper.runAfterDelay(2, () -> {
            MachineTurbofanBlockEntity be = makeBe(helper);
            be.getInventory().setStackInSlot(MachineTurbofanBlockEntity.SLOT_UPGRADE,
                    new ItemStack(ModItems.FLAME_PONY.get()));
            be.getTank().fillMb(ModFluids.KEROSENE.getSource(), 200);

            FT_Combustible trait = FluidType.getTrait(ModFluids.KEROSENE.getSource(), FT_Combustible.class);
            long burnValue = trait.getCombustionEnergy() / 1_000L;

            MachineTurbofanBlockEntity.tick(helper.getLevel(), be.getBlockPos(), be.getBlockState(), be);
            // Оригинал: выход выше maxPower зажимается буфером.
            long expected = Math.min((long) (burnValue * 101 * (1 + Math.min(100 / 3D, 4))), 1_000_000L);
            check(be.getEnergyStored() == expected, "flame pony output mismatch: " + be.getEnergyStored() + " vs " + expected);
            check(be.getAfterburner() == 100, "flame pony must set afterburner to 100");
            helper.succeed();
        });
    }

    @GameTest(template = "empty3x3x3", batch = "turbofan", timeoutTicks = 100)
    public static void redstoneBlocksBurn(GameTestHelper helper) {
        helper.runAfterDelay(2, () -> {
            MachineTurbofanBlockEntity be = makeBe(helper);
            be.getTank().fillMb(ModFluids.KEROSENE.getSource(), 20);

            // Красный камень у внешней клетки порта (передний ряд, колонка центра сетки).
            var level = helper.getLevel();
            level.setBlock(be.getBlockPos().relative(Direction.NORTH, 2), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);

            MachineTurbofanBlockEntity.tick(helper.getLevel(), be.getBlockPos(), be.getBlockState(), be);
            check(be.getEnergyStored() == 0L, "redstone at a port must stop the burn");
            helper.succeed();
        });
    }

}
