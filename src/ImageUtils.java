import java.awt.image.BufferedImage;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class ImageUtils {
    // Sobel operators

    private static final int[][] Gx = {
            {-1, 0, 1},
            {-2, 0, 2},
            {-1, 0, 1},
    };
    private static final int[][] Gy = {
            {-1, -2, -1},
            {0, 0, 0},
            {1, 2, 1},
    };
    private static int colorR(int rgb){
        return (rgb & 0xFF0000) >> 16;
    }
    private static int colorG(int rgb){
        return (rgb & 0xFF00) >> 8;
    }
    private static int colorB(int rgb){
        return rgb & 0xFF;
    }

    private static double[][] GaussianKernelCreate(){


    }
    public static int[][] toLuminance(BufferedImage img){
        int width = img.getWidth();
        int height = img.getHeight();
        int[][] luminosity = new int[height][width];
        for(int y = 0; y<height; y++){
            for(int x = 0; x<width; x++){
                int rgb = img.getRGB(x, y);
                int red = colorR(rgb);
                int green = colorG(rgb);
                int blue = colorB(rgb);

                int luminance = (int) (0.2126*red + 0.7152*green + 0.0722*blue);
                luminosity[y][x] = Math.min(255, Math.max(0,luminance));
            }
        }
        return luminosity;
    }
    public static int[][] extractGradientMagnitude(int[][] processedBlur){
        int width = processedBlur[0].length;
        int height = processedBlur.length;
        int[][] gradientMagnitude = new int[height][width];

        // exclude border of luminosity image
        for(int y = 1; y<height-1; y++){
            for(int x = 1; x<width-1; x++){
                int sumGx = 0;
                int sumGy = 0;
                // apply sobel 3x3 to pixels
                for(int sy = -1; sy<=1; sy++){
                    for(int sx = -1; sx<=1; sx++){
                        int pixelVal = processedBlur[y+sy][x+sx];

                        sumGx += pixelVal*Gx[sy+1][sx+1];
                        sumGy += pixelVal*Gy[sy+1][sx+1];
                    }
                }
                int magnitude = (int) Math.sqrt(sumGx*sumGx+sumGy*sumGy);
                gradientMagnitude[y][x] = Math.min(255, magnitude/4);
            }
        }
        return gradientMagnitude;
    }
    public static int[][] gaussianBlur(int[][] inputImage){
        int width = inputImage[0].length;
        int height = inputImage.length;

        int[][] blurredImage = new int[height][width];
        for(int y = 1; y<height-1; y++){
            for(int x = 1; x<width-1; x++){
                int sum = 0;
                for(int sy = -1; sy<=1; sy++){
                    for(int sx = -1; sx<=1; sx++){
                        int pixelVal = inputImage[y+sy][x+sx];
                        sum += pixelVal * GaussianKernel[sy+1][sx+1];
                    }
                    int blurVal = sum/GaussianDivisor;
                    blurredImage[y][x] = Math.min(255, Math.max(0, blurVal));
                }
            }
        }
        return blurredImage;
    }

    public static BufferedImage createOutputImage(int[][] magnitudeImg){
        int height = magnitudeImg.length;
        int width = magnitudeImg[0].length;
        BufferedImage outputImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        for(int y = 0; y<height; y++){
            for(int x = 0; x<width; x++){
                int pixelVal = magnitudeImg[y][x];
                Color color = new Color(pixelVal, pixelVal, pixelVal);
                outputImage.setRGB(x, y, color.getRGB());
            }
        }
        return outputImage;
    }
}
