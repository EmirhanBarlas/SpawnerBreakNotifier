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

public class SpawnerBreakNotifier extends JavaPlugin implements Listener {

    private WebhookClient client;
    private String webhookUrl;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();

        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        if (client != null) {
            client.close();
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        if (block.getType() == Material.SPAWNER) {
            String playerName = player.getName();
            int x = block.getX();
            int y = block.getY();
            int z = block.getZ();

            sendDiscordMessage(playerName, x, y, z);
        }
    }

    private void sendDiscordMessage(String playerName, int x, int y, int z) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            getLogger().warning("Discord Webhook URL is not set. Please set the webhook URL in the plugin.");
            return;
        }
        if (client == null) {
            client = new WebhookClientBuilder(webhookUrl).build();
        }

        WebhookEmbedBuilder embed = new WebhookEmbedBuilder()
                .setDescription(("**SPAWNER KIRDI**" + "\nOyuncu: " + playerName + "\nKoordinatlar: X: " + x + ", Y: " + y + ", Z: " + z))
                .setColor(0xFF0000);

        client.send(new WebhookMessageBuilder().addEmbeds(embed.build()).build());
    }

    private void loadConfig() {
        FileConfiguration config = getConfig();
        webhookUrl = config.getString("webhookUrl");
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            getLogger().warning("Discord Webhook URL is not set. Please set the webhook URL in the config.yml file.");
        }
    }
}
