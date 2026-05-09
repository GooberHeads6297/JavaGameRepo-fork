package xenoverse.world;

import java.util.HashMap;
import java.util.Map;

public class ChunkManager {
    private final Map<ChunkPos, Chunk> chunks = new HashMap<>();
    private final int renderRadius = 3; // 7x7 chunks

    public void loadChunksAround(int playerChunkX, int playerChunkZ) {
        for (int cz = playerChunkZ - renderRadius; cz <= playerChunkZ + renderRadius; cz++) {
            for (int cx = playerChunkX - renderRadius; cx <= playerChunkX + renderRadius; cx++) {
                ChunkPos pos = new ChunkPos(cx, cz);
                final int finalCx = cx;
                final int finalCz = cz;
                chunks.computeIfAbsent(pos, p -> {
                    Chunk chunk = new Chunk(finalCx, finalCz);
                    chunk.buildMesh();
                    return chunk;
                });
            }
        }
    }

    public void renderAll() {
        for (Chunk chunk : chunks.values()) {
            chunk.render();
        }
    }

    public boolean isSolid(int worldX, int y, int worldZ) {
        if (y < 0) return true;
        int chunkX = Math.floorDiv(worldX, Chunk.WIDTH);
        int chunkZ = Math.floorDiv(worldZ, Chunk.DEPTH);
        ChunkPos pos = new ChunkPos(chunkX, chunkZ);
        Chunk chunk = chunks.get(pos);
        if (chunk == null) {
            return false;
        }
        return chunk.isSolidWorld(worldX, y, worldZ);
    }
}