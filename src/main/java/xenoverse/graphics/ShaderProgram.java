package xenoverse.graphics;

import static org.lwjgl.opengl.GL33.*;

public class ShaderProgram {
    private final int programId;

    public ShaderProgram(String vertexSource, String fragmentSource) {
        int vertexId = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertexId, vertexSource);
        glCompileShader(vertexId);
        checkCompile(vertexId);

        int fragmentId = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragmentId, fragmentSource);
        glCompileShader(fragmentId);
        checkCompile(fragmentId);

        programId = glCreateProgram();
        glAttachShader(programId, vertexId);
        glAttachShader(programId, fragmentId);
        glLinkProgram(programId);
        checkLink(programId);

        glDeleteShader(vertexId);
        glDeleteShader(fragmentId);
    }

    public void use() {
        glUseProgram(programId);
    }

    public int getUniformLocation(String name) {
        return glGetUniformLocation(programId, name);
    }

    private void checkCompile(int shaderId) {
        if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == 0) {
            System.err.println("Shader compilation failed: " + glGetShaderInfoLog(shaderId));
            throw new RuntimeException("Shader compilation failed");
        }
    }

    private void checkLink(int programId) {
        if (glGetProgrami(programId, GL_LINK_STATUS) == 0) {
            System.err.println("Program linking failed: " + glGetProgramInfoLog(programId));
            throw new RuntimeException("Program linking failed");
        }
    }
}
