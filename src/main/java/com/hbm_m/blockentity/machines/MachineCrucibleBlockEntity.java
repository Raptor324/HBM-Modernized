package com.hbm_m.blockentity.machines;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.blockentity.BaseHbmBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.material.MaterialStack;
import com.hbm_m.inventory.material.MaterialType;
import com.hbm_m.platform.ModItemStackHandler;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.recipe.MoltenAlloyRecipe;
import com.hbm_m.util.CrucibleUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;

/**
 * Порт {@code TileEntityCrucible} (1.7.10): тигель с двумя списками расплава —
 * {@code recipeStack} (материалы под выбранный рецепт) и {@code wasteStack}
 * (посторонние материалы). Нагрев затягивается от {@code IHeatSource} снизу
 * (диффузия 0.25), плавка требует не меньше половины теплоёмкости, скорость
 * плавки растёт с перегревом. Рецепт выбирается в GUI и пересылается пакетом
 * (аналог {@code receiveControl("index"/"selection")}). Налив: тыльная сторона —
 * шлак, лицевая — материалы рецепта, порциями 3 самородка за тик в
 * {@code ICrucibleAcceptor} вниз по потоку.
 *
 * <p>Ёмкости в mB (оригинал: BLOCK.q(16) = 144 слитка на список):
 * {@code 144 × 1000 mB}. Порция налива: 3 самородка = 333 mB.
 */
public class MachineCrucibleBlockEntity extends BaseHbmBlockEntity implements com.hbm_m.api.block.ICrucibleAcceptor, com.hbm_m.interfaces.IMetalCopiable {

    /** Слоты плавильного буфера (в оригинале слоты 1..9 при 10-слотовом инвентаре; слот 0 никогда не использовался). */
    public static final int SMELT_SLOTS = 9;

    public static final int RECIPE_CAPACITY_MB = 144_000;
    public static final int WASTE_CAPACITY_MB  = 144_000;
    public static final int PROCESS_TIME       = 20_000;
    public static final double DIFFUSION       = 0.25D;
    public static final int MAX_HEAT           = 100_000;

    /** Минимальная порция налива — 3 самородка (оригинал NUGGET.q(3)). */
    public static final int MIN_POUR_MB = MaterialStack.MB_PER_NUGGET * 3;

    private static final int POUR_RANGE = 6;

    private final ModItemStackHandler itemHandler = new ModItemStackHandler(SMELT_SLOTS) {
        @Override
        public int getSlotLimit(int slot) {
            return 1; // prevents clogging — как в оригинале getInventoryStackLimit()
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    public int heat = 0;
    public int progress = 0;

    /** Выбранный рецепт — строка-идентификатор (оригинал: "null" = не выбран). */
    public String recipe = "null";

    public final List<MaterialStack> recipeStack = new ArrayList<>();
    public final List<MaterialStack> wasteStack = new ArrayList<>();

    private float fillLevel = 0f;
    private int fillColor = 0xFFC18336;

    protected final ContainerData data = new ContainerData() {
        @Override public int get(int i) { return switch (i) {
            case 0 -> progress; case 1 -> PROCESS_TIME;
            case 2 -> heat;     case 3 -> MAX_HEAT;
            case 4 -> totalAmount(recipeStack);
            case 5 -> RECIPE_CAPACITY_MB;
            case 6 -> totalAmount(wasteStack);
            case 7 -> WASTE_CAPACITY_MB;
            default -> 0; }; }
        @Override public void set(int i, int v) { }
        @Override public int getCount() { return 8; }
    };

    public MachineCrucibleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRUCIBLE_BE.get(), pos, state);
    }

    public ContainerData getData() { return data; }
    public ModItemStackHandler getModItemStackHandler() { return itemHandler; }
    public float getFillLevel() { return fillLevel; }
    public int getFillColor() { return fillColor; }

    public int totalRecipeAmount() { return totalAmount(recipeStack); }
    public int totalWasteAmount()  { return totalAmount(wasteStack); }
    public int getRecipeCap() { return RECIPE_CAPACITY_MB; }
    public int getWasteCap()  { return WASTE_CAPACITY_MB; }

    /** Наибольший стакан расплава — для цвета заливки. */
    public @Nullable MaterialStack getMaterialStack() {
        MaterialStack biggest = null;
        for (MaterialStack ms : recipeStack) if (biggest == null || ms.amount > biggest.amount) biggest = ms;
        for (MaterialStack ms : wasteStack) if (biggest == null || ms.amount > biggest.amount) biggest = ms;
        return biggest;
    }

    public List<MaterialStack> getAllStacks() {
        List<MaterialStack> all = new ArrayList<>(recipeStack.size() + wasteStack.size());
        all.addAll(recipeStack);
        all.addAll(wasteStack);
        return all;
    }

    public void clearStacks() {
        recipeStack.clear();
        wasteStack.clear();
    }

    /** Лопата: высыпать всё содержимое шлаком игроку (порт onBlockActivated-ветки). */
    public void dumpToPlayer(Player player, Vec3 hit) {
        for (MaterialStack stack : getAllStacks()) {
            ItemStack scrap = CrucibleUtil.createScrap(stack);
            if (!scrap.isEmpty()) {
                if (!player.getInventory().add(scrap)) {
                    if (level != null) {
                        player.drop(scrap, false, false);
                    }
                }
            }
        }
        clearStacks();
        setChanged();
        syncToClient();
    }

    // ═══════════════════════════════ Сервер ═══════════════════════════════

    public static void serverTick(Level level, BlockPos pos, BlockState state, MachineCrucibleBlockEntity be) {
        int oldHeat = be.heat, oldProgress = be.progress;
        int oldTotal = be.totalAmount(be.recipeStack) + be.totalAmount(be.wasteStack);

        be.tryPullHeat();

        /* collect items — раз в 5 тиков, из чаши над ядром */
        if (level.getGameTime() % 5 == 0) {
            List<ItemEntity> list = level.getEntities(EntityType.ITEM,
                    new AABB(pos.getX() - 0.5, pos.getY() + 0.5, pos.getZ() - 0.5,
                             pos.getX() + 1.5, pos.getY() + 1.0, pos.getZ() + 1.5),
                    EntitySelector.ENTITY_STILL_ALIVE);
            for (ItemEntity item : list) {
                if (item.isRemoved()) continue;
                ItemStack stack = item.getItem();
                if (!be.isItemSmeltable(stack)) continue;

                for (int i = 0; i < be.itemHandler.getSlots(); i++) {
                    if (be.itemHandler.getStackInSlot(i).isEmpty()) {
                        if (stack.getCount() == 1) {
                            be.itemHandler.setStackInSlot(i, stack.copy());
                            item.discard();
                            break;
                        } else {
                            be.itemHandler.setStackInSlot(i, stack.split(1));
                        }
                        be.setChanged();
                        break;
                    }
                }
            }
        }

        /* урон существам в расплаве */
        int totalCap = RECIPE_CAPACITY_MB + WASTE_CAPACITY_MB;
        int totalMass = be.totalAmount(be.recipeStack) + be.totalAmount(be.wasteStack);
        double meltLevel = ((double) totalMass / (double) totalCap) * 0.875D;

        List<LivingEntity> living = level.getEntitiesOfClass(LivingEntity.class,
                new AABB(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                         pos.getX() + 0.5, pos.getY() + 0.5 + meltLevel, pos.getZ() + 0.5).inflate(1, 0, 1));
        for (LivingEntity entity : living) {
            DamageSource lava = level.damageSources().lava();
            entity.hurt(lava, 5.0F);
            com.hbm_m.platform.PlatformHooks.setSecondsOnFire(entity, 5);
        }

        /* плавка */
        if (!be.trySmelt()) {
            be.progress = 0;
        }

        be.tryRecipe();

        /* налив шлака — тыльная сторона */
        if (!be.wasteStack.isEmpty()) {
            Direction dir = be.getFacing().getOpposite();
            be.pourSide(level, pos, dir, be.wasteStack, false);
        }

        /* налив расплава рецепта — лицевая сторона */
        if (!be.recipeStack.isEmpty()) {
            Direction dir = be.getFacing();
            MoltenAlloyRecipe loaded = be.getLoadedRecipe(level);
            List<MaterialStack> toCast = new ArrayList<>();
            if (loaded == null) {
                toCast.addAll(be.recipeStack);
            } else {
                for (MaterialStack stack : be.recipeStack) {
                    for (MaterialStack output : loaded.getOutputs()) {
                        if (stack.type == output.type) {
                            toCast.add(stack);
                            break;
                        }
                    }
                }
            }
            be.pourSide(level, pos, dir, toCast, true);
        }

        /* чистка пустых стаканов */
        be.recipeStack.removeIf(o -> o.amount <= 0);
        be.wasteStack.removeIf(x -> x.amount <= 0);

        /* визуальное состояние (клиент через full-NBT sync) */
        int newTotal = be.totalAmount(be.recipeStack) + be.totalAmount(be.wasteStack);
        MaterialStack primary = be.getMaterialStack();
        be.fillLevel = (float) ((double) newTotal / (double) totalCap);
        be.fillColor = primary != null ? (0xFF000000 | primary.type.color) : 0xFFC18336;

        if (oldHeat != be.heat || oldProgress != be.progress || oldTotal != newTotal) {
            be.setChanged();
            be.syncToClient();
        }
    }

    /** Налив одной стороны: порция за тик, при успехе — поток лавовых частиц. */
    private void pourSide(Level level, BlockPos pos, Direction dir, List<MaterialStack> stacks, boolean isRecipe) {
        double px = pos.getX() + 0.5D + dir.getStepX() * 1.875D;
        double py = pos.getY() + 0.25D;
        double pz = pos.getZ() + 0.5D + dir.getStepZ() * 1.875D;

        Vec3[] impact = new Vec3[1];
        MaterialStack didPour = CrucibleUtil.pourFullStack(level, px, py, pz, POUR_RANGE, true, stacks, MIN_POUR_MB, impact);

        if (didPour != null) {
            // Порт aux-частицы "foundry": поток расплава (ParticleFoundry) от точки налива
            if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                CompoundTag data = new CompoundTag();
                data.putString("type", "foundry");
                data.putInt("color", didPour.type.color);
                data.putByte("dir", (byte) dir.get3DDataValue());
                data.putFloat("off", 0.625F);
                data.putFloat("base", 0.625F);
                data.putFloat("len", Math.max(1F, pos.getY() - (float) (Math.ceil(impact[0] != null ? impact[0].y : py - 1.0) - 0.875)));
                com.hbm_m.particle.helper.IParticleCreator.sendPacket(serverLevel,
                        pos.getX() + 0.5D + dir.getStepX() * 1.875D, pos.getY(),
                        pos.getZ() + 0.5D + dir.getStepZ() * 1.875D, 50, data);
            }
            syncToClient();
        }
    }

    /** Клиент: дым над тиглем, пока есть расплав — порт aux-частицы "tower" (ParticleCoolingTower). */
    public static void clientTick(Level level, BlockPos pos, BlockState state, MachineCrucibleBlockEntity be) {
        if (!be.recipeStack.isEmpty() || !be.wasteStack.isEmpty()) {
            if (level.getGameTime() % 10 == 0) {
                CompoundTag fx = new CompoundTag();
                fx.putString("type", "tower");
                fx.putFloat("lift", 10F);
                fx.putFloat("base", 0.75F);
                fx.putFloat("max", 3.5F);
                fx.putInt("life", 100 + level.random.nextInt(20));
                fx.putInt("color", 0x202020);
                fx.putDouble("posX", pos.getX() + 0.5);
                fx.putDouble("posY", pos.getY() + 1);
                fx.putDouble("posZ", pos.getZ() + 0.5);
                com.hbm_m.particle.helper.ParticleEffectClient.effectNT(fx);
            }
        }
    }

    // ═══════════════════════════════ Логика плавки ═══════════════════════════════

    /** Порт tryPullHeat: диффузионный обмен теплом с IHeatSource снизу. */
    protected void tryPullHeat() {
        if (this.heat >= MAX_HEAT) return;
        if (level == null) return;

        BlockEntity below = level.getBlockEntity(worldPosition.below());
        if (below instanceof com.hbm_m.interfaces.IHeatSource source) {
            int diff = source.getHeatStored() - this.heat;
            if (diff == 0) return;

            diff = Math.min(diff, MAX_HEAT - this.heat);
            if (diff > 0) {
                diff = (int) Math.ceil(diff * DIFFUSION);
                source.useUpHeat(diff);
                this.heat = Math.min(MAX_HEAT, this.heat + diff);
                return;
            }
        }
        this.heat = Math.max(this.heat - Math.max(this.heat / 1000, 1), 0);
    }

    /** Порт trySmelt. false — плавка не идёт (сброс прогресса). */
    protected boolean trySmelt() {
        if (this.heat < MAX_HEAT / 2) return false;

        int slot = this.getFirstSmeltableSlot();
        if (slot == -1) return false;

        int delta = this.heat - (MAX_HEAT / 2);
        delta = (int) (delta * 0.05);

        this.progress += delta;
        this.heat -= delta;

        if (this.progress >= PROCESS_TIME) {
            this.progress = 0;

            ItemStack stack = itemHandler.getStackInSlot(slot);
            List<MaterialStack> materials = getSmeltingMaterials(level, stack);
            MoltenAlloyRecipe recipe = getLoadedRecipe(level);

            for (MaterialStack material : materials) {
                boolean recipeMaterial = recipe != null
                        && (getAmountFromInputs(recipe, material.type) > 0 || getAmountFromOutputs(recipe, material.type) > 0);

                if (recipeMaterial) {
                    addToStack(this.recipeStack, material);
                } else {
                    addToStack(this.wasteStack, material);
                }
            }

            itemHandler.getStackInSlot(slot).shrink(1);
            setChanged();
        }

        return true;
    }

    /** Порт tryRecipe: конверсия по выбранному рецепту каждые frequency тиков. */
    protected void tryRecipe() {
        MoltenAlloyRecipe recipe = getLoadedRecipe(level);
        if (recipe == null) return;
        if (level == null || level.getGameTime() % Math.max(1, recipe.getFrequency()) > 0) return;

        for (MaterialStack stack : recipe.getInputs()) {
            if (getStoredAmount(this.recipeStack, stack.type) < stack.amount) return;
        }

        for (MaterialStack stack : this.recipeStack) {
            stack.amount -= getAmountFromInputs(recipe, stack.type);
        }

        outer:
        for (MaterialStack out : recipe.getOutputs()) {
            for (MaterialStack stack : this.recipeStack) {
                if (stack.type == out.type) {
                    stack.amount += out.amount;
                    continue outer;
                }
            }
            this.recipeStack.add(out.copy());
        }
    }

    protected int getFirstSmeltableSlot() {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.isEmpty() && isItemSmeltable(stack)) {
                return i;
            }
        }
        return -1;
    }

    /** Порт isItemSmeltable: симуляция лимитов рецепта и ёмкостей. */
    public boolean isItemSmeltable(ItemStack stack) {
        List<MaterialStack> materials = getSmeltingMaterials(level, stack);
        if (materials.isEmpty()) return false;

        MoltenAlloyRecipe recipe = getLoadedRecipe(level);
        boolean matchesRecipe = recipe == null;

        int recipeContent = 0;
        if (recipe != null) {
            for (MaterialStack in : recipe.getInputs()) recipeContent += in.amount;
        }

        int recipeAmount = totalAmount(this.recipeStack);
        int wasteAmount = totalAmount(this.wasteStack);

        for (MaterialStack mat : materials) {
            int recipeInputRequired = recipe != null ? getAmountFromInputs(recipe, mat.type) : 0;

            // выход рецепта можно заливать обратно
            if (recipe != null && getAmountFromOutputs(recipe, mat.type) > 0) {
                recipeAmount += mat.amount;
                matchesRecipe = true;
                continue;
            }

            if (recipeInputRequired == 0) {
                wasteAmount += mat.amount;
            } else {
                int matMaximum = recipeInputRequired * RECIPE_CAPACITY_MB / recipeContent;
                int amountStored = getStoredAmount(this.recipeStack, mat.type);

                matchesRecipe = true;
                recipeAmount += mat.amount;

                if (recipe != null && amountStored + mat.amount > matMaximum) {
                    return false;
                }
            }
        }

        return recipeAmount <= RECIPE_CAPACITY_MB && wasteAmount <= WASTE_CAPACITY_MB && matchesRecipe;
    }

    public void addToStack(List<MaterialStack> stack, MaterialStack matStack) {
        for (MaterialStack mat : stack) {
            if (mat.type == matStack.type) {
                mat.amount += matStack.amount;
                return;
            }
        }
        stack.add(matStack.copy());
    }

    /** Выбранный рецепт по строке-идентификатору (аналог recipeNameMap.get(recipe)). */
    public @Nullable MoltenAlloyRecipe getLoadedRecipe(Level level) {
        if (level == null || "null".equals(this.recipe)) return null;
        for (java.util.Map.Entry<ResourceLocation, MoltenAlloyRecipe> e :
                RecipeHooks.getAllRecipesById(level, MoltenAlloyRecipe.Type.INSTANCE).entrySet()) {
            if (this.recipe.equals(e.getKey().toString())) {
                return e.getValue();
            }
        }
        return null;
    }

    /** Смена выбранного рецепта (пакет из GUI, аналог receiveControl index=0). */
    public void setSelectedRecipe(@Nullable String selection) {
        this.recipe = selection == null ? "null" : selection;
        setChanged();
        syncToClient();
    }

    public String getSelectedRecipe() {
        return recipe;
    }

    /** Выбран ли конкретный рецепт (не "null"). */
    public boolean hasRecipe() {
        return !"null".equals(recipe) && !recipe.isEmpty();
    }

    // ══════════════════════════ Налив В тигель (ICrucibleAcceptor) ══════════════════════════

    /** Порт canAcceptPartialPour: без рецепта — приём в шлак, с рецептом — лимит по материалу. */
    @Override
    public boolean canAcceptPartialPour(Level level, BlockPos pos, Direction side, MaterialStack stack) {
        MoltenAlloyRecipe recipe = getLoadedRecipe(level);

        if (recipe == null) {
            return totalAmount(this.wasteStack) < WASTE_CAPACITY_MB;
        }

        int recipeContent = 0;
        for (MaterialStack in : recipe.getInputs()) recipeContent += in.amount;
        int recipeInputRequired = getAmountFromInputs(recipe, stack.type);
        int matMaximum = recipeInputRequired * RECIPE_CAPACITY_MB / recipeContent;
        int amountStored = getStoredAmount(this.recipeStack, stack.type);

        return amountStored < matMaximum && totalAmount(this.recipeStack) < RECIPE_CAPACITY_MB;
    }

    /** Порт pour: заливка расплава в тигель (в т.ч. возврат выхода рецепта). */
    @Override
    public @Nullable MaterialStack pour(Level level, BlockPos pos, Direction side, MaterialStack stack) {
        MoltenAlloyRecipe recipe = getLoadedRecipe(level);

        if (recipe == null) {
            int amount = totalAmount(this.wasteStack);
            if (amount + stack.amount <= WASTE_CAPACITY_MB) {
                addToStack(this.wasteStack, stack.copy());
                syncToClient();
                return null;
            } else {
                int toAdd = WASTE_CAPACITY_MB - amount;
                addToStack(this.wasteStack, new MaterialStack(stack.type, toAdd));
                syncToClient();
                return new MaterialStack(stack.type, stack.amount - toAdd);
            }
        }

        int recipeContent = 0;
        for (MaterialStack in : recipe.getInputs()) recipeContent += in.amount;
        int recipeInputRequired = getAmountFromInputs(recipe, stack.type);
        int matMaximum = recipeInputRequired * RECIPE_CAPACITY_MB / recipeContent;

        if (recipeInputRequired + stack.amount <= matMaximum) {
            addToStack(this.recipeStack, stack.copy());
            syncToClient();
            return null;
        }

        // Порт формулы оригинала; ограничение снизу исключает отрицательный добавляемый объём
        int toAdd = Math.min(matMaximum - stack.amount, RECIPE_CAPACITY_MB - totalAmount(this.recipeStack));
        if (toAdd <= 0) {
            return stack;
        }
        addToStack(this.recipeStack, new MaterialStack(stack.type, toAdd));
        syncToClient();
        return new MaterialStack(stack.type, stack.amount - toAdd);
    }

    @Override public boolean canAcceptPartialFlow(Level level, BlockPos pos, Direction side, MaterialStack stack) { return false; }
    @Override public @Nullable MaterialStack flow(Level level, BlockPos pos, Direction side, MaterialStack stack) { return null; }

    // ═══════════════════════════════ Утилиты ═══════════════════════════════

    private static int totalAmount(List<MaterialStack> stacks) {
        int sum = 0;
        for (MaterialStack ms : stacks) sum += ms.amount;
        return sum;
    }

    /** Порт getQuantaFromType(список, материал): первый совпавший; null = сумма. */
    private static int getStoredAmount(List<MaterialStack> stacks, @Nullable MaterialType mat) {
        if (mat == null) return totalAmount(stacks);
        for (MaterialStack stack : stacks) {
            if (stack.type == mat) return stack.amount;
        }
        return 0;
    }

    /** Порт getQuantaFromType(массив входов, материал): первый совпавший. */
    private static int getAmountFromInputs(MoltenAlloyRecipe recipe, MaterialType mat) {
        for (MaterialStack stack : recipe.getInputs()) {
            if (stack.type == mat) return stack.amount;
        }
        return 0;
    }

    private static int getAmountFromOutputs(MoltenAlloyRecipe recipe, MaterialType mat) {
        for (MaterialStack stack : recipe.getOutputs()) {
            if (stack.type == mat) return stack.amount;
        }
        return 0;
    }

    /**
     * Материалы предмета — data-driven через {@code hbm_m:crucible_smelting}
     * (порт Mats.getSmeltingMaterialsFromItem; руды дают несколько материалов —
     * побочные продукты MatDistribution; дедуп по материалу, порядок рецептов = приоритет).
     */
    private static List<MaterialStack> getSmeltingMaterials(Level level, ItemStack stack) {
        List<MaterialStack> out = new ArrayList<>();
        if (level == null || stack.isEmpty()) return out;

        // Плавление литейных отходов (ScrapItem): количество из тега "amount" (в квантах), материал из ModMaterials
        if (stack.getItem() instanceof com.hbm_m.item.material.ScrapItem scrap) {
            com.hbm_m.item.material.ModMaterials mat = scrap.getMaterial();
            if (mat == null) {
                for (com.hbm_m.item.material.ModMaterialItems.ScrapEntry entry : com.hbm_m.item.material.ModMaterialItems.FOUNDRY_SCRAPS) {
                    if (com.hbm_m.item.material.ModMaterialItems.scrapItem(entry.mat()) == stack.getItem()) {
                        mat = entry.mat();
                        break;
                    }
                }
            }
            if (mat != null) {
                MaterialType type = MaterialType.byName(mat.getId());
                if (type != null) {
                    int quanta = com.hbm_m.item.material.ScrapItem.getAmount(stack);
                    int amountMb = (int) Math.max(1, Math.round((double) quanta * MaterialStack.MB_PER_INGOT / com.hbm_m.item.material.ScrapItem.QUANTA_PER_INGOT));
                    out.add(new MaterialStack(type, amountMb));
                    return out;
                }
            }
        }

        for (com.hbm_m.recipe.CrucibleSmeltingRecipe recipe : RecipeHooks.getAllRecipes(level, com.hbm_m.recipe.CrucibleSmeltingRecipe.Type.INSTANCE)) {
            if (!recipe.matchesInput(stack)) continue;
            for (MaterialStack ms : recipe.toMaterialStacks(1)) {
                if (ms == null || ms.isEmpty()) continue;
                boolean dup = false;
                for (MaterialStack existing : out) {
                    if (existing.type == ms.type) { dup = true; break; }
                }
                if (!dup) out.add(ms);
            }
        }
        return out;
    }

    private Direction getFacing() {
        BlockState state = getBlockState();
        if (state.hasProperty(com.hbm_m.block.machines.MachineCrucibleBlock.FACING)) {
            return state.getValue(com.hbm_m.block.machines.MachineCrucibleBlock.FACING);
        }
        return Direction.NORTH;
    }

    /** Полный NBT-sync клиенту (BaseHbmBlockEntity не имеет sendUpdateToClient). */
    private void syncToClient() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // ═══════════════════════════════ NBT ═══════════════════════════════

    @Override
    protected void writeNbtData(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putString("recipe", recipe);
        tag.putInt("heat", heat);
        tag.putInt("progress", progress);
        tag.putFloat("fillLevel", fillLevel);
        tag.putInt("fillColor", fillColor);

        tag.put("rec", stacksToNbt(recipeStack));
        tag.put("was", stacksToNbt(wasteStack));
        tag.put("inventory", com.hbm_m.platform.ItemStackSerialization.serialize(itemHandler, registries));
    }

    @Override
    protected void readNbtData(CompoundTag tag, HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        recipe = tag.contains("recipe") ? tag.getString("recipe") : "null";
        heat = tag.getInt("heat");
        progress = tag.getInt("progress");
        fillLevel = tag.getFloat("fillLevel");
        fillColor = tag.getInt("fillColor");

        recipeStack.clear();
        wasteStack.clear();
        readStacks(tag.getList("rec", net.minecraft.nbt.Tag.TAG_COMPOUND), recipeStack);
        readStacks(tag.getList("was", net.minecraft.nbt.Tag.TAG_COMPOUND), wasteStack);

        if (tag.contains("inventory")) {
            com.hbm_m.platform.ItemStackSerialization.deserialize(itemHandler, tag.getCompound("inventory"), registries);
        } else if (tag.contains("material")) {
            // legacy single-material save format
            MaterialStack ms = MaterialStack.readFromNBT(tag.getCompound("material"));
            if (ms != null) wasteStack.add(ms);
        }
    }

    private static ListTag stacksToNbt(List<MaterialStack> stacks) {
        ListTag list = new ListTag();
        for (MaterialStack ms : stacks) {
            if (ms.isEmpty()) continue;
            CompoundTag entry = new CompoundTag();
            ms.writeToNBT(entry);
            list.add(entry);
        }
        return list;
    }

    private static void readStacks(ListTag list, List<MaterialStack> out) {
        for (int i = 0; i < list.size(); i++) {
            MaterialStack ms = MaterialStack.readFromNBT(list.getCompound(i));
            if (ms != null && !ms.isEmpty()) out.add(ms);
        }
    }

    /** Границы рендера: модель 3×3×1.5 от ядра (порт getRenderBoundingBox). */
    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox() {
        return new net.minecraft.world.phys.AABB(
                worldPosition.getX() - 1, worldPosition.getY(),     worldPosition.getZ() - 1,
                worldPosition.getX() + 2, worldPosition.getY() + 2, worldPosition.getZ() + 2);
    }

    @Override
    public @Nullable Object getItemHandler(@Nullable Direction side) {
        return this.itemHandler;
    }

    /* ── Устройство настройки: металлы, которыми заряжается селектор рецептов ── */

    @Override
    public int[] getMatsToCopy() {
        java.util.ArrayList<Integer> types = new java.util.ArrayList<>();
        for (MaterialStack stack : recipeStack) {
            types.add(stack.type.id);
        }
        for (MaterialStack stack : wasteStack) {
            types.add(stack.type.id);
        }
        return types.stream().mapToInt(Integer::intValue).toArray();
    }
}
