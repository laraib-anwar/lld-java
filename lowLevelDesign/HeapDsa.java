import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

// 1. The main driver class (Must match the filename Solution.java)
public class HeapDsa {
    public static void main(String[] args) {
        // Quick demonstration to show the interviewer your code works
        MinHeap<Integer> minHeap = new MinHeap<>();
        minHeap.insert(15);
        minHeap.insert(10);
        minHeap.insert(20);

        System.out.println("Min element (Peek): " + minHeap.peek()); // Should be 10
        System.out.println("Extracted: " + minHeap.extractMin());    // Removes 10
        System.out.println("Next Min: " + minHeap.peek());          // Should be 15
    }
}

// 2. The Heap implementation (Drop the "public" keyword so it sits perfectly in the same file)
class MinHeap<T extends Comparable<T>> {
    private final List<T> heap;

    public MinHeap() {
        this.heap = new ArrayList<>();
    }

    public void insert(T element) {
        if (element == null) throw new IllegalArgumentException("Null not allowed");
        heap.add(element);
        heapifyUp(heap.size() - 1);
    }

    public T extractMin() {
        if (isEmpty()) throw new NoSuchElementException("Heap is empty");
        T minElement = heap.get(0);
        T lastElement = heap.remove(heap.size() - 1);
        if (!heap.isEmpty()) {
            heap.set(0, lastElement);
            heapifyDown(0);
        }
        return minElement;
    }

    public T peek() {
        if (isEmpty()) throw new NoSuchElementException("Heap is empty");
        return heap.get(0);
    }

    public boolean isEmpty() { return heap.isEmpty(); }

    private void heapifyUp(int index) {
        T current = heap.get(index);
        while (index > 0) {
            int parentIdx = (index - 1) / 2;
            if (current.compareTo(heap.get(parentIdx)) < 0) {
                swap(index, parentIdx);
                index = parentIdx;
            } else {
                break;
            }
        }
    }

    private void heapifyDown(int index) {
        int size = heap.size();
        T current = heap.get(index);
        while ((2 * index + 1) < size) {
            int left = 2 * index + 1;
            int right = 2 * index + 2;
            int smaller = left;

            if (right < size && heap.get(right).compareTo(heap.get(left)) < 0) {
                smaller = right;
            }
            if (current.compareTo(heap.get(smaller)) <= 0) break;

            swap(index, smaller);
            index = smaller;
        }
    }

    private void swap(int i, int j) {
        T temp = heap.get(i);
        heap.set(i, heap.get(j));
        heap.set(j, temp);
    }
}