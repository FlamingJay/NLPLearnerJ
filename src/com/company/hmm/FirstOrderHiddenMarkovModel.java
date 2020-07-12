package com.company.hmm;

/** 一阶马尔科夫模型
 * @author Jay
 */

public class FirstOrderHiddenMarkovModel extends HiddenMarkovModel {
    /**
     * 创建空白的隐马尔科夫模型以供训练
     */
    public FirstOrderHiddenMarkovModel() {
        this(null, null, null);
    }

    /**
     *
     * @param start_probability
     * @param transition_probabolity
     * @param emission_probability
     */
    public FirstOrderHiddenMarkovModel(float[] start_probability, float[][] transition_probabolity, float[][] emission_probability) {
        super(start_probability, transition_probabolity, emission_probability);
        toLog();
    }

    /**
     * 采样过程就是沿着马尔科夫链走T步，并且得到每个时刻的状态和观测值
     * 首先，根据初始概率向量采样得到第一个状态y_1=s_i，且y1~pi
     * 然后，根据状态转移矩阵第i行的概率向量采样下一时刻的状态y_{t+1}，即y_{t+1}~A_{i,:}
     * 最后，对每个y_t = s_i，根据发射矩阵的第i行B_{i,:}，采样x_t，即x_t~B_{i,:}
     * @param length：序列长度
     * @return
     */
    @Override
    public int[][] generate(int length) {
        double[] pi = logToCdf(start_probability);
        double[][] A = logToCdf(transition_probability);
        double[][] B = logToCdf(emission_probability);

        int xy[][] = new int[2][length];
        // 采样第一个隐状态，xy对应着y 和 x（联想隐马尔科夫链的图结构）
        xy[1][0] = drawFrom(pi);
        // 根据第一个隐状态采样其对应的显状态
        xy[0][0] = drawFrom(B[xy[1][0]]);
        for (int t = 1; t < length; t++) {
            xy[1][t] = drawFrom(A[xy[1][t-1]]);
            xy[0][t] = drawFrom(B[xy[1][t]]);
        }
        return xy;
    }

    /**
     * 采用维特比算法进行预测，即给定观测序列，求解最有可能的状态序列及其概率
     * 首先，t=1时，初始最优路径的备选由N个状态组成
     * 其次，当t>1时，根据转移概率和发射概率计算消耗，选择每条备选路径的最优路径
     * 最终，找到最后时刻的最大概率，弄回溯前驱，得到最终的最优路径。
     * @param observation
     * @param state
     * @return
     */
    @Override
    public float predict(int[] observation, int[] state) {
        // 序列长度
        final int time = observation.length;
        // 状态数
        final int max_s = start_probability.length;

        // 每一个状态的得分，方便计算每条备用路径里的最优状态
        float[] score = new float[max_s];

        // link[t][s]表示第t个时刻在当前状态为s时，前一个状态是什么
        int[][] link = new int[time][max_s];

        // 第一个时刻，使用初始概率乘以发射概率矩阵
        for (int cur_s = 0; cur_s < max_s; cur_s++){
            score[cur_s] = start_probability[cur_s] + emission_probability[cur_s][observation[0]];
        }

        // 第二个时刻，使用前一个时刻的概率向量乘以一阶转移矩阵乘以发射概率
        float[] pre = new float[max_s];
        for (int t = 1; t < observation.length; t++) {
            float[] temp = pre;
            pre = score;
            score = temp;

            for (int s = 0; s < max_s; s++) {
                score[s] = Integer.MIN_VALUE;
                for (int f = 0; f < max_s; f++) {
                    float p = pre[f] + transition_probability[f][s] + emission_probability[s][observation[t]];
                    if (p > score[s]) {
                        score[s] = p;
                        link[t][s] = f;
                    }
                }
            }
        }

        // 找到最终时刻的最可能的状态
        float max_score = Integer.MIN_VALUE;
        int best_s = 0;
        for (int s = 0; s < max_s; s++) {
            if (score[s] > max_score) {
                max_score = score[s];
                best_s = s;
            }
        }

        // 通过link进行回溯
        for (int t = link.length - 1; t >= 0; --t) {
            state[t] = best_s;
            best_s = link[t][best_s];
        }

        return max_score;
    }
}
