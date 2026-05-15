/**
 * @file GameObserver.java
 * @author Team
 * @brief Source file GameObserver.java for the IJA Advance-Wars-inspired game project.
 */
package model.game;

/**
 * @interface GameObserver
 * @brief Makes sure that classes implementing this will have function called -> update.
 */
@FunctionalInterface
public interface GameObserver {

    /**
     * @brief Handles a game event sent by the observed game.
     *
     * @param event event about the change observed
     */
    void update(GameEvent event);
}
