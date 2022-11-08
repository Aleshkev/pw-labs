package lab04shareddata;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.IntBinaryOperator;
import java.util.stream.IntStream;

public class MatrixRowSumsThreadsafe {
  private static final int ROWS = 10;
  private static final int COLUMNS = 100;

  public static void main(String[] args) {
    MatrixRowSumsThreadsafe.Matrix matrix = new MatrixRowSumsThreadsafe.Matrix(ROWS, COLUMNS, (row, column) -> {
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
//      Map<Integer, LinkedBlockingQueue<Integer>> elementsOfRow = new ConcurrentHashMap<>();
//      for (int y = 0; y < rows; ++y)
//        elementsOfRow.put(y, new LinkedBlockingQueue<>());

      List<LinkedBlockingQueue<Integer>> elementsOfRow =
              IntStream.range(0, rows).mapToObj(i -> new LinkedBlockingQueue<Integer>()).toList();

      for (int x = 0; x < columns; ++x) {
        int column = x;
        var thread = new Thread(() -> {
          for (int row = 0; row < rows; ++row) {
            var cell = definition.applyAsInt(row, column);
            try {
              elementsOfRow.get(row).put(cell);
            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            }
          }
//          System.out.println("Column " + column + " ready.");
        });
        thread.start();
      }

      int[] rowSums = new int[rows];
      var waitForSums = new CountDownLatch(rows);
      for (int i = 0; i < rows; ++i) {
        int row = i;
        var thread = new Thread(() -> {
          for (int column = 0; column < columns; ++column) {
            try {
              rowSums[row] += elementsOfRow.get(row).take();
            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            }
          }
//          System.out.println("Row " + row + " ready.");
          waitForSums.countDown();
        });
        thread.start();
      }
      try {
        waitForSums.await();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }

      return rowSums;
    }
  }
}
