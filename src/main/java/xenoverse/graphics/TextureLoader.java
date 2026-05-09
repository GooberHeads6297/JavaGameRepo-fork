package xenoverse.graphics;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import static org.lwjgl.opengl.GL33.*;

public class TextureLoader {
    public static int loadTextureFromResource(String resourcePath) {
        try (InputStream input = TextureLoader.class.getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new RuntimeException("Resource not found: " + resourcePath);
            }

            ByteBuffer imageBuffer = ioResourceToByteBuffer(input, 8192);
            IntBuffer width = BufferUtils.createIntBuffer(1);
            IntBuffer height = BufferUtils.createIntBuffer(1);
            IntBuffer channels = BufferUtils.createIntBuffer(1);

            ByteBuffer image = STBImage.stbi_load_from_memory(imageBuffer, width, height, channels, 4);
            if (image == null) {
                throw new RuntimeException("Failed to load texture: " + resourcePath + " - " + STBImage.stbi_failure_reason());
            }

            int textureId = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, textureId);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width.get(), height.get(), 0,
                         GL_RGBA, GL_UNSIGNED_BYTE, image);

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

            STBImage.stbi_image_free(image);
            return textureId;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read texture resource: " + resourcePath, e);
        }
    }

    private static ByteBuffer ioResourceToByteBuffer(InputStream input, int bufferSize) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[bufferSize];
        int bytesRead;
        while ((bytesRead = input.read(buffer, 0, buffer.length)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
        byte[] data = output.toByteArray();
        ByteBuffer byteBuffer = BufferUtils.createByteBuffer(data.length);
        byteBuffer.put(data);
        byteBuffer.flip();
        return byteBuffer;
    }
}