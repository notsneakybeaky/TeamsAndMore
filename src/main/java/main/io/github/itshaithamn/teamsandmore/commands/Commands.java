package main.io.github.itshaithamn.teamsandmore.commands;

import main.io.github.itshaithamn.teamsandmore.teammanager.TeamManager;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Commands implements CommandExecutor {

    private final TeamManager teamManager;

    public Commands(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String s, @NotNull String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command"));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(Component.text("§6§lUsage: /team <create|test>"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> {
                return handleCreate(player, args);
            }
            case "test" -> {
                player.sendMessage(Component.text("Test command executed!"));
                return true;
            }
            default -> {
                player.sendMessage(Component.text("§c§lDNE"));
                return true;
            }
        }
    }

    private boolean handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("§cUsage: /team create <name>"));
            return true;
        }

        if (args[1].length() > 16) {
            player.sendMessage(Component.text("§cTeam name must be 16 characters or less."));
            return true;
        }

        if (!args[1].matches("^[a-zA-Z0-9_]+$")) {
            player.sendMessage(Component.text("§cTeam name can only contain letters, numbers, and underscores."));
            return true;
        }

        teamManager.createNewTeam(player, args[1]);
        return true;
    }
}