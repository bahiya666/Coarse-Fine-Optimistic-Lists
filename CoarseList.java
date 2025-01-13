import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CoarseList<T> {
    private class Node {
        T item;
        int key;
        Node next;

        Node(T item) {
            this.item = item;
            this.key = item.hashCode();
        }

        Node(int key) { // sentinel node constructor
            this.key = key;
        }
    }

    private final Node head;
    private final Lock lock = new ReentrantLock();

    public CoarseList() {
        // Sentinel nodes for boundaries of the list
        head = new Node(Integer.MIN_VALUE);
        head.next = new Node(Integer.MAX_VALUE);
    }

    // Add method
    public boolean add(T item) {
        Node pred, curr;
        int key = item.hashCode();
        lock.lock();
        try {
            pred = head;
            curr = pred.next;
            while (curr.key < key) {
                pred = curr;
                curr = curr.next;
            }
            if (key == curr.key) {
                return false; // Item already exists
            } else {
                Node node = new Node(item);
                node.next = curr;
                pred.next = node;
                return true; // Item added successfully
            }
        } finally {
            lock.unlock(); // Release lock
        }
    }

    // Remove method
    public boolean remove(T item) {
        Node pred, curr;
        int key = item.hashCode();
        lock.lock();
        try {
            pred = head;
            curr = pred.next;
            while (curr.key < key) {
                pred = curr;
                curr = curr.next;
            }
            if (key == curr.key) {
                pred.next = curr.next; // Remove the item
                return true; // Item removed
            } else {
                return false; // Item not found
            }
        } finally {
            lock.unlock(); // Release lock
        }
    }


    // Contains method
    public boolean contains(T item) {
        Node curr;
        int key = item.hashCode();
        lock.lock();
        try {
            curr = head.next;
            while (curr.key < key) {
                curr = curr.next;
            }
            return (curr.key == key); // Return true if item found
        } finally {
            lock.unlock(); // Release lock
        }
    }

    public static void main(String[] args) {

        int[] threadCounts = {5,10,15,20,25}; // Number of threads to test

        for (int numThreads : threadCounts) {
            System.out.println("Testing Low Contention with " + numThreads + " threads:");
            testLowContention(numThreads);
        }

        for (int numThreads : threadCounts) {
            System.out.println("Testing High Contention with " + numThreads + " threads:");
            testHighContention(numThreads);
        }

        for (int numThreads : threadCounts) {
            System.out.println("Testing Burst of Writes with " + numThreads + " threads:");
            testburstofwrites(numThreads);
        }
    }






    // Testing function for low contention
    //how well it handles multiple threads trying to access and modify it at the same time with low contention.
    //tests how efficiently a multi-threaded environment can perform operations on a shared list with low competition for resources.
    public static void testLowContention(int numThreads) {
        CoarseList<Integer> set = new CoarseList<>();
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        int totalOperations = 1_000_000;

        // Variables for metrics
        long startTime = System.nanoTime();
        long[] waitTimes = new long[totalOperations];
        int containsCount = (int) (totalOperations * 0.9);
        int addCount = (int) (totalOperations * 0.05);
        int removeCount = (int) (totalOperations * 0.05);

        // Execute contains operations
        for (int i = 0; i < containsCount; i++) {
            final int index = i; // Create a final variable to capture the current index
            executor.submit(() -> {
                long waitStart = System.nanoTime();
                set.contains((int) (Math.random() * 1000));
                waitTimes[index] = System.nanoTime() - waitStart; // Use the final variable
            });
        }

        // Execute add operations
        for (int i = 0; i < addCount; i++) {
            executor.submit(() -> set.add((int) (Math.random() * 1000)));
        }

        // Execute remove operations
        for (int i = 0; i < removeCount; i++) {
            executor.submit(() -> set.remove((int) (Math.random() * 1000)));
        }

        // Shutdown executor
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long endTime = System.nanoTime();
        long totalExecutionTime = endTime - startTime;

        // Calculate throughput and average wait time
        double throughput = (double) totalOperations / (totalExecutionTime / 1_000_000_000.0);
        double averageWaitTime = 0;
        for (long waitTime : waitTimes) {
            averageWaitTime += waitTime;
        }
        averageWaitTime /= totalOperations;

        // Print results
        System.out.printf("Number of Threads: %d\n", numThreads);
        System.out.printf("Total Execution Time: %.3f seconds\n", totalExecutionTime / 1_000_000_000.0);
        System.out.printf("Throughput: %.2f operations per second\n", throughput);
        System.out.printf("Average Waiting Time: %.3f nanoseconds\n", averageWaitTime);
        System.out.println("-------------------------------------------");
    }

    // Testing function for high contention
    //Each of these operations is submitted to the thread pool. 
    //This means that many threads are simultaneously trying to add, remove, or check the existence of items in the list. 
    //This creates a situation of high contention.
    public static void testHighContention(int numThreads) {
        CoarseList<Integer> set = new CoarseList<>();
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        int totalOperations = 1_000_000;

        // Variables for metrics
        long startTime = System.nanoTime();
        int addCount = (int) (totalOperations * 0.40);
        int removeCount = (int) (totalOperations * 0.40);
        int containsCount = (int) (totalOperations * 0.20);

        // Execute add operations (40%)
        for (int i = 0; i < addCount; i++) {
            executor.submit(() -> set.add((int) (Math.random() * 1000)));
        }

        // Execute remove operations (40%)
        for (int i = 0; i < removeCount; i++) {
            executor.submit(() -> set.remove((int) (Math.random() * 1000)));
        }

        // Execute contains operations (20%)
        for (int i = 0; i < containsCount; i++) {
            executor.submit(() -> set.contains((int) (Math.random() * 1000)));
        }

        // Shutdown executor
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long endTime = System.nanoTime();
        long totalExecutionTime = endTime - startTime;

        // Calculate throughput
        double throughput = (double) totalOperations / (totalExecutionTime / 1_000_000_000.0);

        // Print results
        System.out.printf("Number of Threads: %d\n", numThreads);
        System.out.printf("Total Execution Time: %.3f seconds\n", totalExecutionTime / 1_000_000_000.0);
        System.out.printf("Throughput: %.2f operations per second\n", throughput);
        System.out.println("-------------------------------------------");
    }

     // Testing function for Burst of Writes
     //many threads are trying to write to the list at the same time.
     // quick, high-volume changes are made
     public static void testburstofwrites(int numThreads) {
        CoarseList<Integer> set = new CoarseList<>();
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        int totalOperations = 1_000_000;

        // Metrics tracking variables
        long startTime = System.nanoTime();
        int addCount = totalOperations / 2;    // 50% add operations
        int removeCount = totalOperations / 2; // 50% remove operations
        int retries = 0;  // Track retries for optimistic synchronization if implemented (not in this case)
        int deadlockCount = 0;  // Track deadlock occurrences (optional for fine-grained synchronization)

        // Execute add operations (50%)
        for (int i = 0; i < addCount; i++) {
            executor.submit(() -> {
                try {
                    set.add((int) (Math.random() * 1000)); // Add random integer
                } catch (Exception e) {
                    // Handle any potential deadlock, retry, or failure case here
                    // E.g., increment retries or deadlockCount if applicable
                }
            });
        }

        // Execute remove operations (50%)
        for (int i = 0; i < removeCount; i++) {
            executor.submit(() -> {
                try {
                    set.remove((int) (Math.random() * 1000)); // Remove random integer
                } catch (Exception e) {
                    // Handle potential failures similarly
                }
            });
        }

        // Shutdown executor
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long endTime = System.nanoTime();
        long totalExecutionTime = endTime - startTime;

        // Calculate throughput
        double throughput = (double) totalOperations / (totalExecutionTime / 1_000_000_000.0);

        // Print metrics
        System.out.printf("Burst of Writes Test with %d threads:\n", numThreads);
        System.out.printf("Total Execution Time: %.3f seconds\n", totalExecutionTime / 1_000_000_000.0);
        System.out.printf("Throughput: %.2f operations per second\n", throughput);
        System.out.printf("Retries: %d\n", retries);
        System.out.printf("Deadlock occurrences: %d\n", deadlockCount);
        System.out.println("-------------------------------------------");
    }
   
    
}