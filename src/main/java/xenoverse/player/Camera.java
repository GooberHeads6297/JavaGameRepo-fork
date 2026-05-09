package xenoverse.player;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import xenoverse.blocks.Block;
import xenoverse.world.ChunkManager;

public class Camera {
    // Position and rotation
    public Vector3f position = new Vector3f(8, 36, 32);
    public float yaw = -90f;
    public float pitch = -20f;

    // Physics
    private Vector3f velocity = new Vector3f(0, 0, 0);
    private static final float GRAVITY = 0.08f;
    private static final float GROUND_DRAG = 0.8f;
    private static final float AIR_DRAG = 0.98f;
    private static final float JUMP_FORCE = 0.5f;
    private boolean onGround = false;

    // Movement input
    private boolean movingForward = false;
    private boolean movingBackward = false;
    private boolean movingLeft = false;
    private boolean movingRight = false;
    private static final float MOVE_SPEED = 0.15f;

    // Inventory
    private static final int INVENTORY_SIZE = 36;
    private Block[] inventory = new Block[INVENTORY_SIZE];
    private int selectedSlot = 0;

    public Camera() {
        // Initialize inventory with some starting blocks
        for (int i = 0; i < 9; i++) {
            inventory[i] = i < 3 ? Block.DIRT : (i < 6 ? Block.GRASS : Block.STONE);
        }
    }

    public void update(ChunkManager world) {
        applyPhysics();
        applyMovement();
        resolveCollisions(world);
    }

    private void applyPhysics() {
        // Apply gravity
        if (!onGround) {
            velocity.y -= GRAVITY;
        }

        // Apply drag
        float drag = onGround ? GROUND_DRAG : AIR_DRAG;
        velocity.x *= drag;
        velocity.z *= drag;
    }

    private void applyMovement() {
        Vector3f moveDir = new Vector3f(0, 0, 0);

        if (movingForward) {
            moveDir.x += (float) Math.cos(Math.toRadians(yaw)) * MOVE_SPEED;
            moveDir.z += (float) Math.sin(Math.toRadians(yaw)) * MOVE_SPEED;
        }
        if (movingBackward) {
            moveDir.x -= (float) Math.cos(Math.toRadians(yaw)) * MOVE_SPEED;
            moveDir.z -= (float) Math.sin(Math.toRadians(yaw)) * MOVE_SPEED;
        }
        if (movingLeft) {
            moveDir.x += (float) Math.cos(Math.toRadians(yaw - 90)) * MOVE_SPEED;
            moveDir.z += (float) Math.sin(Math.toRadians(yaw - 90)) * MOVE_SPEED;
        }
        if (movingRight) {
            moveDir.x += (float) Math.cos(Math.toRadians(yaw + 90)) * MOVE_SPEED;
            moveDir.z += (float) Math.sin(Math.toRadians(yaw + 90)) * MOVE_SPEED;
        }

        velocity.x += moveDir.x;
        velocity.z += moveDir.z;

        // Limit speed
        float horizontalSpeed = (float) Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        if (horizontalSpeed > 0.3f) {
            float scale = 0.3f / horizontalSpeed;
            velocity.x *= scale;
            velocity.z *= scale;
        }
    }

    public void jump() {
        if (onGround) {
            velocity.y = JUMP_FORCE;
            onGround = false;
        }
    }

    public void setMovement(boolean forward, boolean backward, boolean left, boolean right) {
        this.movingForward = forward;
        this.movingBackward = backward;
        this.movingLeft = left;
        this.movingRight = right;
    }

    public void updateRotation(float deltaYaw, float deltaPitch) {
        this.yaw += deltaYaw;
        this.pitch += deltaPitch;
        this.pitch = Math.max(-89.9f, Math.min(89.9f, this.pitch));
    }

    public Block getSelectedBlock() {
        Block block = inventory[selectedSlot];
        return block != null ? block : Block.AIR;
    }

    public void selectSlot(int slot) {
        if (slot >= 0 && slot < Math.min(9, INVENTORY_SIZE)) {
            this.selectedSlot = slot;
        }
    }

    public void addBlockToInventory(Block block) {
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            if (inventory[i] == null || inventory[i] == Block.AIR) {
                inventory[i] = block;
                return;
            }
        }
    }

    public void removeSelectedBlock() {
        if (inventory[selectedSlot] != null && inventory[selectedSlot] != Block.AIR) {
            inventory[selectedSlot] = Block.AIR;
        }
    }

    private void resolveCollisions(ChunkManager world) {
        // Try X movement
        float newX = position.x + velocity.x;
        if (!collides(world, newX, position.y, position.z)) {
            position.x = newX;
        } else {
            velocity.x = 0;
        }

        // Try Z movement
        float newZ = position.z + velocity.z;
        if (!collides(world, position.x, position.y, newZ)) {
            position.z = newZ;
        } else {
            velocity.z = 0;
        }

        // Try Y movement
        float newY = position.y + velocity.y;
        if (!collides(world, position.x, newY, position.z)) {
            position.y = newY;
            onGround = false;
        } else {
            velocity.y = 0;
            if (velocity.y < 0) { // was falling
                onGround = true;
            }
        }
    }

    private boolean collides(ChunkManager world, float x, float y, float z) {
        // Check all blocks in bounding box
        int minX = (int) Math.floor(x - 0.4f);
        int maxX = (int) Math.ceil(x + 0.4f);
        int minY = (int) Math.floor(y);
        int maxY = (int) Math.ceil(y + 1.8f);
        int minZ = (int) Math.floor(z - 0.4f);
        int maxZ = (int) Math.ceil(z + 0.4f);

        for (int bx = minX; bx <= maxX; bx++) {
            for (int by = minY; by <= maxY; by++) {
                for (int bz = minZ; bz <= maxZ; bz++) {
                    if (world.isSolid(bx, by, bz)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public Matrix4f getViewMatrix() {
        Vector3f front = new Vector3f();
        front.x = (float) Math.cos(Math.toRadians(yaw)) * (float) Math.cos(Math.toRadians(pitch));
        front.y = (float) Math.sin(Math.toRadians(pitch));
        front.z = (float) Math.sin(Math.toRadians(yaw)) * (float) Math.cos(Math.toRadians(pitch));
        front.normalize();

        return new Matrix4f().lookAt(position, position.add(front, new Vector3f()), new Vector3f(0, 1, 0));
    }
}