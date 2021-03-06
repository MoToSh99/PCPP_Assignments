// For week 7
// raup@itu.dk * 10/10/2021
package exercises07;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntToDoubleFunction;

class TestCASLockHistogram {

    // Testing correctness and evaluating performance
    public static void main(String[] args) {

	// Create an object `histogramCAS` with your Histogram CAS implementation
	// Create an object `histogramLock` with your Histogram Lock from week 5
    Histogram histogramCAS = new CasHistogram(30);
    Histogram histogramLock = new LockHistogram(30);

	// Testing correctness (uncomment lines below to test correctness)
	//countParallel(5_000_000, 10, histogramCAS);
	//dump(histogramCAS);

	// Evaluating performance of CAS vs Locks histograms Uncomment
	// snippet below to evaluate the performance both Histogram
	// implementations
	
	int noThreads = 32;
	int range     = 100_000;	

	for (int i = 1; i < noThreads; i++) {
	    int threadCount = i;
	    Mark7(String.format("Count Lock histogram %2d", threadCount),
		  (j) -> {
		      countParallel(range, threadCount, histogramLock);
		      return 1.0;
		  });
	}
	
	for (int i = 1; i < noThreads; i++) {
	    int threadCount = i;
	    Mark7(String.format("Count CAS histogram %2d", threadCount),
		  (j) -> {
		      countParallel(range, threadCount, histogramCAS);
		      return 1.0;
		  });
	}

    }
    
    // Function to count the prime factors of a number `p`
    private static int countFactors(int p) {
	if (p < 2) return 0;
	int factorCount = 1, k = 2;
	while (p >= k * k) {
	    if (p % k == 0) {
		factorCount++;
		p= p/k;
	    } else 
		k= k+1;
	}
	return factorCount;
    }

    // Parallel execution of counting the number of primes for numbers in `range`
    private static void countParallel(int range, int threadCount, Histogram h) {
	final int perThread= range / threadCount;
	Thread[] threads= new Thread[threadCount];
	for (int t=0; t<threadCount; t= t+1) {
	    final int from= perThread * t, 
		to= (t+1==threadCount) ? range : perThread * (t+1); 
	    threads[t]= new Thread( () -> {
		    for (int i= from; i<to; i++) h.increment(countFactors(i));
                
	    });
	} 
	for (int t= 0; t<threadCount; t= t+1) 
	    threads[t].start();
	try {
	    for (int t= 0; t<threadCount; t= t+1) 
		threads[t].join();
	} catch (InterruptedException exn) { }
    }
    
    // Auxiliary method to print the histogram data
    public static void dump(Histogram histogram) {
	for (int bin= 0; bin < histogram.getSpan(); bin= bin+1) {
	    System.out.printf("%4d: %9d%n", bin, histogram.getCount(bin));
	}
    }

    // Benchmark function
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


/* # Exercise 7.1.2 solution

   ## Output of performance evaluation

   We observe better performance with the CAS implementation after
   running the execution with 3 threads.

   ### Output

Count Lock histogram  1        19512127.1 ns 1596745.05         16
Count Lock histogram  2        17951632.3 ns 2264954.13         16
Count Lock histogram  3        22938858.3 ns 1423311.29         16
Count Lock histogram  4        21519720.8 ns 3972400.15         16
Count Lock histogram  5        22270519.8 ns 2136094.76         16
Count Lock histogram  6        21537509.1 ns 2736546.43         16
Count Lock histogram  7        20230182.0 ns  412935.95         16
Count Lock histogram  8        21462694.3 ns 1972070.73         16
Count Lock histogram  9        21795556.8 ns 1902246.40         16
Count Lock histogram 10        20510326.4 ns 2848116.52         32
Count Lock histogram 11        20047849.0 ns 3678495.94         16
Count Lock histogram 12        19705991.7 ns 3829988.28         16
Count Lock histogram 13        20016555.7 ns 3251060.41         16
Count Lock histogram 14        18980630.6 ns 3047764.02         32
Count Lock histogram 15        20135822.9 ns 2463911.25         32
Count Lock histogram 16        24816691.1 ns 7033245.36          8
Count Lock histogram 17        23839801.0 ns 3234739.12         16
Count Lock histogram 18        25088808.9 ns 3352291.76          8
Count Lock histogram 19        25290202.6 ns 3019661.13         16
Count Lock histogram 20        23409126.8 ns 3428585.97         16
Count Lock histogram 21        25837354.2 ns 2835243.47         16
Count Lock histogram 22        24633969.5 ns 2533130.35         16
Count Lock histogram 23        26162238.0 ns 4138641.91         16
Count Lock histogram 24        27113662.8 ns 3758624.51         16
Count Lock histogram 25        26207404.2 ns 3097798.01         16
Count Lock histogram 26        25765614.3 ns 3670031.71         16
Count Lock histogram 27        27442189.1 ns 2293530.04          8
Count Lock histogram 28        24267122.7 ns 3376355.92         16
Count Lock histogram 29        24494088.3 ns 3689559.67         16
Count Lock histogram 30        23741726.8 ns 2601903.50         16
Count Lock histogram 31        25985627.9 ns 3951310.45         16


Count CAS histogram  1         16814135.9 ns 2046440.97         32
Count CAS histogram  2         13190143.2 ns 2157157.33         32
Count CAS histogram  3         10801405.2 ns 2138857.80         32
Count CAS histogram  4          9383392.7 ns 3147811.42         16
Count CAS histogram  5         11371164.5 ns 3329979.41         32
Count CAS histogram  6         11784174.7 ns 2398663.62         32
Count CAS histogram  7         10531852.3 ns 2654949.68         32
Count CAS histogram  8         10605567.2 ns 1907480.93         64
Count CAS histogram  9          9346844.0 ns 2149057.52         32
Count CAS histogram 10          8875200.7 ns 1214415.58         32
Count CAS histogram 11          9335431.0 ns 2544543.79         32
Count CAS histogram 12          9992259.8 ns 1837190.56         64
Count CAS histogram 13         10147075.1 ns 1491263.56         32
Count CAS histogram 14          9745776.4 ns 1618700.10         32
Count CAS histogram 15         10197066.2 ns 2047652.14         32
Count CAS histogram 16         10602248.2 ns 3052354.83         16
Count CAS histogram 17          9842033.7 ns 2335673.20         32
Count CAS histogram 18         10211753.5 ns 1875798.21         32
Count CAS histogram 19         10268735.2 ns 3081644.49         32
Count CAS histogram 20         10897362.2 ns 2731822.98         32
Count CAS histogram 21         10019144.7 ns 2273233.39         32
Count CAS histogram 22         10145225.0 ns 4087759.12         16
Count CAS histogram 23         10674738.5 ns 2076995.04         32
Count CAS histogram 24         11128068.9 ns 2663345.23         64
Count CAS histogram 25         10995666.9 ns 2590714.20         32
Count CAS histogram 26         10722325.7 ns 2763066.50         64
Count CAS histogram 27         11076423.2 ns 3523246.92         32
Count CAS histogram 28         10550917.5 ns 2518028.09         64
Count CAS histogram 29         10331768.8 ns 3674644.02         32
Count CAS histogram 30         10418705.1 ns 3449133.59         32
Count CAS histogram 31         10618799.5 ns 2769324.08         64

*/

