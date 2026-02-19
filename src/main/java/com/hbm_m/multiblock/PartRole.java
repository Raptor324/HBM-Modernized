package com.hbm_m.multiblock;

import net.minecraft.util.StringRepresentable;

public enum PartRole implements StringRepresentable {
    DEFAULT("default"),
    ENERGY_CONNECTOR("energy_connector"),
    FLUID_CONNECTOR("fluid_connector"),
    ITEM_INPUT("item_input"),
    ITEM_OUTPUT("item_output"),
    /**
     * Универсальный коннектор.
     * - Принимает энергию (как ENERGY_CONNECTOR)
     * - Является точкой подключения fluid/item conveyor систем
     * - САМ по себе НЕ передаёт и не принимает fluid/items
     * - Передача предметов/жидкостей происходит через связанные conveyor блоки
     */
    UNIVERSAL_CONNECTOR("universal_connector"),
    /**
     * Блок-часть, по которому можно взобраться как по лестнице.
     * Работает с любых боковых сторон.
     */
    LADDER("ladder"),
    /**
     * Блок контроллера мультиблочной структуры.
     * Может быть размещён в любой позиции структуры (не только в центре).
     * ОБЯЗАТЕЛЬНО должен быть ровно ОДИН контроллер в структуре.
     */
    CONTROLLER("controller");
    
    private final String name;
    
    PartRole(String name) {
        this.name = name;
    }
    
    @Override
    public String getSerializedName() {
        return this.name;
    }
    
    /**
     * Проверяет, может ли эта роль принимать энергию.
     */
    public boolean canReceiveEnergy() {
        return this == ENERGY_CONNECTOR || this == UNIVERSAL_CONNECTOR;
    }
    
    /**
     * Проверяет, может ли эта роль отдавать энергию.
     */
    public boolean canSendEnergy() {
        return this == ENERGY_CONNECTOR || this == UNIVERSAL_CONNECTOR;
    }
    
    /**
     * Проверяет, является ли эта роль точкой подключения conveyor системы.
     * САМ по себе НЕ передаёт предметы/жидкости - только через связанные блоки.
     */
    public boolean isConveyorConnectionPoint() {
        return this == UNIVERSAL_CONNECTOR;
    }
    
    /**
     * Проверяет, может ли эта роль использоваться как лестница (climbable).
     */
    public boolean isLadder() {
        return this == LADDER;
    }
    
    /**
     * Проверяет, является ли эта роль контроллером структуры.
     */
    public boolean isController() {
        return this == CONTROLLER;
    }
}
