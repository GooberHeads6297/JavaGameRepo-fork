package xenoverse;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.system.MemoryUtil.NULL;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.joml.Matrix4f;
import xenoverse.graphics.ShaderProgram;
import xenoverse.graphics.TextureLoader;
import xenoverse.world.Chunk;
import xenoverse.world.ChunkManager;
import xenoverse.player.Camera;

public class App {
    private long window;
    private ShaderProgram shader;
    private ChunkManager world;
    private Camera camera;
    private int textureId;

    // Mouse tracking
    private double lastMouseX = 400;
    private double lastMouseY = 300;
    private boolean firstMouse = true;

    public static void main(String[] args) {
        new App().run();
    }

    public void run() {
        init();
        loop();
        cleanup();
    }

    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        window = glfwCreateWindow(800, 600, "Xenoverse", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        if (vidmode != null) {
            glfwSetWindowPos(window,
                (vidmode.width() - 800) / 2,
                (vidmode.height() - 600) / 2);
        }

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);

        // Capture mouse
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);

        GL.createCapabilities();
        glEnable(GL_DEPTH_TEST);

        shader = new ShaderProgram(vertexSource(), fragmentSource());
        world = new ChunkManager();
        camera = new Camera();
        world.loadChunksAround(0, 0);

        // Load texture atlas from classpath resources
        shader.use();
        textureId = TextureLoader.loadTextureFromResource("/atlas.png");
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textureId);
        int texLoc = shader.getUniformLocation("textureAtlas");
        glUniform1i(texLoc, 0);

        // Set up input callbacks
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (action == GLFW_PRESS || action == GLFW_REPEAT) {
                switch (key) {
                    case GLFW_KEY_W -> camera.setMovement(true, false, false, false);
                    case GLFW_KEY_S -> camera.setMovement(false, true, false, false);
                    case GLFW_KEY_A -> camera.setMovement(false, false, true, false);
                    case GLFW_KEY_D -> camera.setMovement(false, false, false, true);
                    case GLFW_KEY_SPACE -> camera.jump();
                    case GLFW_KEY_ESCAPE -> glfwSetWindowShouldClose(window, true);
                }
            } else if (action == GLFW_RELEASE) {
                switch (key) {
                    case GLFW_KEY_W, GLFW_KEY_S, GLFW_KEY_A, GLFW_KEY_D ->
                        camera.setMovement(false, false, false, false);
                }
            }
        });

        glfwSetCursorPosCallback(window, (window, xpos, ypos) -> {
            if (firstMouse) {
                lastMouseX = xpos;
                lastMouseY = ypos;
                firstMouse = false;
            }

            double xoffset = xpos - lastMouseX;
            double yoffset = lastMouseY - ypos; // Reversed since y-coordinates go from bottom to top

            lastMouseX = xpos;
            lastMouseY = ypos;

            camera.updateRotation((float) xoffset * 0.1f, (float) yoffset * 0.1f);
        });
    }

    private void loop() {
        Matrix4f projection = new Matrix4f().perspective((float)Math.toRadians(70), 800f/600f, 0.1f, 100f);

        while (!glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            int playerChunkX = (int) Math.floor(camera.position.x / Chunk.WIDTH);
            int playerChunkZ = (int) Math.floor(camera.position.z / Chunk.DEPTH);
            world.loadChunksAround(playerChunkX, playerChunkZ);

            camera.update(world);

            shader.use();
            int projLoc = shader.getUniformLocation("projection");
            int viewLoc = shader.getUniformLocation("view");
            int modelLoc = shader.getUniformLocation("model");

            glUniformMatrix4fv(projLoc, false, projection.get(new float[16]));
            glUniformMatrix4fv(viewLoc, false, camera.getViewMatrix().get(new float[16]));
            glUniformMatrix4fv(modelLoc, false, new Matrix4f().identity().get(new float[16]));

            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, textureId);
            world.renderAll();

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    private void cleanup() {
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    private String vertexSource() {
        return """
            #version 330 core
            layout(location = 0) in vec3 position;
            layout(location = 1) in vec2 uv;
            uniform mat4 projection;
            uniform mat4 view;
            uniform mat4 model;
            out vec2 fragUV;
            void main() {
                fragUV = uv;
                gl_Position = projection * view * model * vec4(position, 1.0);
            }
            """;
    }

    private String fragmentSource() {
        return """
            #version 330 core
            in vec2 fragUV;
            out vec4 color;
            uniform sampler2D textureAtlas;
            void main() {
                color = texture(textureAtlas, fragUV);
            }
            """;
    }
}