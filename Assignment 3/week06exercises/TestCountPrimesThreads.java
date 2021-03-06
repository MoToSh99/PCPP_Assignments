// Counting primes, using multiple threads for better performance.
// (Much simplified from CountprimesMany.java)
// sestoft@itu.dk * 2014-08-31, 2015-09-15
// modified rikj@itu.dk 2017-09-20
// modified jst@itu.dk 2021-09-24
import java.util.function.IntToDoubleFunction;
import java.util.concurrent.atomic.AtomicLong;
import java.util.List;
import java.util.concurrent.*;
import java.util.*;

public class TestCountPrimesThreads {

  public static void main(String[] args) { new TestCountPrimesThreads(); }

  public TestCountPrimesThreads() {
    final int range = 100_000;
    Mark7("countSequential", i -> countSequential(range));
    for (int c=1; c<=32; c++) {
      final int threadCount = c;
      Mark7(String.format("countParallelN %2d", threadCount), 
            i -> countParallelN(range, threadCount));
      Mark7(String.format("countParallelNLocal %2d", threadCount), 
            i -> countParallelNLocal(range, threadCount));
    }
  }

  private static boolean isPrime(int n) {
    int k = 2;
    while (k * k <= n && n % k != 0)
      k++;
    return n >= 2 && k * k > n;
  }

  // Sequential solution
  private static long countSequential(int range) {
    long count = 0;
    final int from = 0, to = range;
    for (int i=from; i<to; i++)
      if (isPrime(i)) 
        count++;
    return count;
  }



  // General parallel solution, using multiple threads
  private static long countParallelN(int range, int threadCount) {
    final int perThread = range / threadCount;
    final LongCounter lc = new LongCounter();
    ExecutorService pool = Executors.newFixedThreadPool(threadCount);
    List<Future<?>> futures = new ArrayList<Future<?>>();
    for (int t=0; t<threadCount; t++) {
      final int from = perThread * t, to = (t+1==threadCount) ? range : perThread * (t+1); 
      futures.add(pool.submit(() -> {
        for (int i=from; i<to; i++)
          if (isPrime(i)) 
            lc.increment();
      })); 
    }

    try {
      for (Future<?> fut : futures)
        fut.get();
      pool.shutdown();
    } catch (Exception e) { e.printStackTrace(); } 
    
    return lc.get();
  }

  // General parallel solution, using multiple threads
  private static long countParallelNLocal(int range, int threadCount) {
    final int perThread = range / threadCount;
    final long[] results = new long[threadCount];
    ExecutorService pool = Executors.newFixedThreadPool(threadCount);
    List<Callable<Long>> tasks = new ArrayList<Callable<Long>>(); 
    for (int t=0; t<threadCount; t++) {
      final int from = perThread * t, to = (t+1==threadCount) ? range : perThread * (t+1); 
      final int threadNo = t;
      tasks.add(() -> { 
        long count = 0;
        for (int i=from; i<to; i++) {
          if (isPrime(i))
            count++;
        }
        return count;
      });
    }

    long result = 0;

    try {
      List<Future<Long>> futures = pool.invokeAll(tasks);
      for (Future<Long> fut : futures)
        result += fut.get(); 
      pool.shutdown();
    } catch (Exception e) { e.printStackTrace(); } 
    
    return result;
  }

  // --- Benchmarking infrastructure ---

  public static double Mark7(String msg, IntToDoubleFunction f) {
    int n = 10, count = 1, totalCount = 0;
    double dummy = 0.0, runningTime = 0.0, st = 0.0, sst = 0.0;
    do { 
      count *= 2;
      st = sst = 0.0;
      for (int j=0; j<n; j++) {
        Timer t = new Timer();
        for (int i=0; i<count; i++) 
          dummy += f.applyAsDouble(i);
        runningTime = t.check();
        double time = runningTime * 1e9 / count; 
        st += time; 
        sst += time * time;
        totalCount += count;
      }
    } while (runningTime < 0.25 && count < Integer.MAX_VALUE/2);
    double mean = st/n, sdev = Math.sqrt((sst - mean*mean*n)/(n-1));
    System.out.printf("%-25s %15.1f ns %10.2f %10d%n", msg, mean, sdev, count);
    return dummy / totalCount;
  }
}

