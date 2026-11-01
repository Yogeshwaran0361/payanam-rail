package com.payanam.rail.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SortedListsTest {

    @Test
    @DisplayName("DS: binary-search insert keeps an ArrayList sorted")
    void insertKeepsOrder() {
        List<Integer> list = new ArrayList<>();
        for (int v : new int[]{5, 1, 9, 3, 7, 3}) {
            SortedLists.insertSorted(list, v, Comparator.naturalOrder());
        }
        assertThat(list).containsExactly(1, 3, 3, 5, 7, 9);
    }

    @Test
    @DisplayName("DS: upper bound returns the first index strictly greater than the key")
    void upperBound() {
        List<Integer> list = List.of(1, 3, 3, 5);
        assertThat(SortedLists.upperBound(list, 3, Comparator.naturalOrder())).isEqualTo(3);
        assertThat(SortedLists.upperBound(list, 0, Comparator.naturalOrder())).isZero();
        assertThat(SortedLists.upperBound(list, 9, Comparator.naturalOrder())).isEqualTo(4);
        assertThat(SortedLists.upperBound(new ArrayList<Integer>(), 1, Comparator.naturalOrder())).isZero();
    }

    @Test
    @DisplayName("DS: equal keys keep insertion order (stable insert)")
    void stable() {
        List<String> list = new ArrayList<>();
        Comparator<String> byLength = Comparator.comparingInt(String::length);
        SortedLists.insertSorted(list, "bb", byLength);
        SortedLists.insertSorted(list, "aa", byLength);
        SortedLists.insertSorted(list, "c", byLength);
        assertThat(list).containsExactly("c", "bb", "aa");
    }
}
