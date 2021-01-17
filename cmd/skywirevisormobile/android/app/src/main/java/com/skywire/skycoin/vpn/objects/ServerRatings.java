package com.skywire.skycoin.vpn.objects;

import com.skywire.skycoin.vpn.R;

import java.util.HashMap;

public enum ServerRatings {
    Gold(0),
    Silver (1),
    Bronze (2);

    /**
     * Allows to easily get the value related to an specific number.
     */
    private static HashMap<Integer, ServerRatings> numericValues;

    // Initializes the enum and saves the value.
    private final int val;
    ServerRatings(int val) {
        this.val = val;
    }

    /**
     * Gets the associated numeric value.
     */
    public int val() {
        return val;
    }

    /**
     * Allows to get the resource ID of the string corresponding to the rating. If no resource is
     * found for the rating, -1 is returned.
     */
    public static int getTextForRating(ServerRatings rating) {
        if (rating == Gold) {
            return R.string.rating_gold;
        } else if (rating == Silver) {
            return R.string.rating_silver;
        } else if (rating == Bronze) {
            return R.string.rating_bronze;
        }

        return -1;
    }

    public static ServerRatings valueOf(int value) {
        // Initialize the map for getting the values, if needed.
        if (numericValues == null) {
            numericValues = new HashMap<>();

            for (ServerRatings v : ServerRatings.values()) {
                numericValues.put(v.val(), v);
            }
        }

        if (!numericValues.containsKey(value)) {
            return Bronze;
        }

        return numericValues.get(value);
    }
}
