import java.awt.event.MouseAdapter;
import java.awt.image.BufferedImage;
import javax.swing.*;
import java.awt.geom.Path2D;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.*;


public class ImageMergerApp extends JFrame{
    private BufferedImage sourceImage;
    private BufferedImage extractedImage;
    private BufferedImage backgroundImage;

    private LassoPanel lassoPanel;
    private CardLayout cardLayout;
    private CompositePanel compositePanel;
    private JPanel mainContainer;

    private JButton btnUpload;
    private JButton btnNext;
    private JButton btnReset;
    private JButton btnMerge;

    public ImageMergerApp() {
        super("Image Merging Tool");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 700);
        setLocationRelativeTo(null);
        cardLayout = new CardLayout();
        mainContainer = new JPanel(cardLayout);
        lassoPanel = new LassoPanel();
        compositePanel = new CompositePanel();

        mainContainer.add(lassoPanel, "LASSO");
        mainContainer.add(compositePanel, "COMPOSITE");

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        btnUpload = new JButton("Upload");
        btnNext = new JButton("Next");
        btnReset = new JButton("Reset");
        btnMerge = new JButton("Merge");

        btnNext.setEnabled(false);
        btnMerge.setEnabled(false);

        toolBar.add(btnUpload);
        toolBar.add(btnNext);
        toolBar.add(btnReset);
        toolBar.add(btnMerge);

        add(toolBar, BorderLayout.NORTH);
        add(mainContainer, BorderLayout.CENTER);

        btnUpload.addActionListener(e -> handleUpload());
        btnNext.addActionListener(e -> goToCompositeStep());
        btnReset.addActionListener(e -> resetAll());
        btnMerge.addActionListener(e -> {
            try {
                mergeImage();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    private void handleUpload() {
        JFileChooser chooser = new JFileChooser();
        chooser.setAcceptAllFileFilterUsed(false);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Image Files",
                                                         "jpg", "png", "jpeg");
        chooser.addChoosableFileFilter(filter);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                BufferedImage loadedImage = ImageIO.read(chooser.getSelectedFile());
                if (loadedImage == null) {
                    JOptionPane.showMessageDialog(this, "Image not found!", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (compositePanel.isVisible()) {
                    backgroundImage = loadedImage;
                    btnMerge.setEnabled(true);
                    compositePanel.repaint();
                } else {
                    sourceImage = loadedImage;
                    lassoPanel.resetLasso();
                    lassoPanel.repaint();
                    btnNext.setEnabled(false);
                }
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Failed to load image" + ex.getMessage());
            }
        }
    }

    private void goToCompositeStep() {
        if (extractedImage == null) {
            return;
        }

        btnUpload.setText("Upload background");
        btnNext.setEnabled(false);
        compositePanel.initPlacement(extractedImage);
        cardLayout.show(mainContainer, "COMPOSITE");
    }

    private void resetAll() {
        sourceImage = null;
        extractedImage = null;
        backgroundImage = null;
        compositePanel.reset();
        lassoPanel.resetLasso();
        cardLayout.show(mainContainer, "LASSO");
        btnUpload.setText("Upload Image");
        btnNext.setEnabled(false);
        btnMerge.setEnabled(false);
        repaint();
    }

    private void mergeImage() throws IOException {
        BufferedImage mergedResult = compositePanel.merge();
        SaveImage.saveToDownloads(mergedResult, "Merge test", "jpeg");
    }
    // Panel 1: Lasso image extraction
    private class LassoPanel extends JPanel {
        private Path2D.Float lassoPath = null;

        public LassoPanel() {

            MouseAdapter mouseHandler = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (sourceImage == null) {
                        return;
                    }
                    lassoPath = new Path2D.Float();
                    lassoPath.moveTo(e.getX(), e.getY());
                    extractedImage = null;
                    btnNext.setEnabled(false);
                    repaint();
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (lassoPath != null) {
                        lassoPath.lineTo(e.getX(), e.getY());
                        repaint();
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (lassoPath != null) {
                        lassoPath.closePath();
                        extractLassoSelection();
                        btnNext.setEnabled(true);
                        repaint();
                    }
                }
            };
            addMouseListener(mouseHandler);
            addMouseMotionListener(mouseHandler);
        }

        public void resetLasso() {
            lassoPath = null;
        }

        public void extractLassoSelection() {
            Rectangle bounds = lassoPath.getBounds();
            if (bounds.width <= 0 || bounds.height <= 0) {
                return;
            }
            // clamp user drawn lasso
            int x = Math.max(0, bounds.x);
            int y = Math.max(0, bounds.y);
            int w = Math.min(bounds.width, sourceImage.getWidth() - x);
            int h = Math.min(bounds.height, sourceImage.getHeight() - y);

            if (w <= 0 || h <= 0) {
                return;
            }

            extractedImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = extractedImage.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2d.translate(-x, -y);
            g2d.setClip(lassoPath);
            g2d.drawImage(sourceImage, 0, 0,null);
            g2d.dispose();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            if (sourceImage != null) {
                g2d.drawImage(sourceImage, 0, 0, null);
            } else {
                g2d.setColor(Color.lightGray);
                g2d.drawString("Upload an image to start lasso selection", 30, 40);
            }
            if (lassoPath != null) {
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // highlight selection area
                g2d.setColor(new Color(0, 150, 255, 60));
                g2d.fill(lassoPath);

                // Dashed lines for lasso path
                Stroke dashedLine = new BasicStroke(1.0f,
                                                    BasicStroke.CAP_BUTT,
                                                    BasicStroke.JOIN_BEVEL,
                                                    0, new float[]{6},
                                                    0);
                g2d.setStroke(dashedLine);
                g2d.setColor(Color.BLACK);
                g2d.draw(lassoPath);
            }
            g2d.dispose();
        }
    }

    // Panel 2: Composite Panel
    private class CompositePanel extends JPanel {
        private BufferedImage cutoutImage;
        private Point cutoutPos;
        private Point dragOffSet = null;
        public CompositePanel() {
            setBackground(Color.GRAY);
            MouseAdapter mouseHandler = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (cutoutImage == null) {
                        return;
                    }
                    Rectangle cutoutBound = new Rectangle(cutoutPos.x, cutoutPos.y, cutoutImage.getWidth(), cutoutImage.getHeight());
                    if (cutoutBound.contains(e.getPoint())) {
                        dragOffSet = new Point(e.getX() - cutoutPos.x, e.getY() - cutoutPos.y);
                    }
                }
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (dragOffSet != null) {
                        cutoutPos = new Point(e.getX() - dragOffSet.x, e.getY() - dragOffSet.y);
                        repaint();
                    }
                }
                @Override
                public void mouseReleased(MouseEvent e) {
                    dragOffSet = null;
                }
            };
            addMouseListener(mouseHandler);
            addMouseMotionListener(mouseHandler);
        }

        public void initPlacement(BufferedImage cutoutImage) {
            cutoutPos = new Point(40, 40);
            this.cutoutImage = cutoutImage;
            repaint();
        }

        public void reset() {
            this.cutoutImage = null;
            this.dragOffSet = null;
            this.cutoutPos = new Point(40, 40);
        }

        public BufferedImage merge() {
            if (cutoutImage == null || backgroundImage == null) {
                return null;
            }
            boolean[][] mask = BlendOperation.ExtractMask(cutoutImage);
            BlendOperation.PoissonData pd = BlendOperation.BuildInitialSystem(cutoutImage, backgroundImage, mask,
                                                                              cutoutPos.x, cutoutPos.y);
            PoissonSolver ps = new PoissonSolver(pd.N, pd.initialGuess, pd.rhs, pd.neighborhood,
                                                 1.95, 1e-10);
            ps.solve(10000);
            BufferedImage mergeResult = BlendOperation.rebuildImage(backgroundImage, ps.getSolution(), pd.targetCoords);
            backgroundImage = mergeResult;
            this.cutoutImage = null;
            repaint();
            return mergeResult;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();

            if (backgroundImage != null) {
                g2d.drawImage(backgroundImage, 0, 0, null);
            } else {
                g2d.drawString("Upload Background Image", 40, 40);
            }

            if (cutoutImage != null) {
                g2d.drawImage(cutoutImage, cutoutPos.x, cutoutPos.y, null);
                g2d.setColor(new Color(255, 255, 120));
                g2d.drawRect(cutoutPos.x, cutoutPos.y, cutoutImage.getWidth(), cutoutImage.getHeight());
            }

            g2d.dispose();
        }
    }
}