# East Prussia 1945 — IJA 2025/26 Project

## Project identification

**Project:** East Prussia 1945 — turn-based tactical strategy game  
**Course:** IJA 2025/26  
**Technology:** Java SE 21, JavaFX, Maven  
**Team leader login:** `xdubsil00`

## Authors

- xdubsil00
- xbobekp00

## Short description

This project implements a turn-based tactical strategy game inspired by the East Prussian campaign of January–February 1945. The game is played on a hexagonal battlefield map. Players control Soviet and German units, move them across terrain, attack enemy units, capture buildings, buy reinforcements from factories, and can replay recorded matches.

The game contains two prepared historical-inspired scenarios:

1. **Balga / Heiligenbeil corridor**  
   A scenario representing fighting around the Heiligenbeil pocket and the coastal evacuation corridor.

2. **Allenstein lakes approaches**  
   A scenario representing combat around the lake-district approaches near Allenstein/Olsztyn.

The scenarios are historical abstractions for gameplay purposes. They are not intended as exact archival order-of-battle reconstructions.

## Implemented features

The current version includes:

- JavaFX graphical user interface
- main menu with scenario selection
- faction selection
- human vs bot mode
- bot vs bot observation mode
- two large playable maps
- terrain and overlay system
- map loading from external files
- unit placement loading from external files
- building ownership loading from external files
- Soviet and German unit sets
- movement range calculation
- weighted movement cost by terrain and unit type
- combat with deterministic damage
- counterattacks
- artillery / ranged unit restrictions
- infantry capture of buildings
- HQ-based victory condition
- factory unit purchase system
- economy and city income
- unit repair on owned valid buildings
- turn handling
- replay/log saving
- replay/log loading
- stepping replay forward and backward
- resuming play from loaded replay
- basic AI bot
- cached map rendering for smoother large-map interaction
- side information panel with terrain, unit, building, armament, action, and historical descriptions

## Grid and movement model

The original project assignment describes movement in terms of a square-grid 4-neighborhood. This implementation intentionally uses a **hexagonal map** and therefore uses **six adjacent hexes** as neighboring fields. This matches the visual representation and the tactical board structure used by the GUI.

Movement is still terrain-cost based. Friendly units may be passed through where supported by the movement algorithm, but they are not valid final destination tiles. Enemy units block movement.

## Project directory structure

```text
xdubsil00/
  src/              Java source files
  data/             reference data tables
  lib/              assets, maps, scenario files
  pom.xml           Maven configuration
  readme.txt        required basic project description
  README.md         extended project description
  ai_audit.md       required AI usage audit
  git_history.txt   required Git history export
  rozdeleni.txt     optional team point division file
```

## Data files

The project uses these scenario/map files:

```text
lib/maps/*.map        map terrain and overlays
lib/maps/*.units      initial unit placement
lib/maps/*.buildings  initial building ownership
```

The project also includes reference tables:

```text
data/terrain.tsv
data/units.tsv
data/units-damage.tsv
```

Some game parameters are implemented directly in Java enums/classes for easier integration with the GUI and game model.

## Requirements

Recommended environment:

- Java SE 21
- Maven
- JavaFX-compatible runtime

Check Java version:

```bash
java --version
```

Expected major version:

```text
21
```

## Build

From the project root, run:

```bash
mvn clean package
```

This compiles the source code and creates Maven build output under `target/`.

## Run

Recommended development/run command:

```bash
mvn javafx:run
```

If the project is submitted with a runnable JAR configuration, the JAR will be created under:

```text
target/
```

Because JavaFX applications may require module/runtime arguments depending on the local installation, `mvn javafx:run` is the primary supported launch method.

## Controls

### Main menu

On startup, choose:

- scenario
- player faction / mode

Available modes include human play and bot-controlled sides. Bot vs bot can be used to observe an automatically played match.

### Map controls

- **Mouse wheel** — zoom in / zoom out
- **Left mouse drag** — pan the map
- **Left click tile/unit** — select or inspect
- **Click blue highlighted tile** — move selected unit
- **Click red highlighted enemy tile** — attack selected enemy
- **Capture button** — capture enemy/neutral building with infantry
- **Wait button** — end selected unit action
- **Next turn** — finish current player turn

### Side panel

The side panel displays:

- terrain information
- overlay information
- movement costs
- building ownership and capture state
- unit owner, HP, status, movement, price
- armament statistics
- attack/counterattack preview
- historical or terrain description
- factory purchase buttons where applicable

## Game mechanics

### Players

The project uses this convention:

```text
P1 = Soviets
P2 = Germans
```

### Units

Units have:

- owner
- HP
- movement type
- movement value
- price
- armaments
- attack ranges
- soft/hard attack behavior through armaments

### Movement

Movement uses terrain costs. Water/impassable tiles cannot be entered by units that cannot traverse them. Friendly units may be passed through where the pathfinding allows it, but cannot be used as the final destination.

### Combat

Combat is deterministic. Damage depends on:

- attacking unit / weapon values
- attacker HP
- target type
- range
- defender terrain/overlay protection

If the defender survives and has a valid counterattack, it may counterattack with reduced HP.

### Capture

Only infantry-type units can capture buildings. Capture progress is tracked on buildings. If the capturing condition is no longer present, capture progress can reset. HQ capture controls victory.

### Economy

Cities generate income for their owner. Factories allow purchasing new units of the correct faction. Newly purchased units start at the factory and cannot act immediately on the same turn.

### Repairs

Units may repair only on valid owned buildings, such as city/factory/HQ tiles. Repairs cost money.

### Victory

Victory is based on HQ ownership. If one side loses all HQ ownership while the other still owns an HQ, the other side wins.

## Replay / log system

The game supports saving and loading match replays/logs.

Replay files are used to review a played match turn by turn. They are not intended as a general-purpose save-game format in the same sense as a full persistent save file.

Replay controls:

- **Save replay** — export the current match log
- **Load replay** — load a replay/log file
- **Next replay turn** — step forward through the loaded replay
- **Prev turn** — step backward
- **Resume play** — leave replay mode and continue from the current replay state, truncating the old future branch where applicable

When testing replay, first perform at least one turn with movement/attack/purchase/capture, then save and reload the replay.

## Bot

The project includes a simple dummy AI bot. The bot can:

- select available units
- move units
- attack when possible
- buy faction-correct units where possible
- play automatically in bot-controlled turns

The bot is intentionally simple. Its role is to satisfy the project requirement for an automated opponent / bot simulation rather than to provide advanced strategic AI.

## Known limitations

- The game uses a hexagonal six-neighbor board instead of the square-grid four-neighbor model described in the generic assignment text.
- The replay system is a match log/replay system, not a robust long-term save format.
- The bot is simple and deterministic/heuristic, not a full minimax or strategic planner.
- The scenarios are historically inspired abstractions, not exact historical deployments.
- MVC separation is partial: the model and view are separated, but some controller logic remains in the JavaFX application and canvas classes.
- The command/replay system is command-like but not a fully isolated command-pattern implementation for every action.
