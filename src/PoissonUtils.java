import java.awt.image.BufferedImage;

public class PoissonUtils {

    public static int colorA(int argb) {
        return (argb >> 24) & 0xFF;
    }
    public static int colorR(int argb){
        return (argb >> 16) & 0xFF;
    }
    public static int colorG(int argb){
        return (argb >> 8) & 0xFF;
    }
    public static int colorB(int argb){
        return argb & 0xFF;
    }

    // extracts RGB color channel
    // 0 is red, 1 is green, 2 is blue
    public static int[][] extractColorChannel(BufferedImage image, int channel) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[][] result = new int[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (channel == 0) {
                    result[y][x] = colorR(image.getRGB(x, y));
                } else if (channel == 1) {
                    result[y][x] = colorG(image.getRGB(x, y));
                } else {
                    result[y][x] = colorB(image.getRGB(x, y));
                }
            }
        }
        return result;
    }

}
