import java.util.ArrayList;
import java.util.LinkedList;
import java.util.NoSuchElementException;

// 1. Main Driver Class
public class HashMapDsa {
    public static void main(String[] args) {
        CustomHashMap<String, Integer> map = new CustomHashMap<>();
        
        // Test Put
        map.put("Delhi", 110001);
        map.put("Mumbai", 400001);
        map.put("Bangalore", 560001);
        
        System.out.println("Size after inserts: " + map.size()); // 3
        System.out.println("Get Delhi: " + map.get("Delhi"));   // 110001
        
        // Test Update (Overwriting an existing key)        // Reset size because put() will recalculate it
        size = 0;

        map.put("Delhi", 110011);
        System.out.println("Get Delhi (updated): " + map.get("Delhi")); // 110011
        
        // Test ContainsKey
        System.out.println("Contains Mumbai? " + map.containsKey("Mumbai")); // true
        
        // Test Remove
        System.out.println("Removed Mumbai: " + map.remove("Mumbai")); // 400001
        System.out.println("Contains Mumbai now? " + map.containsKey("Mumbai")); // false
        System.out.println("Final Size: " + map.size()); // 2
    }
}

// 2. Custom HashMap Class (Package-Private for Single File execution)
class CustomHashMap<K, V> {

    // Internal Node structure representing a Key-Value pair
    private static class Node<K, V> {
        K key;
        V value;

        Node(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    private LinkedList<Node<K, V>>[] buckets;
    private int size; // Number of key-value pairs currently in the map
    private static final int INITIAL_CAPACITY = 4; // Intentionally small to showcase rehashing early
    private static final double LOAD_FACTOR_THRESHOLD = 0.75;

    @SuppressWarnings("unchecked")
    public CustomHashMap() {
        this.buckets = new LinkedList[INITIAL_CAPACITY];
        for (int i = 0; i < buckets.length; i++) {
            buckets[i] = new LinkedList<>();
        }
        this.size = 0;
    }

    // ==========================================
    //               Public API
    // ==========================================

    /**
     * Associates the specified value with the specified key.
     * If the key already exists, updates its value.
     * Average Time Complexity: O(1)
     */
    public void put(K key, V value) {
        int bucketIndex = getBucketIndex(key);
        LinkedList<Node<K, V>> chain = buckets[bucketIndex];

        // Check if key already exists in the chain to update it
        for (Node<K, V> node : chain) {
            if (isKeyEqual(node.key, key)) {
                node.value = value;
                return;
            }
        }

        // Key doesn't exist, create a new node and add to the chain
        chain.add(new Node<>(key, value));
        size++;

        // Check if rehashing is required
        double currentLoadFactor = (double) size / buckets.length;
        if (currentLoadFactor >= LOAD_FACTOR_THRESHOLD) {
            rehash();
        }
    }

    /**
     * Returns the value mapped to the specified key.
     * Average Time Complexity: O(1)
     */
    public V get(K key) {
        int bucketIndex = getBucketIndex(key);
        LinkedList<Node<K, V>> chain = buckets[bucketIndex];

        for (Node<K, V> node : chain) {
            if (isKeyEqual(node.key, key)) {
                return node.value;
            }
        }
        return null; // Key not found
    }

    /**
     * Removes the key-value pair and returns the value.
     * Average Time Complexity: O(1)
     */
    public V remove(K key) {
        int bucketIndex = getBucketIndex(key);
        LinkedList<Node<K, V>> chain = buckets[bucketIndex];

        for (Node<K, V> node : chain) {
            if (isKeyEqual(node.key, key)) {
                V val = node.value;
                chain.remove(node);
                size--;
                return val;
            }
        }
        return null; // Key not found to remove
    }

    public boolean containsKey(K key) {
        int bucketIndex = getBucketIndex(key);
        LinkedList<Node<K, V>> chain = buckets[bucketIndex];

        for (Node<K, V> node : chain) {
            if (isKeyEqual(node.key, key)) {
                return true;
            }
        }
        return false;
    }

    public int size() { return this.size; }
    public boolean isEmpty() { return this.size == 0; }

    // ==========================================
    //           Internal Helper Methods
    // ==========================================

    /**
     * Computes the bucket array index for a given key.
     */
    private int getBucketIndex(K key) {
        if (key == null) return 0; // Standard HashMap places null keys at index 0
        int hashCode = key.hashCode();
        // Math.abs handles negative hashCodes safely, modulo restricts it within array bounds
        return Math.abs(hashCode) % buckets.length;
    }

    /**
     * Safely checks equality for keys, handling null smoothly.
     */
    private boolean isKeyEqual(K key1, K key2) {
        if (key1 == null && key2 == null) return true;
        if (key1 == null || key2 == null) return false;
        return key1.equals(key2);
    }

    /**
     * Doubles the bucket array capacity and redistributes elements to minimize collisions.
     */
    @SuppressWarnings("unchecked")
    private void rehash() {
        LinkedList<Node<K, V>>[] oldBuckets = buckets;
        
        // Double the capacity
        buckets = new LinkedList[oldBuckets.length * 2];
        for (int i = 0; i < buckets.length; i++) {
            buckets[i] = new LinkedList<>();
        }
        
        // Reset size because put() will recalculate it
        size = 0;

        // Migrate all elements from old buckets to the new resized buckets
        for (LinkedList<Node<K, V>> chain : oldBuckets) {
            for (Node<K, V> node : chain) {
                put(node.key, node.value);
            }
        }
    }
}