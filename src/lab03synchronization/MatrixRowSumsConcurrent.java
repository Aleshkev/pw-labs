package lab03synchronization;

import java.util.concurrent.CountDownLatch;
import java.util.function.IntBinaryOperator;

public class MatrixRowSumsConcurrent {

    private static final int ROWS = 10;
    private static final int COLUMNS = 100;

    private static class Matrix {

        private final int rows;
        private final int columns;
        private final IntBinaryOperator definition;

        public Matrix(int rows, int columns, IntBinaryOperator definition) {
            this.rows = rows;
            this.columns = columns;
            this.definition = definition;
        }

        public int[] rowSums() {
            int[] rowSums = new int[rows];
            for (int row = 0; row < rows; ++row) {
                int sum = 0;
                for (int column = 0; column < columns; ++column) {
                    sum += definition.applyAsInt(row, column);
                }
                rowSums[row] = sum;
            }
            return rowSums;
        }

        public int[] rowSumsConcurrent() {
            int[][] elements = new int[rows][columns];

            var waitForElementsOfRow = new CountDownLatch[rows];
            for (int i = 0; i < waitForElementsOfRow.length; ++i) waitForElementsOfRow[i] = new CountDownLatch(columns);

            for (int x = 0; x < columns; ++x) {
                int column = x;
                var thread = new Thread(() -> {
                    for (int row = 0; row < rows; ++row) {
                        elements[row][column] = definition.applyAsInt(row, column);
                        waitForElementsOfRow[row].countDown();
                    }
                    System.out.println("Column " + column + " ready.");
                });
                thread.start();
            }

            int[] rowSums = new int[rows];
            var waitForSums = new CountDownLatch(rows);
            for (int i = 0; i < rows; ++i) {
                int row = i;
                var thread = new Thread(() -> {
                    try {
                        waitForElementsOfRow[row].await();
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                    }
                    System.out.println("Row " + row + " ready.");
                    int sum = 0;
                    for (int column = 0; column < columns; ++column)
                        sum += elements[row][column];
                    rowSums[row] = sum;
                    waitForSums.countDown();
                });
                thread.start();
            }

            try {
                waitForSums.await();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(exception);
            }
            return rowSums;
        }
    }

    public static void main(String[] args) {
        MatrixRowSumsConcurrent.Matrix matrix = new MatrixRowSumsConcurrent.Matrix(ROWS, COLUMNS, (row, column) -> {
            int a = 2 * column + 1;
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
            }
            return (row + 1) * (a % 4 - 2) * a;
        });

        int[] rowSums = matrix.rowSums();
        int[] rowSums2 = matrix.rowSumsConcurrent();

        for (int i = 0; i < rowSums.length; i++) {
            System.out.println(i + " -> " + rowSums[i] + ", " + rowSums2[i]);
        }
    }
}
