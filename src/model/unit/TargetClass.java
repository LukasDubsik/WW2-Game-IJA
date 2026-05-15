/**
 * @file TargetClass.java
 * @author Team
 * @brief Source file TargetClass.java for the IJA Advance-Wars-inspired game project.
 */
package model.unit;

/**
 * @brief The broad target class against which the attack is evaluated
 */
public enum TargetClass {
    SOFT, ///< Infantry, crews, other soft targets
    HARD ///< Vehicles, armored units, hard targets
}