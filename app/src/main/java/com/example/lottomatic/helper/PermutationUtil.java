package com.example.lottomatic.helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PermutationUtil {

    public static List<String> getPermutations(String combo) {
        if (combo == null || combo.length() == 0) {
            return Collections.emptyList();
        }

        List<String> permutations = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        permute(combo.toCharArray(), 0, permutations, visited);

        return permutations;
    }

    private static void permute(char[] arr, int index, List<String> permutations, Set<String> visited) {
        if (index == arr.length - 1) {
            String permutation = new String(arr);

            if (!visited.contains(permutation)) {
                permutations.add(permutation);
                visited.add(permutation);
            }
        } else {
            Set<Character> swapped = new HashSet<>();
            for (int i = index; i < arr.length; i++) {
                if (swapped.add(arr[i])) {
                    swap(arr, i, index);
                    permute(arr, index + 1, permutations, visited);
                    swap(arr, i, index); // backtrack
                }
            }
        }
    }

    private static void swap(char[] arr, int i, int j) {
        char temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }
}
