package com.hbm_m.test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import com.hbm_m.world.gen.legacy.LB;
import com.hbm_m.world.gen.legacy.MB;
import com.hbm_m.world.gen.legacy.VB;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
//? if forge {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} elif neoforge {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
 *///?}

/**
 * Phase D (Datenladefehler): alle 1.7.10-Meta-Tabellen der Altbau-Weltgen ({@link MB}, {@link VB}) muessen sich
 * als Blockzustaende parsen lassen - sonst setzen die Altverliese an diesen Stellen Luft und das Log fuellt sich
 * mit "[LegacyBlocks] ... nicht lesbar".
 */
@GameTestHolder("hbm_m")
@PrefixGameTestTemplate(false)
public final class LegacyBlockTableGameTest {

    private LegacyBlockTableGameTest() {
    }

    @GameTest(template = "empty5x5x5", batch = "legacy_blocks", timeoutTicks = 20)
    public static void allLegacyBlockStatesParse(GameTestHelper helper) {
        List<String> bad = new ArrayList<>();
        for (Class<?> table : new Class<?>[] { MB.class, VB.class }) {
            for (Field f : table.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers()) || f.getType() != LB.class) continue;
                try {
                    LB lb = (LB) f.get(null);
                    for (String s : lb.unparsable()) bad.add(table.getSimpleName() + "." + f.getName() + ": " + s);
                } catch (IllegalAccessException e) {
                    bad.add(table.getSimpleName() + "." + f.getName() + ": " + e);
                }
            }
        }
        if (!bad.isEmpty()) throw new GameTestAssertException("Nicht lesbare Altblock-Zustaende (" + bad.size() + "): " + bad);
        helper.succeed();
    }
}
