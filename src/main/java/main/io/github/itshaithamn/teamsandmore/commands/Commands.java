package main.io.github.itshaithamn.teamsandmore.commands;

import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Commands implements CommandExecutor{

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can use this command"));
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "test":
                player.sendMessage(Component.text("Test command executed!"));
                return true;

            default:
                player.sendMessage(Component.text("§c§lDNE"));
                return true;
        }
    }
}