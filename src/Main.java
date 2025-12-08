import java.awt.image.BufferedImage;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

void main() throws IOException {
    String inputFilePath = "C:\\Users\\jonal\\IdeaProjects\\PoissonEditing\\src\\cat.png";
    String outputFilePath = "C:\\Users\\jonal\\IdeaProjects\\PoissonEditing\\src\\output.png";

    File inputFile = new File(inputFilePath);
    if(!inputFile.exists()){
        System.out.println("Input file does not exist");
    }
    BufferedImage colorImage = ImageIO.read(inputFile);
    int[][] luminosityData = ImageUtils.toLuminance(colorImage);
    int[][] blurData = ImageUtils.gaussianBlur(luminosityData);
    int[][] magnitudeData = ImageUtils.extractGradientMagnitude(blurData);

    BufferedImage outputImage = ImageUtils.createOutputImage(magnitudeData);
    File outputFile = new File(outputFilePath);
    ImageIO.write(outputImage, "png", outputFile);

}
