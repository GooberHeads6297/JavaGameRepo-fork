package xenoverse.graphics;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class AtlasBuilder {
    public static void main(String[] args) throws IOException {
        File base = new File("app/src/main/resources");
        List<String> names = List.of("stone.png", "grass.png", "dirt.png", "sand.png");
        int tileSize = 64;
        int cols = 4;
        int rows = 1;
        BufferedImage atlas = new BufferedImage(cols * tileSize, rows * tileSize, BufferedImage.TYPE_INT_ARGB);

        for (int i = 0; i < names.size(); i++) {
            BufferedImage img = ImageIO.read(new File(base, names.get(i)));
            if (img == null) {
                throw new IOException("Unable to read image: " + names.get(i));
            }
            if (img.getWidth() != tileSize || img.getHeight() != tileSize) {
                System.out.println("Warning: resizing " + names.get(i));
                BufferedImage resized = new BufferedImage(tileSize, tileSize, BufferedImage.TYPE_INT_ARGB);
                resized.getGraphics().drawImage(img, 0, 0, tileSize, tileSize, null);
                img = resized;
            }
            int x = (i % cols) * tileSize;
            int y = (i / cols) * tileSize;
            atlas.getGraphics().drawImage(img, x, y, null);
        }

        File out = new File(base, "atlas.png");
        ImageIO.write(atlas, "PNG", out);
        System.out.println("Atlas generated: " + out.getAbsolutePath());
    }
}
