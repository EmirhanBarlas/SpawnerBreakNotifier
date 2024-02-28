package org.splendid.spawnerbreaknotifier;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.List;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class SpawnerBreakNotifier extends JavaPlugin implements Listener {
    private String mysqlHost;
    private int mysqlPort;
    private String mysqlDatabase;
    private String mysqlUsername;
    private String mysqlPassword;
    private Connection connection;
    private Map<Material, BlockMessageInfo> blockMessages = new HashMap<>();
    private WebhookClient client;
    private String webhookUrl;

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        World world = event.getPlayer().getWorld();
        Material blockType = block.getType();
        String worldName = block.getWorld().getName();
        List<String> allowedWorlds = getConfig().getStringList("world");
        if (allowedWorlds.contains(worldName) && blockMessages.containsKey(blockType)) {
            String playerName = player.getName();
            String world1 = world.getName();
            int x = block.getX();
            int y = block.getY();
            int z = block.getZ();

            sendDiscordMessage(playerName, worldName, x, y, z, blockType);
            player.sendMessage(blockMessages.get(blockType).getMessage().replace("{player}", playerName).replace("{x}", String.valueOf(x)).replace("{y}", String.valueOf(y)).replace("{z}", String.valueOf(z)));
        }
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();

        // Establish MySQL connection, if it doesn't exist
        FileConfiguration config = getConfig();
        mysqlHost = config.getString("mysql.host");
        mysqlPort = config.getInt("mysql.port");
        mysqlDatabase = config.getString("mysql.database");
        mysqlUsername = config.getString("mysql.username");
        mysqlPassword = config.getString("mysql.password");

        // Establish MySQL connection
        try {
            connection = DriverManager.getConnection("jdbc:mysql://" + mysqlHost + ":" + mysqlPort + "/" + mysqlDatabase, mysqlUsername, mysqlPassword);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    @Override
    public void onDisable() {
        if (client != null) {
            client.close();
        }
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private void sendDiscordMessage(String playerName, String worldName, int x, int y, int z, Material blockType) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            getLogger().warning("Discord Webhook URL is not set. Please set the webhook URL in the config.yml file.");
            return;
        }
        if (client == null) {
            client = new WebhookClientBuilder(webhookUrl).build();
        }
        BlockMessageInfo messageInfo = blockMessages.get(blockType);
        String message = messageInfo.getMessage()
                .replace("{player}", playerName)
                .replace("{x}", String.valueOf(x))
                .replace("{y}", String.valueOf(y))
                .replace("{z}", String.valueOf(z))
                .replace("{world}", worldName);

        WebhookEmbedBuilder embed = new WebhookEmbedBuilder()
                .setDescription(message)
                .setColor(messageInfo.getColor());

        client.send(new WebhookMessageBuilder().addEmbeds(embed.build()).build());

        try {
            String insertSql = "INSERT INTO block_break_events (player_name, world_name, x, y, z, block_type) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement preparedStatement = connection.prepareStatement(insertSql);
            preparedStatement.setString(1, playerName);
            preparedStatement.setString(2, worldName);
            preparedStatement.setInt(3, x);
            preparedStatement.setInt(4, y);
            preparedStatement.setInt(5, z);
            preparedStatement.setString(6, blockType.name());
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadConfig() {
        FileConfiguration config = getConfig();
        webhookUrl = config.getString("webhookUrl");
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            getLogger().warning("Discord Webhook URL is not set. Please set the webhook URL in the config.yml file.");
        }
        blockMessages.clear();
        for (String blockType : config.getConfigurationSection("blockMessages").getKeys(false)) {
            Material material = Material.matchMaterial(blockType);
            String message = config.getString("blockMessages." + blockType + ".message");
            String world  = config.getString("worlds");
            int color = config.getInt("blockMessages." + blockType + ".color");
            blockMessages.put(material, new BlockMessageInfo(message, color, world));
        }
    }

    private static class BlockMessageInfo {
        private final String message;
        private final int color;

        private  final String world;

        public BlockMessageInfo(String message, int color,  String world) {
            this.message = message;
            this.color = color;
            this.world = world;
        }


        public String getMessage() {
            return message;
        }

        public int getColor() {
            return color;
        }
    }
}
