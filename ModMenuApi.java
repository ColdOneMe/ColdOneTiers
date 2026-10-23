package com.terraformersmc.modmenu.api;

/** Заглушка только для компиляции (в jar не попадает). Настоящий класс даёт Mod Menu. */
public interface ModMenuApi {
    default ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> null;
    }
}
