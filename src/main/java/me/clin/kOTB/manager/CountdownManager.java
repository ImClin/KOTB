package me.clin.kOTB.manager;

import me.clin.kOTB.KOTB;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.Collection;

public class CountdownManager {

    private final KOTB plugin;

    public CountdownManager(KOTB plugin) {
        this.plugin = plugin;
    }

    public BukkitRunnable runMatchCountdown(Collection<Player> players, Runnable onComplete) {
        BukkitRunnable runnable = new BukkitRunnable() {
            private int timer = 5;

            @Override
            public void run() {
                if (timer <= 0) {
                    sendTitle(players, "§c§lGAAN!", "§4Succes!");
                    if (onComplete != null) {
                        onComplete.run();
                    }
                    cancel();
                    return;
                }

                String subtitle = timer <= 2 ? "§4§l" + timer : "§c§l" + timer;
                sendTitle(players, "§c§lMaak je klaar!", subtitle);
                timer--;
            }
        };
        runnable.runTaskTimer(plugin, 0L, 20L);
        return runnable;
    }

    private void sendTitle(Collection<Player> players, String title, String subtitle) {
        if (players == null) {
            return;
        }
        for (Player player : players) {
            if (player != null && player.isOnline()) {
                Title.Times times = Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(1500), Duration.ofMillis(250));
                Component titleComponent = deserialize(title);
                Component subtitleComponent = deserialize(subtitle);
                player.showTitle(Title.title(titleComponent, subtitleComponent, times));
            }
        }
    }

    private Component deserialize(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return LegacyComponentSerializer.legacySection().deserialize(text);
    }
}
