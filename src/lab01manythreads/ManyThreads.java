package lab01manythreads;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public class ManyThreads {

    private static final int N_THREADS = 10;

    private static class DoThread implements Runnable {
        private final int index;

        public DoThread(int index) {
            this.index = index;
        }

        @Override
        public void run() {
            System.out.println(index);

            if (index + 1 < N_THREADS) {
                new Thread(new DoThread(index + 1)).start();
            }

//            Supplier<Double> newDouble = Math::random;
            Supplier<Double> newDouble = () -> ThreadLocalRandom.current().nextDouble(1.0);
//            System.out.println(newDouble.get());
            var numbers = IntStream.range(0, 3000000 * (index + 1)).mapToDouble(i -> Math.random()).sum();

        }
    }

    public static void main(String[] args) {
        new Thread(new DoThread(0)).start();
    }
}