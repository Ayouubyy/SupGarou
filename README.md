# Sup'Garou 🌙🐺

An Android app for the narrator of **Loup Garou** (Werewolf). Set up a game in under a minute, see every
player on one screen, tap to apply each role's action, and undo anything. Fully offline.

## Features

- **Quick setup:** player names autocomplete from past games; drag to set the seating order; role counts
  with presets. Roles can come from physical cards (identified as each role wakes on night 1), be dealt by
  the app (pass the phone), or be assigned by hand.
- **Narrator screen:** every player always visible (pages side by side in landscape for 30+ players), the
  current step with the line to read aloud, and one tap per action. Odd picks are faded and ask first.
- **Full rules engine:** 23 roles — Salva, Voyante, Ours, Grand ours, Berger, Sorcière, Ancien, Renard,
  Cupidon, Chasseur, Voleur, Corbeau, Juge, Troll, Petite fille, Loup, Loup père infecté, Loup rouge,
  Loup noir, Loup blanc, Joueur de flûte, Alien, Villageois — plus your own custom roles.
- **Change anything:** unlimited undo/redo, an editable game log, and a player sheet to change roles,
  statuses, lovers or seats mid-game.
- **Dawn report:** deaths, growls, what the Voyante saw, who was blocked, muted or protected.
- **History and stats:** every game saved, with wins, win rates and favorite roles per player.
- **Lots of settings:** about 46 rule and interface options, night order, English or full French.

## Install

Build the APK (below) or download it from the releases, copy it to an Android 8.0+ phone, open it, and
allow "Install unknown apps" when asked.

## Build

Requires JDK 17+ and the Android SDK (platform 35).

```bash
./gradlew assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/app-debug.apk`. Rules engine tests: `./gradlew testDebugUnitTest`.

## Project layout

| Folder | What |
| --- | --- |
| `app/src/main/java/com/supgarou/app/model` | Roles (names, icons, descriptions), rules, game record |
| `app/src/main/java/com/supgarou/app/engine` | Rules engine: replays the setup + narrator actions into the game state |
| `app/src/main/java/com/supgarou/app/data` | JSON storage on the phone, player stats |
| `app/src/main/java/com/supgarou/app/ui` | Screens: home, setup, narrator screen, history, players, roles, settings |

## Credits

- Created by **Ayoub Said** *(totally not vibe coded by claude)*
- Idea by **Aya Shkiri**
- Honorable mentions: Moslem Brahem, Mohamed Aziz Jouini, Adem Ben Salah

## License

[MIT](LICENSE)
