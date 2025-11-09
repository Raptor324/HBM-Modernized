# Энергосистема HBM-M: Устранение неполадок и лучшие практики

## Содержание
1. [Частые проблемы](#частые-проблемы)
2. [Отладка](#отладка)
3. [Лучшие практики](#лучшие-практики)
4. [Оптимизация производительности](#оптимизация-производительности)
5. [Совместимость](#совместимость)

---

## Частые проблемы

### Энергия не передается между машинами

**Проблема**: Машины соединены, но энергия не передается.

**Решения**:

1. **Проверьте направление capability**
   ```java
   // НЕПРАВИЛЬНО - используется одно направление
   neighbor.getCapability(ModCapabilities.LONG_ENERGY, side);
   
   // ПРАВИЛЬНО - используется противоположное направление
   neighbor.getCapability(ModCapabilities.LONG_ENERGY, side.getOpposite());
   ```

2. **Проверьте canExtract/canReceive**
   ```java
   // Генератор должен уметь извлекать
   new BlockEntityEnergyStorage(capacity, 0L, maxExtract);
   
   // Потребитель должен уметь принимать
   new BlockEntityEnergyStorage(capacity, maxReceive, 0L);
   ```

3. **Проверьте, что capability зарегистрирована**
   ```java
   // В ModEvents или аналогичном классе должно быть:
   @SubscribeEvent
   public static void registerCapabilities(RegisterCapabilitiesEvent event) {
       ModCapabilities.register(event);
   }
   ```

4. **Проверьте invalidateCaps**
   ```java
   @Override
   public void invalidateCaps() {
       super.invalidateCaps();
       energyCap.invalidate(); // НЕ ЗАБЫВАЙТЕ!
   }
   ```

### Провода не соединяются

**Проблема**: Провода размещаются рядом, но визуально не соединяются.

**Решения**:

1. **Проверьте canConnectTo**
   ```java
   // В WireBlock.java
   private boolean canConnectTo(BlockGetter level, BlockPos pos, Direction dir) {
       BlockPos neighborPos = pos.relative(dir);
       BlockEntity be = level.getBlockEntity(neighborPos);
       
       // Провода соединяются с другими проводами
       if (be instanceof WireBlockEntity) return true;
       
       // И с блоками, имеющими энергию
       if (be != null) {
           return be.getCapability(ModCapabilities.LONG_ENERGY, dir.getOpposite()).isPresent()
               || be.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).isPresent();
       }
       
       return false;
   }
   ```

2. **Клиентская синхронизация**
   - На клиенте нужно время для загрузки соседних BlockEntity
   - `WireBlockEntity.scheduleRecheck()` решает эту проблему

3. **Обновление состояния блока**
   ```java
   // После размещения провода обновите соседей
   level.updateNeighborsAt(pos, this);
   ```

### Энергия "исчезает" при перезагрузке

**Проблема**: После перезагрузки мира энергия сбрасывается.

**Решения**:

1. **Проверьте saveAdditional**
   ```java
   @Override
   protected void saveAdditional(CompoundTag tag) {
       super.saveAdditional(tag); // НЕ ЗАБЫВАЙТЕ super!
       tag.putLong("Energy", energy.getEnergyStored());
   }
   ```

2. **Проверьте load**
   ```java
   @Override
   public void load(CompoundTag tag) {
       super.load(tag); // НЕ ЗАБЫВАЙТЕ super!
       energy.setEnergy(tag.getLong("Energy"));
   }
   ```

3. **Проверьте тип NBT**
   ```java
   // НЕПРАВИЛЬНО - int вместо long
   tag.putInt("Energy", (int)energy.getEnergyStored());
   
   // ПРАВИЛЬНО - используйте long
   tag.putLong("Energy", energy.getEnergyStored());
   ```

### GUI показывает неверное количество энергии

**Проблема**: В GUI отображается странное значение энергии.

**Решения**:

1. **Используйте LongDataPacker для синхронизации**
   ```java
   // В BlockEntity
   public int getEnergyHigh() {
       return LongDataPacker.packHigh(energy.getEnergyStored());
   }
   
   public int getEnergyLow() {
       return LongDataPacker.packLow(energy.getEnergyStored());
   }
   
   // В Menu
   public long getEnergy() {
       return LongDataPacker.unpack(data.get(0), data.get(1));
   }
   ```

2. **Добавьте в ContainerData**
   ```java
   // Нужно минимум 2 слота для long
   this.data = new SimpleContainerData(2); // high, low
   ```

3. **Регулярно обновляйте данные**
   ```java
   // В serverTick BlockEntity
   if (level.getGameTime() % 20 == 0) { // Каждую секунду
       setChanged(); // Отправит обновление клиенту
   }
   ```

---

## Отладка

### Логирование энергии

```java
// Базовое логирование
LOGGER.info("Energy: {}/{} FE", 
    energy.getEnergyStored(), 
    energy.getMaxEnergyStored());

// С форматированием
LOGGER.info("Energy: {}", 
    EnergyFormatter.formatFE(energy.getEnergyStored()));

// Детальная информация о передаче
LOGGER.debug("Transferred {} FE from {} to {}, result: {}", 
    amount, fromPos, toPos, success);
```

### Проверка capability

```java
public static void debugCapability(BlockEntity be, Direction side) {
    if (be == null) {
        LOGGER.warn("BlockEntity is null");
        return;
    }
    
    // Проверка long-capability
    LazyOptional<ILongEnergyStorage> longCap = 
        be.getCapability(ModCapabilities.LONG_ENERGY, side);
    LOGGER.info("Has LONG_ENERGY ({}): {}", side, longCap.isPresent());
    
    longCap.ifPresent(storage -> {
        LOGGER.info("  Energy: {}/{} FE", 
            storage.getEnergyStored(), 
            storage.getMaxEnergyStored());
        LOGGER.info("  Can extract: {}, Can receive: {}", 
            storage.canExtract(), 
            storage.canReceive());
    });
    
    // Проверка forge-capability
    LazyOptional<IEnergyStorage> forgeCap = 
        be.getCapability(ForgeCapabilities.ENERGY, side);
    LOGGER.info("Has FORGE_ENERGY ({}): {}", side, forgeCap.isPresent());
}
```

### Отладка Wire Network

```java
// Добавьте в WireNetworkManager для отладки
public void debugNetwork(Level level, BlockPos wirePos) {
    PerLevel pl = levels.get(level);
    if (pl == null) {
        LOGGER.warn("No network for level");
        return;
    }
    
    synchronized(pl.graphLock) {
        BlockPos root = pl.find(wirePos);
        LOGGER.info("Wire {} belongs to component {}", wirePos, root);
        
        Set<BlockPos> members = pl.members.get(root);
        if (members != null) {
            LOGGER.info("Component has {} members", members.size());
        }
        
        List<EnergySource> sources = pl.sources.get(root);
        if (sources != null) {
            LOGGER.info("Component has {} energy sources", sources.size());
            for (EnergySource src : sources) {
                LOGGER.info("  Source at {}, side {}, priority {}", 
                    src.pos(), src.side(), src.priority());
            }
        }
    }
}
```

### Команда для отладки (добавьте в ваш Command класс)

```java
public class DebugEnergyCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("hbm_debug")
            .then(Commands.literal("energy")
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                    .executes(ctx -> {
                        BlockPos pos = BlockPosArgument.getBlockPos(ctx, "pos");
                        ServerLevel level = ctx.getSource().getLevel();
                        BlockEntity be = level.getBlockEntity(pos);
                        
                        if (be != null) {
                            ctx.getSource().sendSuccess(() -> 
                                Component.literal("=== Energy Debug for " + pos + " ==="), 
                                false);
                            
                            for (Direction dir : Direction.values()) {
                                debugCapability(be, dir);
                            }
                        }
                        
                        return 1;
                    })
                )
            )
        );
    }
}
```

---

## Лучшие практики

### 1. Проектирование машин

```java
public class BestPracticeMachine extends BlockEntity {
    // ✅ ХОРОШО: Константы для магических чисел
    private static final long ENERGY_CAPACITY = 50_000L;
    private static final long ENERGY_PER_OPERATION = 100L;
    private static final long MAX_RECEIVE = 1_000L;
    private static final int OPERATION_TIME = 200; // ticks
    
    // ✅ ХОРОШО: Final для неизменяемых полей
    private final BlockEntityEnergyStorage energy = 
        new BlockEntityEnergyStorage(ENERGY_CAPACITY, MAX_RECEIVE, 0L);
    
    private final LazyOptional<ILongEnergyStorage> energyCap = 
        LazyOptional.of(() -> energy);
    
    // ✅ ХОРОШО: Явные проверки
    public static void serverTick(Level level, BlockPos pos, 
                                  BlockState state, BestPracticeMachine be) {
        if (level.isClientSide()) return; // Защита от вызова на клиенте
        
        if (be.canWork()) {
            long needed = ENERGY_PER_OPERATION;
            long stored = be.energy.getEnergyStored();
            
            if (stored >= needed) {
                be.energy.extractEnergy(needed, false);
                be.doWork();
                be.setChanged();
            }
        }
    }
    
    // ✅ ХОРОШО: Валидация в setters
    public void setEnergy(long amount) {
        if (amount < 0) {
            LOGGER.warn("Attempted to set negative energy: {}", amount);
            amount = 0;
        }
        if (amount > ENERGY_CAPACITY) {
            amount = ENERGY_CAPACITY;
        }
        energy.setEnergy(amount);
    }
    
    // ✅ ХОРОШО: Cleanup в invalidateCaps
    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyCap.invalidate();
    }
}
```

### 2. Передача энергии

```java
// ✅ ХОРОШО: Проверяйте и возвращайте неиспользованное
public void transferEnergy(ILongEnergyStorage from, ILongEnergyStorage to, long max) {
    if (!from.canExtract() || !to.canReceive()) return;
    
    long extracted = from.extractEnergy(max, false);
    if (extracted > 0) {
        long received = to.receiveEnergy(extracted, false);
        
        // Возвращаем то, что не приняли
        if (received < extracted) {
            from.receiveEnergy(extracted - received, false);
        }
    }
}

// ❌ ПЛОХО: Не проверяете и не возвращаете
public void transferEnergyBad(ILongEnergyStorage from, ILongEnergyStorage to, long max) {
    from.extractEnergy(max, false); // Энергия теряется!
    to.receiveEnergy(max, false);   // Может принять меньше!
}
```

### 3. NBT сериализация

```java
// ✅ ХОРОШО: Версионирование NBT
@Override
protected void saveAdditional(CompoundTag tag) {
    super.saveAdditional(tag);
    tag.putInt("Version", 1); // Версия формата
    tag.putLong("Energy", energy.getEnergyStored());
    tag.putInt("Progress", progress);
}

@Override
public void load(CompoundTag tag) {
    super.load(tag);
    
    int version = tag.getInt("Version");
    if (version == 1) {
        energy.setEnergy(tag.getLong("Energy"));
        progress = tag.getInt("Progress");
    } else {
        // Миграция со старой версии или дефолтные значения
        LOGGER.warn("Unknown NBT version: {}, using defaults", version);
    }
}
```

### 4. Работа с capability

```java
// ✅ ХОРОШО: Кэшируйте и обновляйте
public class SmartMachine extends BlockEntity {
    private final Map<Direction, LazyOptional<ILongEnergyStorage>> neighborCaps = 
        new EnumMap<>(Direction.class);
    
    @Override
    public void onLoad() {
        super.onLoad();
        scheduleCapabilityRefresh();
    }
    
    private void scheduleCapabilityRefresh() {
        // Обновляем кэш каждые 5 секунд
        if (level != null && !level.isClientSide()) {
            if (level.getGameTime() % 100 == 0) {
                refreshNeighborCapabilities();
            }
        }
    }
    
    private void refreshNeighborCapabilities() {
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = worldPosition.relative(dir);
            BlockEntity neighbor = level.getBlockEntity(neighborPos);
            
            if (neighbor != null) {
                neighborCaps.put(dir, 
                    neighbor.getCapability(ModCapabilities.LONG_ENERGY, dir.getOpposite()));
            } else {
                neighborCaps.remove(dir);
            }
        }
    }
}

// ❌ ПЛОХО: Получаете capability каждый тик
public void badPushEnergy() {
    for (Direction dir : Direction.values()) {
        BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(dir));
        if (neighbor != null) {
            // Каждый тик создается новый LazyOptional!
            neighbor.getCapability(ModCapabilities.LONG_ENERGY, dir.getOpposite())
                .ifPresent(storage -> {
                    // ...
                });
        }
    }
}
```

---

## Оптимизация производительности

### 1. Снижение нагрузки на tick

```java
// ✅ ХОРОШО: Распределяйте операции по тикам
private int tickCounter = 0;

public static void serverTick(Level level, BlockPos pos, 
                              BlockState state, OptimizedMachine be) {
    be.tickCounter++;
    
    // Передача энергии только каждые 5 тиков
    if (be.tickCounter % 5 == 0) {
        be.distributeEnergy();
    }
    
    // Обновление capability только каждые 100 тиков (5 секунд)
    if (be.tickCounter % 100 == 0) {
        be.refreshCapabilities();
    }
    
    // Основная работа каждый тик
    if (be.canWork()) {
        be.doWork();
    }
}
```

### 2. Пакетная передача энергии

```java
// ✅ ХОРОШО: Передавайте большими порциями
private long energyBuffer = 0;
private static final long TRANSFER_THRESHOLD = 1000L;

public void bufferAndTransfer() {
    energyBuffer += generateEnergy(); // Накапливаем
    
    if (energyBuffer >= TRANSFER_THRESHOLD) {
        // Передаем только когда накопили достаточно
        long transferred = distributeEnergy(energyBuffer);
        energyBuffer -= transferred;
    }
}

// ❌ ПЛОХО: Передавайте по 1 FE за раз
public void inefficientTransfer() {
    for (long i = 0; i < generateEnergy(); i++) {
        distributeEnergy(1L); // Много вызовов!
    }
}
```

### 3. Ленивые вычисления

```java
// ✅ ХОРОШО: Вычисляйте только когда нужно
private boolean cachedCanWork = false;
private long lastCanWorkCheck = 0;

public boolean canWork() {
    long now = level.getGameTime();
    
    // Кэшируем результат на 20 тиков
    if (now - lastCanWorkCheck >= 20) {
        cachedCanWork = checkCanWorkExpensive();
        lastCanWorkCheck = now;
    }
    
    return cachedCanWork;
}

private boolean checkCanWorkExpensive() {
    // Дорогие проверки здесь
    return hasInputItems() && hasOutputSpace() && hasEnergy();
}
```

### 4. Оптимизация Wire Network

```java
// Используйте wire network вместо прямых подключений
// когда машин больше 5-10

// ❌ ПЛОХО: 10 машин напрямую соединены
// Generator -> Machine1, Machine2, Machine3, ..., Machine10
// Каждая связь проверяется каждый тик = много операций

// ✅ ХОРОШО: Используйте провода
// Generator -> Wire -> [Wire Network] -> Machine1..10
// Один запрос к сети вместо 10 проверок capability
```

---

## Совместимость

### С Forge Energy (int-based)

```java
// Автоматическая совместимость через обертки
public class CompatibleMachine extends BlockEntity {
    private final BlockEntityEnergyStorage energy = 
        new BlockEntityEnergyStorage(1_000_000L, 10_000L, 10_000L);
    
    private final LazyOptional<ILongEnergyStorage> longCap = 
        LazyOptional.of(() -> energy);
    
    // Создаем обертку для Forge Energy
    private final LazyOptional<IEnergyStorage> forgeCap = 
        longCap.lazyMap(LongToForgeWrapper::new);
    
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        // Поддерживаем обе системы!
        if (cap == ModCapabilities.LONG_ENERGY) {
            return longCap.cast();
        }
        if (cap == ForgeCapabilities.ENERGY) {
            return forgeCap.cast();
        }
        return super.getCapability(cap, side);
    }
}
```

### С другими модами

```java
// Проверяйте обе capability при работе с соседями
public void detectEnergyNeighbors() {
    for (Direction dir : Direction.values()) {
        BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(dir));
        if (neighbor == null) continue;
        
        // Сначала пробуем нашу систему
        LazyOptional<ILongEnergyStorage> longCap = 
            neighbor.getCapability(ModCapabilities.LONG_ENERGY, dir.getOpposite());
        
        if (longCap.isPresent()) {
            // Используем long-систему
            longCap.ifPresent(this::workWithLongEnergy);
        } else {
            // Пробуем Forge Energy
            LazyOptional<IEnergyStorage> forgeCap = 
                neighbor.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite());
            
            forgeCap.ifPresent(forgeStorage -> {
                // Оборачиваем и используем
                ILongEnergyStorage wrapped = new ForgeToLongWrapper(forgeStorage);
                workWithLongEnergy(wrapped);
            });
        }
    }
}
```

---

## Чек-лист для новой машины

- [ ] Создан BlockEntity с energy storage
- [ ] Зарегистрирован BlockEntityType
- [ ] Реализован getCapability для ModCapabilities.LONG_ENERGY
- [ ] Добавлен invalidateCaps с invalidate() для energyCap
- [ ] Реализованы saveAdditional и load для сохранения энергии
- [ ] Добавлена логика работы в serverTick
- [ ] Проверяется canExtract/canReceive перед передачей
- [ ] Возвращается неиспользованная энергия
- [ ] Добавлен Menu с ContainerData для синхронизации
- [ ] Создан Screen с отображением энергии
- [ ] Протестировано сохранение/загрузка
- [ ] Протестирована передача энергии
- [ ] Проверена совместимость с другими модами

---

## Полезные ссылки

- `docs/ENERGY_SYSTEM.md` - Полная документация
- `docs/MACHINE_TUTORIAL.md` - Пошаговый туториал
- `docs/API_REFERENCE.md` - API справочник
- `docs/EnergySystemExamples.java` - Примеры кода

## Поддержка

Если вы столкнулись с проблемой, которой нет в этом руководстве:

1. Проверьте логи на предупреждения и ошибки
2. Включите debug-логирование для энергосистемы
3. Создайте issue на GitHub с подробным описанием и логами
4. Присоединяйтесь к Discord для оперативной помощи

**GitHub Issues**: https://github.com/Raptor324/HBM-Modernized/issues
**Discord**: https://discord.gg/kYW4JBtUDn
