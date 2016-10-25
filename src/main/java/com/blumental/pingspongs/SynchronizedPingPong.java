package com.blumental.pingspongs;

import java.util.Scanner;

public class SynchronizedPingPong {

    private int state = 1;

    public static void main(String[] args) {
        SynchronizedPingPong pingPong = new SynchronizedPingPong();
        pingPong.run();
    }

    private void run() {
        int N, M;
        try (Scanner scanner = new Scanner(System.in)) {
            N = scanner.nextInt();
            M = scanner.nextInt();
        }

        Thread[] threads = new Thread[N];

        for (int i = 0; i < threads.length; i++) {
            threads[i] = createThread(i + 1, N, M);
        }

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                System.err.println("Exception during join()");
            }
        }
        System.out.printf("Final state: %d\n", state);
    }

    private Thread createThread(int n, int N, int M) {
        return new Thread(() -> {
            int iterationsNumber = getIterationsNumber(n, N, M);
            int i = 0;
            while (i < iterationsNumber) {
                synchronized (SynchronizedPingPong.this) {
                    if (state == getPreviousIndex(n, N)) {
                        state = state < N ? state + 1 : 1;
                        i++;
                        SynchronizedPingPong.this.notifyAll();
                    } else {
                        try {
                            SynchronizedPingPong.this.wait();
                        } catch (InterruptedException e) {
                            System.err.println("Exception during wait()");
                        }
                    }
                }
            }
        });
    }

    private int getPreviousIndex(int n, int N) {
        return n > 1 ? n - 1 : N;
    }

    private int getIterationsNumber(int n, int N, int M) {
        int k = ++M / N, r = M - k * N;
        int iterationsNumber = k;
        if (r >= n) iterationsNumber++;
        if (n == 1) iterationsNumber--;
        return iterationsNumber;
    }
}
