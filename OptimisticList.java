import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class OptimisticList<T> {
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

        // Locking and validation to check if node is still in the list
        public void lock() { lock.lock(); }
        public void unlock() { lock.unlock(); }

        public boolean validate(Node pred, Node curr) {
            return pred.next == curr; // Ensure the list structure hasn't changed
        }
    }

    private final Node head;

    public OptimisticList() {
        // Sentinel nodes for boundaries of the list
        head = new Node(Integer.MIN_VALUE);
        head.next = new Node(Integer.MAX_VALUE);
    }

    // Optimistic add method
    public boolean add(T item) {
        int key = item.hashCode();
        while (true) {
            Node pred = head;
            Node curr = pred.next;

            // Traverse optimistically without locks
            while (curr.key < key) {
                pred = curr;
                curr = curr.next;
            }

            // Lock both the predecessor and current node for validation
            //check if predecesoor still points to cureent node = validat 
            pred.lock();
            curr.lock();
            try {
                if (pred.validate(pred, curr)) { // Validate list structure
                    if (key == curr.key) {
                        return false; // Item already exists
                    } else {
                        Node node = new Node(item);
                        node.next = curr;
                        pred.next = node;
                        return true; // Successfully added
                    }
                }
            } finally {
                pred.unlock();
                curr.unlock();
            }
        }
    }

    // Optimistic remove method
    public boolean remove(T item) {
        int key = item.hashCode();
        while (true) {
            Node pred = head;
            Node curr = pred.next;

            // Traverse optimistically without locks
            while (curr.key < key) {
                pred = curr;
                curr = curr.next;
            }

            // Lock both the predecessor and current node for validation
            pred.lock();
            curr.lock();
            try {
                if (pred.validate(pred, curr)) { // Validate list structure
                    if (key == curr.key) {
                        pred.next = curr.next; // Remove the current node
                        return true; // Successfully removed
                    } else {
                        return false; // Item not found
                    }
                }
            } finally {
                pred.unlock();
                curr.unlock();
            }
        }
    }

    // Optimistic contains method
    public boolean contains(T item) {
        int key = item.hashCode();
        while (true) {
            Node pred = head;
            Node curr = pred.next;

            // Traverse optimistically without locks
            while (curr.key < key) {
                pred = curr;
                curr = curr.next;
            }

            // Lock both the predecessor and current node for validation
            pred.lock();
            curr.lock();
            try {
                if (pred.validate(pred, curr)) { // Validate list structure
                    return (curr.key == key); // Return true if item is found
                }
            } finally {
                pred.unlock();
                curr.unlock();
            }
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
            testHighContention(numThreads);
        }

        for (int numThreads : threadCounts) {
            System.out.println("Testing Burst of Writes with " + numThreads + " threads:");
            testBurstofWrites(numThreads);
        }
    }

     // Testing function for low contention
    public static void testLowContention(int numThreads) {
        OptimisticList<Integer> set = new OptimisticList<>();
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
    OptimisticList<Integer> set = new OptimisticList<>();
    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    int totalOperations = 1_000_000;

    // Variables for metrics
    long startTime = System.nanoTime();
    int addCount = (int) (totalOperations * 0.40);   // 40% add operations
    int removeCount = (int) (totalOperations * 0.40); // 40% remove operations
    int containsCount = (int) (totalOperations * 0.20); // 20% contains operations

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

public static void testBurstofWrites(int numThreads) {
    OptimisticList<Integer> set = new OptimisticList<>();
    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    int totalOperations = 1_000_000;

    // Metrics tracking variables
    long startTime = System.nanoTime();
    int addCount = totalOperations / 2;    // 50% add operations
    int removeCount = totalOperations / 2; // 50% remove operations
    int retries = 0;  // Track retries for optimistic synchronization
    int deadlockCount = 0;  // Track deadlock occurrences (optional for fine-grained synchronization)

    // Execute add operations (50%)
    for (int i = 0; i < addCount; i++) {
        executor.submit(() -> {
            try {
                // Generate random integer to add
                // Attempt to add to the list
                set.add((int) (Math.random() * 1000)); // Add random integer

            } catch (Exception e) {
                // Handle any potential exception here
            }
        });
    }

    // Execute remove operations (50%)
    for (int i = 0; i < removeCount; i++) {
        executor.submit(() -> {
            try {
                // Generate random integer to remove
                int valueToRemove = (int) (Math.random() * 1000);
                // Attempt to remove from the list
                if (!set.remove(valueToRemove)) {
                    // Optionally, track items that were not found for removal
                }
            } catch (Exception e) {
                // Handle any potential exception here
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
