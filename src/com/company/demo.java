package com.company;

import com.company.hmm.FirstOrderHiddenMarkovModel;

import java.util.Arrays;

public class demo {
    /**
    隐状态
     */
    enum Status{
        Healthy,
        Fever,
    }

    /**
     * 显状态
     */
    enum Feel {
        normal,
        cold,
        dizzy,
    }

    public static void main(String[] args) {
        /*
         *初始状态概率向量
         */
        float[] start_probability = new float[] {0.6f, 0.4f};
        float[][] transition_probability = new float[][] {
                {0.7f, 0.3f},
                {0.4f, 0.6f},
        };
        float[][] emission_probability = new float[][] {
                {0.5f, 0.4f, 0.1f},
                {0.1f, 0.3f, 0.6f},
        };

        int[] observations = new int[] {Feel.normal.ordinal(), Feel.cold.ordinal(), Feel.dizzy.ordinal()};
        FirstOrderHiddenMarkovModel givenModel = new FirstOrderHiddenMarkovModel(start_probability, transition_probability, emission_probability);
        int length = 10;
        int[][] xy = new int[2][length];
        xy = givenModel.generate(length);
    }
}
