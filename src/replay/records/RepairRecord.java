package replay.records;

import model.map.Position;

import java.io.Serializable;

public record RepairRecord(Position position, int cost) implements Serializable {
}
