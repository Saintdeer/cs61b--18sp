import edu.princeton.cs.algs4.Picture;

import java.awt.Color;
import java.util.Comparator;
import java.util.PriorityQueue;

public class SeamCarver {
    Picture p;
    double[][] energyMatrix;
    static double[][] distToClassVer;
    double[][] distTo;
    int[] horizontalSeam = null, verticalSeam = null;

    public SeamCarver(Picture p) {
        this.p = p;
        energyMatrix = new double[p.width()][p.height()];
        distTo = new double[p.width()][p.height()];

        for (int x = 0; x < p.width(); x++) {
            for (int y = 0; y < p.height(); y++) {
                energyMatrix[x][y] = energy(x, y);
            }
        }
    }

    // current picture
    public Picture picture() {
        return p;
    }

    // width of current picture
    public int width() {
        return p.width();
    }

    // height of current picture
    public int height() {
        return p.height();
    }

    // energy of pixel at column x and row y
    public double energy(int x, int y) {
        if (x < 0 || x >= p.width() || y < 0 || y >= p.height()) {
            throw new IndexOutOfBoundsException();
        }

        int leftX = x == 0 ? p.width() - 1 : x - 1,
                rightX = x == p.width() - 1 ? 0 : x + 1,
                upY = y == 0 ? p.height() - 1 : y - 1,
                downY = y == p.height() - 1 ? 0 : y + 1;

        Color leftC = p.get(leftX, y),
                rightC = p.get(rightX, y),
                upC = p.get(x, upY),
                downC = p.get(x, downY);

        double deltaX = squareOfDifference(leftC.getRed(), rightC.getRed())
                + squareOfDifference(leftC.getBlue(), rightC.getBlue())
                + squareOfDifference(leftC.getGreen(), rightC.getGreen()),
                deltaY = squareOfDifference(upC.getRed(), downC.getRed())
                        + squareOfDifference(upC.getGreen(), downC.getGreen())
                        + squareOfDifference(upC.getBlue(), downC.getBlue());

        return deltaX + deltaY;
    }

    private double squareOfDifference(int rgb1, int rgb2) {
        double abs = rgb1 > rgb2 ? rgb1 - rgb2 : rgb2 - rgb1;
        return abs * abs;
    }

    private void changeEnergyMatrix() {
        double[][] newEnergyMatrix = new double[p.width()][p.height()];
        if (horizontalSeam != null) {
            int column = 0;
            for (int changedIndex : horizontalSeam) {
                for (int row = 0; row < p.height(); row++) {
                    if (row == changedIndex - 1 || row == changedIndex) {
                        newEnergyMatrix[column][row] = energy(column, row);
                    } else if (row > changedIndex) {
                        newEnergyMatrix[column][row] = energyMatrix[column][row + 1];
                    } else {
                        newEnergyMatrix[column][row] = energyMatrix[column][row];
                    }
                }

                column++;
            }
        } else if (verticalSeam != null) {
            int row = 0;
            for (int changedIndex : verticalSeam) {
                for (int column = 0; column < p.width(); column++) {
                    if (column == changedIndex - 1 || column == changedIndex) {
                        newEnergyMatrix[column][row] = energy(column, row);
                    } else if (column > changedIndex) {
                        newEnergyMatrix[column][row] = energyMatrix[column + 1][row];
                    } else {
                        newEnergyMatrix[column][row] = energyMatrix[column][row];
                    }
                }

                row++;
            }
        } else {
            return;
        }
        horizontalSeam = null;
        verticalSeam = null;
        energyMatrix = newEnergyMatrix;
    }

    // sequence of indices for horizontal seam
    public int[] findHorizontalSeam() {
        Picture newP = new Picture(p.height(), p.width());
        for (int x = 0; x < p.width(); x++) {
            for (int y = 0; y < p.height(); y++) {
                newP.set(y, x, p.get(x, y));
            }
        }
        SeamCarver sc = new SeamCarver(newP);
        return sc.findVerticalSeam();
    }

    private static class IntegerComparator implements Comparator<Integer> {
        @Override
        public int compare(Integer o1, Integer o2) {
            int width = distToClassVer.length;

            int height1 = o1 / width, height2 = o2 / width;

            double energy1 = distToClassVer[o1 - height1 * width][height1];
            double energy2 = distToClassVer[o2 - height2 * width][height2];

            double difference = energy1 - energy2;
            if (difference < 0) {
                return -1;
            } else if (difference == 0) {
                return 0;
            } else {
                return 1;
            }
        }
    }

    // sequence of indices for vertical seam
    public int[] findVerticalSeam() {
        changeEnergyMatrix();

        distToClassVer = distTo;
        PriorityQueue<Integer> pq = new PriorityQueue<>(new IntegerComparator());

        int height = p.height(), width = p.width();
        int row = height - 1;
        int sum = row * width;
        for (int column = 0; column < width; column++) {
            distTo[column][row] = energyMatrix[column][row];
            pq.add(sum + column);
        }

        int smallestIndex;

        // bottom to top
        while (true) {
            smallestIndex = pq.remove();
            if (smallestIndex < width) {
                break;
            }

            int smallHeight = smallestIndex / width;
            int smallColumn = smallestIndex - smallHeight * width;

            addNeighbors(smallColumn, smallHeight, pq);
        }

        return findPath(smallestIndex);
    }

    private void addNeighbors(int smallColumn, int smallHeight, PriorityQueue<Integer> pq) {
        int width = p.width();

        double currentDistTo = distTo[smallColumn][smallHeight];
        int topLeftColumn = smallColumn - 1, topRightColumn = smallColumn + 1;
        int topRow = smallHeight - 1;

        // add top middle
        double old = distTo[smallColumn][topRow];
        double current = currentDistTo + energyMatrix[smallColumn][topRow];
        if (old == 0) {
            distTo[smallColumn][topRow] = current;
            pq.add(topRow * width + smallColumn);
        }


        // add top left
        if (topLeftColumn >= 0) {
            old = distTo[topLeftColumn][topRow];
            current = currentDistTo + energyMatrix[topLeftColumn][topRow];
            if (old == 0) {
                distTo[topLeftColumn][topRow] = current;
                pq.add(topRow * width + topLeftColumn);
            }
        }

        // add top right
        if (topRightColumn < width) {
            old = distTo[topRightColumn][topRow];
            current = currentDistTo + energyMatrix[topRightColumn][topRow];
            if (old == 0) {
                distTo[topRightColumn][topRow] = current;
                pq.add(topRow * width + topRightColumn);
            }
        }
    }

    private int[] findPath(int smallColumn) {
        int width = p.width(), height = p.height();
        int smallHeight = 0;

        int[] path = new int[height];
        path[0] = smallColumn;

        // top to bottom
        for (int lowRow = 1; lowRow < height; lowRow++) {
            for (int offset = -1; offset <= 1; offset++) {
                int lowColumn = smallColumn + offset;
                if (lowColumn < 0 || lowColumn >= width) {
                    continue;
                }

                double differenceEnergy = distTo[smallColumn][smallHeight]
                        - distTo[lowColumn][lowRow];
                if (differenceEnergy == energyMatrix[smallColumn][smallHeight]) {
                    path[lowRow] = lowColumn;
                    smallColumn = lowColumn;
                    smallHeight = lowRow;
                    break;
                }
            }
        }
        return path;
    }

    // remove horizontal seam from picture
    public void removeHorizontalSeam(int[] seam) {
        check(seam, p.width());

        p = SeamRemover.removeHorizontalSeam(p, seam);
        horizontalSeam = seam;
        changeEnergyMatrix();
    }

    // remove vertical seam from picture
    public void removeVerticalSeam(int[] seam) {
        check(seam, p.height());

        p = SeamRemover.removeVerticalSeam(p, seam);
        verticalSeam = seam;
        changeEnergyMatrix();
    }

    private void check(int[] seam, int length) {
        if (seam.length != length) {
            throw new IllegalArgumentException();
        }

        int first = seam[0];
        for (int s : seam) {
            int difference = first - s;
            if (difference > 1 || difference < -1) {
                throw new IllegalArgumentException();
            }
            first = s;
        }
    }
}