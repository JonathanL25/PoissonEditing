import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

void main() throws IOException {
    String inputFilePath = "C:\\Users\\jonal\\IdeaProjects\\PoissonEditing\\Images\\Input\\img_1.png";
    String outputFolderPath = "C:\\Users\\jonal\\IdeaProjects\\PoissonEditing\\Images\\output image";

    File inputFile = new File(inputFilePath);
    if(!inputFile.exists()){
        throw new IOException("Input file does not exist");
    }
    BufferedImage outputImage = CannyEdgeDetector.edgeDetectionImage(inputFile);
    if (outputImage != null) {
        File outputFolder = new File(outputFolderPath);
        if (!outputFolder.exists()) {
            outputFolder.mkdir();
        }

        File outputFile = new File(outputFolder, "output.png");

        try {
            ImageIO.write(outputImage, "png", outputFile);
            System.out.println("Image saved to " + outputFile.getAbsolutePath());
        } catch (Exception e) {
            System.out.println("Error saving image" + e.getMessage());
        }
    } else {
        System.out.println("Input file does not exist");
    }
}
