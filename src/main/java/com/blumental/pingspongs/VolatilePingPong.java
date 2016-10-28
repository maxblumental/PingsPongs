package com.blumental.pingspongs;

import org.openjdk.jmh.annotations.*;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
public class VolatilePingPong {

    @Param({"2", "4", "8", "16", "32", "64", "128", "256"})
    private int threadNumber;
    private volatile State currentState = new State();

    public static void main(String[] args) {
        VolatilePingPong pingPong = new VolatilePingPong();

        int statesNumber, transitionsNumber;
        try (Scanner scanner = new Scanner(System.in)) {
            statesNumber = scanner.nextInt();
            transitionsNumber = scanner.nextInt();
        }

        pingPong.run(statesNumber, transitionsNumber);
    }

    @Setup(Level.Invocation)
    public void setUp() {
        currentState = new State();
    }

    @Benchmark
    @BenchmarkMode({Mode.AverageTime})
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @Warmup(iterations = 5)
    @Fork(1)
    public void measureVolatile() {
        run(threadNumber, 1000);
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
        System.out.printf("Final state: %d\n", currentState.get());
    }

    private Thread createThread(int threadState, int statesNumber, int transitionsNumber) {
        return new Thread(() -> {
            int iterationsNumber = getIterationsNumber(threadState, statesNumber, transitionsNumber);
            int i = 0;
            while (i < iterationsNumber) {
                if (currentState.get() == getPreviousState(threadState, statesNumber)) {
                    currentState = new State(currentState, statesNumber);
                    i++;
                }
            }
        });
    }

    private int getPreviousState(int threadState, int transitionsNumber) {
        return threadState > 1 ? threadState - 1 : transitionsNumber;
    }

    private int getIterationsNumber(int threadState, int statesNumber, int transitionsNumber) {
        int cyclesNumber = ++transitionsNumber / statesNumber,
                remainder = transitionsNumber - cyclesNumber * statesNumber;
        int iterationsNumber = cyclesNumber;
        if (remainder >= threadState) iterationsNumber++;
        if (threadState == 1) iterationsNumber--;
        return iterationsNumber;
    }

    private static class State {
        private final int state;

        State() {
            state = 1;
        }

        State(State previous, int statesNumber) {
            state = previous.state < statesNumber ? previous.state + 1 : 1;
        }

        int get() {
            return state;
        }
    }
}