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

        int statesNumber, transitionsNumber;
        try (Scanner scanner = new Scanner(System.in)) {
            statesNumber = scanner.nextInt();
            transitionsNumber = scanner.nextInt();
        }

        pingPong.run(statesNumber, transitionsNumber);
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

    private void run(int statesNumber, int transitionsNumber) {

        Thread[] threads = new Thread[statesNumber];

        for (int i = 0; i < threads.length; i++) {
            threads[i] = createThread(i + 1, statesNumber, transitionsNumber);
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

    private Thread createThread(int threadState, int statesNumber, int transitionsNumber ) {
        return new Thread(() -> {
            int iterationsNumber = getIterationsNumber(threadState, statesNumber, transitionsNumber);
            for (int i = 0; i < iterationsNumber; i++) {
                lock.lock();
                if (state != getPreviousState(threadState, statesNumber)) {
                    try {
                        while (state != getPreviousState(threadState, statesNumber)) {
                            condition.await();
                        }
                    } catch (InterruptedException e) {
                        System.err.println("Exception during wait()");
                    }
                }
                state = state < statesNumber ? state + 1 : 1;
                condition.signalAll();
                lock.unlock();
            }
        });
    }

    private int getPreviousState(int threadState, int statesNumber) {
        return threadState > 1 ? threadState - 1 : statesNumber;
    }

    private int getIterationsNumber(int threadState, int statesNumber, int transitionsNumber) {
        int cyclesNumber = ++transitionsNumber / statesNumber,
                remainder = transitionsNumber - cyclesNumber * statesNumber;
        int iterationsNumber = cyclesNumber;
        if (remainder >= threadState) iterationsNumber++;
        if (threadState == 1) iterationsNumber--;
        return iterationsNumber;
    }
}
