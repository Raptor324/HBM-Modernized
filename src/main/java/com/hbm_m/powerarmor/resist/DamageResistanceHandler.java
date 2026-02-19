package com.hbm_m.powerarmor.resist;

import java.util.HashMap;

import com.hbm_m.item.ModItems;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.common.Mod;

/**
 * Damage Resistance Handler for HBM Modernized
 * Ported from original HBM 1.7.10 with full DT+DR system support
 * 
 * ОСОБЕННОСТИ:
 * - Силовая броня регистрируется ТОЛЬКО через registerSet()
 * - Защита применяется ТОЛЬКО при полном сете
 * - Система отскакивания стрел (deflectArrows)
 * 
 * @author hbm (ported to 1.20.1)
 */
@Mod.EventBusSubscriber(modid = com.hbm_m.main.MainRegistry.MOD_ID)
public class DamageResistanceHandler {
    
    /** Currently cached DT reduction */
    // public static float currentPDT = 0F;
    // /** Currently cached armor piercing % */
    // public static float currentPDR = 0F;

    public static final String CATEGORY_EXPLOSION = "EXPL";
    public static final String CATEGORY_FIRE = "FIRE";
    public static final String CATEGORY_PHYSICAL = "PHYS";
    public static final String CATEGORY_ENERGY = "EN";

    // Damage classes for energy weapons
    public static enum DamageClass {
        PHYSICAL,
        FIRE,
        EXPLOSIVE,
        ELECTRIC,
        LASER,
        MICROWAVE,
        SUBATOMIC,
        OTHER
    }

    public static HashMap<Item, ResistanceStats> itemStats = new HashMap<>();
    public static HashMap<ArmorSet, ResistanceStats> setStats = new HashMap<>();
    public static HashMap<Class<? extends Entity>, ResistanceStats> entityStats = new HashMap<>();

    /**
     * Check if entity should deflect projectiles (arrows, tridents, etc.)
     * Called BEFORE damage is applied
     */
    public static boolean shouldDeflectProjectile(LivingEntity entity, DamageSource damage) {
        // Проверяем только снаряды
        if (!damage.is(DamageTypes.ARROW) && 
            !damage.is(DamageTypes.TRIDENT) &&
            !damage.is(DamageTypes.THROWN)) {
            return false;
        }
        
        // Проверяем полный сет
        ArmorSet wornSet = getWornArmorSet(entity);
        ResistanceStats setResistance = setStats.get(wornSet);
        if (setResistance != null && setResistance.deflectsArrows) {
            return true;
        }
        
        // Проверяем отдельные предметы (если есть флаг на одном - работает на всего игрока)
        for (int i = 0; i < 4; i++) {
            ItemStack armor = entity.getItemBySlot(getEquipmentSlot(i));
            if (armor.isEmpty()) continue;
            
            ResistanceStats stats = itemStats.get(armor.getItem());
            if (stats != null && stats.deflectsArrows) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Calculate damage with full DT+DR system
     * @param pierceDT Armor piercing value that reduces DT (e.g., 3.0 = ignores 3 points of DT)
     * @param pierceDR Armor piercing percentage that reduces DR effectiveness (0.0-1.0)
     */
    public static float calculateDamage(LivingEntity entity, DamageSource damage, 
                                    float amount, float pierceDT, float pierceDR) {
        if (damage.is(DamageTypes.GENERIC_KILL)) return amount;
        
        // Получаем базовые значения защиты
        float[] vals = getDTDR(entity, damage, amount, pierceDT, pierceDR);
        float dt = vals[0];
        float dr = vals[1];
        
        // Применяем бронепробиваемость
        dt = Math.max(0F, dt - pierceDT);
        if (dt >= amount) return 0F;
        
        amount -= dt;
        
        // pierceDR: 0.0 = нет пробития, 1.0 = полное игнорирование DR
        dr *= Math.max(0F, 1F - pierceDR);
        
        return amount * (1F - dr);
    }

    /**
     * Get DT (Damage Threshold) and DR (Damage Resistance) for entity
     */
    public static float[] getDTDR(LivingEntity entity, DamageSource damage, float amount, float pierceDT, float pierceDR) {
        float dt = 0;
        float dr = 0;
        
        // Check for entity-specific resistance
        if (entity instanceof IResistanceProvider resistanceProvider) {
            float[] res = resistanceProvider.getCurrentDTDR(damage, amount, pierceDT, pierceDR);
            dt += res[0];
            dr += res[1];
        }
        
        // SET HANDLING - Check for full armor sets
        ArmorSet wornSet = getWornArmorSet(entity);
        ResistanceStats setResistance = setStats.get(wornSet);
        if (setResistance != null) {
            Resistance res = setResistance.getResistance(damage);
            if (res != null) {
                dt += res.threshold;
                dr += res.resistance;
            }
        }
        
        // INDIVIDUAL ARMOR HANDLING - для отдельной одежды (куртки и т.п.)
        for (int i = 0; i < 4; i++) {
            ItemStack armor = entity.getItemBySlot(getEquipmentSlot(i)).copy();
            if (armor.isEmpty()) continue;
            
            ResistanceStats stats = itemStats.get(armor.getItem());
            if (stats == null) continue;
            
            Resistance res = stats.getResistance(damage);
            if (res == null) continue;
            
            dt += res.threshold;
            dr += res.resistance;
        }
        
        // ENTITY CLASS HANDLING
        ResistanceStats innateResistance = entityStats.get(entity.getClass());
        if (innateResistance != null) {
            Resistance res = innateResistance.getResistance(damage);
            if (res != null) {
                dt += res.threshold;
                dr += res.resistance;
            }
        }
        
        return new float[]{dt, dr};
    }

    /**
     * Convert damage source to category string
     */
    public static String typeToCategory(DamageSource source) {
        // Vanilla damage types
        if (source.is(DamageTypes.EXPLOSION) || source.is(DamageTypes.PLAYER_EXPLOSION)) {
            return CATEGORY_EXPLOSION;
        }
        
        if (source.is(DamageTypes.IN_FIRE) || source.is(DamageTypes.LAVA) || source.is(DamageTypes.ON_FIRE) ||
                source.is(DamageTypes.HOT_FLOOR) || source.is(DamageTypes.FIREBALL)) {
            return CATEGORY_FIRE;
        }
        
        if (source.is(DamageTypes.ARROW) || source.is(DamageTypes.TRIDENT) ||
                source.is(DamageTypes.THROWN)) {
            return CATEGORY_PHYSICAL;
        }
        
        // Custom HBM damage types
        String damageType = source.getMsgId();
        if (damageType.equals(DamageClass.LASER.name()) ||
                damageType.equals(DamageClass.MICROWAVE.name()) ||
                damageType.equals(DamageClass.SUBATOMIC.name()) ||
                damageType.equals(DamageClass.ELECTRIC.name())) {
            return CATEGORY_ENERGY;
        }
        
        // Default to physical for entity attacks
        if (source.getEntity() != null && source.getDirectEntity() != null) {
            return CATEGORY_PHYSICAL;
        }
        
        return damageType;
    }

    /**
     * Setup pierce values for current damage calculation
     */
    // public static void setup(float dt, float dr) {
    //     currentPDT = dt;
    //     currentPDR = dr;
    // }

    /**
     * Reset pierce values
     */
    // public static void reset() {
    //     currentPDT = 0;
    //     currentPDR = 0;
    // }

    /**
     * Register armor set with resistance stats
     */
    public static void registerSet(Item helmet, Item chestplate, Item leggings, Item boots, ResistanceStats stats) {
        ArmorSet set = new ArmorSet(helmet, chestplate, leggings, boots);
        setStats.put(set, stats);
    }

    /**
     * Register individual armor piece (для курток и одиночной одежды)
     */
    public static void registerItem(Item item, ResistanceStats stats) {
        itemStats.put(item, stats);
    }

    /**
     * Register entity resistance
     */
    public static void registerEntity(Class<? extends Entity> entityClass, ResistanceStats stats) {
        entityStats.put(entityClass, stats);
    }

    // Helper method to get equipment slot from index
    private static net.minecraft.world.entity.EquipmentSlot getEquipmentSlot(int index) {
        return switch (index) {
            case 0 -> net.minecraft.world.entity.EquipmentSlot.FEET;
            case 1 -> net.minecraft.world.entity.EquipmentSlot.LEGS;
            case 2 -> net.minecraft.world.entity.EquipmentSlot.CHEST;
            case 3 -> net.minecraft.world.entity.EquipmentSlot.HEAD;
            default -> net.minecraft.world.entity.EquipmentSlot.CHEST;
        };
    }

    // Helper method to get worn armor set
    private static ArmorSet getWornArmorSet(LivingEntity entity) {
        return new ArmorSet(
                entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).getItem(),
                entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).getItem(),
                entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS).getItem(),
                entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET).getItem()
        );
    }

    /**
     * Armor set representation
     */
    public static class ArmorSet {
        public final Item helmet;
        public final Item chestplate;
        public final Item leggings;
        public final Item boots;

        public ArmorSet(Item helmet, Item chestplate, Item leggings, Item boots) {
            this.helmet = helmet;
            this.chestplate = chestplate;
            this.leggings = leggings;
            this.boots = boots;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ArmorSet other)) return false;
            return ItemStack.isSameItem(new ItemStack(this.helmet), new ItemStack(other.helmet)) &&
                   ItemStack.isSameItem(new ItemStack(this.chestplate), new ItemStack(other.chestplate)) &&
                   ItemStack.isSameItem(new ItemStack(this.leggings), new ItemStack(other.leggings)) &&
                   ItemStack.isSameItem(new ItemStack(this.boots), new ItemStack(other.boots));
        }

        @Override
        public int hashCode() {
            int result = helmet != null ? helmet.hashCode() : 0;
            result = 31 * result + (chestplate != null ? chestplate.hashCode() : 0);
            result = 31 * result + (leggings != null ? leggings.hashCode() : 0);
            result = 31 * result + (boots != null ? boots.hashCode() : 0);
            return result;
        }
    }

    /**
     * Resistance statistics for armor/entity
     */
    public static class ResistanceStats {
        public HashMap<String, Resistance> exactResistances = new HashMap<>();
        public HashMap<String, Resistance> categoryResistances = new HashMap<>();
        public Resistance otherResistance;
        
        /** 
         * NEW: Flag for deflecting arrows/projectiles
         * If true, arrows bounce off without dealing damage
         * Works for both full sets and individual items
         */
        public boolean deflectsArrows = false;

        public Resistance getResistance(DamageSource source) {
            // Check exact damage type match first
            Resistance exact = exactResistances.get(source.getMsgId());
            if (exact != null) return exact;
            
            // Then check category
            Resistance category = categoryResistances.get(typeToCategory(source));
            if (category != null) return category;
            
            // Finally check other (if not unblockable)
            return source.is(DamageTypes.GENERIC_KILL) ? null : otherResistance;
        }

        public ResistanceStats addExact(String type, float threshold, float resistance) {
            exactResistances.put(type, new Resistance(threshold, resistance));
            return this;
        }

        public ResistanceStats addCategory(String type, float threshold, float resistance) {
            categoryResistances.put(type, new Resistance(threshold, resistance));
            return this;
        }

        public ResistanceStats setOther(float threshold, float resistance) {
            otherResistance = new Resistance(threshold, resistance);
            return this;
        }
        
        /**
         * Set whether this armor deflects arrows/projectiles
         * @param deflects true to enable arrow deflection
         */
        public ResistanceStats setDeflectArrows(boolean deflects) {
            this.deflectsArrows = deflects;
            return this;
        }
    }

    /**
     * Individual resistance entry
     */
    public static class Resistance {
        public float threshold;  // DT - Damage Threshold
        public float resistance; // DR - Damage Resistance (0-1)

        public Resistance(float threshold, float resistance) {
            this.threshold = threshold;
            this.resistance = Math.max(0F, Math.min(1F, resistance));
        }
    }

    /**
     * Initialize armor stats after items are registered
     * Call this from FMLCommonSetupEvent or similar
     */
    public static void initArmorStats() {
        // ========== T-51 POWER ARMOR ==========
        registerSet(ModItems.T51_HELMET.get(), ModItems.T51_CHESTPLATE.get(),
                ModItems.T51_LEGGINGS.get(), ModItems.T51_BOOTS.get(),
                new ResistanceStats()
                        .addCategory(CATEGORY_PHYSICAL, 2F, 0.15F)
                        .addCategory(CATEGORY_FIRE, 0.5F, 0.35F)
                        .addCategory(CATEGORY_EXPLOSION, 5F, 0.25F)
                        .addExact("fall", 0F, 1F)
                        .setOther(0F, 0.1F)
                        .setDeflectArrows(true));  // ← T-51 отражает стрелы
        
        // ========== AJR POWER ARMOR ==========
        registerSet(ModItems.AJR_HELMET.get(), ModItems.AJR_CHESTPLATE.get(),
                ModItems.AJR_LEGGINGS.get(), ModItems.AJR_BOOTS.get(),
                new ResistanceStats()
                        .addCategory(CATEGORY_PHYSICAL, 4F, 0.15F)
                        .addCategory(CATEGORY_FIRE, 0.5F, 0.35F)
                        .addCategory(CATEGORY_EXPLOSION, 7.5F, 0.25F)
                        .addExact("fall", 0F, 1F)
                        .setOther(0F, 0.15F)
                        .setDeflectArrows(true));  // ← AJR тоже отражает стрелы

        // ========== BISMUTH POWER ARMOR ==========
        // Ported from 1.7.10: CATEGORY_PHYSICAL 2/0.15, CATEGORY_FIRE 5/0.5, CATEGORY_EXPLOSION 5/0.25,
        // fall 0/1, other 2/0.25
        registerSet(ModItems.BISMUTH_HELMET.get(), ModItems.BISMUTH_CHESTPLATE.get(),
                ModItems.BISMUTH_LEGGINGS.get(), ModItems.BISMUTH_BOOTS.get(),
                new ResistanceStats()
                        .addCategory(CATEGORY_PHYSICAL, 2F, 0.15F)
                        .addCategory(CATEGORY_FIRE, 5F, 0.5F)
                        .addCategory(CATEGORY_EXPLOSION, 5F, 0.25F)
                        .addExact("fall", 0F, 1F)
                        .setOther(2F, 0.25F));
        
        /*
        ПРИМЕРЫ ИСПОЛЬЗОВАНИЯ deflectArrows:
        
        1. Для полного сета (работает только с полным сетом):
           registerSet(..., new ResistanceStats()
               .addCategory(...)
               .setDeflectArrows(true));
        
        2. Для отдельного предмета (работает даже если надет только он):
           registerItem(ModItems.SOME_HELMET.get(), 
               new ResistanceStats()
                   .setDeflectArrows(true));
        
        3. Можно комбинировать с обычными резистами:
           new ResistanceStats()
               .addCategory(CATEGORY_PHYSICAL, 5F, 0.5F)
               .setDeflectArrows(true)  // И защита, и отскок стрел
        */
    }
}