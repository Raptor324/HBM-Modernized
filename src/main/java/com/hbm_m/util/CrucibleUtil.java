package com.hbm_m.util;

import com.hbm_m.api.block.ICrucibleAcceptor;
import com.hbm_m.inventory.material.MaterialStack;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;
import com.hbm_m.item.material.ScrapItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Порт {@code com.hbm.util.CrucibleUtil} (1.7.10): вертикальный хитскан вниз
 * от точки налива до первого блока с {@link ICrucibleAcceptor}; розлив списком
 * стаканов порциями не меньше {@code minQuanta} mB; «safe»-семантика — без
 * приёмника ничего не теряется. Количество — в mB (в оригинале кванты 72/слиток).
 */
public class CrucibleUtil {

    /**
     * Finds the first non-air block from startPos (inclusive) downward within range
     * and returns its position if its block entity is an ICrucibleAcceptor, else null.
     */
    public static @Nullable BlockPos getPouringTarget(Level level, BlockPos startPos, int range) {
        for (int i = 0; i <= range; i++) {
            BlockPos pos = startPos.below(i);
            if (level.getBlockState(pos).isAir()) continue;
            BlockEntity be = level.getBlockEntity(pos);
            return be instanceof ICrucibleAcceptor ? pos : null;
        }
        return null;
    }

    /**
     * Порт {@code pourSingleStack(world, x, y, z, range, safe, stack, quanta, impact)}:
     * наливает в приёмник не более {@code maxAmount} mB стакана; при safe=true и
     * отсутствии приёмника ничего не теряется. Возвращает реально налитое количество.
     */
    public static int pourSingleStack(Level level, BlockPos startPos, int range, MaterialStack stack) {
        return pourSingleStack(level, startPos, range, stack, stack.amount);
    }

    /** Как выше, но с ограничением порции (порт параметра quanta). */
    public static int pourSingleStack(Level level, BlockPos startPos, int range, MaterialStack stack, int maxAmount) {
        if (stack.isEmpty()) return 0;

        BlockPos target = getPouringTarget(level, startPos, range);
        if (target == null) return 0;

        BlockEntity be = level.getBlockEntity(target);
        if (!(be instanceof ICrucibleAcceptor acc)) return 0;

        MaterialStack portion = new MaterialStack(stack.type, Math.min(stack.amount, maxAmount));
        if (!acc.canAcceptPartialPour(level, target, Direction.UP, portion)) return 0;

        MaterialStack left = acc.pour(level, target, Direction.UP, portion);
        int poured = left == null ? portion.amount : portion.amount - left.amount;
        stack.amount -= poured;
        return poured;
    }

    /**
     * Порт {@code pourFullStack}: розлив списка стаканов porциями по {@code minQuanta} mB
     * в порядке списка (первый совпавший приёмник). Стаканы мутируются (уменьшаются);
     * {@code impact} (если не null) получает точку попадания потока.
     *
     * @return стакан материала, который удалось налить (для цвета/частиц), либо null.
     */
    public static @Nullable MaterialStack pourFullStack(Level level, double x, double y, double z,
                                                        int range, boolean safe, List<MaterialStack> stacks,
                                                        int minQuanta, @Nullable Vec3[] impact) {
        BlockPos startPos = BlockPos.containing(x, y, z);

        for (MaterialStack stack : stacks) {
            if (stack.isEmpty()) continue;
            // Порт tryPourStack: не-SMELTABLE материалы (добавки) не наливаются
            if (!stack.type.isPourable()) continue;

            int amountToPour = Math.min(stack.amount, minQuanta);
            MaterialStack portion = new MaterialStack(stack.type, amountToPour);
            int poured = pourSingleStack(level, startPos, range, stack, amountToPour);
            if (poured > 0) {
                if (impact != null && impact.length > 0) {
                    impact[0] = new Vec3(x, y - 0.875, z);
                }
                return new MaterialStack(stack.type, poured);
            }
            // В оригинале при неудаче первого стакана выполняется spill (waste) — но кран
            // тигля всегда вызывает safe=true: без приёмника ничего не теряется.
            if (!safe) {
                int toWaste = Math.min(stack.amount, minQuanta);
                stack.amount -= toWaste;
                if (impact != null && impact.length > 0) {
                    impact[0] = new Vec3(x, y - 0.875, z);
                }
                return null;
            }
            break; // как в оригинале: только один стакан за вызов при safe
        }
        return null;
    }

    /**
     * Шлак-предмет из MaterialStack: mB → кванты ({@code ×72/1000}), материал —
     * по имени MaterialType через {@link ModMaterials#byId}; без предмета шлака
     * для материала возвращает EMPTY.
     */
    public static ItemStack createScrap(MaterialStack stack) {
        if (stack == null || stack.isEmpty()) return ItemStack.EMPTY;
        ModMaterials mat = ModMaterials.byId(stack.type.name);
        if (mat == null) return ItemStack.EMPTY;
        Item item = ModMaterialItems.scrapItem(mat);
        if (item == null) return ItemStack.EMPTY;

        int quanta = (int) ((long) stack.amount * ScrapItem.QUANTA_PER_INGOT / MaterialStack.MB_PER_INGOT);
        if (quanta <= 0) return ItemStack.EMPTY;

        ItemStack out = new ItemStack(item, 1);
        // ScrapItem читает количество из тега "amount"
        com.hbm_m.platform.PlatformHooks.editItemTag(out, tag -> tag.putInt("amount", quanta));
        return out;
    }
}
