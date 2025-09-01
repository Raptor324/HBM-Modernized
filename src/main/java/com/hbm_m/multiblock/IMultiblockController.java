package com.hbm_m.multiblock;

/**
 * Интерфейс для блоков, которые являются контроллерами мультиблочных структур.
 * Позволяет универсальному BlockItem получить доступ к хелперу структуры.
 */
public interface IMultiblockController {
    /**
     * @return Экземпляр MultiblockStructureHelper, описывающий данную структуру.
     */
    MultiblockStructureHelper getStructureHelper();
}