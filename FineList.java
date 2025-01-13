import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class FineList<T> {
    private class Node {
        T item;
        int key;
        Node next;
        final Lock lock = new ReentrantLock(); // Each node has its own lock

        Node(T item) {
            this.item = item;
            this.key = item.hashCode();
        }

        Node(int key) { // Sentinel node constructor
            this.key = key;
        }
    }

    private final Node head;

    public FineList() {
        // Sentinel nodes for boundaries of the list
        head = new Node(Integer.MIN_VALUE);
        head.next = new Node(Integer.MAX_VALUE);
    }

    // Fine-grained add method
    public boolean add(T item) {
        int key = item.hashCode();
        Node pred = null, curr = null;
        head.lock.lock(); // Acquire the lock for the head
        try {
            pred = head;
            curr = pred.next;
            curr.lock.lock(); // Lock the next node
            try {
                while (curr.key < key) {
                    pred.lock.unlock(); // Release previous lock
                    pred = curr;
                    curr = curr.next;
                    curr.lock.lock(); // Lock the new current node
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
                curr.lock.unlock(); // Unlock the current node
            }
        } finally {
            pred.lock.unlock(); // Unlock the predecessor node
        }
    }

    // Fine-grained remove method
    public boolean remove(T item) {
        int key = item.hashCode();
        Node pred = null, curr = null;
        head.lock.lock(); // Acquire the lock for the head
        try {
            pred = head;
            curr = pred.next;
            curr.lock.lock(); // Lock the next node
            try {
                while (curr.key < key) {
                    pred.lock.unlock(); // Release previous lock
                    pred = curr;
                    curr = curr.next;
                    curr.lock.lock(); // Lock the new current node
                }
                if (key == curr.key) {
                    pred.next = curr.next; // Remove the current node
                    return true; // Item removed
                } else {
                    return false; // Item not found
                }
            } finally {
                curr.lock.unlock(); // Unlock the current node
            }
        } finally {
            pred.lock.unlock(); // Unlock the predecessor node
        }
    }

    // Fine-grained contains method
    public boolean contains(T item) {
        int key = item.hashCode();
        Node curr = null;
        head.lock.lock(); // Acquire the lock for the head
        try {
            curr = head.next;
            curr.lock.lock(); // Lock the next node
            try {
                while (curr.key < key) {
                    Node pred = curr;
                    curr = curr.next;
                    curr.lock.lock(); // Lock the new current node
                    pred.lock.unlock(); // Unlock the predecessor
                }
                return (curr.key == key); // Return true if item found
            } finally {
                curr.lock.unlock(); // Unlock the current node
            }
        } finally {
            head.lock.unlock(); // Unlock the head node
        }
    }

    public static void main(String[] args) {
        int[] threadCounts = {5,10,15,20,25}; // Number of threads to test

        for (int numThreads : threadCounts) {
            System.out.println("Testing Low Contention with " + numThreads + " threads:");
            System.out.println("Testing with " + numThreads + " threads:");
            testLowContention(numThreads);
        }

        for (int numThreads : threadCounts) {
            System.out.println("Testing High Contention with " + numThreads + " threads:");
            System.out.println("Testing with " + numThreads + " threads:");
            testHighContention(numThreads); // New test for high contention
        }

        for (int numThreads : threadCounts) {
            System.out.println("Testing Burst of Writes with " + numThreads + " threads:");
            System.out.println("Testing with " + numThreads + " threads:");
            testBurstOfWrites(numThreads); // New test for high contention
        }
    }


    // Testing function for low contention
    public static void testLowContention(int numThreads) {
        FineList<Integer> set = new FineList<>();
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
    public static void testHighContention(int numThreads) {
        FineList<Integer> set = new FineList<>();
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
public static void testBurstOfWrites(int numThreads) {
    FineList<Integer> set = new FineList<>();
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
