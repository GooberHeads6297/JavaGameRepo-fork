package xenoverse.blocks;

public enum Block {
    AIR(false, -1),
    STONE(true, 0),
    GRASS(true, 1),
    DIRT(true, 2),
    SAND(true, 3);

    public final boolean solid;
    public final int atlasIndex;

    Block(boolean solid, int atlasIndex) {
        this.solid = solid;
        this.atlasIndex = atlasIndex;
    }
}