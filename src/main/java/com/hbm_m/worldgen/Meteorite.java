package com.hbm_m.worldgen;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.bomb.BlockTaint;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.damagesource.ModDamageSources;
import com.hbm_m.explosion.ExplosionLarge;
import com.hbm_m.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code com.hbm.world.feature.Meteorite}: der Einschlagskoerper eines {@code EntityMeteor}. Grosse, mittlere
 * oder kleine Meteoriten aus Huelle, aeusserer/innerer Fuellung und Kern; mit {@code enableSpecialMeteors} in 1/300
 * der Faelle eine Sonderform (reine Erz-/Schatzmeteoriten, Giftkern, Taint, "Bamboozle", Star Blaster).
 * Die Listen des Originals (ItemStacks mit Metadaten) sind hier Blocklisten, die Metadaten sind eigene Bloecke.
 */
public class Meteorite {

    public static boolean safeMode = false;

    public void generate(Level world, RandomSource rand, int x, int y, int z, boolean safe, boolean allowSpecials, boolean damagingImpact) {
        safeMode = safe;

        if (replacables.isEmpty()) {
            generateReplacables();
        }

        if (damagingImpact) {
            List<Entity> list = world.getEntities((Entity) null, new AABB(x - 7.5, y - 7.5, z - 7.5, x + 7.5, y + 7.5, z + 7.5));

            for (Entity e : list) {
                e.hurt(ModDamageSources.meteorite(world), 1000);
            }
        }

        if (ModClothConfig.get().enableSpecialMeteors && allowSpecials)
            switch (rand.nextInt(300)) {
            case 0: {
                // Meteor-only tiny meteorite
                List<Block> list0 = new ArrayList<>();
                list0.add(ModBlocks.BLOCK_METEOR.get());
                generateBox(world, rand, x, y, z, list0);
                return;
            }
            case 1: {
                // Large ore-only meteorite
                List<Block> list1 = new ArrayList<>(this.getRandomOre(rand));
                int i = list1.size();
                for (int j = 0; j < i; j++)
                    list1.add(ModBlocks.BLOCK_METEOR_BROKEN.get());
                generateSphere7x7(world, rand, x, y, z, list1);
                return;
            }
            case 2: {
                // Medium ore-only meteorite
                List<Block> list2 = new ArrayList<>(this.getRandomOre(rand));
                int k = list2.size() / 2;
                for (int j = 0; j < k; j++)
                    list2.add(ModBlocks.BLOCK_METEOR_BROKEN.get());
                generateSphere5x5(world, rand, x, y, z, list2);
                return;
            }
            case 3: {
                // Small pure ore meteorite
                generateBox(world, rand, x, y, z, new ArrayList<>(this.getRandomOre(rand)));
                return;
            }
            case 4:
                // Bamboozle
                world.explode(null, x + 0.5, y + 0.5, z + 0.5, 15F, safe ? Level.ExplosionInteraction.NONE : Level.ExplosionInteraction.TNT);
                ExplosionLarge.spawnRubble(world, x, y, z, 25);
                return;
            case 5: {
                // Large treasure-only meteorite
                List<Block> list4 = new ArrayList<>();
                list4.add(ModBlocks.BLOCK_METEOR_TREASURE.get());
                list4.add(ModBlocks.BLOCK_METEOR_BROKEN.get());
                generateSphere7x7(world, rand, x, y, z, list4);
                return;
            }
            case 6: {
                // Medium treasure-only meteorite
                List<Block> list5 = new ArrayList<>();
                list5.add(ModBlocks.BLOCK_METEOR_TREASURE.get());
                list5.add(ModBlocks.BLOCK_METEOR_TREASURE.get());
                list5.add(ModBlocks.BLOCK_METEOR_BROKEN.get());
                generateSphere5x5(world, rand, x, y, z, list5);
                return;
            }
            case 7: {
                // Small pure treasure meteorite
                List<Block> list6 = new ArrayList<>();
                list6.add(ModBlocks.BLOCK_METEOR_TREASURE.get());
                generateBox(world, rand, x, y, z, list6);
                return;
            }
            case 8: {
                // Large nuclear meteorite
                List<Block> list7 = new ArrayList<>();
                list7.add(ModBlocks.BLOCK_METEOR_TREASURE.get());
                List<Block> list8 = new ArrayList<>();
                list8.add(ModBlocks.TOXIC_BLOCK.get());
                generateSphere7x7(world, rand, x, y, z, list7);
                generateSphere5x5(world, rand, x, y, z, list8);
                return;
            }
            case 9: {
                // Giant ore meteorite
                List<Block> list9 = new ArrayList<>();
                list9.add(ModBlocks.BLOCK_METEOR_BROKEN.get());
                generateSphere9x9(world, rand, x, y, z, list9);
                generateSphere7x7(world, rand, x, y, z, this.getRandomOre(rand));
                return;
            }
            case 10: {
                // Tainted Meteorite
                List<Block> list10 = new ArrayList<>();
                list10.add(ModBlocks.BLOCK_METEOR_BROKEN.get());
                generateSphere5x5(world, rand, x, y, z, list10);
                setBlock(world, x, y, z, ModBlocks.TAINT.get().defaultBlockState().setValue(BlockTaint.AGE, 9), 2);
                return;
            }
            case 12: {
                // Star Blaster
                world.explode(null, x + 0.5, y + 0.5, z + 0.5, 10F, safe ? Level.ExplosionInteraction.NONE : Level.ExplosionInteraction.TNT);
                ItemStack stack = new ItemStack(ModItems.GUN_B92.get());
                stack.setHoverName(Component.literal("§9Star Blaster§r"));
                ItemEntity blaster = new ItemEntity(world, x + 0.5, y + 0.5, z + 0.5, stack);
                world.addFreshEntity(blaster);
                return;
            }
            }

        switch (rand.nextInt(3)) {
        case 0:
            generateLarge(world, rand, x, y, z);
            break;
        case 1:
            generateMedium(world, rand, x, y, z);
            break;
        case 2:
            generateSmall(world, rand, x, y, z);
            break;
        }
    }

    private List<Block> hullList(int hull) {
        List<Block> hullL = new ArrayList<>();
        switch (hull) {
        case 0:
            hullL.add(ModBlocks.BLOCK_METEOR_MOLTEN.get());
            break;
        case 1:
            hullL.add(ModBlocks.BLOCK_METEOR_COBBLE.get());
            break;
        case 2:
            for (int i = 0; i < 99; i++)
                hullL.add(ModBlocks.BLOCK_METEOR_BROKEN.get());
            hullL.add(ModBlocks.BLOCK_METEOR_TREASURE.get());
            break;
        case 3:
            hullL.add(ModBlocks.BLOCK_METEOR_MOLTEN.get());
            hullL.add(ModBlocks.BLOCK_METEOR_BROKEN.get());
            break;
        }
        return hullL;
    }

    private List<Block> outerPaddingList(int outerPadding) {
        List<Block> opL = new ArrayList<>();
        switch (outerPadding) {
        case 0:
            opL.add(ModBlocks.BLOCK_METEOR_COBBLE.get());
            break;
        case 1:
            for (int i = 0; i < 99; i++)
                opL.add(ModBlocks.BLOCK_METEOR_BROKEN.get());
            opL.add(ModBlocks.BLOCK_METEOR_TREASURE.get());
            break;
        case 2:
            opL.add(ModBlocks.BLOCK_METEOR_COBBLE.get());
            opL.add(ModBlocks.BLOCK_METEOR_BROKEN.get());
            break;
        }
        return opL;
    }

    private List<Block> innerPaddingList(int innerPadding) {
        List<Block> ipL = new ArrayList<>();
        switch (innerPadding) {
        case 0:
            for (int i = 0; i < 99; i++)
                ipL.add(ModBlocks.BLOCK_METEOR_BROKEN.get());
            ipL.add(ModBlocks.BLOCK_METEOR_TREASURE.get());
            break;
        case 1:
            ipL.add(ModBlocks.BLOCK_METEOR_BROKEN.get());
            break;
        case 2:
            ipL.add(ModBlocks.BLOCK_METEOR_COBBLE.get());
            break;
        }
        return ipL;
    }

    private List<Block> coreList(RandomSource rand, int core) {
        List<Block> coreL = new ArrayList<>();
        switch (core) {
        case 0:
            coreL.add(ModBlocks.BLOCK_METEOR.get());
            break;
        case 1:
            coreL.add(ModBlocks.BLOCK_METEOR_TREASURE.get());
            break;
        case 2:
            coreL.addAll(this.getRandomOre(rand));
            break;
        }
        return coreL;
    }

    private List<Block> smallCoreList(int core) {
        List<Block> sCore = new ArrayList<>();
        switch (core) {
        case 0:
            sCore.add(ModBlocks.BLOCK_METEOR.get());
            break;
        case 1:
            sCore.add(ModBlocks.BLOCK_METEOR_TREASURE.get());
            break;
        case 2:
            sCore.add(ModBlocks.BLOCK_METEOR_TREASURE.get());
            sCore.add(ModBlocks.BLOCK_METEOR.get());
            break;
        }
        return sCore;
    }

    public void generateLarge(Level world, RandomSource rand, int x, int y, int z) {
        // 0 - Molten, 1 - Cobble, 2 - Broken, 3 - Mix
        int hull = rand.nextInt(4);
        // 0 - Cobble, 1 - Broken, 2 - Mix
        int outerPadding = 0;
        if (hull == 2)
            outerPadding = 1 + rand.nextInt(2);
        else if (hull == 3)
            outerPadding = 2;
        // 0 - Broken, 1 - Stone, 2 - Netherrack
        int innerPadding = rand.nextInt(hull == 0 ? 3 : 2);
        // 0 - Meteor, 1 - Treasure, 2 - Ore
        int core = rand.nextInt(2);
        if (innerPadding > 0)
            core = 2;

        List<Block> hullL = hullList(hull);
        List<Block> opL = outerPaddingList(outerPadding);
        List<Block> ipL = innerPaddingList(innerPadding);
        List<Block> coreL = coreList(rand, core);

        switch (rand.nextInt(5)) {
        case 0: genL1(world, rand, x, y, z, hullL, opL, ipL, coreL); break;
        case 1: genL2(world, rand, x, y, z, hullL, opL, ipL, coreL); break;
        case 2: genL3(world, rand, x, y, z, hullL, opL, ipL, coreL); break;
        case 3: genL4(world, rand, x, y, z, hullL, opL, ipL, coreL); break;
        case 4: genL5(world, rand, x, y, z, hullL, opL, ipL, coreL); break;
        }
    }

    public void generateMedium(Level world, RandomSource rand, int x, int y, int z) {
        int hull = rand.nextInt(4);
        int outerPadding = 0;
        if (hull == 2)
            outerPadding = 1 + rand.nextInt(2);
        else if (hull == 3)
            outerPadding = 2;
        int innerPadding = rand.nextInt(hull == 0 ? 3 : 2);
        int core = rand.nextInt(2);
        if (innerPadding > 0)
            core = 2;

        List<Block> hullL = hullList(hull);
        List<Block> opL = outerPaddingList(outerPadding);
        List<Block> ipL = innerPaddingList(innerPadding);
        List<Block> coreL = coreList(rand, core);
        List<Block> sCore = smallCoreList(core);

        switch (rand.nextInt(6)) {
        case 0: genM1(world, rand, x, y, z, hullL, opL, ipL, sCore); break;
        case 1: genM2(world, rand, x, y, z, hullL, opL, ipL, coreL); break;
        case 2: genM3(world, rand, x, y, z, hullL, opL, ipL, coreL); break;
        case 3: genM4(world, rand, x, y, z, hullL, opL, ipL, coreL); break;
        case 4: genM5(world, rand, x, y, z, hullL, opL, ipL, coreL); break;
        case 5: genM6(world, rand, x, y, z, hullL, opL, ipL, coreL); break;
        }
    }

    public void generateSmall(Level world, RandomSource rand, int x, int y, int z) {
        int hull = rand.nextInt(4);
        int core = rand.nextInt(3);

        List<Block> hullL = hullList(hull);
        List<Block> sCore = smallCoreList(core);

        generateBox(world, rand, x, y, z, hullL);
        setCore(world, rand, x, y, z, sCore);
    }

    private void setCore(Level world, RandomSource rand, int x, int y, int z, List<Block> core) {
        Block b = core.get(rand.nextInt(core.size()));
        setBlock(world, x, y, z, b.defaultBlockState(), 2);
    }

    public void genL1(Level world, RandomSource rand, int x, int y, int z, List<Block> hull, List<Block> op, List<Block> ip, List<Block> core) {
        generateSphere7x7(world, rand, x, y, z, hull);
        generateStar5x5(world, rand, x, y, z, op);
        generateStar3x3(world, rand, x, y, z, ip);
        setCore(world, rand, x, y, z, core);
    }

    public void genL2(Level world, RandomSource rand, int x, int y, int z, List<Block> hull, List<Block> op, List<Block> ip, List<Block> core) {
        generateSphere7x7(world, rand, x, y, z, hull);
        generateSphere5x5(world, rand, x, y, z, op);
        generateStar3x3(world, rand, x, y, z, ip);
        setCore(world, rand, x, y, z, core);
    }

    public void genL3(Level world, RandomSource rand, int x, int y, int z, List<Block> hull, List<Block> op, List<Block> ip, List<Block> core) {
        generateSphere7x7(world, rand, x, y, z, hull);
        generateSphere5x5(world, rand, x, y, z, op);
        generateBox(world, rand, x, y, z, ip);
        setCore(world, rand, x, y, z, core);
    }

    public void genL4(Level world, RandomSource rand, int x, int y, int z, List<Block> hull, List<Block> op, List<Block> ip, List<Block> core) {
        generateSphere7x7(world, rand, x, y, z, hull);
        generateSphere5x5(world, rand, x, y, z, op);
        generateBox(world, rand, x, y, z, ip);
        generateStar3x3(world, rand, x, y, z, this.getRandomOre(rand));
        setCore(world, rand, x, y, z, core);
    }

    public void genL5(Level world, RandomSource rand, int x, int y, int z, List<Block> hull, List<Block> op, List<Block> ip, List<Block> core) {
        generateSphere7x7(world, rand, x, y, z, hull);
        generateSphere5x5(world, rand, x, y, z, op);
        generateStar5x5(world, rand, x, y, z, ip);
        generateStar3x3(world, rand, x, y, z, this.getRandomOre(rand));
        setCore(world, rand, x, y, z, core);
    }

    public void genM1(Level world, RandomSource rand, int x, int y, int z, List<Block> hull, List<Block> op, List<Block> ip, List<Block> core) {
        generateSphere5x5(world, rand, x, y, z, hull);
        setCore(world, rand, x, y, z, core);
    }

    public void genM2(Level world, RandomSource rand, int x, int y, int z, List<Block> hull, List<Block> op, List<Block> ip, List<Block> core) {
        generateSphere5x5(world, rand, x, y, z, hull);
        generateStar3x3(world, rand, x, y, z, op);
        setCore(world, rand, x, y, z, core);
    }

    public void genM3(Level world, RandomSource rand, int x, int y, int z, List<Block> hull, List<Block> op, List<Block> ip, List<Block> core) {
        generateSphere5x5(world, rand, x, y, z, hull);
        generateBox(world, rand, x, y, z, op);
        setCore(world, rand, x, y, z, core);
    }

    public void genM4(Level world, RandomSource rand, int x, int y, int z, List<Block> hull, List<Block> op, List<Block> ip, List<Block> core) {
        generateSphere5x5(world, rand, x, y, z, hull);
        generateBox(world, rand, x, y, z, op);
        generateStar3x3(world, rand, x, y, z, ip);
        setCore(world, rand, x, y, z, core);
    }

    public void genM5(Level world, RandomSource rand, int x, int y, int z, List<Block> hull, List<Block> op, List<Block> ip, List<Block> core) {
        generateSphere5x5(world, rand, x, y, z, hull);
        generateBox(world, rand, x, y, z, ip);
        setCore(world, rand, x, y, z, core);
    }

    public void genM6(Level world, RandomSource rand, int x, int y, int z, List<Block> hull, List<Block> op, List<Block> ip, List<Block> core) {
        generateSphere5x5(world, rand, x, y, z, hull);
        generateBox(world, rand, x, y, z, ip);
        generateStar3x3(world, rand, x, y, z, this.getRandomOre(rand));
        setCore(world, rand, x, y, z, core);
    }

    /** Fuellt den Quader [a0,a1) x [b0,b1) x [c0,c1) um (x,y,z), je Block zufaellig aus der Liste. */
    private void fill(Level world, RandomSource rand, int x, int y, int z, List<Block> set, int a0, int a1, int b0, int b1, int c0, int c1) {
        for (int a = a0; a < a1; a++)
            for (int b = b0; b < b1; b++)
                for (int c = c0; c < c1; c++) {
                    Block block = set.get(rand.nextInt(set.size()));
                    setBlock(world, x + a, y + b, z + c, block.defaultBlockState(), 2);
                }
    }

    public void generateSphere7x7(Level world, RandomSource rand, int x, int y, int z, List<Block> set) {
        fill(world, rand, x, y, z, set, -3, 4, -1, 2, -1, 2);
        fill(world, rand, x, y, z, set, -1, 2, -3, 4, -1, 2);
        fill(world, rand, x, y, z, set, -1, 2, -1, 2, -3, 4);
        fill(world, rand, x, y, z, set, -2, 3, -2, 3, -1, 2);
        fill(world, rand, x, y, z, set, -1, 2, -2, 3, -2, 3);
        fill(world, rand, x, y, z, set, -2, 3, -1, 2, -2, 3);
    }

    public void generateSphere5x5(Level world, RandomSource rand, int x, int y, int z, List<Block> set) {
        fill(world, rand, x, y, z, set, -2, 3, -1, 2, -1, 2);
        fill(world, rand, x, y, z, set, -1, 2, -2, 3, -1, 2);
        fill(world, rand, x, y, z, set, -1, 2, -1, 2, -2, 3);
    }

    public void generateSphere9x9(Level world, RandomSource rand, int x, int y, int z, List<Block> set) {
        fill(world, rand, x, y, z, set, -4, 5, -1, 2, -1, 2);
        fill(world, rand, x, y, z, set, -1, 2, -4, 5, -1, 2);
        fill(world, rand, x, y, z, set, -1, 2, -1, 2, -4, 5);
        fill(world, rand, x, y, z, set, -1, 2, -3, 4, -3, 4);
        fill(world, rand, x, y, z, set, -3, 4, -1, 2, -3, 4);
        fill(world, rand, x, y, z, set, -3, 4, -3, 4, -1, 2);
        fill(world, rand, x, y, z, set, -3, 4, -2, 3, -2, 3);
        fill(world, rand, x, y, z, set, -2, 3, -3, 4, -2, 3);
        fill(world, rand, x, y, z, set, -2, 3, -2, 3, -3, 4);
    }

    public void generateBox(Level world, RandomSource rand, int x, int y, int z, List<Block> set) {
        fill(world, rand, x, y, z, set, -1, 2, -1, 2, -1, 2);
    }

    public void generateStar5x5(Level world, RandomSource rand, int x, int y, int z, List<Block> set) {
        fill(world, rand, x, y, z, set, -1, 2, -1, 2, -1, 2);
        single(world, rand, x + 2, y, z, set);
        single(world, rand, x - 2, y, z, set);
        single(world, rand, x, y + 2, z, set);
        single(world, rand, x, y - 2, z, set);
        single(world, rand, x, y, z + 2, set);
        single(world, rand, x, y, z - 2, set);
    }

    public void generateStar3x3(Level world, RandomSource rand, int x, int y, int z, List<Block> set) {
        single(world, rand, x, y, z, set);
        single(world, rand, x + 1, y, z, set);
        single(world, rand, x - 1, y, z, set);
        single(world, rand, x, y + 1, z, set);
        single(world, rand, x, y - 1, z, set);
        single(world, rand, x, y, z + 1, set);
        single(world, rand, x, y, z - 1, set);
    }

    private void single(Level world, RandomSource rand, int x, int y, int z, List<Block> set) {
        Block block = set.get(rand.nextInt(set.size()));
        setBlock(world, x, y, z, block.defaultBlockState(), 2);
    }

    public List<Block> getRandomOre(RandomSource rand) {
        List<Block> ores = new ArrayList<>();
        ores.add(ModBlocks.ORE_METEOR_IRON.get());
        ores.add(ModBlocks.ORE_METEOR_COPPER.get());
        ores.add(ModBlocks.ORE_METEOR_ALUMINIUM.get());
        ores.add(ModBlocks.ORE_METEOR_RAREEARTH.get());
        ores.add(ModBlocks.ORE_METEOR_COBALT.get());
        return ores;
    }

    private void setBlock(Level world, int x, int y, int z, BlockState b, int flag) {
        BlockPos pos = new BlockPos(x, y, z);
        BlockState target = world.getBlockState(pos);

        if (safeMode) {
            if (!target.canBeReplaced() && !replacables.contains(target.getBlock())) return;
        }

        float hardness = target.getDestroySpeed(world, pos);
        if (hardness != -1 && hardness < 10_000)
            world.setBlock(pos, b, flag);
    }

    public static Set<Block> replacables = new HashSet<>();

    public static void generateReplacables() {
        replacables.add(ModBlocks.BLOCK_METEOR.get());
        replacables.add(ModBlocks.BLOCK_METEOR_BROKEN.get());
        replacables.add(ModBlocks.BLOCK_METEOR_COBBLE.get());
        replacables.add(ModBlocks.BLOCK_METEOR_MOLTEN.get());
        replacables.add(ModBlocks.BLOCK_METEOR_TREASURE.get());
        replacables.add(ModBlocks.ORE_METEOR_IRON.get());
        replacables.add(ModBlocks.ORE_METEOR_COPPER.get());
        replacables.add(ModBlocks.ORE_METEOR_ALUMINIUM.get());
        replacables.add(ModBlocks.ORE_METEOR_RAREEARTH.get());
        replacables.add(ModBlocks.ORE_METEOR_COBALT.get());
    }
}
