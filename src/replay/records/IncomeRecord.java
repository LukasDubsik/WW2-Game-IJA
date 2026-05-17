/**
 * @file IncomeRecord.java
 * @author xdubsil00, xbobekp00
 * @brief Source file IncomeRecord.java for the IJA Advance-Wars-inspired game project.
 */
package replay.records;

import java.io.Serializable;

/**
 * @brief Immutable record of income for one player
 *
 * @param player receiver of the income
 * @param income value of the income
 */
public record IncomeRecord(String player, int income) implements Serializable {
}
