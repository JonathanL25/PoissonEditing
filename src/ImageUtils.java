import java.awt.image.BufferedImage;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
/*
Contains methods for:
- gaussian blur
- kernels
- gradient with direction of x/y
- image output
- color extraction
 */

public class ImageUtils {
    // gaussian blur sigma value
    private static final double sigma = 1.0;
    private static final int kernelSize = 3;
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

    // RGB value extraction
    private static int colorR(int rgb){
        return (rgb & 0xFF0000) >> 16;
    }
    private static int colorG(int rgb){
        return (rgb & 0xFF00) >> 8;
    }
    private static int colorB(int rgb){
        return rgb & 0xFF;
    }
    private static class GradientResult{
        public int[][] magnitude;
        public double[][] direction;
        public GradientResult(int[][] magnitude, double[][] direction){
            this.magnitude = magnitude;
            this.direction = direction;
        }
    }

    // size should be 3/5, create a 3x3 or 5x5 gaussian kernel
    private static double[][] GaussianKernelCreate(int size, double sigma){
        double[][] kernel = new double[size][size];
        double sum = 0.0;

        double sigmaSquare = sigma*sigma;
        double gaussianCoefficient = 1/(2*Math.PI*sigmaSquare);
        int center = size/2;
        for(int i=0; i<size; i++){
            for(int j=0; j<size; j++){
                int x = i-center;
                int y = j-center;
                // Gaussian blur equation;
                kernel[i][j] = gaussianCoefficient * Math.exp(-(x^2+y^2)/(2*sigmaSquare));
                sum+=kernel[i][j];
            }
        }
        //normalization of the kernel so that all values add up to 1
        for(int i =0; i<size; i++){
            for(int j=0; j<size; j++){
                kernel[i][j]/=sum;
            }
        }
        return kernel;
    }

    // converts given image into gray scale image to process
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

                // based upon the human eye
                int luminance = (int) (0.2126*red + 0.7152*green + 0.0722*blue);
                luminosity[y][x] = Math.min(255, Math.max(0,luminance));
            }
        }
        return luminosity;
    }
    public static double[][] gaussianBlur(int[][] inputImage, double[][] kernel){
        int width = inputImage[0].length;
        int height = inputImage.length;
        double[][] blurredImage = new double[height][width];

        // center is 1 if size is 3, 2 if size is 5
        //[0,1,2] [0,1,2,3,4]
        int kernelCenter = kernelSize/2;

        // ignore edge of the input image
        for(int y = 1; y<height-kernelCenter; y++){
            for(int x = 1; x<width-kernelCenter; x++){
                double sum = 0.0;
                for(int sy = -kernelCenter; sy<=kernelCenter; sy++){
                    for(int sx = -kernelCenter; sx<=kernelCenter; sx++){
                        double pixelVal = inputImage[y+sy][x+sx];
                        sum += pixelVal * kernel[y+sy][x+sx];
                    }

                }
                int blurVal = (int)Math.round(sum);
                blurredImage[y][x] = Math.min(255, Math.max(0, blurVal));
            }
        }
        return blurredImage;
    }
    private static GradientResult extractGradientMagnitude(int[][] processedBlur){
        int width = processedBlur[0].length;
        int height = processedBlur.length;
        int[][] magnitude = new int[height][width];
        double[][] direction = new double[height][width];
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
                magnitude[y][x]= (int) Math.sqrt(sumGx*sumGx+sumGy*sumGy);

                // convert angle to only positive values
                double angle = Math.toRadians(Math.atan2(sumGy,sumGx));
                if(angle<0){
                    angle+=180;
                }
                direction[y][x] = angle;
            }
        }
        return new GradientResult(magnitude, direction);
    }


    // non-maximum suppression - thins out the lines
    public static int[][] NonMaximumSuppression(int[][] magnitude, int[][] direction){
        int width = magnitude[0].length;
        int height = magnitude.length;
        int[][] nonMaximumSuppression = new int[height][width];
        for(int y = 1 ; y<height-1; y++){
            for(int x = 1 ; x<width-1; x++) {
                // 0 degrees
                int neighbor1 = 0;
                int neighbor2 = 0;
                int currPixel = magnitude[y][x];
                if(direction[y][x]<22.5&&direction[y][x]>=0 || direction[y][x]>=157.5&&direction[y][x]<=180){ // horizontal
                    neighbor1 = magnitude[y][x+1];
                    neighbor2 = magnitude[y][x-1];
                }else if(direction[y][x]>=22.5&&direction[y][x]<67.5){ // diagonal pointing NE
                    neighbor1 = magnitude[y-1][x+1];
                    neighbor2 = magnitude[y+1][x-1];
                } else if(direction[y][x]>=67.5 && direction[y][x]<112.5){ // vertical
                    neighbor1 = magnitude[y-1][x];
                    neighbor2 = magnitude[y+1][x];
                } else if(direction[y][x]>=112.5 &&  direction[y][x]<157.5){
                    neighbor1 = magnitude[y-1][x-1];
                    neighbor2 = magnitude[y+1][x+1];
                }
                if(currPixel<neighbor1 && currPixel < neighbor2){
                    nonMaximumSuppression[y][x] = 0;
                } else{
                    nonMaximumSuppression[y][x] = currPixel;
                }
            }
        }
        return nonMaximumSuppression;
    }
    // hysteresis thresholding
    // uses two thresholds rather than one threshold
    // find the maximum magnitude, identify what pixels are strong and weak, connect pixes by spreading
    // edge of strong pixels to the neighboring weak pixels
    public static int[][] HysteresisThresholding(int[][] magnitude, int[][] direction, int lowThreshold, int highThreshold){
        int width = magnitude[0].length;
        int height = magnitude.length;
        int[][] hysteresisThresholding = new int[height][width];
        for(int y = 1; y<height-1; y++){
            for(int x = 1; x<width-1; x++){

            }
        }
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
