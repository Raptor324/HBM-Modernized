package com.hbm_m.item;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum ModIngots {
    // Конструктор принимает название и пары "язык, перевод"
    URANIUM("uranium",
            "ru_ru", "Урановый слиток",
            "en_us", "Uranium Ingot"),
    URANIUM233("u233",
            "ru_ru", "Слиток урана-233",
            "en_us", "Uranium-233 Ingot"),
    URANIUM235("u235",
            "ru_ru", "Слиток урана-235",
            "en_us", "Uranium-235 Ingot"),
    URANIUM238("u238",
            "ru_ru", "Слиток урана-238",
            "en_us", "Uranium-238 Ingot"),
    THORIUM232("th232",
            "ru_ru", "Слиток тория-232",
            "en_us", "Thorium-232 Ingot"),
    PLUTONIUM("plutonium",
            "ru_ru", "Плутониевый слиток",
            "en_us", "Plutonium Ingot"),
    PLUTONIUM238("pu238",
            "ru_ru", "Слиток плутония-238",
            "en_us", "Plutonium-238 Ingot"),
    PLUTONIUM239("pu239",
            "ru_ru", "Слиток плутония-239",
            "en_us", "Plutonium-239 Ingot"),
    PLUTONIUM240("pu240",
            "ru_ru", "Слиток плутония-240",
            "en_us", "Plutonium-240 Ingot"),
    PLUTONIUM241("pu241",
            "ru_ru", "Слиток плутония-241",
            "en_us", "Plutonium-241 Ingot"),
    ACTINIUM("actinium",
            "ru_ru", "Слиток актиния-227",
            "en_us", "Actinium Ingot"),
    STEEL("steel",
            "ru_ru", "Стальной слиток",
            "en_us", "Steel Ingot"),
    ADVANCED_ALLOY("advanced_alloy",
            "ru_ru", "Слиток продвинутого сплава",
            "en_us", "Advanced Alloy Ingot"),
    ALUMINIUM("aluminium",
            "ru_ru", "Слиток алюминия",
            "en_us", "Aluminum Ingot"),
    SCHRABIDIUM("schrabidium",
            "ru_ru", "Шрабидиевый слиток",
            "en_us", "Schrabidium Ingot"),
    SATURNITE("saturnite",
            "ru_ru", "Сатурнитовый слиток",
            "en_us", "Saturnite Ingot"),
    LEAD("lead",
            "ru_ru", "Свинцовый слиток",
            "en_us", "Lead Ingot"),
    GUNMETAL("gunmetal",
            "ru_ru", "Cлиток пушечной бронзы",
            "en_us", "Gunmetal Ingot"),
    GUNSTEEL("gunsteel",
            "ru_ru", "Слиток оружейной стали",
            "en_us", "Gunsteel Ingot");
            
    // Чтобы добавить новый слиток, просто добавьте новую запись с его переводами

    private final String name;
    private final Map<String, String> translations;

    /**
     * Конструктор для слитков с поддержкой нескольких языков.
     * @param name Системное имя (например, "uranium")
     * @param translationPairs Пары строк: "код_языка", "перевод". Например: "ru_ru", "Слиток", "en_us", "Ingot"
     */
    ModIngots(String name, String... translationPairs) {
        this.name = name;
        
        // Проверка, что количество элементов четное (пары ключ-значение)
        if (translationPairs.length % 2 != 0) {
            throw new IllegalArgumentException("Translation pairs must be even for ingot: " + name);
        }

        Map<String, String> translationMap = new HashMap<>();
        for (int i = 0; i < translationPairs.length; i += 2) {
            String locale = translationPairs[i];
            String translation = translationPairs[i + 1];
            translationMap.put(locale, translation);
        }
        // Делаем карту неизменяемой после создания
        this.translations = Collections.unmodifiableMap(translationMap);
    }

    public String getName() {
        return name;
    }

    /**
     * Получает перевод для указанного языка.
     * @param locale Код языка (например, "ru_ru").
     * @return Перевод или null, если он не найден.
     */
    public String getTranslation(String locale) {
        return translations.get(locale);
    }
}