package com.blumental.pingspongs;

import org.openjdk.jmh.annotations.*;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

@State(Scope.Benchmark)
public class ReentrantLockPingPong {

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();
    @Param({"2", "4", "8", "16", "32", "64", "128", "256"})
    private int threadNumber;
    private int state = 1;

    public static void main(String[] args) {
        ReentrantLockPingPong pingPong = new ReentrantLockPingPong();

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
    public void measureReentrantLock() {
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
        System.out.printf("Final state: %d\n",   state);
    }

    private Thread createThread(int n, int N, int M) {
        return new Thread(() -> {
            int iterationsNumber = getIterationsNumber(n, N, M);
            int i = 0;
            while (i < iterationsNumber) {
                lock.lock();
                try {
                    if (state == getPreviousIndex(n, N)) {
                        state = state < N ? state + 1 : 1;
                        i++;
                        condition.signalAll();
                    } else {
                        try {
                            condition.await();
                        } catch (InterruptedException e) {
                            System.err.println("Exception during wait()");
                        }
                    }
                } finally {
                    lock.unlock();
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
