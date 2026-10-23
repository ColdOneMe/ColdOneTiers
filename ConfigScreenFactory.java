package com.terraformersmc.modmenu.api;

import net.minecraft.client.gui.screens.Screen;

/** Заглушка только для компиляции (в jar не попадает). Настоящий класс даёт Mod Menu. */
@FunctionalInterface
public interface ConfigScreenFactory<S extends Screen> {
    S create(Screen parent);
}
