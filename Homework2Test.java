package ija.ija2025.homework2;

import ija.ija2025.homework2.common.Position;
import ija.ija2025.homework2.game.Game;
import ija.ija2025.homework2.game.GameFactory;
import ija.ija2025.homework2.tool.GameObserver;
import org.junit.jupiter.api.*;

import java.util.List;

/**
 * IJA 2025/26: Úkol 2 - Veřejná testovací sada (9 bodů)
 * Verze pro 100 HP systém a standardní pravidla terénu.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class Homework2Test {

    private Game game;

    @BeforeEach
    public void setUp() {
        // Mapa 3x3
        // P = Plain (Cost: 1)
        // F = Forest (Cost: Tank=2, Infantry=1)
        // M = Mountain (Cost: Infantry=2, Tank=Impassable)
        // W = Water (Impassable)
        String[] mapDefinition = {
            "P P M",
            "P F W",
            "P P P"
        };
        game = GameFactory.createGame(mapDefinition);
    }

    // =========================================================================
    // KATEGORIE 1: Logika, Pathfinding a Stav (6 bodů)
    // =========================================================================
    @Nested
    @DisplayName("1. Logika a Pathfinding (6 bodů)")
    class LogicTests {

        @Test
        @DisplayName("1.1 Inicializace a toString (100 HP)")
        void testUnitToString() {
            // Vytvoříme Tank. Dle 'units.tsv' má mít 100 HP.
            var tank = game.createUnit("Tank", "Player1", 2, 0);
            
            String desc = tank.toString();
            Assertions.assertTrue(desc.contains("Tank"), "toString musí obsahovat typ jednotky");
            Assertions.assertTrue(desc.contains("[2,0]") || desc.contains("[2, 0]"), "toString musí obsahovat souřadnice");
            
            // KONTROLA 100 HP
            Assertions.assertTrue(desc.contains("[100]") || desc.contains("100"), 
                "toString musí obsahovat aktuální HP (očekáváno 100 pro novou jednotku)");
        }

        @Test
        @DisplayName("1.2 Pathfinding: Tank v Lese (Cost 2)")
        void testPathfindingForest() {
            // Tank (Move 6). Start [0,0]. 
            // Cesta do [1,1] (Les):
            // 1. [0,0]->[0,1] (Plain, cost 1).
            // 2. [0,1]->[1,1] (Forest, cost 2) -> Tanky v lese zpomalují!
            // Celkem cost = 3.
            var tank = game.createUnit("Tank", "P1", 0, 0);
            List<Position> path = game.getReachableTiles(tank.getPosition());

            Assertions.assertTrue(path.contains(new Position(1, 1)), "Tank má dojít do lesa");
            
            // Ověříme, že to student nefláká (že opravdu počítá cost > 1)
            // Kdyby byl les za 1 bod, tank by dojel dál. 
            // V této malé mapě to těžko ověříme jinak než v privátním testu,
            // ale tento test musí projít.
        }

        @Test
        @DisplayName("1.3 Pathfinding: Pěchota v Horách (Cost 2)")
        void testInfantryInMountains() {
            // Pěchota (Move 3). Start [0,0]. Hora je na [0,2].
            // Cesta: [0,0]->[0,1](Plain, 1) -> [0,2](Mountain, 2). 
            // Celkem cost 3. Pěchota má 3 body, takže tam dojde.
            var infantry = game.createUnit("Infantry", "P1", 0, 0);
            List<Position> path = game.getReachableTiles(infantry.getPosition());
            
            Assertions.assertTrue(path.contains(new Position(0, 2)), 
                "Pěchota (Move 3) musí být schopna vstoupit do Hor (Cost 2).");
        }

        @Test
        @DisplayName("1.4 Pathfinding: Tank vs Hory/Voda")
        void testTankRestrictions() {
            // Tank (Move 6) na [0,1]. [0,2]=Hora, [1,2]=Voda.
            var tank = game.createUnit("Tank", "P1", 0, 1);
            List<Position> path = game.getReachableTiles(tank.getPosition());

            Assertions.assertFalse(path.contains(new Position(0, 2)), "Tank nesmí do Hor");
            Assertions.assertFalse(path.contains(new Position(1, 2)), "Tank nesmí do Vody");
        }

        @Test
        @DisplayName("1.5 Pohyb a Validace")
        void testMoveExecution() {
            var unit = game.createUnit("Infantry", "P1", 0, 0);
            
            // Validní pohyb
            boolean success = game.moveUnit(unit.getPosition(), new Position(0, 1));
            Assertions.assertTrue(success, "Validní pohyb vrátí true");
            Assertions.assertEquals(new Position(0, 1), unit.getPosition());
            
            // Neplatný pohyb (Moc daleko nebo Voda)
            boolean invalid = game.moveUnit(unit.getPosition(), new Position(1, 2));
            Assertions.assertFalse(invalid, "Neplatný pohyb vrátí false");
            Assertions.assertEquals(new Position(0, 1), unit.getPosition());
        }
    }

    // =========================================================================
    // KATEGORIE 2: Architektura Observer (3 body)
    // =========================================================================
    @Nested
    @DisplayName("2. Architektura Observer (3 body)")
    class ObserverTests {
        @Test
        @DisplayName("2.1 Notifikace")
        void testObserverNotification() {
            class TestObserver implements GameObserver {
                boolean notified = false;
                @Override public void update(ija.ija2025.homework2.common.GameEvent e) { notified = true; }
            }
            TestObserver observer = new TestObserver();
            game.addObserver(observer);

            var unit = game.createUnit("Infantry", "P1", 0, 0);
            game.moveUnit(unit.getPosition(), new Position(0, 1));

            Assertions.assertTrue(observer.notified, "Observer musí být notifikován!");
        }
        
        @Test
        @DisplayName("2.2 Nezávislost her")
        void testGameIndependence() {
            Game g1 = GameFactory.createGame(new String[]{"P"});
            Game g2 = GameFactory.createGame(new String[]{"P"});
            Assertions.assertNotSame(g1, g2, "Factory musí vytvářet nové instance");
        }
    }
}