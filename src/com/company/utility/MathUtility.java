package com.company.utility;

/**
 * @author Jay
 */

public class MathUtility {
    public static int sum(int ... var) {
        int sum = 0;
        for (int x: var) {
            sum += x;
        }
        return sum;
    }

    public static float sum(float ... var) {
        float sum = 0.0F;
        for (float x: var) {
            sum += x;
        }
        return sum;
    }
}
