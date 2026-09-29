import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.io.IOException;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
Contains methods for:
- gaussian blur
- kernels
- gradient with direction of x/y
- image output
- color extraction
 */

public class CannyEdgeDetector {
    // gaussian blur sigma value
    private static final double sigma = 1.4;
    private static final int blurKernelSize = 7;

    // Sobel operators
    // horizontal operator
    private static final int[][] Gx = {
            {-1, 0, 1},
            {-2, 0, 2},
            {-1, 0, 1},
    };
    // vertical operator
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

    // struct to store gradient result
    private static class GradientResult{
        public int[][] magnitude;
        public double[][] direction;
        public GradientResult(int[][] magnitude, double[][] direction){
            this.magnitude = magnitude;
            this.direction = direction;
        }
    }

    // create a gaussian blur kernel of size * size dimensions
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
                // Gaussian blur equation
                kernel[i][j] = gaussianCoefficient * Math.exp( -(Math.pow(x, 2)+Math.pow(y, 2))/(2*sigmaSquare));
                sum+=kernel[i][j];
            }
        }
        // normalization of the kernel so that all values add up to 1
        for(int i =0; i<size; i++){
            for(int j=0; j<size; j++){
                kernel[i][j]/=sum;
            }
        }
        return kernel;
    }

    // converts given image into gray scale image to process
    private static int[][] toLuminance(BufferedImage img){
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

    // applies gaussian blur kernel to image to decrease noise
    // which makes edge detecting easier
    private static int[][] gaussianBlur(int[][] inputImage, double[][] kernel){
        int width = inputImage[0].length;
        int height = inputImage.length;
        int[][] blurredImage = new int[height][width];

        // center is 1 if size is 3, 2 if size is 5
        //[0,1,2] [0,1,2,3,4]
        int kernelCenter = blurKernelSize/2;

        // ignore edge of the input image
        for(int y = kernelCenter; y<height-kernelCenter; y++){
            for(int x = kernelCenter; x<width-kernelCenter; x++){
                double sum = 0.0;

                for(int sy = -kernelCenter; sy<=kernelCenter; sy++){
                    for(int sx = -kernelCenter; sx<=kernelCenter; sx++){
                        double pixelVal = inputImage[y+sy][x+sx];
                        sum += pixelVal * kernel[sy+kernelCenter][sx+kernelCenter];
                    }

                }
                int blurVal = (int)Math.round(sum);
                blurredImage[y][x] = Math.min(255, Math.max(0, blurVal));
            }
        }
        return blurredImage;
    }

    // apply sobel, then return magnitude and direction object of image
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
                double angle = Math.toDegrees(Math.atan2(-sumGy,sumGx));
                if(angle<0){
                    angle+=180.0;
                }
                direction[y][x] = angle;
            }
        }
        return new GradientResult(magnitude, direction);
    }

    // Edge detection methods

    // non-maximum suppression - thins out the lines
    private static int[][] NonMaximumSuppression(int[][] magnitude, double[][] direction){
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
                if(currPixel >= neighbor1 && currPixel >= neighbor2){
                    nonMaximumSuppression[y][x] = currPixel;
                } else{
                    nonMaximumSuppression[y][x] = 0;
                }
            }
        }
        return nonMaximumSuppression;
    }

    // hysteresis thresholding
    // uses two thresholds rather than one threshold
    // find the maximum magnitude, identify what pixels are strong and weak, connect pixels by spreading
    // edge of strong pixels to the neighboring weak pixels
    private static int[][] hysteresisThresholding(int[][] magnitude, int lowThreshold, int highThreshold){
        int width = magnitude[0].length;
        int height = magnitude.length;

        int strong = 255;
        int weak = 75;
        int zero = 0;

        int[][] hysteresisThresholding = new int[height][width];
        for(int y = 1; y<height-1; y++){
            for(int x = 1; x<width-1; x++){
                int mag = magnitude[y][x];
                if (mag >= highThreshold) {
                    hysteresisThresholding[y][x] = strong;
                } else if (mag >= lowThreshold) {
                    hysteresisThresholding[y][x] = weak;
                } else  {
                    hysteresisThresholding[y][x] = zero;
                }
            }
        }

        // Link weak edges to strong edges
        for(int y = 1; y<height-1; y++){
            for(int x = 1; x<width-1; x++){
                if (hysteresisThresholding[y][x] == weak) {
                    if (Check8Connectivity(hysteresisThresholding, y, x, strong)) {
                        hysteresisThresholding[y][x] = strong;
                    } else {
                        hysteresisThresholding[y][x] = 0;
                    }
                }
            }
        }

        // then after linking, set all weak edges to 0
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (hysteresisThresholding[y][x] == weak) {
                    hysteresisThresholding[y][x] = 0;
                }
            }
        }
        return hysteresisThresholding;
    }

    private static boolean Check8Connectivity(int[][] edges, int y, int x, int strongEdge) {
        for (int ny = y-1; ny < y+1; ny++) {
            for (int nx = x-1; nx < x+1; nx++) {
                if (ny == y && nx == x) {
                    continue;
                }
                if (edges[ny][nx] == strongEdge) {
                    return true;
                }
            }
        }
        return false;
    }

    private static BufferedImage createOutputImage(int[][] magnitudeImg){
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

    // don't use, results are not great
    private static int[] calculateHysteresisThresholds(int[][] magnitude) {
        List<Integer> values = new ArrayList<>();
        for (int[] row : magnitude) {
            for (int pixel : row) {
                if (pixel > 0) {
                    values.add(pixel);
                }
            }
        }
        Collections.sort(values);

        int median = values.isEmpty() ? 0 : values.get(values.size()/2);
        int low = (int) Math.max(0, (1.0 - 0.33) * median);
        int high = (int) Math.min(255, (1.0 + 0.33) * median);
        return new int[]{low, high};
    }

    // master function to output an image reduced to black and white edges
    public static BufferedImage edgeDetectionImage(File inputImageFile){
        if (!inputImageFile.exists()) {
            return null;
        }
        double[][] gaussianBlurKernel = GaussianKernelCreate(blurKernelSize, sigma);
        try {
            BufferedImage colorImage = ImageIO.read(inputImageFile);
            int[][] luminosityData = toLuminance(colorImage);
            int[][] blurData = gaussianBlur(luminosityData, gaussianBlurKernel);
            GradientResult gradientData = extractGradientMagnitude(blurData);
            int[][] thinnedImageData = NonMaximumSuppression(gradientData.magnitude, gradientData.direction);

            int[][] edgeData = hysteresisThresholding(thinnedImageData, 35, 80);

            return createOutputImage(edgeData);
        } catch (IOException e) {
            System.err.println("Error: Failed to read image file" + e.getMessage());
            return null;
        }
    }
}
