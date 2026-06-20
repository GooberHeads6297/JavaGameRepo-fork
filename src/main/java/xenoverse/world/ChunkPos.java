package xenoverse.world;

public final class ChunkPos {
    private final int x;
    private final int z;

    public ChunkPos(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public int x() {
        return x;
    }

    public int z() {
        return z;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ChunkPos)) {
            return false;
        }
        ChunkPos that = (ChunkPos) other;
        return x == that.x && z == that.z;
    }

    @Override
    public int hashCode() {
        return 31 * x + z;
    }

    @Override
    public String toString() {
        return "ChunkPos[x=" + x + ", z=" + z + "]";
    }
}
