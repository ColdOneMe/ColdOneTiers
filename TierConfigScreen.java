package com.coldonetiers;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Окно настроек: режим, где показывать, ссылка на сайт. */
public class TierConfigScreen extends Screen {
    private static final String[] MODE_IDS = new String[TierMode.values().length + 1];

    static {
        MODE_IDS[0] = "highest";
        for (int i = 0; i < TierMode.values().length; i++) MODE_IDS[i + 1] = TierMode.values()[i].id;
    }

    private final Screen parent;
    private EditBox urlBox;
    private Button modeButton;
    private Button nameTagButton;
    private Button tabButton;
    private Button textButton;

    public TierConfigScreen(Screen parent) {
        super(Component.literal("ColdOneTiers — настройки"));
        this.parent = parent;
    }

    private static String modeLabel(String id) {
        TierMode m = TierMode.byId(id);
        return m == null ? "Лучший тир игрока" : m.label;
    }

    private static Component onOff(String name, boolean v) {
        return Component.literal(name + ": " + (v ? "ВКЛ" : "ВЫКЛ"));
    }

    @Override
    protected void init() {
        TierConfig cfg = ColdOneTiersClient.config();
        int w = 260;
        int x = this.width / 2 - w / 2;
        int y = this.height / 6 + 8;

        modeButton = Button.builder(Component.literal("Режим: " + modeLabel(cfg.mode)), b -> {
            int idx = 0;
            for (int i = 0; i < MODE_IDS.length; i++) if (MODE_IDS[i].equals(cfg.mode)) idx = i;
            cfg.mode = MODE_IDS[(idx + 1) % MODE_IDS.length];
            cfg.save();
            b.setMessage(Component.literal("Режим: " + modeLabel(cfg.mode)));
        }).bounds(x, y, w, 20).build();
        addRenderableWidget(modeButton);

        nameTagButton = Button.builder(onOff("Над головой", cfg.showNameTag), b -> {
            cfg.showNameTag = !cfg.showNameTag;
            cfg.save();
            b.setMessage(onOff("Над головой", cfg.showNameTag));
        }).bounds(x, y + 26, w, 20).build();
        addRenderableWidget(nameTagButton);

        tabButton = Button.builder(onOff("В списке игроков (Tab)", cfg.showTab), b -> {
            cfg.showTab = !cfg.showTab;
            cfg.save();
            b.setMessage(onOff("В списке игроков (Tab)", cfg.showTab));
        }).bounds(x, y + 52, w, 20).build();
        addRenderableWidget(tabButton);

        textButton = Button.builder(onOff("Текст тира (HT3)", cfg.showTierText), b -> {
            cfg.showTierText = !cfg.showTierText;
            cfg.save();
            b.setMessage(onOff("Текст тира (HT3)", cfg.showTierText));
        }).bounds(x, y + 78, w, 20).build();
        addRenderableWidget(textButton);

        urlBox = new EditBox(this.font, x, y + 120, w, 20, Component.literal("Ссылка на сайт"));
        urlBox.setMaxLength(512);
        urlBox.setValue(cfg.dataUrl == null ? "" : cfg.dataUrl);
        addRenderableWidget(urlBox);

        addRenderableWidget(Button.builder(Component.literal("Обновить данные сейчас"), b -> {
            cfg.dataUrl = urlBox.getValue().trim();
            cfg.save();
            ColdOneTiersClient.refreshAsync();
        }).bounds(x, y + 146, w, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Готово"), b -> onClose())
                .bounds(x, y + 180, w, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        int cx = this.width / 2;
        int y = this.height / 6 + 8;
        g.drawCenteredString(this.font, this.title, cx, y - 24, 0xFFFFFFFF);
        g.drawCenteredString(this.font, Component.literal("Ссылка на сайт тир-листа (страница или JSON):"), cx, y + 108, 0xFFAAAAAA);
        g.drawCenteredString(this.font, Component.literal(ColdOneTiersClient.lastStatus()), cx, y + 206, 0xFF8FE3FF);
    }

    @Override
    public void onClose() {
        TierConfig cfg = ColdOneTiersClient.config();
        String url = urlBox == null ? cfg.dataUrl : urlBox.getValue().trim();
        boolean changed = !url.equals(cfg.dataUrl);
        cfg.dataUrl = url;
        cfg.save();
        if (changed) ColdOneTiersClient.refreshAsync();
        this.minecraft.setScreen(parent);
    }
}
