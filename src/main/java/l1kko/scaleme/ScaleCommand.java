package l1kko.scaleme;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class ScaleCommand implements TabExecutor {

    private final ScaleMe plugin;
    private final Map<UUID, BukkitTask> activeAnimations = new HashMap<>();

    public ScaleCommand(ScaleMe plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendMessage(sender, "usage");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("scaleme.reload")) {
                sendMessage(sender, "no_permission");
                return true;
            }
            plugin.reloadConfig();
            sendMessage(sender, "reloaded");
            return true;
        }

        double size;
        try {
            size = Double.parseDouble(args[0]);
            // Жесткий лимит защиты от краша сервера/клиента
            if (size < 0.05) size = 0.05;
        } catch (NumberFormatException e) {
            sendMessage(sender, "invalid_number");
            return true;
        }

        if (args.length >= 2) {
            if (!sender.hasPermission("scaleme.others")) {
                sendMessage(sender, "no_permission");
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sendMessage(sender, "player_not_found");
                return true;
            }

            animateScale(target, size);

            String msgSender = getMessage("scale_set_other")
                    .replace("%player%", target.getName())
                    .replace("%size%", String.valueOf(size));
            sender.sendMessage(msgSender);

            String msgTarget = getMessage("scale_received")
                    .replace("%size%", String.valueOf(size));
            target.sendMessage(msgTarget);

            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("Эту команду может использовать только игрок!");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("scaleme.use")) {
            sendMessage(player, "no_permission");
            return true;
        }

        if (!player.hasPermission("scaleme.bypass")) {
            double[] limits = getScaleLimits(player);
            double minAllowed = limits[0];
            double maxAllowed = limits[1];

            if (size < minAllowed || size > maxAllowed) {
                String msg = getMessage("out_of_bounds")
                        .replace("%min%", String.valueOf(minAllowed))
                        .replace("%max%", String.valueOf(maxAllowed));
                player.sendMessage(msg);
                return true;
            }
        }

        animateScale(player, size);
        String msg = getMessage("scale_set_self").replace("%size%", String.valueOf(size));
        player.sendMessage(msg);

        return true;
    }

    private void animateScale(Player player, double targetSize) {
        AttributeInstance scaleAttribute = player.getAttribute(Attribute.GENERIC_SCALE);
        if (scaleAttribute == null) return;

        double startSize = scaleAttribute.getBaseValue();
        UUID playerId = player.getUniqueId();

        if (activeAnimations.containsKey(playerId)) {
            activeAnimations.get(playerId).cancel();
        }

        int animationTicks = 20;

        BukkitTask task = new BukkitRunnable() {
            int currentTick = 0;

            @Override
            public void run() {
                currentTick++;
                double step = startSize + (targetSize - startSize) * ((double) currentTick / animationTicks);
                scaleAttribute.setBaseValue(step);

                if (currentTick >= animationTicks) {
                    scaleAttribute.setBaseValue(targetSize);
                    activeAnimations.remove(playerId);
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        activeAnimations.put(playerId, task);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String[] suggestions = {"0.25", "0.5", "0.75", "1.0", "1.25", "1.5", "2.0", "3.0"};
            for (String s : suggestions) {
                if (s.startsWith(args[0].toLowerCase())) {
                    completions.add(s);
                }
            }
        } else if (args.length == 2 && sender.hasPermission("scaleme.others")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    completions.add(p.getName());
                }
            }
        }

        return completions;
    }

    private double[] getScaleLimits(Player player) {
        ConfigurationSection limits = plugin.getConfig().getConfigurationSection("limits");

        double minAllowed = 1.0;
        double maxAllowed = 1.0;
        boolean hasAnyPerm = false;

        if (limits != null) {
            for (String group : limits.getKeys(false)) {

                String permission = "scaleme.limit." + group;

                if (player.hasPermission(permission)) {
                    double permMin = limits.getDouble(group + ".min", 1.0);
                    double permMax = limits.getDouble(group + ".max", 1.0);

                    if (!hasAnyPerm) {
                        minAllowed = permMin;
                        maxAllowed = permMax;
                        hasAnyPerm = true;
                    } else {
                        if (permMin < minAllowed) minAllowed = permMin;
                        if (permMax > maxAllowed) maxAllowed = permMax;
                    }
                }
            }
            if (!hasAnyPerm && limits.contains("default")) {
                minAllowed = limits.getDouble("default.min", 1.0);
                maxAllowed = limits.getDouble("default.max", 1.0);
            }
        }
        return new double[]{minAllowed, maxAllowed};
    }

    private void sendMessage(CommandSender sender, String key) {
        sender.sendMessage(getMessage(key));
    }

    private String getMessage(String key) {
        String msg = plugin.getConfig().getString("messages." + key, "&cСообщение не найдено: " + key);
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}