# Руководство по добавлению новых машин с энергией

## Быстрый старт: создание простой машины

Это пошаговое руководство покажет, как создать новую машину, работающую от энергии.

### Шаг 1: Создание BlockEntity

```java
package com.hbm_m.block.entity;

import com.hbm_m.capability.ModCapabilities;
import com.hbm_m.energy.BlockEntityEnergyStorage;
import com.hbm_m.energy.ILongEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MyMachineBlockEntity extends BlockEntity implements MenuProvider {
    
    // Параметры энергии
    public static final long ENERGY_CAPACITY = 50000L;    // Вместимость
    public static final long ENERGY_PER_TICK = 100L;      // Потребление за тик
    public static final long MAX_RECEIVE = 1000L;         // Макс. прием энергии
    
    // Хранилище энергии
    private final BlockEntityEnergyStorage energy = 
        new BlockEntityEnergyStorage(ENERGY_CAPACITY, MAX_RECEIVE, 0L);
    
    private final LazyOptional<ILongEnergyStorage> energyCap = 
        LazyOptional.of(() -> energy);
    
    // Прогресс обработки
    private int progress = 0;
    private static final int MAX_PROGRESS = 200; // 10 секунд при 20 tps
    
    public MyMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MY_MACHINE_BE.get(), pos, state);
    }
    
    // Серверный тик - основная логика работы
    public static void serverTick(Level level, BlockPos pos, 
                                  BlockState state, MyMachineBlockEntity be) {
        // Проверяем условия работы
        if (be.canWork()) {
            // Проверяем наличие энергии
            if (be.energy.getEnergyStored() >= ENERGY_PER_TICK) {
                // Потребляем энергию
                be.energy.extractEnergy(ENERGY_PER_TICK, false);
                
                // Увеличиваем прогресс
                be.progress++;
                
                // Завершаем работу
                if (be.progress >= MAX_PROGRESS) {
                    be.finishWork();
                    be.progress = 0;
                }
                
                be.setChanged();
            }
        } else {
            // Сбрасываем прогресс если не можем работать
            if (be.progress > 0) {
                be.progress = 0;
                be.setChanged();
            }
        }
    }
    
    private boolean canWork() {
        // TODO: Проверяйте наличие входных предметов, 
        // место для выходных и т.д.
        return true;
    }
    
    private void finishWork() {
        // TODO: Создайте выходные предметы, дайте опыт и т.д.
    }
    
    // Предоставление энергокапабилити
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(
            @NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ModCapabilities.LONG_ENERGY) {
            return energyCap.cast();
        }
        return super.getCapability(cap, side);
    }
    
    // Сохранение в NBT
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("Energy", energy.getEnergyStored());
        tag.putInt("Progress", progress);
    }
    
    // Загрузка из NBT
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energy.setEnergy(tag.getLong("Energy"));
        progress = tag.getInt("Progress");
    }
    
    // Инвалидация капабилити при удалении
    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyCap.invalidate();
    }
    
    // Методы для GUI
    public int getProgress() {
        return progress;
    }
    
    public int getMaxProgress() {
        return MAX_PROGRESS;
    }
    
    public long getEnergy() {
        return energy.getEnergyStored();
    }
    
    public long getMaxEnergy() {
        return energy.getMaxEnergyStored();
    }
    
    @Override
    public Component getDisplayName() {
        return Component.translatable("container.hbm_m.my_machine");
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new MyMachineMenu(id, inventory, this);
        // TODO: Создайте MyMachineMenu
    }
}
```

### Шаг 2: Регистрация BlockEntity

В `ModBlockEntities.java`:

```java
public static final RegistryObject<BlockEntityType<MyMachineBlockEntity>> MY_MACHINE_BE =
    BLOCK_ENTITIES.register("my_machine_be", () ->
        BlockEntityType.Builder.of(
            MyMachineBlockEntity::new,
            ModBlocks.MY_MACHINE_BLOCK.get()
        ).build(null)
    );
```

### Шаг 3: Создание Block

```java
package com.hbm_m.block;

import com.hbm_m.block.entity.MyMachineBlockEntity;
import com.hbm_m.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class MyMachineBlock extends BaseEntityBlock {
    
    public MyMachineBlock(Properties properties) {
        super(properties);
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MyMachineBlockEntity(pos, state);
    }
    
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
    
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MyMachineBlockEntity) {
                NetworkHooks.openScreen((ServerPlayer) player, 
                    (MyMachineBlockEntity) be, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        
        return createTickerHelper(type, ModBlockEntities.MY_MACHINE_BE.get(),
            MyMachineBlockEntity::serverTick);
    }
    
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                        BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MyMachineBlockEntity machine) {
                // TODO: Выбросить содержимое инвентаря
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
```

### Шаг 4: Создание Menu (контейнер)

```java
package com.hbm_m.menu;

import com.hbm_m.block.entity.MyMachineBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class MyMachineMenu extends AbstractContainerMenu {
    
    private final MyMachineBlockEntity blockEntity;
    private final ContainerData data;
    
    // Для сервера
    public MyMachineMenu(int id, Inventory inventory, MyMachineBlockEntity be) {
        super(ModMenuTypes.MY_MACHINE_MENU.get(), id);
        this.blockEntity = be;
        this.data = new SimpleContainerData(4); // energy_high, energy_low, progress, max_progress
        
        // TODO: Добавьте слоты для предметов
        
        addPlayerInventory(inventory);
        addPlayerHotbar(inventory);
        addDataSlots(data);
    }
    
    // Для клиента (получает данные из пакета)
    public MyMachineMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, (MyMachineBlockEntity) inventory.player.level()
            .getBlockEntity(extraData.readBlockPos()));
    }
    
    // Добавление инвентаря игрока
    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, 
                    col + row * 9 + 9, 
                    8 + col * 18, 
                    84 + row * 18));
            }
        }
    }
    
    // Добавление хотбара игрока
    private void addPlayerHotbar(Inventory playerInventory) {
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }
    
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // TODO: Реализуйте Shift-клик
        return ItemStack.EMPTY;
    }
    
    @Override
    public boolean stillValid(Player player) {
        return blockEntity.getLevel().getBlockEntity(blockEntity.getBlockPos()) == blockEntity
            && player.distanceToSqr(blockEntity.getBlockPos().getX() + 0.5,
                                    blockEntity.getBlockPos().getY() + 0.5,
                                    blockEntity.getBlockPos().getZ() + 0.5) <= 64;
    }
    
    // Геттеры для GUI
    public int getProgress() {
        return data.get(2);
    }
    
    public int getMaxProgress() {
        return data.get(3);
    }
    
    public long getEnergy() {
        // Распаковываем long из двух int
        int high = data.get(0);
        int low = data.get(1);
        return ((long)high << 32) | (low & 0xFFFFFFFFL);
    }
}
```

### Шаг 5: Создание Screen (GUI)

```java
package com.hbm_m.client.overlay;

import com.hbm_m.menu.MyMachineMenu;
import com.hbm_m.util.EnergyFormatter;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class MyMachineScreen extends AbstractContainerScreen<MyMachineMenu> {
    
    private static final ResourceLocation TEXTURE = 
        new ResourceLocation("hbm_m", "textures/gui/my_machine.png");
    
    public MyMachineScreen(MyMachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageHeight = 166;
        this.imageWidth = 176;
    }
    
    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        
        graphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
        
        // Отрисовка шкалы прогресса
        int progress = menu.getProgress();
        int maxProgress = menu.getMaxProgress();
        if (maxProgress > 0) {
            int progressPixels = (progress * 24) / maxProgress;
            graphics.blit(TEXTURE, x + 79, y + 35, 176, 0, progressPixels, 16);
        }
        
        // Отрисовка шкалы энергии
        long energy = menu.getEnergy();
        long maxEnergy = 50000L; // TODO: Получать из BlockEntity
        if (maxEnergy > 0) {
            int energyPixels = (int)((energy * 52) / maxEnergy);
            graphics.blit(TEXTURE, x + 8, y + 70 - energyPixels, 
                         176, 52 - energyPixels, 16, energyPixels);
        }
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        
        // Подсказка для энергии
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        
        if (mouseX >= x + 8 && mouseX <= x + 24 && 
            mouseY >= y + 18 && mouseY <= y + 70) {
            long energy = menu.getEnergy();
            graphics.renderTooltip(this.font, 
                Component.literal(EnergyFormatter.formatFE(energy)), 
                mouseX, mouseY);
        }
    }
}
```

### Шаг 6: Регистрация всего

В `ModBlocks.java`:
```java
public static final RegistryObject<Block> MY_MACHINE_BLOCK = 
    BLOCKS.register("my_machine", () -> 
        new MyMachineBlock(BlockBehaviour.Properties.of()
            .strength(3.5F)
            .requiresCorrectToolForDrops()));
```

В `ModItems.java`:
```java
public static final RegistryObject<Item> MY_MACHINE_ITEM = 
    ITEMS.register("my_machine", () -> 
        new BlockItem(ModBlocks.MY_MACHINE_BLOCK.get(), 
            new Item.Properties()));
```

В `ModMenuTypes.java`:
```java
public static final RegistryObject<MenuType<MyMachineMenu>> MY_MACHINE_MENU =
    MENUS.register("my_machine", () ->
        IForgeMenuType.create(MyMachineMenu::new));
```

В `ClientSetup.java`:
```java
MenuScreens.register(ModMenuTypes.MY_MACHINE_MENU.get(), 
    MyMachineScreen::new);
```

## Дополнительные возможности

### Машина с внутренним инвентарем

Добавьте в BlockEntity:

```java
private final ItemStackHandler itemHandler = new ItemStackHandler(3) {
    @Override
    protected void onContentsChanged(int slot) {
        setChanged();
    }
};

private final LazyOptional<IItemHandler> itemCap = 
    LazyOptional.of(() -> itemHandler);

@Override
public @NotNull <T> LazyOptional<T> getCapability(
        @NotNull Capability<T> cap, @Nullable Direction side) {
    if (cap == ForgeCapabilities.ITEM_HANDLER) {
        return itemCap.cast();
    }
    return super.getCapability(cap, side);
}
```

### Машина, работающая от сети проводов

Вместо хранения энергии, запрашивайте ее напрямую:

```java
public static void serverTick(Level level, BlockPos pos, 
                              BlockState state, MyMachineBlockEntity be) {
    if (be.canWork()) {
        long received = WireNetworkManager.get()
            .requestEnergy(level, pos, ENERGY_PER_TICK, false);
        
        if (received >= ENERGY_PER_TICK) {
            be.progress++;
            // ...
        }
    }
}
```

### Машина с разными режимами работы

```java
public enum WorkMode {
    SLOW(50L, 400),   // 50 FE/t, 20 секунд
    NORMAL(100L, 200), // 100 FE/t, 10 секунд
    FAST(200L, 100);   // 200 FE/t, 5 секунд
    
    public final long energyPerTick;
    public final int processTime;
    
    WorkMode(long energyPerTick, int processTime) {
        this.energyPerTick = energyPerTick;
        this.processTime = processTime;
    }
}

private WorkMode currentMode = WorkMode.NORMAL;
```

## Полезные советы

1. **Всегда вызывайте `setChanged()`** после изменения данных BlockEntity
2. **Используйте `@NotNull` и `@Nullable`** для лучшей безопасности типов
3. **Валидируйте данные** при загрузке из NBT
4. **Не забывайте `invalidateCaps()`** при удалении BlockEntity
5. **Тестируйте на сервере** - много ошибок проявляются только там

## Отладка

### Логирование энергии

```java
LOGGER.info("Energy: {}/{} FE", 
    energy.getEnergyStored(), 
    energy.getMaxEnergyStored());
```

### Проверка капабилити

```java
LazyOptional<ILongEnergyStorage> cap = 
    be.getCapability(ModCapabilities.LONG_ENERGY, Direction.UP);
LOGGER.info("Has energy cap: {}", cap.isPresent());
```

## Дальнейшее чтение

- `docs/ENERGY_SYSTEM.md` - Полная документация по энергосистеме
- `docs/EnergySystemExamples.java` - Примеры кода
- Исходники `WoodBurnerBlockEntity` - Реальный пример генератора
- Исходники `MachineBatteryBlockEntity` - Реальный пример батареи
