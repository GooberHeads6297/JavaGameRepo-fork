package xenoverse.graphics;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;
import org.lwjgl.stb.STBImageWrite;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.List;

public class AtlasGenerator {
    public static void generateAtlas(List<String> texturePaths, String outputPath, int tilesPerRow, int tileSize) {
        int atlasWidth = tilesPerRow * tileSize;
        int atlasHeight = (int) Math.ceil((double) texturePaths.size() / tilesPerRow) * tileSize;

        ByteBuffer atlasBuffer = BufferUtils.createByteBuffer(atlasWidth * atlasHeight * 4); // RGBA

        for (int i = 0; i < texturePaths.size(); i++) {
            String path = texturePaths.get(i);
            IntBuffer width = BufferUtils.createIntBuffer(1);
            IntBuffer height = BufferUtils.createIntBuffer(1);
            IntBuffer channels = BufferUtils.createIntBuffer(1);

            ByteBuffer image = STBImage.stbi_load(path, width, height, channels, 4);
            if (image == null) {
                System.err.println("Failed to load texture: " + path);
                continue;
            }

            int tileX = (i % tilesPerRow) * tileSize;
            int tileY = (i / tilesPerRow) * tileSize;

            // Copy the tile into the atlas
            for (int y = 0; y < tileSize; y++) {
                for (int x = 0; x < tileSize; x++) {
                    int srcIndex = (y * width.get() + x) * 4;
                    int dstIndex = ((tileY + y) * atlasWidth + (tileX + x)) * 4;

                    atlasBuffer.put(dstIndex, image.get(srcIndex));     // R
                    atlasBuffer.put(dstIndex + 1, image.get(srcIndex + 1)); // G
                    atlasBuffer.put(dstIndex + 2, image.get(srcIndex + 2)); // B
                    atlasBuffer.put(dstIndex + 3, image.get(srcIndex + 3)); // A
                }
            }

            STBImage.stbi_image_free(image);
        }

        atlasBuffer.flip();
        STBImageWrite.stbi_write_png(outputPath, atlasWidth, atlasHeight, 4, atlasBuffer, atlasWidth * 4);
        System.out.println("Atlas generated: " + outputPath);
    }

    public static void main(String[] args) {
        // Use the PNG files in resources folder
        List<String> textures = List.of(
            "src/main/resources/stone.png",
            "src/main/resources/grass.png",
            "src/main/resources/dirt.png",
            "src/main/resources/sand.png"
        );

        generateAtlas(textures, "src/main/resources/atlas.png", 4, 64);
    }
}