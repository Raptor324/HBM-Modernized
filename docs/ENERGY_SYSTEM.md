# Энергосистема HBM Nuclear Tech Modernized

## Обзор

Энергосистема HBM-M представляет собой полностью переработанную систему для генерации, хранения и передачи энергии между машинами в Minecraft 1.20.1 на Forge.

## Ключевые особенности

### 1. Long-based энергия

В отличие от стандартной Forge энергосистемы (которая использует `int` и ограничена ~2.1 миллиарда FE), наша система использует `long` для хранения и передачи энергии, что позволяет работать с гораздо большими значениями (до ~9.2 квинтиллионов FE).

### 2. Обратная совместимость

Система полностью совместима со стандартным Forge Energy через адаптеры-обертки:
- `ForgeToLongWrapper` - преобразует `IEnergyStorage` в `ILongEnergyStorage`
- `LongToForgeWrapper` - преобразует `ILongEnergyStorage` в `IEnergyStorage`

### 3. Эффективная сеть проводов

Wire Network Manager использует алгоритм Union-Find для эффективного управления сетями проводов:
- Автоматическое объединение соединенных проводов в компоненты
- Быстрый поиск источников энергии
- Минимальные накладные расходы при добавлении/удалении проводов
- Поддержка приоритетов для батарей

## Архитектура

### Основные компоненты

```
com.hbm_m.energy/
├── ILongEnergyStorage.java           # Основной интерфейс для энергохранилищ
├── BlockEntityEnergyStorage.java     # Реализация для блоков
├── ItemEnergyStorage.java            # Реализация для предметов
├── WireNetworkManager.java           # Менеджер сетей проводов
├── ForgeToLongWrapper.java           # Адаптер Forge -> Long
├── LongToForgeWrapper.java           # Адаптер Long -> Forge
└── LongDataPacker.java               # Упаковка long в int для синхронизации
```

### Интерфейс ILongEnergyStorage

```java
public interface ILongEnergyStorage {
    // Принять энергию
    long receiveEnergy(long maxReceive, boolean simulate);
    
    // Извлечь энергию
    long extractEnergy(long maxExtract, boolean simulate);
    
    // Получить текущую энергию
    long getEnergyStored();
    
    // Получить максимальную емкость
    long getMaxEnergyStored();
    
    // Может ли извлекать
    boolean canExtract();
    
    // Может ли принимать
    boolean canReceive();
}
```

## Использование

### Создание энергохранилища для блока

```java
public class MyMachineBlockEntity extends BlockEntity {
    private final BlockEntityEnergyStorage energy = 
        new BlockEntityEnergyStorage(
            100000L,  // capacity - емкость
            1000L,    // maxReceive - макс. прием в тик
            1000L     // maxExtract - макс. извлечение в тик
        );
    
    private final LazyOptional<ILongEnergyStorage> energyCap = 
        LazyOptional.of(() -> energy);
    
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == ModCapabilities.LONG_ENERGY) {
            return energyCap.cast();
        }
        return super.getCapability(cap, side);
    }
}
```

### Создание энергохранилища для предмета

```java
public class MyBatteryItem extends Item {
    public static final long CAPACITY = 1000000L;
    
    @Override
    public <T> LazyOptional<T> getCapability(ItemStack stack, Capability<T> cap) {
        if (cap == ModCapabilities.LONG_ENERGY) {
            ItemEnergyStorage storage = new ItemEnergyStorage(
                stack, CAPACITY, 1000L, 1000L
            );
            return LazyOptional.of(() -> storage).cast();
        }
        return LazyOptional.empty();
    }
}
```

### Работа с проводами

Провода автоматически обнаруживают соседние блоки с энергией и создают сети. Менеджер проводов (`WireNetworkManager`) автоматически:

1. Обнаруживает новые провода при размещении
2. Объединяет провода в компоненты
3. Находит источники энергии (машины, батареи)
4. Распределяет энергию по приоритетам

```java
// Пример запроса энергии из сети проводов
long needed = 1000L;
long received = WireNetworkManager.get()
    .requestEnergy(level, wirePos, needed, false);
```

### Система приоритетов батарей

Батареи могут иметь три уровня приоритета:
- `HIGH` (2) - высокий приоритет, извлекается первой
- `NORMAL` (1) - обычный приоритет (по умолчанию)
- `LOW` (0) - низкий приоритет, извлекается последней

Пример в `MachineBatteryBlockEntity`:
```java
public enum Priority {
    LOW,    // 0
    NORMAL, // 1
    HIGH    // 2
}
```

## Потокобезопасность

Все критические операции с энергией защищены через `synchronized` блоки:
- `BlockEntityEnergyStorage` использует `energyLock`
- `ItemEnergyStorage` использует `stackLock`
- `WireNetworkManager` использует `ReentrantReadWriteLock`

## Регистрация Capability

Capability регистрируется в `ModCapabilities`:

```java
public static void register(RegisterCapabilitiesEvent event) {
    event.register(ILongEnergyStorage.class);
}
```

Регистрация вызывается в основном классе мода:

```java
@Mod.EventBusSubscriber(modid = MainRegistry.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEvents {
    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        ModCapabilities.register(event);
    }
}
```

## Оптимизация

### Wire Network Manager

- **Union-Find**: O(α(n)) для объединения и поиска компонент
- **Spanning Tree**: Минимальное количество ребер для связности
- **Кэширование источников**: Список источников пересчитывается только при изменении топологии
- **Ограничение на источники**: Максимум 2048 источников на компоненту для предотвращения зависаний

### Энергопередача

- **BFS с ограничением**: Максимум 2000 узлов при поиске источников
- **Приоритизация**: Сортировка источников по приоритету перед извлечением
- **Lazy evaluation**: Источники проверяются только при запросе энергии

## Отладка

### Логирование

Система использует `MainRegistry.LOGGER` для отладочных сообщений:
- Предупреждения о недопустимых значениях
- Ошибки при отрицательных значениях энергии
- Информация о топологии сети (при включении debug)

### Проверки безопасности

- Автоматическая коррекция отрицательных значений
- Проверка на переполнение при сложении
- Валидация параметров конструктора

## Примеры машин

### Генератор

```java
public class GeneratorBlockEntity extends BlockEntity {
    private final BlockEntityEnergyStorage energy = 
        new BlockEntityEnergyStorage(10000L, 0L, 100L);
    
    public static void serverTick(Level level, BlockPos pos, 
                                  BlockState state, GeneratorBlockEntity be) {
        if (be.isBurning()) {
            // Генерируем 20 FE/t
            be.energy.receiveEnergy(20L, false);
        }
        
        // Отдаем энергию в соседние блоки
        be.distributeEnergy();
    }
    
    private void distributeEnergy() {
        for (Direction dir : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(dir));
            if (neighbor != null) {
                neighbor.getCapability(ModCapabilities.LONG_ENERGY, dir.getOpposite())
                    .ifPresent(storage -> {
                        if (storage.canReceive()) {
                            long extracted = energy.extractEnergy(100L, false);
                            long received = storage.receiveEnergy(extracted, false);
                            // Возвращаем неиспользованное
                            if (received < extracted) {
                                energy.receiveEnergy(extracted - received, false);
                            }
                        }
                    });
            }
        }
    }
}
```

### Потребитель

```java
public class MachineBlockEntity extends BlockEntity {
    private final BlockEntityEnergyStorage energy = 
        new BlockEntityEnergyStorage(50000L, 1000L, 0L);
    
    public static void serverTick(Level level, BlockPos pos, 
                                  BlockState state, MachineBlockEntity be) {
        if (be.canWork() && be.energy.getEnergyStored() >= 100L) {
            // Потребляем 100 FE для работы
            be.energy.extractEnergy(100L, false);
            be.doWork();
        }
    }
}
```

## Совместимость с другими модами

Благодаря `ForgeToLongWrapper`, наша система может:
- Принимать энергию от стандартных Forge генераторов
- Передавать энергию стандартным Forge машинам
- Работать с любыми модами, использующими Forge Energy

Адаптер автоматически обрезает значения до `Integer.MAX_VALUE` при необходимости.

## Производительность

### Бенчмарки (теоретические)

- **Union-Find операция**: < 1 мкс
- **Поиск источников в сети из 1000 проводов**: < 1 мс
- **Передача энергии через 100 проводов**: < 100 мкс
- **Обновление топологии при добавлении провода**: < 5 мс

### Рекомендации

1. Избегайте слишком больших сетей (>10000 проводов)
2. Используйте батареи для буферизации энергии
3. Устанавливайте приоритеты батарей для оптимизации извлечения
4. Не создавайте циклические зависимости в энергосистеме

## Будущие улучшения

- [ ] Визуализация потоков энергии в проводах
- [ ] Статистика энергопотребления/генерации
- [ ] Беспроводная передача энергии
- [ ] Трансформаторы для преобразования напряжения
- [ ] Энергопотери в длинных проводах
- [ ] Различные типы проводов с разной пропускной способностью

## FAQ

**Q: Почему используется long вместо int?**  
A: Стандартный int ограничен 2.1 миллиарда, что недостаточно для поздней игры в HBM Nuclear Tech.

**Q: Совместима ли система с другими энергосистемами?**  
A: Да, через `ForgeToLongWrapper` поддерживается Forge Energy. Для других систем можно написать свои адаптеры.

**Q: Как работает приоритет батарей?**  
A: При запросе энергии источники сортируются по приоритету (от высокого к низкому), и извлечение идет в этом порядке.

**Q: Что происходит при удалении провода из сети?**  
A: `WireNetworkManager` автоматически пересчитывает компоненты сети и обновляет списки источников.

**Q: Потокобезопасна ли система?**  
A: Да, все критические операции защищены синхронизацией.

## Контакты

Вопросы и предложения: [GitHub Issues](https://github.com/Raptor324/HBM-Modernized/issues)
