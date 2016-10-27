package com.blumental.pingspongs;

import org.openjdk.jmh.annotations.*;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
public class SynchronizedPingPong {

    @Param({"2", "4", "8", "16", "32", "64", "128", "256"})
    private int threadNumber;
    private int state = 1;

    public static void main(String[] args) {
        SynchronizedPingPong pingPong = new SynchronizedPingPong();

        int N, M;
        try (Scanner scanner = new Scanner(System.in)) {
            N = scanner.nextInt();
            M = scanner.nextInt();
        }

        pingPong.run(N, M);
    }

    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @Warmup(iterations = 5)
    @Fork(1)
    @Benchmark
    public void measureSynchronized() {
        run(threadNumber, 1000);
    }

    @Setup(Level.Invocation)
    public void setUp() {
        state = 1;
    }

    private void run(int N, int M) {

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
