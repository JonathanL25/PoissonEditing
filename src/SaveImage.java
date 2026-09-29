import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.imageio.ImageIO;

public class SaveImage {
    public static void saveToDownloads(BufferedImage image, String fileName, String format) throws IOException {
        Path downloadsPath = Paths.get(System.getProperty("user.home"), "Downloads");

        File downloadsDirectory = downloadsPath.toFile();
        if (!downloadsDirectory.exists()) {
            return;
        }
        File outputFile = downloadsPath.resolve(fileName + '.' + format).toFile();
        boolean fileOutputSuccess = ImageIO.write(image, format, outputFile);
        if (!fileOutputSuccess) {
            throw new IOException("Failed to save image to " + outputFile.getAbsolutePath());
        }
        System.out.println("Saved image to " + outputFile.getAbsolutePath());
    }
}
