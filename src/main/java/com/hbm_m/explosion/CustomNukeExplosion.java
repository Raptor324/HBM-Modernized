package com.hbm_m.explosion;

import java.util.HashMap;
import java.util.Map;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.effect.EntityCloudFleijaRainbow;
import com.hbm_m.entity.logic.EntityBalefireExplosion;
import com.hbm_m.entity.logic.EntityNukeExplosionMK3;
import com.hbm_m.entity.logic.EntityNukeExplosionMK5;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;
import com.hbm_m.particle.helper.NukeTorexCreator;
import com.hbm_m.util.WorldUtil;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code NukeCustom.explodeCustom} und {@code TileEntityNukeCustom.entries/registerBombItems}: Zutatentabelle der
 * Baukasten-Bombe (Additive und Multiplikatoren je Stufe), Obergrenzen und die Stufenwahl Euphemium > Schrabidium >
 * Antimaterie > Wasserstoff > Atom > grosses TNT (N2-artig ab 75) > kleines TNT.
 */
public final class CustomNukeExplosion {

    private CustomNukeExplosion() {}

    public static final int maxTnt = 150;
    public static final int maxNuke = 200;
    public static final int maxHydro = 350;
    public static final int maxAmat = 350;
    public static final int maxSchrab = 250;

    public enum EnumBombType {
        TNT("TNT"),
        NUKE("Nuclear"),
        HYDRO("Hydrogen"),
        AMAT("Antimatter"),
        DIRTY("Salted"),
        SCHRAB("Schrabidium"),
        EUPH("Anti Mass");

        final String name;

        EnumBombType(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum EnumEntryType {
        ADD,
        MULT
    }

    public record CustomNukeEntry(EnumBombType type, float value, EnumEntryType entry) {
        public CustomNukeEntry(EnumBombType type, float value) {
            this(type, value, EnumEntryType.ADD);
        }
    }

    private static Map<Item, CustomNukeEntry> entries;

    public static Map<Item, CustomNukeEntry> entries() {
        if (entries == null) {
            entries = new HashMap<>();
            registerBombItems();
        }
        return entries;
    }

    private static Item mat(ModMaterials mat, MaterialShape shape) {
        return ModMaterialItems.item(mat, shape);
    }

    private static Item reg(String name) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(com.hbm_m.lib.RefStrings.MODID, name));
    }

    private static void put(Item item, CustomNukeEntry entry) {
        if (item != null && item != Items.AIR) entries.put(item, entry);
    }

    private static void registerBombItems() {

        put(Items.GUNPOWDER, new CustomNukeEntry(EnumBombType.TNT, 0.8F));
        put(Items.TNT, new CustomNukeEntry(EnumBombType.TNT, 4F));
        put(ModBlocks.DET_CORD.get().asItem(), new CustomNukeEntry(EnumBombType.TNT, 1.5F));
        put(mat(ModMaterials.SEMTEX, MaterialShape.INGOT), new CustomNukeEntry(EnumBombType.TNT, 8F));
        put(ModBlocks.DET_CHARGE.get().asItem(), new CustomNukeEntry(EnumBombType.TNT, 15F));
        put(ModBlocks.BARREL_RED.get().asItem(), new CustomNukeEntry(EnumBombType.TNT, 2.5F));
        put(ModBlocks.BARREL_PINK.get().asItem(), new CustomNukeEntry(EnumBombType.TNT, 4F));
        put(reg("custom_tnt"), new CustomNukeEntry(EnumBombType.TNT, 10F));

        put(mat(ModMaterials.URANIUM233, MaterialShape.INGOT), new CustomNukeEntry(EnumBombType.NUKE, 15F));
        put(mat(ModMaterials.URANIUM235, MaterialShape.INGOT), new CustomNukeEntry(EnumBombType.NUKE, 15F));
        put(mat(ModMaterials.PLUTONIUM239, MaterialShape.INGOT), new CustomNukeEntry(EnumBombType.NUKE, 25F));
        put(mat(ModMaterials.PLUTONIUM241, MaterialShape.INGOT), new CustomNukeEntry(EnumBombType.NUKE, 25F));
        put(mat(ModMaterials.NEPTUNIUM, MaterialShape.INGOT), new CustomNukeEntry(EnumBombType.NUKE, 30F));
        put(mat(ModMaterials.URANIUM233, MaterialShape.NUGGET), new CustomNukeEntry(EnumBombType.NUKE, 1.5F));
        put(mat(ModMaterials.URANIUM235, MaterialShape.NUGGET), new CustomNukeEntry(EnumBombType.NUKE, 1.5F));
        put(mat(ModMaterials.PLUTONIUM239, MaterialShape.NUGGET), new CustomNukeEntry(EnumBombType.NUKE, 2.5F));
        put(mat(ModMaterials.PLUTONIUM241, MaterialShape.NUGGET), new CustomNukeEntry(EnumBombType.NUKE, 2.5F));
        put(mat(ModMaterials.NEPTUNIUM, MaterialShape.NUGGET), new CustomNukeEntry(EnumBombType.NUKE, 3.0F));
        put(mat(ModMaterials.NEPTUNIUM, MaterialShape.POWDER), new CustomNukeEntry(EnumBombType.NUKE, 30F));
        put(reg("custom_nuke"), new CustomNukeEntry(EnumBombType.NUKE, 30F));

        put(ModItems.CELL_DEUTERIUM.get(), new CustomNukeEntry(EnumBombType.HYDRO, 20F));
        put(reg("cell_tritium"), new CustomNukeEntry(EnumBombType.HYDRO, 30F));
        put(reg("lithium"), new CustomNukeEntry(EnumBombType.HYDRO, 20F));
        put(reg("custom_hydro"), new CustomNukeEntry(EnumBombType.HYDRO, 30F));

        put(reg("cell_antimatter"), new CustomNukeEntry(EnumBombType.AMAT, 5F));
        put(reg("custom_amat"), new CustomNukeEntry(EnumBombType.AMAT, 15F));
        put(reg("egg_balefire_shard"), new CustomNukeEntry(EnumBombType.AMAT, 15F));
        put(reg("egg_balefire"), new CustomNukeEntry(EnumBombType.AMAT, 150F));

        put(mat(ModMaterials.TUNGSTEN, MaterialShape.INGOT), new CustomNukeEntry(EnumBombType.DIRTY, 1F));
        put(reg("custom_dirty"), new CustomNukeEntry(EnumBombType.DIRTY, 10F));

        put(mat(ModMaterials.SCHRABIDIUM, MaterialShape.INGOT), new CustomNukeEntry(EnumBombType.SCHRAB, 5F));
        put(reg("block_schrabidium"), new CustomNukeEntry(EnumBombType.SCHRAB, 50F));
        put(mat(ModMaterials.SCHRABIDIUM, MaterialShape.NUGGET), new CustomNukeEntry(EnumBombType.SCHRAB, 0.5F));
        put(mat(ModMaterials.SCHRABIDIUM, MaterialShape.POWDER), new CustomNukeEntry(EnumBombType.SCHRAB, 5F));
        put(ModItems.CELL_SAS3.get(), new CustomNukeEntry(EnumBombType.SCHRAB, 7.5F));
        put(reg("cell_anti_schrabidium"), new CustomNukeEntry(EnumBombType.SCHRAB, 15F));
        put(reg("custom_schrab"), new CustomNukeEntry(EnumBombType.SCHRAB, 15F));

        put(mat(ModMaterials.EUPHEMIUM, MaterialShape.NUGGET), new CustomNukeEntry(EnumBombType.EUPH, 1F));
        put(mat(ModMaterials.EUPHEMIUM, MaterialShape.INGOT), new CustomNukeEntry(EnumBombType.EUPH, 1F));

        put(Items.REDSTONE, new CustomNukeEntry(EnumBombType.TNT, 1.05F, EnumEntryType.MULT));
        put(Items.REDSTONE_BLOCK, new CustomNukeEntry(EnumBombType.TNT, 1.5F, EnumEntryType.MULT));

        put(mat(ModMaterials.URANIUM, MaterialShape.INGOT), new CustomNukeEntry(EnumBombType.NUKE, 1.05F, EnumEntryType.MULT));
        put(mat(ModMaterials.PLUTONIUM, MaterialShape.INGOT), new CustomNukeEntry(EnumBombType.NUKE, 1.15F, EnumEntryType.MULT));
        put(mat(ModMaterials.URANIUM238, MaterialShape.INGOT), new CustomNukeEntry(EnumBombType.NUKE, 1.1F, EnumEntryType.MULT));
        put(mat(ModMaterials.PLUTONIUM238, MaterialShape.INGOT), new CustomNukeEntry(EnumBombType.NUKE, 1.15F, EnumEntryType.MULT));
        put(mat(ModMaterials.URANIUM, MaterialShape.NUGGET), new CustomNukeEntry(EnumBombType.NUKE, 1.005F, EnumEntryType.MULT));
        put(mat(ModMaterials.PLUTONIUM, MaterialShape.NUGGET), new CustomNukeEntry(EnumBombType.NUKE, 1.15F, EnumEntryType.MULT));
        put(mat(ModMaterials.URANIUM238, MaterialShape.NUGGET), new CustomNukeEntry(EnumBombType.NUKE, 1.01F, EnumEntryType.MULT));
        put(mat(ModMaterials.PLUTONIUM238, MaterialShape.NUGGET), new CustomNukeEntry(EnumBombType.NUKE, 1.015F, EnumEntryType.MULT));
        put(mat(ModMaterials.URANIUM, MaterialShape.POWDER), new CustomNukeEntry(EnumBombType.NUKE, 1.05F, EnumEntryType.MULT));
        put(mat(ModMaterials.PLUTONIUM, MaterialShape.POWDER), new CustomNukeEntry(EnumBombType.NUKE, 1.15F, EnumEntryType.MULT));

        put(mat(ModMaterials.PLUTONIUM240, MaterialShape.INGOT), new CustomNukeEntry(EnumBombType.DIRTY, 1.05F, EnumEntryType.MULT));
        put(reg("nuclear_waste"), new CustomNukeEntry(EnumBombType.DIRTY, 1.025F, EnumEntryType.MULT));
        put(reg("block_waste"), new CustomNukeEntry(EnumBombType.DIRTY, 1.25F, EnumEntryType.MULT));
        put(ModBlocks.BARREL_YELLOW.get().asItem(), new CustomNukeEntry(EnumBombType.DIRTY, 1.2F, EnumEntryType.MULT));
    }

    /** Die sieben Stufenwerte nach {@code TileEntityNukeCustom.updateEntity}. */
    public static final class Values {
        public float tnt, nuke, hydro, amat, dirty, schrab, euph;

        public float getNukeAdj() {
            if (nuke == 0) return 0;
            return Math.min(nuke + tnt / 2, maxNuke);
        }

        public float getHydroAdj() {
            if (hydro == 0) return 0;
            return Math.min(hydro + nuke / 2 + tnt / 4, maxHydro);
        }

        public float getAmatAdj() {
            if (amat == 0) return 0;
            return Math.min(amat + hydro / 2 + nuke / 4 + tnt / 8, maxAmat);
        }

        public float getSchrabAdj() {
            if (schrab == 0) return 0;
            return Math.min(schrab + amat / 2 + hydro / 4 + nuke / 8 + tnt / 16, maxSchrab);
        }
    }

    /** 1:1 {@code updateEntity}: Summen und Multiplikatoren, dann die Mindeststufen (TNT 16 fuer Atom usw.). */
    public static Values compute(Iterable<ItemStack> slots) {
        float tnt = 0F, tntMod = 1F;
        float nuke = 0F, nukeMod = 1F;
        float hydro = 0F, hydroMod = 1F;
        float amat = 0F, amatMod = 1F;
        float dirty = 0F, dirtyMod = 1F;
        float schrab = 0F, schrabMod = 1F;
        float euph = 0F;

        for (ItemStack stack : slots) {

            if (stack.isEmpty())
                continue;

            CustomNukeEntry ent = entries().get(stack.getItem());

            if (ent == null)
                continue;

            if (ent.entry() == EnumEntryType.ADD) {

                switch (ent.type()) {
                    case TNT -> tnt += ent.value() * stack.getCount();
                    case NUKE -> nuke += ent.value() * stack.getCount();
                    case HYDRO -> hydro += ent.value() * stack.getCount();
                    case AMAT -> amat += ent.value() * stack.getCount();
                    case DIRTY -> dirty += ent.value() * stack.getCount();
                    case SCHRAB -> schrab += ent.value() * stack.getCount();
                    case EUPH -> euph += ent.value() * stack.getCount();
                }

            } else if (ent.entry() == EnumEntryType.MULT) {

                switch (ent.type()) {
                    case TNT -> tntMod *= ent.value() * stack.getCount();
                    case NUKE -> nukeMod *= ent.value() * stack.getCount();
                    case HYDRO -> hydroMod *= ent.value() * stack.getCount();
                    case AMAT -> amatMod *= ent.value() * stack.getCount();
                    case DIRTY -> dirtyMod *= ent.value() * stack.getCount();
                    case SCHRAB -> schrabMod *= ent.value() * stack.getCount();
                    default -> { }
                }
            }
        }

        tnt *= tntMod;
        nuke *= nukeMod;
        hydro *= hydroMod;
        amat *= amatMod;
        dirty *= dirtyMod;
        schrab *= schrabMod;

        if (tnt < 16) nuke = 0;
        if (nuke < 100) hydro = 0;
        if (nuke < 50) amat = 0;
        if (nuke < 50) schrab = 0;
        if (schrab == 0) euph = 0;

        Values v = new Values();
        v.tnt = tnt;
        v.nuke = nuke;
        v.hydro = hydro;
        v.amat = amat;
        v.dirty = dirty;
        v.schrab = schrab;
        v.euph = euph;
        return v;
    }

    public static void explodeCustom(Level worldObj, double xCoord, double yCoord, double zCoord, Values v) {
        explodeCustom(worldObj, xCoord, yCoord, zCoord, v.tnt, v.nuke, v.hydro, v.amat, v.dirty, v.schrab, v.euph);
    }

    // genuinely some of the worst fucking code i've ever written
    public static void explodeCustom(Level worldObj, double xCoord, double yCoord, double zCoord, float tnt, float nuke, float hydro, float amat, float dirty, float schrab, float euph) {

        if (worldObj.isClientSide) return;

        dirty = Math.min(dirty, 100);

        /// EUPHEMIUM ///
        if (euph > 0) {

            EntityNukeExplosionMK3 ex = new EntityNukeExplosionMK3(ModEntities.NUKE_MK3.get(), worldObj);
            ex.setPos(xCoord, yCoord, zCoord);
            ex.destructionRange = 150;
            ex.speed = ModClothConfig.get().blastSpeed;
            ex.coefficient = 1.0F;
            WorldUtil.loadAndSpawnEntityInWorld(ex);

            worldObj.playSound(null, xCoord, yCoord, zCoord, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 100000.0F, 1.0F);

            EntityCloudFleijaRainbow cloud = new EntityCloudFleijaRainbow(ModEntities.CLOUD_FLEIJA_RAINBOW.get(), worldObj, 50);
            cloud.setPos(xCoord, yCoord, zCoord);
            worldObj.addFreshEntity(cloud);

        // SCHRABIDIUM ///
        } else if (schrab > 0) {

            schrab += amat / 2 + hydro / 4 + nuke / 8 + tnt / 16;
            schrab = Math.min(schrab, maxSchrab);

            FleijaExplosionAPI.start(worldObj, xCoord + 0.5, yCoord + 0.5, zCoord + 0.5, (int) schrab);

        /// ANTIMATTER ///
        } else if (amat > 0) {

            amat += hydro / 2 + nuke / 4 + tnt / 8;
            amat = Math.min(amat, maxAmat);

            // Port-EntityBalefireExplosion setzt den Balefire-Torex (y + 5, amat) selbst beim ersten Tick
            WorldUtil.loadAndSpawnEntityInWorld(EntityBalefireExplosion.statFac(worldObj, xCoord + 0.5, yCoord + 0.5, zCoord + 0.5, (int) amat));

        /// HYDROGEN ///
        } else if (hydro > 0) {

            hydro += nuke / 2 + tnt / 4;
            hydro = Math.min(hydro, maxHydro);
            dirty *= 0.25F;

            EntityNukeExplosionMK5 mk5 = EntityNukeExplosionMK5.start(worldObj, (int) hydro, xCoord + 0.5, yCoord + 0.5, zCoord + 0.5);
            mk5.setFalloutAdd((int) dirty);
            NukeTorexCreator.statFacStandard(worldObj, xCoord + 0.5, yCoord + 5, zCoord + 0.5, hydro);

        /// NUCLEAR ///
        } else if (nuke > 0) {

            nuke += tnt / 2;
            nuke = Math.min(nuke, maxNuke);

            EntityNukeExplosionMK5 mk5 = EntityNukeExplosionMK5.start(worldObj, (int) nuke, xCoord + 0.5, yCoord + 5, zCoord + 0.5);
            mk5.setFalloutAdd((int) dirty);
            NukeTorexCreator.statFacStandard(worldObj, xCoord + 0.5, yCoord + 5, zCoord + 0.5, nuke);

        /// NON-NUCLEAR ///
        } else if (tnt >= 75) {

            tnt = Math.min(tnt, maxTnt);

            EntityNukeExplosionMK5 mk5 = EntityNukeExplosionMK5.start(worldObj, (int) tnt, xCoord + 0.5, yCoord + 0.5, zCoord + 0.5);
            mk5.fallout = false;
            NukeTorexCreator.statFacStandard(worldObj, xCoord + 0.5, yCoord + 5, zCoord + 0.5, tnt);
        } else if (tnt > 0) {

            ExplosionLarge.explode(worldObj, xCoord + 0.5, yCoord + 0.5, zCoord + 0.5, tnt, true, true, true);
        }
    }
}
