package tool;

import common.GameEvent;

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
