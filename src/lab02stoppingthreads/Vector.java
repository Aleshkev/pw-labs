package lab02stoppingthreads;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;

public class Vector {

    volatile int[] numbers;

    private final int chunk_length = 10;

    public Vector(int[] numbers) {
        this.numbers = Arrays.copyOf(numbers, numbers.length);
    }

    private static class SumVectors implements Runnable {
        private final Vector a, b;
        private final Vector result;
        private final int start, stop;

        private SumVectors(Vector a, Vector b, Vector result, int start, int stop) {
            this.start = start;
            this.stop = stop;
            this.a = a;
            this.b = b;
            this.result = result;
        }

        @Override
        public void run() {
            for (int i = start; i < stop; ++i) {
                result.numbers[i] = a.numbers[i] + b.numbers[i];
            }
        }
    }

    private static class DotVectors implements Runnable {
        private final Vector a, b;
        private int result;
        private final int start, stop;

        private DotVectors(Vector a, Vector b, int start, int stop) {
            this.a = a;
            this.b = b;
            this.start = start;
            this.stop = stop;
            result = 0;
        }

        @Override
        public void run() {
            for (var i = start; i < stop; ++i) {
                result += a.numbers[i] * b.numbers[i];
            }
        }
    }

    public Vector sum(Vector other) {
        if (numbers.length != other.numbers.length)
            throw new IllegalArgumentException("vectors have different dimensions");

        var result = new Vector(new int[numbers.length]);
        var threads = new ArrayList<Thread>();
        for (int start = 0; start < numbers.length; start += chunk_length) {
            int stop = Math.min(start + chunk_length, numbers.length);
            var thread = new Thread(new SumVectors(this, other, result, start, stop));
            thread.start();
            threads.add(thread);
        }

        try {
            for (var thread : threads) thread.join();
        } catch (InterruptedException exception) {
            throw new RuntimeException(exception);
        }

        return result;
    }

    public int dot(Vector other) {
        if (numbers.length != other.numbers.length)
            throw new IllegalArgumentException("vectors have different dimensions");

        var runnables = new ArrayList<DotVectors>();
        var threads = new ArrayList<Thread>();
        for (int start = 0; start < numbers.length; start += chunk_length) {
            int stop = Math.min(start + chunk_length, numbers.length);
            var runnable = new DotVectors(this, other, start, stop);
            var thread = new Thread(runnable);
            thread.start();
            runnables.add(runnable);
            threads.add(thread);
        }

        try {
            for (var thread : threads) thread.join();
        } catch (InterruptedException exception) {
            throw new RuntimeException(exception);
        }

        // If the threads operated on a single variable, with += not being an atomic operation, the code would be wrong.
        int result = 0;
        for (var runnable : runnables) result += runnable.result;
        return result;
    }

    public Vector sumSeq(Vector other) {
        if (numbers.length != other.numbers.length)
            throw new IllegalArgumentException("vectors have different dimensions");
        var result = new Vector(new int[numbers.length]);
        for (var i = 0; i < numbers.length; ++i) {
            result.numbers[i] = numbers[i] + other.numbers[i];
        }
        return result;
    }

    public int dotSeq(Vector other) {
        if (numbers.length != other.numbers.length)
            throw new IllegalArgumentException("vectors have different dimensions");
        int result = 0;
        for (var i = 0; i < numbers.length; ++i) {
            result += numbers[i] * other.numbers[i];
        }
        return result;
    }

    public static Vector random(Random random, int n) {
        return new Vector(IntStream.range(0, n).map(i -> random.nextInt(20)).toArray());
    }

    @Override
    public String toString() {
        return "Vector{" + Arrays.toString(numbers) + '}';
    }

    public static void main(String[] args) {
        var random = new Random();

        var a = random(random, 95);
        var b = random(random, 95);

        System.out.println("a = " + a);
        System.out.println("b = " + b);
        System.out.println("a + b = " + a.sum(b));
        System.out.println("a + b = " + a.sumSeq(b));
        System.out.println("a * b = " + a.dot(b));
        System.out.println("a * b = " + a.dotSeq(b));
    }
}
