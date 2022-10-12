package lab02stoppingthreads;

import lab01manythreads.Primes;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Primes2 {
    private static class Result {
        public volatile boolean isPrime;

        public Result() {
            isPrime = true;
        }
    }

    private static class Check implements Runnable {
        private final int start;
        private final int step;
        private final int n;
        private final Primes2.Result result;

        public Check(int start, int step, int n, Primes2.Result result) {
            this.start = start;
            this.step = step;
            this.n = n;
            this.result = result;
        }

        @Override
        public void run() {
            for (int x = start; x * x <= n; x += step) {
                if (!result.isPrime) return;

                if (n % x == 0 && n != x) {
                    result.isPrime = false;
                    return;
                }
            }
        }
    }

    public static boolean isPrime(int n) {
        for (int x : List.of(2, 3, 5, 7, 11, 13, 17, 19, 23, 29)) {
            if (n % x == 0 && n != x) return false;
        }

        var result = new Primes2.Result();
        var threads = Stream.of(31, 37, 41, 43, 47, 49, 53, 59).map(start -> new Thread(new Primes2.Check(start, 30, n, result))).toList();
        threads.forEach(Thread::start);

        try {
            for (var thread : threads)
                thread.join();
        } catch (InterruptedException exception) {
            throw new RuntimeException("main thread has been interrupted");
        }

        return result.isPrime;
    }

    public static void main(String[] args) {
        System.out.println(IntStream.range(2, 10000 + 1).map(x -> (isPrime(x) ? 1 : 0)).sum());
    }
}
