import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class BlendOperation {
    public static boolean[][] ExtractMask(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        boolean[][] mask = new boolean[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = image.getRGB(x, y);
                if (PoissonUtils.colorA(argb) > 30) {
                    mask[y][x] = true;
                }
            }
        }
        return mask;
    }

    public static class PoissonData {
        public int N;                           // number of masked pixels
        public double[][] rhs;                  // solved laplacian gradient
        public double[][] initialGuess;         // initial x in Ax = b
        public int[][] neighborhood;            // int[N][4], adjacency map
        public List<Point> targetCoords;        // target coords to repair target image afterward
    }

    // imageA is cut out image
    // imageB is background image
    // mask is imageA mask
    // placeX and placeY are offset from top left
    public static PoissonData BuildInitialSystem(BufferedImage imageA, BufferedImage imageB,
                                                 boolean[][] mask, int placeX, int placeY) {
        int widthA = imageA.getWidth();
        int heightA = imageA.getHeight();
        int widthB = imageB.getWidth();
        int heightB = imageB.getHeight();

        int[][] numericalMask = new int[heightA][widthA];
        for (int y = 0; y < heightA; y++) {
            for (int x = 0 ; x < widthA; x++) {
                numericalMask[y][x] = -1;
            }
        }

        List<Point> sourceCoords = new ArrayList<>();
        List<Point> targetCoords = new ArrayList<>();
        for (int y = 0; y < heightA; y++) {
            for (int x = 0; x < widthA; x++) {
                if (mask[y][x]) {
                    int bx = x + placeX;
                    int by = y + placeY;
                    if (bx > 0 && bx < widthB - 1 && by > 0 && by < heightB - 1) {
                        numericalMask[y][x] = sourceCoords.size();
                        sourceCoords.add(new Point(x, y));
                        targetCoords.add(new Point(bx, by));
                    }
                }
            }
        }

        // need number of masked pixels, need RHS side (retrieve channels -> calculate laplacian)
        // need neighbor (check all four directions for each pixel and check
        int N = sourceCoords.size();
        int[][] channelR = PoissonUtils.extractColorChannel(imageA, 0);
        int[][] channelG = PoissonUtils.extractColorChannel(imageA, 1);
        int[][] channelB = PoissonUtils.extractColorChannel(imageA, 2);

        // Poisson Data struct vars
        double[][] rhs = new double[N][3];
        int[][] neighborhood = new int[N][4];
        double[][] initialGuess = new double[N][3];

        // utility directions map
        int[][] dirs = new int[][]{{1, 0}, {-1, 0},
                {0, 1}, {0, -1}};

        for (int n = 0; n < N; n++) {
            int ax = sourceCoords.get(n).x;
            int ay = sourceCoords.get(n).y;
            int bx = targetCoords.get(n).x;
            int by = targetCoords.get(n).y;

            // creating initial X based on background image of masked section
            int argbB_init = imageB.getRGB(bx, by);
            initialGuess[n][0] = PoissonUtils.colorR(argbB_init);
            initialGuess[n][1] = PoissonUtils.colorG(argbB_init);
            initialGuess[n][2] = PoissonUtils.colorB(argbB_init);

            // build rhs, via finite difference scheme of laplacian operator
            // minor workaround to account for mask behavior
            for (int d = 0; d < 4; d++) {
                int dax = ax + dirs[d][0];
                int day = ay + dirs[d][1];
                boolean inMask = (dax >= 0 && dax < widthA && day >= 0 && day < heightA &&
                        numericalMask[day][dax] != -1);

                if (inMask) {
                    neighborhood[n][d] = numericalMask[day][dax];

                    // workaround due to mask having transparent/(0,0,0,0) pixels, screws w/ calculation
                    rhs[n][0] += (channelR[ay][ax] - channelR[day][dax]);
                    rhs[n][1] += (channelG[ay][ax] - channelG[day][dax]);
                    rhs[n][2] += (channelB[ay][ax] - channelB[day][dax]);
                } else {
                    neighborhood[n][d] = -1;
                    int dbx = bx + dirs[d][0];
                    int dby = by + dirs[d][1];

                    // tethers finite laplacian difference to border color
                    // behaves like heat diff eq so it bleeds inside accordingly to border color
                    int argbB = imageB.getRGB(dbx, dby);
                    rhs[n][0] += PoissonUtils.colorR(argbB);
                    rhs[n][1] += PoissonUtils.colorG(argbB);
                    rhs[n][2] += PoissonUtils.colorB(argbB);
                }
            }

        }

        PoissonData pd = new PoissonData();
        pd.N = N;
        pd.rhs = rhs;
        pd.initialGuess = initialGuess;
        pd.targetCoords = targetCoords;
        pd.neighborhood = neighborhood;

        return pd;
    }

    public static BufferedImage rebuildImage(BufferedImage image, double[][] solution,
                                             List<Point> targetCoords) {
        int N = targetCoords.size();
        for (int i = 0; i < N; i++) {
            Point pt = targetCoords.get(i);
            int r = Math.min(255, Math.max(0, (int) Math.round(solution[i][0])));
            int g = Math.min(255, Math.max(0, (int) Math.round(solution[i][1])));
            int b = Math.min(255, Math.max(0, (int) Math.round(solution[i][2])));

            image.setRGB(pt.x, pt.y, (0xFF << 24| r << 16 | g << 8 | b));
        }
        return image;
    }

}
