package com.hbm_m.hazard;

import java.util.ArrayList;
import java.util.List;

/**
 * Контейнер для набора правил об опасностях, применяемых к предмету или тегу.
 * Содержит список опасностей и правила их применения (переопределение, мьютекс).
 */

public final class HazardData {
    public final List<HazardEntry> entries;
    public boolean doesOverride = false;
    private int mutex = 0; // Для системы взаимного исключения

    public HazardData(HazardEntry... entries) {
        this.entries = new ArrayList<>(List.of(entries));
    }

    /**
     * Делает это правило переопределяющим.
     * Все правила с более низким приоритетом для этого предмета будут проигнорированы.
     */
    public HazardData setAsOverride() {
        this.doesOverride = true;
        return this;
    }

    // В будущем здесь можно добавить методы для настройки мьютекса
    public int getMutex() {
        return mutex;
    }
}