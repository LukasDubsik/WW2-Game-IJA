IJA 2025/26 Project - East Prussia 1945

Authors:
- Lukáš Dubšík and project team

Description:
Advance-Wars-inspired JavaFX strategy game with two historical East-Prussian winter 1945 scenarios. The game contains terrain movement costs, factories, cities, HQ victory, deterministic combat, repairs, building capture, replay logging, replay stepping, and bot support including Bot vs Bot mode.

Build and run:
- Required: Java SE 21 and Maven.
- Run from the project root:
  mvn clean javafx:run

Build documentation / package:
  mvn clean package javadoc:javadoc

Controls:
- Choose scenario and player/faction in the startup menu.
- Choose Bot vs Bot to watch automatic play.
- Click own active unit to show movement range and attack targets.
- Click blue tile to move.
- Click red enemy tile to attack.
- Use side-panel buttons for Capture and Wait when available.
- Use Next turn to end the current player phase.
- Use Save replay / Load replay / Prev turn / Resume play for replay workflow.

Scenario data:
- lib/maps/*.map: terrain and overlays.
- lib/maps/*.units: initial unit placement.
- lib/maps/*.buildings: initial building ownership.
- data/*.tsv: reference tables for terrain/unit/damage values; main runtime values are embedded in enums/classes.

Notes:
- The historical maps and unit setups are historically inspired abstractions, not archival order-of-battle reconstructions.
- Replay files are meant for replay/log review and branching from replay mode, not as a long-term stable save format across code/stat changes.
