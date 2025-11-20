# KOTB

KOTB is een Paper-plugin die automatisch 1v1 Bridge-toernooien organiseert. Je stelt eenmaal de arena-posities en een Y-level in, voegt spelers toe en de plugin regelt de rest: teleportatie, countdowns, bevriezing, uitschakelingen en ronde-voortgang.

## Belangrijkste features
- **Volledig toersistem** - het toernooi wordt automatisch in rondes verdeeld, willekeurig geschuffeld en bij een oneven aantal spelers krijgt iemand een bye.
- **Match flow zonder gedoe** - spelers worden naar positie A/B geteleporteerd, tijdelijk bevroren, krijgen een 3-2-1 countdown en kunnen dan beginnen.
- **Automatische verliesdetectie** - vallen onder het ingestelde Y-level, overlijden of disconnecten levert direct een verlies (met melding voor beide spelers).
- **Offline-afhandeling** - als één van de spelers offline is zodra de match start, wint de tegenstander automatisch; zijn beide offline dan wordt de match geskipt.
- **Handige admin-commando's** - `/kotb` bevat subcommando's voor posities instellen, deelnemers beheren, de lijst tonen, het toernooi starten of (nood)winnaars aanwijzen.
- **Duidelijke feedback** - spelers ontvangen titelmeldingen en chatberichten voor countdowns, ronde-starts en winnaars.
- **Stats & PlaceholderAPI** - duel- en toernooiwinsten worden opgeslagen in `stats.yml` en zijn via PlaceholderAPI opvraagbaar in je scoreboards of hologrammen.
- **Automatische eliminatie** - zodra iemand verliest verdwijnt die speler automatisch uit de deelnemerslijst, zodat je overzicht schoon blijft.

## Vereisten
- Java 21 (zoals ingesteld in `build.gradle`).
- PaperMC 1.21.x server.

## Installatie
1. Bouw de plugin lokaal of download een release (zie Build-instructies hieronder).
2. Plaats het gegenereerde `KOTB-1.0.jar` bestand in de `plugins/` map van je Paper-server.
3. Start of reload de server.

## Configuratie
Bestanden:

- `plugins/KOTB/config.yml` voor arena-posities (A, B, C) en het Y-level.
- `plugins/KOTB/stats.yml` wordt automatisch beheerd voor winstatistieken.

```yaml
ylevel: 50
positions:
  a: ""
  b: ""
```

1. Gebruik `/kotb setpos a`, `/kotb setpos b` (duellocaties) en `/kotb setpos c` (spectatorruimte) om de locaties vast te leggen; ze worden automatisch opgeslagen in `config.yml`.
2. Pas `ylevel` aan naar de hoogte waarop spelers verliezen wanneer ze eronder vallen.
3. Herstart of gebruik `/reload` alleen indien noodzakelijk (posities worden direct opgeslagen).

## Commando's
Alle commando's vereisen de permissie `kotb.admin` (standaard voor operators). `/tourney` blijft als alias beschikbaar.

| Commando | Uitleg |
| --- | --- |
| `/kotb setpos <a|b|c>` | Slaat je huidige locatie op als startpositie A/B of spectatorpositie C. |
| `/kotb setylevel <y>` | Stelt het verliesniveau in (double). |
| `/kotb add <speler>` | Voegt een online speler toe aan de deelnemerslijst. |
| `/kotb remove <speler>` | Verwijdert een speler uit de lijst zolang het toernooi nog niet loopt. |
| `/kotb list` | Toont alle geregistreerde deelnemers. |
| `/kotb start` | Start het toernooi (minimaal 2 spelers en posities A+B vereist). |
| `/kotb win <speler>` | Forceert een winst voor de match van deze speler. |
| `/kotb finish` | Stopt het lopende toernooi direct met een reden in de broadcast. |
| `/kotb teleport` | Teleporteert alle (online) deelnemers naar positie C, ideaal voor spectator lobbies. |

## Tournament flow
1. Voeg spelers toe via het commando of met een eigen lobby/procedure.
2. Start het toernooi: deelnemers worden geschuffeld en per ronde gekoppeld.
3. Elke match:
   - teleportatie naar positie A/B;
   - spelers worden bevroren en krijgen een 3-seconden countdown;
   - na "GAAN" wordt de freeze opgeheven en telt het duel mee.
4. Bij verliescondities (dood, Y-level, disconnect) meldt de plugin automatisch een winnaar.
5. Als een ronde klaar is en er meerdere spelers over zijn, begint de volgende ronde; bij één speler resteert de winnaar, die ook een titel ontvangt.

## PlaceholderAPI integratie
Plaats de [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) plugin en gebruik vervolgens deze placeholders (identifier `kotb`).

| Placeholder | Output | Voorbeeld |
| --- | --- | --- |
| `%kotb_meeste_duels%` | Naam + aantal duelwinsten | `SpelerA - 7 duelwinsten` |
| `%kotb_meeste_toernooien%` | Naam + aantal toernooiwinsten | `SpelerB - 3 toernooiwinsten` |
| `%kotb_mijn_duels%` | Aantal duelwinsten van de kijkende speler | `5` |
| `%kotb_mijn_toernooien%` | Aantal toernooiwinsten van de kijkende speler | `1` |

## Build & ontwikkeling
Gebaseerd op Gradle, met Paper-API als `compileOnly` afhankelijkheid.

```powershell
./gradlew.bat build
```

Het resulterende .jar-bestand verschijnt in `build/libs/`. Gebruik `./gradlew.bat runServer` om lokaal een Paper-testserver (1.21) te draaien met de plugin automatisch geladen.

## Bekende beperkingen
- Er is (nog) geen ingebouwde opslag/herstel van spelerinventarissen buiten matches.

Bijdragen of uitbreidingen zijn welkom; open gerust een issue of stuur een pull request.
