# IJA project – handover README

This file is meant as a practical handover note for continuing the project.

## 1. What the project currently is

The project is a JavaFX turn-based hex-grid strategy game inspired by Advance Wars / Panzer General style movement on a winter map.

At the moment, the project already has a working base in these areas:

- JavaFX application startup through Maven
- hex map rendering
- terrain assets rendering
- overlay assets rendering
- unit sprite rendering
- movement/pathfinding with weighted terrain cost
- overlay movement modifiers included in pathfinding
- turn system
- one-unit-one-move-per-turn restriction
- step-by-step movement animation along the computed path
- unit definitions with movement, range, price, HP, provisional damage values
- visual bottom panel with turn info and next-turn button
- observer mechanism / game-change notifications already partially present in the engine
- basic separation of model / view / startup logic

So the project is no longer just a skeleton. It is already a playable movement prototype with visuals.

---

## 2. How the project is structured

Main folders/classes:

- `src/app/StartApp.java`
  - JavaFX application entry point
  - scene creation
  - UI layout
  - currently also contains some demo spawning / setup

- `src/model/game/`
  - game state
  - turn logic
  - unit placement
  - pathfinding
  - move execution
  - observer notifications
  - factory-style game creation

- `src/model/map/`
  - terrain
  - overlays
  - positions
  - map-related enums / data types

- `src/model/unit/`
  - unit classes
  - movement type
  - unit type definitions
  - unit stats and provisional damage values

- `src/view/board/`
  - map canvas drawing
  - terrain catalogue
  - terrain style helpers
  - unit and overlay rendering
  - movement animation

- `lib/assets/`
  - terrain graphics
  - overlay graphics
  - unit graphics

---

## 3. How to run the project

### Requirements

- Java 21
- Maven
- JavaFX dependencies are resolved through Maven from the `pom.xml`

### Run command

From the project root:

```bash
mvn clean javafx:run
```

I recommend `clean` every time during development, because stale compiled classes previously caused confusing behavior.

### Build-only check

```bash
mvn clean compile
```

---

## 4. Current gameplay status

### Already working

- map is drawn correctly
- terrain and overlays are visible
- units are rendered using images
- movement range is computed using weighted pathfinding
- overlay modifiers are included in movement cost
- unit movement is animated tile by tile
- turns alternate between players
- a unit cannot move more than once in one turn
- path of the last movement is stored in the engine
- active-player restriction exists

### Not finished yet

The project still lacks the major systems that make it a full game:

- actual combat resolution
- counterattack logic
- target range handling in UI
- capture of cities / HQ
- economy and income
- factory buying / spawning
- repairs / start-of-turn recovery
- unit destruction handling in gameplay loop
- victory condition handling
- logging to file
- replay / stepping forward-backward
- dummy bot
- proper context action menu after movement (`Attack / Capture / Wait`)

---

## 5. What I can still do myself

I can still finish the **combat part**, or more, but with what I have done I think, if I include the homework which is part of this, is around ~50%.

That means, if needed, I can implement:

- base attack action
- range checks
- provisional damage resolution using current unit damage values
- defender HP reduction
- destruction on HP <= 0
- terrain defense use in combat
- counterattack
- artillery special range rule

So if we agree on it, I can complete the combaat part.

---

## 6. What would remain mainly on you

If I finish combat, the larger remaining blocks would still be:

### A. Capture system
Needed:
- infantry-only capture
- city / HQ capture points
- capture progress over turns
- reset when leaving the tile

### B. Economy
Needed:
- player money
- city income per turn
- purchase rules
- blocked factory handling
- spawn placement

### C. Logging and replay
Needed:
- action log format
- storing move / attack / buy / end turn
- loading from log
- stepping forward/backward
- resuming normal game from replay state

### D. Bot
Needed:
- at least one dummy bot that can play legally through engine calls only
- optionally later tactical bot
- for now can be random, or even very simple greedy based algorithm
- the best version would be Minimax with some lookup to the future with an evaluation function (like three steps ahead)

### E. Unit/Tile info
Needed:
- the info is currently just a list of text debug messages in the left panel
- should be a bit "niccer" to look at then just this

### F. Final integration / cleanup
Needed:
- make all action flow consistent
- connect model state changes cleanly to the UI
- remove temporary debug shortcuts
- make sure all rules interact correctly

---

## 7. Important current design decisions

### Damage
For now, unit damage is stored directly in `UnitType` as values:
- `soft_damage`
- `hard_damage`

This is intentionally simple. Soft attack is against infantry units and ard attack against vehicle types (artillery is taken as infantry in terms of movement and damage taken).

### Movement
Movement is currently based on:
- movement points per unit
- terrain movement cost
- overlay movement modifiers

This is already a decent system and should stay.

### Animation
Movement animation is visual only.
The logical game state updates at the end of the animation, not during intermediate tiles.
That is intentional and should stay, because it keeps the engine much simpler and safer.

---

## 8. Polishes that would make the project feel much better

These are not the first priority, but they would improve the project a lot:

### Useful gameplay polish
- grey out units that already acted
- show HP on top of unit sprites
- show attack range preview
- show estimated damage preview before attack
- proper action menu after movement
- better end-turn feedback
- clearer active-player visuals

### Technical / structural polish
- move more UI logic out of `StartApp`
- cleaner controller layer
- less demo setup hardcoded in startup
- more data-driven unit loading
- command-style action objects for replay/undo-friendly architecture

### Nice-to-have polish (if we really have a lot of time)
- sound
- death animation
- capture animation / progress display
- nicer build menu

---

## 9. Current technical debt / things to watch

- there are still some temporary / debug-oriented code paths
- combat is not yet integrated, so some unit fields are not fully used yet
- economy/capture/logging are still absent, so current turn flow is incomplete
- `StartApp` is doing more than it ideally should
- before final submission, we should do a cleanup pass on naming consistency and dead code

---

## 10. Final note

The project already has a good base now.
The map, movement, overlays, turns, unit visuals, and animation are all in a workable state.
What remains are the big “game loop” systems.

If you can somehow find and generate better tiles then I would be very glad. I don't know very much about AI, since I don't use it that much, but when I tried generating assets though ChatGPT it came out suboptimal to what I expected (you will see when you play the game). If you can create better tile assets or even generate better unit assets, I would be very thankful. Even better would be to implement the concept of "asset depending on position". Like when forest is on all sides surrounded by forest tiles it will be as a whole a forest, but if it is in two hexes plain, those two sides wwould transition to plain while the rest of the sides will be all trees. This is easy to do coding wise, but would require to generate that many assets, which I utterly failed with ChatGPT cause he started generating absolute nonsense. 

So the project is **around halfway finished**, but it is also **not in danger of being a nonfunctional skeleton** anymore.
The remaining work is substantial, but it is concentrated in several clear subsystems rather than being spread chaotically everywhere.