package me.clin.kOTB.command;

import me.clin.kOTB.manager.ConfigManager;
import me.clin.kOTB.manager.PositionManager;
import me.clin.kOTB.manager.PositionManager.ArenaPosition;
import me.clin.kOTB.manager.TournamentManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class TournamentCommand implements CommandExecutor, TabCompleter {

    private static final String PERMISSION = "kotb.admin";

    private final TournamentManager tournamentManager;
    private final PositionManager positionManager;
    private final ConfigManager configManager;

    public TournamentCommand(TournamentManager tournamentManager,
                             PositionManager positionManager,
                             ConfigManager configManager) {
        this.tournamentManager = tournamentManager;
        this.positionManager = positionManager;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }

        if (args[0].equalsIgnoreCase("join")) {
            handleJoin(sender);
            return true;
        }

        if (!hasPermission(sender)) {
            sender.sendMessage("§cJe hebt hier geen permissie voor.");
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "setpos" -> handleSetPos(sender, args, label);
            case "setylevel" -> handleSetYLevel(sender, args);
            case "add" -> handleAdd(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "list" -> handleList(sender);
            case "start" -> tournamentManager.startTournament(sender);
            case "teleport" -> handleTeleport(sender);
            case "tournament" -> handleTournamentSub(sender, args);
            default -> sendUsage(sender, label);
        }
        return true;
    }

    private boolean hasPermission(CommandSender sender) {
        return !(sender instanceof Player player) || player.hasPermission(PERMISSION) || sender.isOp();
    }

    private void handleJoin(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cAlleen spelers kunnen zich inschrijven.");
            return;
        }
        if (!player.hasPermission("kotb.join")) {
            sender.sendMessage("§cJe hebt geen permissie om je in te schrijven.");
            return;
        }
        if (!tournamentManager.canJoin()) {
            player.sendMessage("§cJe kunt je nu niet inschrijven. Wacht tot een nieuw toernooi start.");
            return;
        }
        if (tournamentManager.isParticipant(player.getUniqueId())) {
            player.sendMessage("§eJe staat al op de deelnemerslijst.");
            return;
        }
        boolean added = tournamentManager.addParticipant(player.getUniqueId());
        if (added) {
            player.sendMessage("§aJe bent succesvol aangemeld voor KOTB!");
        } else {
            player.sendMessage("§cInschrijving mislukt. Probeer het later opnieuw.");
        }
    }

    private void handleSetPos(CommandSender sender, String[] args, String label) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cAlleen spelers kunnen posities instellen.");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("§cGebruik: /" + label + " setpos <a|b|c>");
            return;
        }
        ArenaPosition position = parsePosition(args[1]);
        if (position == null) {
            sender.sendMessage("§cOngeldige positie. Gebruik a of b.");
            return;
        }
        positionManager.setPosition(position, player.getLocation());
        sender.sendMessage("§aPositie " + position.name() + " opgeslagen.");
    }

    private void handleSetYLevel(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cGebruik: /tourney setylevel <y>");
            return;
        }
        try {
            double yLevel = Double.parseDouble(args[1]);
            configManager.setYLevel(yLevel);
            sender.sendMessage("§aY-level ingesteld op " + yLevel + ".");
        } catch (NumberFormatException ex) {
            sender.sendMessage("§cOngeldig getal.");
        }
    }

    private void handleAdd(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cGebruik: /tourney add <speler>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage("§cSpeler moet online zijn om toegevoegd te worden.");
            return;
        }
        if (tournamentManager.isRunning()) {
            sender.sendMessage("§cHet toernooi is al gestart. Wacht tot het klaar is.");
            return;
        }
        boolean added = tournamentManager.addParticipant(target.getUniqueId());
        if (added) {
            sender.sendMessage("§a" + target.getName() + " toegevoegd aan het toernooi.");
        } else if (tournamentManager.isParticipant(target.getUniqueId())) {
            sender.sendMessage("§c" + target.getName() + " staat al op de lijst.");
        } else {
            sender.sendMessage("§cInschrijven is momenteel gesloten.");
        }
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cGebruik: /tourney remove <speler>");
            return;
        }
        OfflinePlayer offlinePlayer = findOfflinePlayer(args[1]);
        if (offlinePlayer == null || offlinePlayer.getUniqueId() == null) {
            sender.sendMessage("§cSpeler niet gevonden.");
            return;
        }
        if (tournamentManager.isRunning()) {
            sender.sendMessage("§cHet toernooi is bezig, verwijderen kan nu niet.");
            return;
        }
        boolean removed = tournamentManager.removeParticipant(offlinePlayer.getUniqueId());
        if (removed) {
            sender.sendMessage("§a" + offlinePlayer.getName() + " verwijderd van de deelnemerslijst.");
        } else {
            sender.sendMessage("§c" + offlinePlayer.getName() + " staat niet op de lijst.");
        }
    }

    private void handleList(CommandSender sender) {
        List<String> names = tournamentManager.getParticipantNames();
        if (names.isEmpty()) {
            sender.sendMessage("§eNog geen deelnemers toegevoegd.");
            return;
        }
        sender.sendMessage("§6§lDeelnemers (§f" + names.size() + "§6§l): §e" + String.join(", ", names));
    }

    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage("§eGebruik:");
        sender.sendMessage("§7/" + label + " join");
        sender.sendMessage("§7/" + label + " setpos <a|b|c>");
        sender.sendMessage("§7/" + label + " setylevel <y>");
        sender.sendMessage("§7/" + label + " add <speler>");
        sender.sendMessage("§7/" + label + " remove <speler>");
        sender.sendMessage("§7/" + label + " list");
        sender.sendMessage("§7/" + label + " start");
        sender.sendMessage("§7/" + label + " teleport");
        sender.sendMessage("§7/" + label + " tournament <start|pause|win|finish>");
    }

    private ArenaPosition parsePosition(String input) {
        if (input == null) {
            return null;
        }
        return switch (input.toLowerCase(Locale.ROOT)) {
            case "a" -> ArenaPosition.A;
            case "b" -> ArenaPosition.B;
            case "c" -> ArenaPosition.C;
            default -> null;
        };
    }

    private OfflinePlayer findOfflinePlayer(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online;
        }
        OfflinePlayer cached = Bukkit.getOfflinePlayerIfCached(name);
        if (cached != null) {
            return cached;
        }
        return Bukkit.getOfflinePlayer(name);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!hasPermission(sender)) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            return partial(Arrays.asList("join", "setpos", "setylevel", "add", "remove", "list", "start", "teleport", "tournament"), args[0]);
        }
        if (args.length == 2) {
            return switch (args[0].toLowerCase(Locale.ROOT)) {
                case "setpos" -> partial(Arrays.asList("a", "b", "c"), args[1]);
                case "add", "win" -> onlineNames(args[1]);
                case "remove" -> participantNames(args[1]);
                case "setylevel" -> partial(Collections.singletonList(String.valueOf((int) configManager.getYLevel())), args[1]);
                case "tournament" -> partial(Arrays.asList("start", "pause", "win", "finish"), args[1]);
                default -> Collections.emptyList();
            };
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("tournament") && args[1].equalsIgnoreCase("win")) {
            return onlineNames(args[2]);
        }
        return Collections.emptyList();
    }

    private void handleTeleport(CommandSender sender) {
        Location spectator = positionManager.getPosition(ArenaPosition.C).orElse(null);
        if (spectator == null) {
            sender.sendMessage("§cSpectatorpositie C is nog niet ingesteld. Gebruik /kotb setpos c.");
            return;
        }
        List<UUID> participants = tournamentManager.getRegisteredParticipants();
        if (participants.isEmpty()) {
            sender.sendMessage("§eEr zijn nog geen deelnemers om te teleporteren.");
            return;
        }
        int moved = 0;
        for (UUID uuid : participants) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.teleport(spectator);
                player.sendMessage("§6§l[Toernooi] §eJe bent naar de spectatorruimte verplaatst.");
                moved++;
            }
        }
        sender.sendMessage("§a" + moved + " spelers verplaatst naar positie C.");
    }

    private List<String> onlineNames(String prefix) {
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            names.add(player.getName());
        }
        return partial(names, prefix);
    }

    private List<String> participantNames(String prefix) {
        List<String> names = new ArrayList<>();
        for (UUID uuid : tournamentManager.getRegisteredParticipants()) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            if (offlinePlayer.getName() != null) {
                names.add(offlinePlayer.getName());
            }
        }
        return partial(names, prefix);
    }

    private List<String> partial(List<String> options, String token) {
        if (token == null || token.isEmpty()) {
            return options;
        }
        String lower = token.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                matches.add(option);
            }
        }
        return matches;
    }

    private void handleTournamentSub(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cGebruik: /kotb tournament <start|pause|win|finish> [speler]");
            return;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "start" -> tournamentManager.beginMatchSequence(sender);
            case "pause" -> tournamentManager.pauseCountdown(sender);
            case "win" -> handleTournamentWin(sender, args);
            case "finish" -> handleTournamentFinish(sender);
            default -> sender.sendMessage("§cOnbekend tournament subcommando.");
        }
    }

    private void handleTournamentWin(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cGebruik: /kotb tournament win <speler>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            sender.sendMessage("§cSpeler moet online zijn.");
            return;
        }
        boolean success = tournamentManager.forceWin(target.getUniqueId());
        if (success) {
            sender.sendMessage("§a" + target.getName() + " uitgeroepen tot winnaar van de huidige match.");
        } else {
            sender.sendMessage("§c" + target.getName() + " speelt momenteel geen duel.");
        }
    }

    private void handleTournamentFinish(CommandSender sender) {
        if (!tournamentManager.isRunning()) {
            sender.sendMessage("§cEr draait momenteel geen toernooi.");
            return;
        }
        String reason = "§6§l[Toernooi] §cHet toernooi is handmatig beëindigd door " + sender.getName() + ".";
        boolean finished = tournamentManager.forceFinish(reason);
        if (finished) {
            sender.sendMessage("§aToernooi beëindigd.");
        } else {
            sender.sendMessage("§cKon het toernooi niet beëindigen.");
        }
    }
}
