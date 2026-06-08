import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.io.IOException;
import java.nio.Buffer;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PoissonSolver {

    private final int N;
    private final double[][] X;
    private final double[][] b;
    private final int[][] R;
    private final double[] D;
    private final double omega;
    private double[][] prevX;
    private final double convergenceThreshold;

    // constructor
    public PoissonSolver(int N, double[][] initialGuess, double[][] rhs, int[][] neighborhoodMap,
                         double[] diagonals, double omega, double convergenceThreshold) {
        this.N = N;
        this.X = initialGuess;
        this.b = rhs;
        this.R = neighborhoodMap;
        this.D = diagonals;
        this.omega = omega;
        this.convergenceThreshold = convergenceThreshold;
        this.prevX = new double[N][3];
    }

    // Successive Over-Relaxation method to solve poisson equation
    // Pass by
    public void nextIteration(){
        if(prevX == null || prevX.length != N){
            prevX = new double[N][3];
        }
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < 3; j++) {
                prevX[i][j] = X[i][j];
                X[i][j] = b[i][j];
            }
            for (int n = 0; n < 4; n++) {
                if (R[i][n] >= 0) {
                    int index = R[i][n];
                    for (int j = 0; j < 3; j++) {
                        X[i][j] += X[index][j];
                    }
                }
                for (int j = 0; j < 3; j++) {
                    X[i][j] = prevX[i][j] + omega * (X[i][j]/D[i]) - prevX[i][j];
                }
            }
        }
    }
}
