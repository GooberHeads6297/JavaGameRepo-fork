package xenoverse.world;

import xenoverse.blocks.*;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import org.lwjgl.BufferUtils;
import static org.lwjgl.opengl.GL33.*;

public class Chunk {
    public static final int WIDTH = 16;
    public static final int HEIGHT = 64;
    public static final int DEPTH = 16;

    private final int chunkX;
    private final int chunkZ;
    private final Block[][][] blocks = new Block[WIDTH][HEIGHT][DEPTH];
    private int vao;
    private int vbo;
    private int ebo;
    private int indexCount;

    public Chunk(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        generateBlocks();
    }

    private void generateBlocks() {
        for (int x = 0; x < WIDTH; x++) {
            for (int z = 0; z < DEPTH; z++) {
                int worldX = chunkX * WIDTH + x;
                int worldZ = chunkZ * DEPTH + z;
                int height = getHeight(worldX, worldZ);

                for (int y = 0; y < HEIGHT; y++) {
                    if (y > height) {
                        blocks[x][y][z] = Block.AIR;
                    } else if (y == height) {
                        blocks[x][y][z] = Block.GRASS;
                    } else if (y > height - 4) {
                        blocks[x][y][z] = Block.DIRT;
                    } else {
                        blocks[x][y][z] = Block.STONE;
                    }
                }
            }
        }
    }

    private int getHeight(int worldX, int worldZ) {
        // simple hill terrain; replace with noise later
        return 32 + (int)(5 * Math.sin(worldX * 0.1) * Math.cos(worldZ * 0.1));
    }

    public boolean isSolidWorld(int worldX, int y, int worldZ) {
        if (y < 0 || y >= HEIGHT) {
            return false;
        }

        int localX = worldX - chunkX * WIDTH;
        int localZ = worldZ - chunkZ * DEPTH;
        if (localX < 0 || localX >= WIDTH || localZ < 0 || localZ >= DEPTH) {
            return false;
        }

        return blocks[localX][y][localZ].solid;
    }

    public void buildMesh() {
        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(100000);
        IntBuffer indexBuffer = BufferUtils.createIntBuffer(100000);
        int vertexIndex = 0;

        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                for (int z = 0; z < DEPTH; z++) {
                    Block block = blocks[x][y][z];
                    if (block == Block.AIR) continue;

                    for (Face face : Face.values()) {
                        int nx = x + face.dx;
                        int ny = y + face.dy;
                        int nz = z + face.dz;

                        if (!isSolid(nx, ny, nz)) {
                            addFace(vertexBuffer, indexBuffer, vertexIndex, x, y, z, face, block);
                            vertexIndex += 4;
                        }
                    }
                }
            }
        }

        vertexBuffer.flip();
        indexBuffer.flip();
        indexCount = indexBuffer.limit();

        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);

        ebo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);

        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 5 * Float.BYTES, 0);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 5 * Float.BYTES, 3 * Float.BYTES);

        glBindVertexArray(0);
    }

    private boolean isSolid(int x, int y, int z) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT || z < 0 || z >= DEPTH) {
            return false;
        }
        return blocks[x][y][z].solid;
    }

    private void addFace(FloatBuffer verts, IntBuffer inds, int baseIndex,
                         int x, int y, int z, Face face, Block block) {
        float[][] corners = face.getCorners();
        float[] uv = face.getUv();
        float tileSize = 1.0f / 4.0f; // for a 4x4 atlas
        int atlasIndex = block.atlasIndex;
        float tx = (atlasIndex % 4) * tileSize;
        float ty = (atlasIndex / 4) * tileSize;

        for (int i = 0; i < 4; i++) {
            verts.put(x + corners[i][0]);
            verts.put(y + corners[i][1]);
            verts.put(z + corners[i][2]);
            verts.put(tx + uv[i * 2] * tileSize);
            verts.put(ty + uv[i * 2 + 1] * tileSize);
        }

        inds.put(baseIndex);
        inds.put(baseIndex + 1);
        inds.put(baseIndex + 2);
        inds.put(baseIndex);
        inds.put(baseIndex + 2);
        inds.put(baseIndex + 3);
    }

    public void render() {
        glBindVertexArray(vao);
        glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }

    private enum Face {
        NORTH(0, 0, -1, new float[][]{{0, 0, 0},{1, 0, 0},{1, 1, 0},{0, 1, 0}}, new float[]{0,0,1,0,1,1,0,1}),
        SOUTH(0, 0, 1, new float[][]{{1, 0, 1},{0, 0, 1},{0, 1, 1},{1, 1, 1}}, new float[]{0,0,1,0,1,1,0,1}),
        WEST(-1,0,0,new float[][]{{0, 0, 1},{0, 0, 0},{0, 1, 0},{0, 1, 1}}, new float[]{0,0,1,0,1,1,0,1}),
        EAST(1,0,0,new float[][]{{1, 0, 0},{1, 0, 1},{1, 1, 1},{1, 1, 0}}, new float[]{0,0,1,0,1,1,0,1}),
        UP(0,1,0,new float[][]{{0, 1, 1},{1, 1, 1},{1, 1, 0},{0, 1, 0}}, new float[]{0,0,1,0,1,1,0,1}),
        DOWN(0,-1,0,new float[][]{{0, 0, 0},{1, 0, 0},{1, 0, 1},{0, 0, 1}}, new float[]{0,0,1,0,1,1,0,1});

        final int dx, dy, dz;
        final float[][] corners;
        final float[] uv;

        Face(int dx, int dy, int dz, float[][] corners, float[] uv) {
            this.dx = dx; this.dy = dy; this.dz = dz;
            this.corners = corners;
            this.uv = uv;
        }

        float[][] getCorners() { return corners; }
        float[] getUv() { return uv; }
    }
}