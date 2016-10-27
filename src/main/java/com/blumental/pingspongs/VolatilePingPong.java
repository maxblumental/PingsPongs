package com.blumental.pingspongs;

import org.openjdk.jmh.annotations.*;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
public class VolatilePingPong {

    @Param({"2", "4", "8", "16", "32", "64", "128", "256"})
    private int threadNumber;
    private volatile ImmutableWrapper state = new ImmutableWrapper();

    public static void main(String[] args) {
        VolatilePingPong pingPong = new VolatilePingPong();

        int N, M;
        try (Scanner scanner = new Scanner(System.in)) {
            N = scanner.nextInt();
            M = scanner.nextInt();
        }

        pingPong.run(N, M);
    }

    @Setup(Level.Invocation)
    public void setUp() {
        state = new ImmutableWrapper();
    }

    @Benchmark
    @BenchmarkMode({Mode.AverageTime})
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @Warmup(iterations = 5)
    @Fork(1)
    public void measureVolatile() {
        run(threadNumber, 1000);
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
        System.out.printf("Final state: %d\n", state.get());
    }

    private Thread createThread(int n, int N, int M) {
        return new Thread(() -> {
            int iterationsNumber = getIterationsNumber(n, N, M);
            int i = 0;
            while (i < iterationsNumber) {
                if (state.get() == getPreviousIndex(n, N)) {
                    state = new ImmutableWrapper(state, N, n);
                    i++;
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

    private static class ImmutableWrapper {
        private final int state;

        ImmutableWrapper() {
            state = 1;
        }

        ImmutableWrapper(ImmutableWrapper previous, int N, int n) {
            state = previous.state < N ? previous.state + 1 : 1;
        }

        int get() {
            return state;
        }
    }
}