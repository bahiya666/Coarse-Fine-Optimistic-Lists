Thread Synchronization Strategies - CoarseList, FineList, OptimisticList
Overview
This repository contains implementations of three different synchronization strategies for concurrent data structures in Java: CoarseList, FineList, and OptimisticList. The goal is to compare their performance under various scenarios, including low contention, high contention, and bursts of write operations. Each synchronization strategy is implemented for a List structure, where operations like add, remove, and contains are performed in a multithreaded environment.

Synchronization Strategies

Coarse-Grained Locking (CoarseList):
A single lock is used for the entire data structure, ensuring that only one thread can modify the list at a time.
This approach is simple and performs well in scenarios with low contention, but as the number of threads increases, the lock creates a bottleneck.

Fine-Grained Locking (FineList):
Each node in the list has its own lock, allowing multiple threads to operate on different parts of the list concurrently.
This strategy provides better concurrency control but introduces overhead due to the complexity of managing multiple locks.

Optimistic Locking (OptimisticList):
No locks are used initially. Threads proceed with their operations and only check for conflicts (such as attempting to modify the same element) when necessary.
Optimistic locking reduces waiting time and contention, providing the best performance in high-throughput scenarios with many threads.

Features
Concurrency Control: All three lists are designed for concurrent environments and ensure thread-safety.
Performance Metrics: The lists are tested under varying conditions, with results showing how each synchronization strategy performs in scenarios with different levels of contention.

Test Scenarios
Low Contention:
A scenario where fewer threads are accessing the list concurrently. The goal is to evaluate how each synchronization strategy performs with minimal contention.

High Contention:
A scenario where many threads attempt to access and modify the list simultaneously. This tests how each strategy handles contention and lock management.

Burst of Writes:
A scenario with a high volume of add and remove operations. The goal is to evaluate the strategies' ability to handle a large number of concurrent write operations efficiently.

Compile the Code:
javac *.java
Run the Tests:
java CoarseList
java FineList
java OptimisticList

Test Results
Low Contention
CoarseList: Performs well with low execution time but does not scale well with increased threads.
FineList: Performs worse than CoarseList due to overhead from multiple locks.
OptimisticList: Performs the best by reducing contention and not immediately locking threads.

High Contention
CoarseList: Increased contention due to a single lock leads to higher execution times, though it still performs better than FineList.
FineList: Struggles with high contention, leading to excessive locking and waiting times.
OptimisticList: Maintains the lowest execution times by reducing the need for locks until absolutely necessary.

Burst of Writes
CoarseList: Performs consistently well but shows some contention during burst write operations.
FineList: Suffers from increased contention due to the multiple locks, leading to higher execution times.
OptimisticList: Handles bursts of write operations most efficiently, reducing contention and maximizing throughput.
