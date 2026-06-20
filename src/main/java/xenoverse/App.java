package xenoverse;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.system.MemoryUtil.NULL;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.joml.Matrix4f;
import javax.swing.JOptionPane;
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
    private boolean glfwInitialized;

    // Mouse tracking
    private double lastMouseX = 400;
    private double lastMouseY = 300;
    private boolean firstMouse = true;

    public static void main(String[] args) {
        try {
            new App().run();
        } catch (RuntimeException error) {
            error.printStackTrace(System.err);
            if (isWindows() && System.console() == null) {
                JOptionPane.showMessageDialog(
                    null,
                    error.getMessage(),
                    "Xenoverse could not start",
                    JOptionPane.ERROR_MESSAGE
                );
            }
            System.exit(1);
        }
    }

    public void run() {
        try {
            init();
            loop();
        } finally {
            cleanup();
        }
    }

    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        configureGlfwPlatform();

        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }
        glfwInitialized = true;

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

        int platform = glfwGetPlatform();
        if (platform == GLFW_PLATFORM_X11) {
            glfwWindowHintString(GLFW_X11_CLASS_NAME, "Xenoverse");
            glfwWindowHintString(GLFW_X11_INSTANCE_NAME, "xenoverse");
        } else if (platform == GLFW_PLATFORM_WAYLAND) {
            glfwWindowHintString(GLFW_WAYLAND_APP_ID, "xenoverse");
        }

        window = glfwCreateWindow(800, 600, "Xenoverse", NULL, NULL);
        if (window == NULL) {
            throw new IllegalStateException(graphicsStartupError());
        }

        if (platform != GLFW_PLATFORM_WAYLAND) {
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
            if (vidmode != null) {
                glfwSetWindowPos(window,
                    (vidmode.width() - 800) / 2,
                    (vidmode.height() - 600) / 2);
            }
            TextureLoader.setWindowIconFromResource(window, "/atlas.png");
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
                    case GLFW_KEY_W:
                        camera.setMovement(true, false, false, false);
                        break;
                    case GLFW_KEY_S:
                        camera.setMovement(false, true, false, false);
                        break;
                    case GLFW_KEY_A:
                        camera.setMovement(false, false, true, false);
                        break;
                    case GLFW_KEY_D:
                        camera.setMovement(false, false, false, true);
                        break;
                    case GLFW_KEY_SPACE:
                        camera.jump();
                        break;
                    case GLFW_KEY_ESCAPE:
                        glfwSetWindowShouldClose(window, true);
                        break;
                    default:
                        break;
                }
            } else if (action == GLFW_RELEASE) {
                switch (key) {
                    case GLFW_KEY_W:
                    case GLFW_KEY_S:
                    case GLFW_KEY_A:
                    case GLFW_KEY_D:
                        camera.setMovement(false, false, false, false);
                        break;
                    default:
                        break;
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

    private void configureGlfwPlatform() {
        if (!System.getProperty("os.name", "").toLowerCase().contains("linux")) {
            return;
        }

        String requestedPlatform = System.getenv().getOrDefault("XENOVERSE_GLFW_PLATFORM", "auto");
        switch (requestedPlatform.toLowerCase()) {
            case "auto":
                // XWayland avoids EGL/libdecor issues seen on some native Wayland drivers.
                if (System.getenv("DISPLAY") != null) {
                    glfwInitHint(GLFW_PLATFORM, GLFW_PLATFORM_X11);
                }
                break;
            case "x11":
                glfwInitHint(GLFW_PLATFORM, GLFW_PLATFORM_X11);
                break;
            case "wayland":
                glfwInitHint(GLFW_PLATFORM, GLFW_PLATFORM_WAYLAND);
                glfwInitHint(GLFW_WAYLAND_LIBDECOR, GLFW_WAYLAND_DISABLE_LIBDECOR);
                break;
            default:
                throw new IllegalArgumentException(
                    "XENOVERSE_GLFW_PLATFORM must be auto, x11, or wayland"
                );
        }
    }

    private String graphicsStartupError() {
        if (isWindows()) {
            return "Xenoverse requires OpenGL 3.3, but Windows could not provide a usable "
                + "OpenGL context. Install the graphics driver directly from Intel, AMD, or "
                + "NVIDIA instead of using Microsoft Basic Display Adapter. In a virtual "
                + "machine, install its guest graphics tools and enable 3D acceleration. "
                + "Remote Desktop can also hide OpenGL support, so try a local session.";
        }
        return "Xenoverse requires OpenGL 3.3, but no compatible OpenGL context was available.";
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("windows");
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
        if (window != NULL) {
            glfwFreeCallbacks(window);
            glfwDestroyWindow(window);
            window = NULL;
        }
        if (glfwInitialized) {
            glfwTerminate();
            glfwInitialized = false;
        }
        GLFWErrorCallback errorCallback = glfwSetErrorCallback(null);
        if (errorCallback != null) {
            errorCallback.free();
        }
    }

    private String vertexSource() {
        return "#version 330 core\n"
            + "layout(location = 0) in vec3 position;\n"
            + "layout(location = 1) in vec2 uv;\n"
            + "uniform mat4 projection;\n"
            + "uniform mat4 view;\n"
            + "uniform mat4 model;\n"
            + "out vec2 fragUV;\n"
            + "void main() {\n"
            + "    fragUV = uv;\n"
            + "    gl_Position = projection * view * model * vec4(position, 1.0);\n"
            + "}\n";
    }

    private String fragmentSource() {
        return "#version 330 core\n"
            + "in vec2 fragUV;\n"
            + "out vec4 color;\n"
            + "uniform sampler2D textureAtlas;\n"
            + "void main() {\n"
            + "    color = texture(textureAtlas, fragUV);\n"
            + "}\n";
    }
}
