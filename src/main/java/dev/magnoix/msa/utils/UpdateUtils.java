package dev.magnoix.msa.utils;

import com.google.gson.annotations.SerializedName;
import com.google.gson.Gson;
import dev.magnoix.msa.MSA;
import dev.magnoix.msa.messages.Msg;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;

public class UpdateUtils {
    private static final Gson GSON = new Gson();

    public static GitHubRelease latestRelease;
    public static LocalDateTime latestReleaseCheck;

    private static void getLatestRelease(Consumer<GitHubRelease> callback) {
        MSA plugin = MSA.getInstance();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URI("https://api.github.com/repos/magnite5/MyServerAlive/releases/latest").toURL();
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.setRequestProperty("User-Agent", "MyServerAlive-UpdateChecker");
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json");

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    try (InputStreamReader reader = new InputStreamReader(connection.getInputStream())) {
                        GitHubRelease release = GSON.fromJson(reader, GitHubRelease.class);
                        latestRelease = release;
                        latestReleaseCheck = LocalDateTime.now();
                        Bukkit.getScheduler().runTask(plugin, () -> callback.accept(release));
                    }
                } else {
                    Msg.log("Failed to get latest GitHub Release. Response code: " + connection.getResponseCode());
                }
            } catch (IOException | URISyntaxException e) {
                Msg.log("Failed to check for updates: " + e.getMessage());
            }
        });
    }

    public static void scheduleUpdateCheck() {
        new BukkitRunnable() {
            @Override
            public void run() {
                getLatestRelease(release -> {
                    Msg.log(Level.INFO, "Checking for updates...");
                    if (release == null || release.getDownloadUrl() == null) {
                        Msg.log(Level.WARNING, "Failed to get latest release");
                        return;
                    }

                    String currentVersion = MSA.getInstance().getPluginMeta().getVersion();

                    if (!currentVersion.equalsIgnoreCase(release.getTagName())) {
                        Msg.log(Level.INFO, "New release available: " + release.getTagName());
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            if (player.hasPermission("msa.updates") || player.isOp()) {
                                Msg.miniMsg("<gold>A new version of MSA is available. <aqua>" + currentVersion + "<dark_aqua> -> <gold>" + release.getTagName(), player);
                                Msg.miniMsg("<gold>Download it <aqua><u><click:open_url:" + release.getDownloadUrl() + ">here</click></u>", player);
                            }
                        }
                    } else {
                        Msg.log(Level.INFO, "No new release found.");
                    }
                });
            }
        }.runTaskTimer(MSA.getInstance(), 0L, 432000L); // 6 hours.
    }

    public static class GitHubRelease {
        @SerializedName("tag_name")
        private String tagName;

        @SerializedName("name")
        private String name;

        @SerializedName("assets")
        private List<Asset> assets;

        public String getTagName() {
            return tagName;
        }

        public String getDownloadUrl() {
            return assets.stream()
                    .map(asset -> asset.downloadUrl)
                    .filter(url -> url.endsWith(".jar"))
                    .findFirst()
                    .orElse(null);
        }

        private static class Asset {
            @SerializedName("browser_download_url")
            private String downloadUrl;
        }
    }
}
