package lab02stoppingthreads;

import java.io.IOException;
import java.time.Clock;

public class ResponsiveFileDownloader {

    private static volatile int progress = 0;
    private static final int PROGRESS_MAX = 100;

    private static class Download implements Runnable {
        @Override
        public void run() {
            progress = 0;
            while (progress < PROGRESS_MAX) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    return;
                }
                progress = progress + 1;
            }
        }
    }

    public static void main(String[] args) {
        var thread = new Thread(new Download());

        while (true) {
            // This clears the console
            System.out.print("\033[H\033[2J");
            System.out.flush();

            if (progress == 0) {
                System.out.println("Press enter to start downloading");
            } else if (progress == 100) {
                System.out.println("Download complete");
            }
            if (thread.isInterrupted()) {
                System.out.println("Download interrupted, press enter to restart downloading");
            }
            System.out.println("Time: " + Clock.systemDefaultZone().instant().toString());
            System.out.println("Progress: " + progress + " / " + PROGRESS_MAX);
            try {
                // Check if user pressed enter
                if (System.in.available() > 0 && System.in.read() == '\n') {
                    switch (thread.getState()) {
                        case NEW -> thread.start();
                        case TERMINATED -> {
                            thread = new Thread(new Download());
                            thread.start();
                        }
                        default -> thread.interrupt();
                    }
                } else {
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (IOException e) {
                return;
            }
        }
    }
}

