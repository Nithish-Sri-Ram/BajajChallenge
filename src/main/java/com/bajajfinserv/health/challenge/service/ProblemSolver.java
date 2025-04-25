package com.bajajfinserv.health.challenge.service;

import com.bajajfinserv.health.challenge.model.User;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class ProblemSolver {

    public List<List<Integer>> findMutualFollowers(List<User> users) {
        List<List<Integer>> mutualPairs = new ArrayList<>();

        // Create a map for quick lookup
        for (User user1 : users) {
            if (user1.getFollows() != null) {
                for (Integer followedId : user1.getFollows()) {
                    // Find the followed user
                    for (User user2 : users) {
                        if (user2.getId() == followedId && user2.getFollows() != null) {
                            // Check if the followed user also follows back
                            if (user2.getFollows().contains(user1.getId())) {
                                int min = Math.min(user1.getId(), user2.getId());
                                int max = Math.max(user1.getId(), user2.getId());
                                List<Integer> pair = Arrays.asList(min, max);

                                // Check if this pair already exists
                                if (!containsPair(mutualPairs, pair)) {
                                    mutualPairs.add(pair);
                                }
                            }
                            break;
                        }
                    }
                }
            }
        }

        return mutualPairs;
    }

    private boolean containsPair(List<List<Integer>> pairs, List<Integer> pair) {
        for (List<Integer> existingPair : pairs) {
            if (existingPair.get(0).equals(pair.get(0)) && existingPair.get(1).equals(pair.get(1))) {
                return true;
            }
        }
        return false;
    }
}