public class PoissonSolver {
    private final int N;           // Number of pixels in the mask
    private final double[][] X;    // current unsolved solution, [N][3] for RGB channels
    private final double[][] b;    // RHS, also the extracted gradient we have
    private final int[][] R;       // neighborhood map, up/down/right/left for ease of calculation
    private final double omega;    // used in successive over-relaxation, val between 1.0-2.0
    private final double convergenceThreshold;  // threshold at which we stop the estimation loop

    // constructor
    public PoissonSolver(int N, double[][] initialGuess, double[][] rhs, int[][] neighborhoodMap,
                         double omega, double convergenceThreshold) {
        this.N = N;
        this.X = initialGuess;
        this.b = rhs;
        this.R = neighborhoodMap;
        this.omega = omega;
        this.convergenceThreshold = convergenceThreshold;
    }

    // Successive Over-Relaxation method to solve poisson equation
    // moves into next iteration of the gauss-seidel method each time the function is called
    //
    // Returns:
    // - change amount
    public double nextIteration(){
        double maxChange = 0.0;
        // needs to loop over the entire masked portion
        for (int n = 0; n < N; n++) {
            // need to store old value and current pixel
            // used to calculate finite laplacian diff
            double[] currentRHS = new double[3];

            for (int i = 0 ; i < 3; i++) {
                currentRHS[i] = b[n][i];
            }

            // calculating finite laplacian difference
            // sum all adjacent pixels
            // need to account for border pixels
            for (int i = 0; i < 4; i++) {
                int neighborIDX = R[n][i];
                if (neighborIDX < 0) {
                    continue;
                }
                for (int j = 0; j < 3; j++) {
                    currentRHS[j] += X[neighborIDX][j];
                }
            }

            // run gauss seidel on each color channel
            for (int i = 0; i < 3; i++) {
                double oldVal = X[n][i];
                double gsUpdate = (currentRHS[i])/4.0;
                X[n][i] = oldVal + omega * (gsUpdate - oldVal);

                double change = Math.abs(X[n][i] - oldVal);
                if (change > maxChange) {
                    maxChange = change;
                }
            }
        }
        return maxChange;
    }

    public int solve(int maxIteration) {
        int iterationCount = 0;
        while (iterationCount < maxIteration) {
            double error = nextIteration();
            iterationCount++;
            if (error < convergenceThreshold) {
                System.out.printf("Solver converged at iteration %d (Error: %.5f)\n",
                                  iterationCount, error);
                break;
            }
        }

        if (iterationCount == maxIteration) {
            System.out.printf("Solver exceeded maximum iterations of %d\n", maxIteration);
        }
        return iterationCount;
    }

    public double[][] getSolution() {
        return this.X;
    }
}
