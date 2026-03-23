package main.io.github.itshaithamn.teamsandmore.commands;

import main.io.github.itshaithamn.teamsandmore.nametag.NametagColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TeamTabCompleter implements TabCompleter {

    private static final List<String> SUBCOMMANDS =
            List.of("create", "invite", "kick", "remove", "leave", "color", "disband", "view", "accept", "reject");

    private static final List<String> COLOR_NAMES =
            Arrays.stream(NametagColor.values())
                    .map(c -> c.name().toLowerCase())
                    .collect(Collectors.toList());

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,
                                                @NotNull Command command,
                                                @NotNull String alias,
                                                @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            return List.of();
        }

        if (args.length == 1) {
            return filterStartingWith(SUBCOMMANDS, args[0]);
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            return switch (sub) {
                case "invite", "kick", "remove" -> filterStartingWith(getOnlinePlayerNames(sender), args[1]);
                case "color" -> filterStartingWith(COLOR_NAMES, args[1]);
                default -> List.of();
            };
        }

        return List.of();
    }

    private List<String> getOnlinePlayerNames(CommandSender sender) {
        return Bukkit.getOnlinePlayers().stream()
                .filter(p -> !(p.equals(sender)))
                .map(Player::getName)
                .collect(Collectors.toList());
    }

    private List<String> filterStartingWith(List<String> options, String prefix) {
        String lower = prefix.toLowerCase();
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(lower))
                .sorted()
                .collect(Collectors.toCollection(ArrayList::new));
    }
}