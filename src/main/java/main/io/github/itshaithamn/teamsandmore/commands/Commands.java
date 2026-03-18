package main.io.github.itshaithamn.teamsandmore.commands;

import main.io.github.itshaithamn.teamsandmore.teammanager.TeamManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
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
            player.sendMessage(Component.text("§6§lUsage: /team <create|invite|kick|leave|color>"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> {
                return handleTeamCreate(player, args);
            }
            case "invite" -> {
                return handleTeamInvite(player, args);
            }
            case "kick", "remove" -> {
                return handleTeamRemove(player, args);
            }
            case "leave" -> {
                return handleTeamLeave(player);
            }
            case "color" -> {
                return handleTeamColor(player, args);
            }
            default -> {
                player.sendMessage(Component.text("§c§lDNE"));
                return true;
            }
        }
    }

    private boolean handleTeamCreate(Player player, String[] args) {
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

    private boolean handleTeamInvite(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("§cUsage: /team invite <player name>"));
            return true;
        }

        String targetName = args[1];

        if (targetName.equalsIgnoreCase(player.getName())) {
            player.sendMessage(Component.text("§cYou can't invite yourself."));
            return true;
        }

        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            player.sendMessage(Component.text("§cPlayer not found or offline."));
            return true;
        }

        teamManager.addPlayerToTeam(player, target.getName());
        return true;
    }

    private boolean handleTeamLeave(Player player) {
        teamManager.leaveTeam(player);
        return true;
    }

    private boolean handleTeamColor(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("§cUsage: /team color <color>"));
            return true;
        }

        teamManager.setTeamColor(player, args[1]);
        return true;
    }

    private boolean handleTeamRemove(Player player, String[] args) {

        if (args.length < 2) {
            player.sendMessage(Component.text("§cUsage: /team kick <player name>"));
            return true;
        }

        String targetName = args[1];

        if (targetName.equalsIgnoreCase(player.getName())) {
            player.sendMessage(Component.text("§cUse a leave command to remove yourself."));
            return true;
        }

        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            player.sendMessage(Component.text("§cPlayer not found or offline."));
            return true;
        }

        teamManager.removePlayerFromTeam(player, target.getName());
        return true;
    }
}