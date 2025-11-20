package me.clin.kOTB.model;

import me.clin.kOTB.manager.TournamentManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Round {

    private final TournamentManager tournamentManager;
    private final int roundNumber;
    private final List<Match> matches = new ArrayList<>();
    private final List<UUID> winners = new ArrayList<>();
    private int matchIndex = -1;

    public Round(TournamentManager tournamentManager, int roundNumber, List<UUID> players) {
        this.tournamentManager = tournamentManager;
        this.roundNumber = roundNumber;
        createMatches(players);
    }

    private void createMatches(List<UUID> players) {
        for (int i = 0; i < players.size(); i += 2) {
            UUID first = players.get(i);
            if (i + 1 >= players.size()) {
                winners.add(first);
                notifyBye(first);
                continue;
            }
            UUID second = players.get(i + 1);
            matches.add(new Match(tournamentManager, first, second, roundNumber));
        }
    }

    private void notifyBye(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            player.sendMessage("§6§l[Toernooi] §eJe krijgt een bye en gaat automatisch door naar de volgende ronde.");
        }
    }

    public void start() {
        tournamentManager.broadcast("§6§l[Toernooi] §eRonde " + roundNumber + " start!");
        if (matches.isEmpty()) {
            tournamentManager.onRoundComplete(new ArrayList<>(winners));
            return;
        }
        startNextMatch();
    }

    private void startNextMatch() {
        matchIndex++;
        if (matchIndex >= matches.size()) {
            tournamentManager.onRoundComplete(new ArrayList<>(winners));
            return;
        }
        tournamentManager.prepareMatchStart(matches.get(matchIndex));
    }

    public void onMatchFinished(UUID winnerId) {
        winners.add(winnerId);
        startNextMatch();
    }

    public List<Match> getMatches() {
        return List.copyOf(matches);
    }
}
