# Fluid Pipe Parity Test Matrix

## 1. Линейная труба (базовый flow)
- [ ] Provider → 1 труба → Receiver: передача каждый тик
- [ ] Provider > capacity: Receiver заполняется, излишек у provider
- [ ] Provider EMPTY: ничего не передаётся
- [ ] Receiver FULL: provider сохраняет fluid

## 2. Ветвление сети
- [ ] 1→2 равный приоритет: 50/50 пропорционально demand
- [ ] HIGH+NORMAL: HIGH первый, NORMAL — остаток
- [ ] 2→1: списание пропорционально доступному объёму
- [ ] Кольцо: одна сеть без дублирования

## 3. Давление
- [ ] pressure=0+0: передача работает
- [ ] pressure=1+0: НЕ передаётся
- [ ] Forge machine: только pressure=0

## 4. Клапан
- [ ] Открыт: работает как труба
- [ ] Закрыт (ред.камень): граф разорван
- [ ] Открыть в живой сети: граф восстановлен через тик

## 5. Насос
- [ ] Без ред.камня: вход→выход
- [ ] С ред.камнем: только вход
- [ ] Буфер полный: getDemand()=0
- [ ] priority=HIGH: получает раньше NORMAL

## 6. Выхлоп
- [ ] SMOKE → exhaust: canConnect=true
- [ ] WATER → exhaust: canConnect=false
- [ ] 3 типа дыма: 3 отдельных FluidNode

## 7. Fluid identifier
- [ ] Shift+use: BFS-покраска сети
- [ ] Смена типа: узел пересоздаётся
- [ ] NONE: очищает тип, узел удаляется

## 8. Persistence
- [ ] Чанк reload: FluidType из NBT, узел из onLoad()
- [ ] Server restart: UniNodespace перестраивается
- [ ] Насос буфер: сохранён в NBT

## 9. Edge cases
- [ ] 512 труб: < 1 мс/тик
- [ ] Timeout 3000 мс: подписка удаляется
- [ ] Forge machine без IFluidHandler: адаптер не создаётся

## Намеренные отклонения от 1.7.10
- Forge-машины: только pressure=0 (IFluidHandler API)
- Duct не экспонирует IFluidHandler (MK2-мост через ForgeFluidHandlerAdapter)
- Phase.END (1.20.1) вместо Phase.START (1.7.10)
- null вместо UNKNOWN для Direction
