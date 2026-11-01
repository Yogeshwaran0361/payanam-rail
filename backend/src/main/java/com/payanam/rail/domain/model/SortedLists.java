package com.payanam.rail.domain.model;

import java.util.Comparator;
import java.util.List;

/**
 * Hand-written binary-search helpers that keep an {@link java.util.ArrayList} sorted on every insert.
 * Used for: platform occupation intervals, the timetable, the reservation history and the event log.
 *
 * <p>Why a sorted ArrayList? These lists are read (drawn on screen, scanned for overlaps) far more often
 * than they are written, and every read needs them in order. Keeping them sorted on insert means reads
 * never sort, and finding the insert position costs only O(log n) comparisons (plus an O(n) array shift).
 * A TreeMap would also work but cannot hold two items with equal keys without extra tie-breakers.</p>
 */
public final class SortedLists {

    private SortedLists() {
    }

    /**
     * Returns the first index whose element is STRICTLY greater than {@code key} ("upper bound").
     * Inserting there keeps equal elements in insertion order (a stable insert).
     */
    public static <T> int upperBound(List<T> list, T key, Comparator<? super T> order) {
        int low = 0;
        int high = list.size();               // search window is [low, high)
        while (low < high) {
            int mid = (low + high) >>> 1;     // unsigned shift avoids int overflow
            if (order.compare(list.get(mid), key) <= 0) {
                low = mid + 1;                // mid <= key: the answer is to the right
            } else {
                high = mid;                   // mid > key: mid could be the answer
            }
        }
        return low;
    }

    /** Inserts {@code item} at its sorted position and returns that index. O(log n) search + O(n) shift. */
    public static <T> int insertSorted(List<T> list, T item, Comparator<? super T> order) {
        int index = upperBound(list, item, order);
        list.add(index, item);
        return index;
    }
}
