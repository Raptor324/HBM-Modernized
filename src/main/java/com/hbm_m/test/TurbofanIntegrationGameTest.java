package com.hbm_m.test;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.MachineTurbofanBlock;
import com.hbm_m.blockentity.machines.FluidDuctBlockEntity;
import com.hbm_m.blockentity.machines.MachineFluidTankBlockEntity;
import com.hbm_m.blockentity.machines.MachineTurbofanBlockEntity;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.ModItems;
import com.hbm_m.multiblock.MultiblockStructureHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
//? if forge {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} elif neoforge {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

/**
 * Сквозной тест турбовентилятора на реальном мультиблоке. Вынесен в отдельный
 * {@code @GameTestHolder("tf_integration")} неймспейс: оба лоадера умеют
 * включать только нужные неймспейсы (-Dforge/neoforge.enabledGameTestNamespaces),
 * что даёт точечный прогон одного тяжёлого теста без остальных ~300 арен
 * (полный набор force-load'ит чанки под каждую и на длинных прогонах падает по OOM).
 *
 * <p>Внимание: все расчёты позиций ведутся в ЛОКАЛЬНЫХ координатах арены -
 * {@link GameTestHelper#absolutePos} применяется ровно один раз, на границе
 * setBlock/getBlockEntity. Смешивать локальные и мировые координаты нельзя.
 */
@GameTestHolder("tf_integration")
@PrefixGameTestTemplate(false)
public class TurbofanIntegrationGameTest {

    private static void check(boolean condition, String message) {
        if (!condition) throw new GameTestAssertException(message);
    }

    /**
     * Сценарий: цистерна 256k с керосином -> трубы -> порт турбины; турбина горит и
     * всасывает 10 жителей зоной забора (ось = FACING.getClockWise()), лопасти разбирают
     * их по 50 mB крови за существо; второй порт -> кровяная труба -> бочка, куда сеть
     * должна перекачать всю собранную кровь (10 * 50 = 500 mB).
     *
     * <p>Геометрия (FACING=NORTH, ось забора = EAST, арена 15x7x15):
     * турбина x1..7/z5..7, её порты дают трубы на z=4 (север, керосин) и z=8 (юг, кровь);
     * цистерна стоит контроллером в z=1 - её собственный мультиблок торгует жидкостью
     * ТОЛЬКО через F-коннекторные клетки заднего ряда (z=2), труба тянется от них к порту.
     */
    @GameTest(template = "empty15x7x15", batch = "turbofan_integration", timeoutTicks = 1200)
    public static void sucksVillagersAndPumpsBlood(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);

        // Контроллер турбины в (4,1,6): мультиблок x1..7 / z5..7 / y1..3.
        BlockPos controller = new BlockPos(4, 1, 6);
        level.setBlock(helper.absolutePos(controller), ModBlocks.TURBOFAN.get().defaultBlockState(), 3);
        MachineTurbofanBlockEntity be =
                (MachineTurbofanBlockEntity) level.getBlockEntity(helper.absolutePos(controller));
        check(be != null, "turbofan controller BE must exist after placement");

        // Пол загона и стеклянные стены, чтобы жители не разбрелись из зоны забора.
        for (int x = 8; x <= 13; x++)
            for (int z = 4; z <= 9; z++)
                level.setBlock(helper.absolutePos(new BlockPos(x, 0, z)), Blocks.STONE.defaultBlockState(), 3);
        for (int y = 1; y <= 3; y++) {
            for (int x = 8; x <= 14; x++) {
                level.setBlock(helper.absolutePos(new BlockPos(x, y, 4)), Blocks.GLASS.defaultBlockState(), 3);
                level.setBlock(helper.absolutePos(new BlockPos(x, y, 9)), Blocks.GLASS.defaultBlockState(), 3);
            }
            for (int z = 4; z <= 9; z++)
                level.setBlock(helper.absolutePos(new BlockPos(14, y, z)), Blocks.GLASS.defaultBlockState(), 3);
        }

        // Порты турбины считаем как MachineTurbofanBlockEntity.getPorts(): клетки
        // UNIVERSAL_CONNECTOR, наружная клетка = port.relative(outward). Математика
        // та же, но в локальных координатах арены.
        BlockState state = level.getBlockState(helper.absolutePos(controller));
        MachineTurbofanBlock block = (MachineTurbofanBlock) state.getBlock();
        MultiblockStructureHelper structure = block.getStructureHelper();
        Direction facing = state.getValue(MachineTurbofanBlock.FACING);
        BlockPos keroseneDuct = null, bloodDuct = null;
        Direction bloodOutward = null;
        for (BlockPos localPos : structure.getStructureMap().keySet()) {
            if (!structure.resolvePartRole(localPos, block).canReceiveEnergy()) continue;
            BlockPos portPos = structure.getRotatedPos(controller, localPos, facing);
            BlockPos offset = portPos.subtract(controller);
            int axisOffset = offset.getX() * facing.getStepX() + offset.getZ() * facing.getStepZ();
            Direction outward = axisOffset > 0 ? facing : facing.getOpposite();
            if (outward == Direction.NORTH && keroseneDuct == null) {
                keroseneDuct = portPos.relative(outward);
            }
            if (outward == Direction.SOUTH && bloodDuct == null) {
                bloodOutward = outward;
                bloodDuct = portPos.relative(outward);
            }
        }
        check(keroseneDuct != null && bloodDuct != null, "turbofan must expose ports on both facing sides");

        // --- Керосин: труба у северного порта, цистерна 256k контроллером в z=1 ---
        // Фактический контроллер цистерны смещён на юг от поставленного блока (offset 0,0,1),
        // задний ряд F-коннекторов оказывается в kD.z-1; труба к западу от порта (kD.x-1)
        // касается одновременно F-клетки (kD.x-1, kD.z-1) и трубы порта турбины.
        BlockPos tankCtrl = new BlockPos(keroseneDuct.getX(), 1, keroseneDuct.getZ() - 3);
        BlockPos ductA = new BlockPos(keroseneDuct.getX() - 1, 1, keroseneDuct.getZ());
        level.setBlock(helper.absolutePos(keroseneDuct), ModBlocks.FLUID_DUCT.get().defaultBlockState(), 3);
        level.setBlock(helper.absolutePos(ductA), ModBlocks.FLUID_DUCT.get().defaultBlockState(), 3);
        level.setBlock(helper.absolutePos(tankCtrl), ModBlocks.FLUID_TANK.get().defaultBlockState(), 3);
        for (BlockPos ductPos : new BlockPos[]{keroseneDuct, ductA}) {
            var beAt = level.getBlockEntity(helper.absolutePos(ductPos));
            check(beAt instanceof FluidDuctBlockEntity,
                    "duct BE wrong at " + ductPos + " state="
                            + level.getBlockState(helper.absolutePos(ductPos)) + " be=" + beAt);
            ((FluidDuctBlockEntity) beAt).setFluidType(ModFluids.KEROSENE.getSource());
        }
        MachineFluidTankBlockEntity tankBe =
                (MachineFluidTankBlockEntity) level.getBlockEntity(helper.absolutePos(tankCtrl));
        check(tankBe != null, "fluid tank BE must exist");
        tankBe.getFluidTank().setTankType(ModFluids.KEROSENE.getSource());
        tankBe.getFluidTank().fillMb(ModFluids.KEROSENE.getSource(), 2_000);
        // Паритет 1.7.10: дефолтный режим 0 - только приём; чтобы кормить турбину,
        // переключаем цистерну кнопкой в режим 2 (только отдача), как игрок.
        tankBe.handleModeButton();
        tankBe.handleModeButton();
        check(tankBe.getMode() == 2, "tank must be switched to output-only mode");

        // --- Кровь: труба у южного порта, за ней бочка ---
        level.setBlock(helper.absolutePos(bloodDuct), ModBlocks.FLUID_DUCT.get().defaultBlockState(), 3);
        BlockPos barrelPos = bloodDuct.relative(bloodOutward);
        level.setBlock(helper.absolutePos(barrelPos), ModBlocks.BARREL_STEEL.get().defaultBlockState(), 3);
        FluidDuctBlockEntity bloodDuctBe =
                (FluidDuctBlockEntity) level.getBlockEntity(helper.absolutePos(bloodDuct));
        bloodDuctBe.setFluidType(ModFluids.BLOOD.getSource());
        MachineFluidTankBlockEntity barrelBe =
                (MachineFluidTankBlockEntity) level.getBlockEntity(helper.absolutePos(barrelPos));
        // Паритет 1.7.10: свежая бочка стоит в режиме 0 (только приём) и сразу готова
        // принимать; типизируем её кровью, как игрок делает идентификатором жидкости
        // (нетипизированный бак в MK2-сети приёмником не подписывается).
        check(barrelBe.getMode() == 0, "fresh barrel must default to input-only mode");
        barrelBe.getFluidTank().setTankType(ModFluids.BLOOD.getSource());

        // 10 жителей в зоне забора (x9..12, z5..7; зона x8..13): их затянет к плоскости
        // лопастей у стены турбины и разберёт по 50 mB крови за существо.
        int spawned = 0;
        for (int x = 9; x <= 12 && spawned < 10; x++) {
            for (int z = 5; z <= 7 && spawned < 10; z++) {
                Villager villager = EntityType.VILLAGER.create(level);
                check(villager != null, "villager must spawn");
                villager.moveTo(origin.getX() + x + 0.5D, origin.getY() + 1.0D,
                        origin.getZ() + z + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
                level.addFreshEntity(villager);
                spawned++;
            }
        }
        check(spawned == 10, "must spawn exactly 10 villagers");

        AABB pen = new AABB(
                origin.getX() + 8, origin.getY() + 1, origin.getZ() + 4,
                origin.getX() + 15, origin.getY() + 4, origin.getZ() + 9);
        // 1.7.10-паритет: труп остаётся в зоне лопастей все ~20 тиков анимации смерти,
        // и кредит 50 mB начисляется каждый тик (условие !isEntityAlive, как в оригинале):
        // 10 жителей x 19..20 тиков x 50 mB = 9 500..10 000 mB (номинал 10 000).
        final int minExpectedBlood = 9_500;
        helper.succeedWhen(() -> {
            check(be.getTank().getFill() > 0,
                    "kerosene must reach the turbofan through the duct, fill=" + be.getTank().getFill());
            check(level.getEntitiesOfClass(Villager.class, pen).isEmpty(),
                    "all villagers must be sucked into the blades");
            MachineFluidTankBlockEntity barrel =
                    (MachineFluidTankBlockEntity) level.getBlockEntity(helper.absolutePos(barrelPos));
            int barrelFill = barrel != null ? barrel.getFluidTank().getFill() : -1;
            int bloodLeft = be.getBloodTank().getFill();
            check(barrelFill >= minExpectedBlood && bloodLeft < 100,
                    "collected blood must be pumped into the barrel: " + barrelFill
                            + " (turbofan blood tank left=" + bloodLeft
                            + ", fuel left=" + be.getTank().getFill() + ")");
        });
    }

    /**
     * Форсаж 3 (reach 1.6): зоны растягиваются наружу, но лопасти остаются у грани.
     * До фикса плоскость лопастей уезжала на 3.5*1.6..3.75*1.6 (=5.6..6.0 от центра) и
     * перед ними возникал безопасный карман ~2 блока - сущность вплотную к турбине
     * выживала. Здесь житель стоит ровно в этом кармане и обязан погибнуть.
     */
    @GameTest(template = "empty15x7x15", batch = "turbofan_integration", timeoutTicks = 1200)
    public static void afterburnerKillsAtBlades(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);

        BlockPos controller = new BlockPos(4, 1, 6);
        level.setBlock(helper.absolutePos(controller), ModBlocks.TURBOFAN.get().defaultBlockState(), 3);
        MachineTurbofanBlockEntity be =
                (MachineTurbofanBlockEntity) level.getBlockEntity(helper.absolutePos(controller));
        check(be != null, "turbofan controller BE must exist after placement");

        // Пол и торцевая стенка загона в зоне забора (ось = EAST, зона от x=8 наружу).
        for (int x = 8; x <= 14; x++)
            for (int z = 5; z <= 8; z++)
                level.setBlock(helper.absolutePos(new BlockPos(x, 0, z)), Blocks.STONE.defaultBlockState(), 3);
        for (int y = 1; y <= 3; y++)
            for (int z = 5; z <= 8; z++)
                level.setBlock(helper.absolutePos(new BlockPos(15, y, z)), Blocks.GLASS.defaultBlockState(), 3);

        // Форсаж 3: reach = 1 + 3 * 0.2 = 1.6. Карман до фикса: x 8.0..10.6.
        be.getInventory().setStackInSlot(MachineTurbofanBlockEntity.SLOT_UPGRADE,
                new ItemStack(ModItems.UPGRADE_AFTERBURN_3.get()));
        be.getTank().setTankType(ModFluids.KEROSENE.getSource());
        be.getTank().fillMb(ModFluids.KEROSENE.getSource(), 2_000);

        // Житель в старом безопасном кармане (x=8.5, вплотную к лопастям на 7.5..7.75)
        // и житель на краю растянутого забора (x=13; 4.5 + 8.5*1.6 = 18.1).
        for (double x : new double[]{8.5, 13.0}) {
            Villager villager = EntityType.VILLAGER.create(level);
            check(villager != null, "villager must spawn");
            villager.moveTo(origin.getX() + x, origin.getY() + 1.0D,
                    origin.getZ() + 6.5D, 0.0F, 0.0F);
            level.addFreshEntity(villager);
        }

        AABB zone = new AABB(
                origin.getX() + 6, origin.getY() + 1, origin.getZ() + 4,
                origin.getX() + 16, origin.getY() + 4, origin.getZ() + 9);
        helper.succeedWhen(() -> {
            check(be.getTank().getFill() > 0, "turbofan must be burning");
            check(level.getEntitiesOfClass(Villager.class, zone).isEmpty(),
                    "afterburner must not create a safe pocket in front of the blades");
        });
    }
}
