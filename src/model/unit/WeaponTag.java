/**
 * @file WeaponTag.java
 * @author Team
 * @brief Source file WeaponTag.java for the IJA Advance-Wars-inspired game project.
 */
package model.unit;

/**
 * @brief Tags describing the qualitative behavior of a weapon / armament
 */
public enum WeaponTag {
    MG, ///< Machine-gun type armament
    HE, ///< High-explosive / anti-soft focused armament
    ANTI_TANK, ///< Anti-armor focused armament
    DIRECT_FIRE, ///< Fires directly at the target
    INDIRECT_FIRE ///< Fires indirectly, typically artillery-like
}