package org.splendid.spawnerbreaknotifier;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class SpawnerBreakNotifier extends JavaPlugin implements Listener {

    private WebhookClient client;
    private String webhookUrl;
    private Map<Material, String> blockMessages = new HashMap<>();

    @Override
    public void onEnable() {
        // Config dosyasını kontrol et ve eksikse oluştur
        saveDefaultConfig();

        // Config dosyasını yükle
        loadConfig();

        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        // Webhook client'ı kapat
        if (client != null) {
            client.close();
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material blockType = block.getType();

        // Kontrol etmek istediğiniz bloklar
        if (blockMessages.containsKey(blockType)) {
            String playerName = player.getName();
            int x = block.getX();
            int y = block.getY();
            int z = block.getZ();

            sendDiscordMessage(playerName, x, y, z, blockType);
        }
    }

    private void sendDiscordMessage(String playerName, int x, int y, int z, Material blockType) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            getLogger().warning("Discord Webhook URL is not set. Please set the webhook URL in the config.yml file.");
            return;
        }

        // Webhook client'ı oluştur (her seferinde yeni bir client oluşturmanıza gerek yok)
        if (client == null) {
            client = new WebhookClientBuilder(webhookUrl).build();
        }

        // Mesajı config dosyasından al
        String message = blockMessages.get(blockType);

        // Mesaj içindeki değişkenleri doldur
        message = message.replace("{player}", playerName)
                .replace("{x}", String.valueOf(x))
                .replace("{y}", String.valueOf(y))
                .replace("{z}", String.valueOf(z));

        WebhookEmbedBuilder embed = new WebhookEmbedBuilder()
                .setDescription(message)
                .setColor(0xFF0000);

        client.send(new WebhookMessageBuilder().addEmbeds(embed.build()).build());
    }

    private void loadConfig() {
        // Config dosyasını al
        FileConfiguration config = getConfig();

        // Config dosyasındaki webhookUrl değerini al
        webhookUrl = config.getString("webhookUrl");

        // Eğer webhookUrl değeri null veya boşsa uyarı ver
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            getLogger().warning("Discord Webhook URL is not set. Please set the webhook URL in the config.yml file.");
        }

        // Blok mesajlarını yükle
        blockMessages.clear();
        for (String blockType : config.getConfigurationSection("blockMessages").getKeys(false)) {
            Material material = Material.matchMaterial(blockType);
            String message = config.getString("blockMessages." + blockType);
            blockMessages.put(material, message);
        }
    }
}
