package lab05computing;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.IntBinaryOperator;
import java.util.stream.IntStream;

public class MatrixRowSumsExecutors {
    private static final int ROWS = 10;
    private static final int COLUMNS = 100;

    public static void main(String[] args) {
        MatrixRowSumsExecutors.Matrix matrix = new MatrixRowSumsExecutors.Matrix(ROWS, COLUMNS, (row, column) -> {
            int a = 2 * column + 1;
            try {
                Thread.sleep((ROWS + COLUMNS - column - row) / (ROWS + COLUMNS) * 300L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return (row + 1) * (a % 4 - 2) * a;
        });

        int[] rowSums = matrix.rowSums();
        int[] rowSums2 = matrix.rowSumsConcurrent();

        for (int i = 0; i < rowSums.length; i++) {
            System.out.println(i + " -> " + rowSums[i] + ", " + rowSums2[i]);
        }
    }

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

            var tasks = new ArrayList<Callable<Integer>>();
            for (int y = 0; y < rows; ++y) {
                for (int x = 0; x < columns; ++x) {
                    int column = x;
                    int row = y;
                    tasks.add(() -> elements[row][column] = definition.applyAsInt(row, column));
                }
            }
            List<Future<Integer>> promises;
            try {
                promises = Executors.newFixedThreadPool(4).invokeAll(tasks);
                for (Future<Integer> promise : promises) {
                    promise.get();
                }
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }

            int[] rowSums = new int[rows];
            for (int y = 0; y < rows; ++y) {
                for (int x = 0; x < columns; ++x) {
                    rowSums[y] += elements[y][x];
                }
            }

            return rowSums;
        }
    }
}
