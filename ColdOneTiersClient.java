package com.coldonetiers;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

public class ColdOneTiersClient implements ClientModInitializer {
    public static final String MOD_ID = "coldonetiers";

    private static TierConfig config;
    private static final TierDatabase DATABASE = new TierDatabase();
    private static final AtomicBoolean REFRESHING = new AtomicBoolean(false);
    private static volatile String lastStatus = "";

    private static Path cacheFile;
    private static int openScreenIn = -1;
    private static int ticksSinceRefresh = 0;

    public static TierConfig config() {
        return config;
    }

    public static TierDatabase database() {
        return DATABASE;
    }

    public static String lastStatus() {
        return lastStatus;
    }

    @Override
    public void onInitializeClient() {
        Path dir = FabricLoader.getInstance().getConfigDir();
        config = TierConfig.load(dir.resolve("coldonetiers.json"));
        cacheFile = dir.resolve("coldonetiers-cache.json");

        DATABASE.loadBundled();
        DATABASE.loadCache(cacheFile);
        refreshAsync();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (openScreenIn >= 0 && --openScreenIn < 0) {
                client.setScreen(new TierConfigScreen(null));
            }
            if (++ticksSinceRefresh >= Math.max(1, config.refreshMinutes) * 60 * 20) {
                refreshAsync();
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(LiteralArgumentBuilder.<FabricClientCommandSource>literal("coldtiers")
                        .executes(ctx -> {
                            openScreenIn = 2; // откроем на следующих тиках, иначе чат закроет окно
                            return 1;
                        })
                        .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("reload")
                                .executes(ctx -> {
                                    refreshAsync();
                                    ctx.getSource().sendFeedback(Component.literal("[ColdOneTiers] Обновляю список игроков..."));
                                    return 1;
                                }))));
    }

    /** Открыть окно настроек (через пару тиков). */
    public static void scheduleOpenSettings() {
        openScreenIn = 2;
    }

    /** Загружает свежие данные с сайта в фоне. Без ссылки в настройках используется встроенный список. */
    public static void refreshAsync() {
        ticksSinceRefresh = 0;
        String url = config == null ? "" : config.dataUrl;
        if (url == null || url.isBlank()) {
            lastStatus = "Ссылка не задана — используется встроенный список (" + DATABASE.size() + " игр.)";
            return;
        }
        if (!REFRESHING.compareAndSet(false, true)) return;
        Thread t = new Thread(() -> {
            try {
                int n = DATABASE.fetch(url, cacheFile);
                lastStatus = "Загружено игроков: " + n;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                lastStatus = "Прервано";
            } catch (Exception e) {
                lastStatus = "Ошибка: " + e.getMessage();
            } finally {
                REFRESHING.set(false);
            }
        }, "ColdOneTiers-Refresh");
        t.setDaemon(true);
        t.start();
    }
}
