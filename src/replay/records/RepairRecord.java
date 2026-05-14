package replay.records;

import model.map.Serializable.Position;

import java.io.Serializable;

public record RepairRecord(Position position, int cost) implements Serializable {
}
