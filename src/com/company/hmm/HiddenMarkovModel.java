package com.company.hmm;

import com.company.utility.MathUtility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public abstract class HiddenMarkovModel {
    /**
     * 初始状态概率向量
     */
    float[] start_probability;
    /**
     * 观测概率矩阵
     */
    float[][] emission_probability;
    /**
     * 状态转移概率矩阵
     */
    float[][] transition_probability;

    public HiddenMarkovModel(float[] start_probability, float[][] transition_probability, float[][] emission_probability) {
        this.start_probability = start_probability;
        this.transition_probability = transition_probability;
        this.emission_probability = emission_probability;
    }

    /**
     * 对数概率转化为累积分布函数
     * @param log
     * @return
     */
    protected static double[] logToCdf(float[] log) {
        double[] cdf = new double[log.length];
        cdf[0] = Math.exp(log[0]);
        // 概率的累加，最后一个是1
        for (int i = 1; i < cdf.length - 1; i++) {
            cdf[i] = cdf[i - 1] + Math.exp(log[i]);
        }
        cdf[cdf.length - 1] = 1.0;
        return cdf;
    }

    /**
     * 对数概率转化为累积分布函数
     * @param log
     * @return
     */
    protected static double[][] logToCdf(float[][] log) {
        double[][] cdf = new double[log.length][log[0].length];
        for (int i = 0; i < log.length; i++) {
            cdf[i] = logToCdf(log[i]);
        }
        return cdf;
    }

    /**
     * 采样
     * @param cdf：累积分布函数
     * @return:返回随机数的下标，或者要插入值的随机数下标
     */
    protected static int drawFrom(double[] cdf) {
        // random产生随机数在[0,1)之间，所以不会超出cdf的边界，也就相当于是在cdf或者说state的下标中随机选取了一个
        int res = -Arrays.binarySearch(cdf, Math.random()) - 1;
        return res;
    }

    /**
     * 频次向量归一化为概率分布
     * @param freq
     */
    protected void normalize(float[] freq) {
        float sum = MathUtility.sum(freq);
        for (int i = 0; i < freq.length; i++) {
            freq[i] /= sum;
        }
    }

    /**
     * 模型参数反对数化
     */
    public void unLog() {
        for (int i = 0; i < start_probability.length; i++) {
            start_probability[i] = (float) Math.exp(start_probability[i]);
        }
        for (int i = 0; i < emission_probability.length; i++){
            for (int j = 0; j < emission_probability[i].length; j++) {
                emission_probability[i][j] = (float)Math.exp(emission_probability[i][j]);
            }
        }
        for (int i = 0; i < transition_probability.length; i++){
            for (int j = 0; j < transition_probability[i].length; j++) {
                transition_probability[i][j] = (float)Math.exp(transition_probability[i][j]);
            }
        }
    }

    /**
     * 模型参数对数化
     */
    protected void toLog() {
        if (start_probability == null || transition_probability == null || emission_probability == null){
            return;
        }
        for (int i = 0; i < start_probability.length; i++) {
            start_probability[i] = (float) Math.log(start_probability[i]);
            for (int j = 0; j < start_probability.length; j++) {
                transition_probability[i][j] = (float) Math.log(transition_probability[i][j]);
            }
            for (int j = 0; j <emission_probability[0].length; j++) {
                emission_probability[i][j] = (float)emission_probability[i][j];
            }
        }
    }

    /**
     * 生成样本序列
     * @param length：序列长度
     * @return：序列
     */
    public abstract int[][] generate(int length);

    /**
     * 生成序列
     * @param minLength 序列最短长度
     * @param maxLength 序列最长长度
     * @param size 需要生成多少个样本
     * @return 样本序列集合
     */
    public List<int[][]> generate(int minLength, int maxLength, int size) {
        List<int[][]> samples = new ArrayList<int[][]>(size);
        for (int i = 0; i < size; i++) {
            samples.add(generate(
                    (int)(Math.floor(Math.random() * (maxLength - minLength)) + minLength)
            ));
        }
        return samples;
    }

    /**
     * 预测（维特比算法）
     * @param o 观测序列
     * @param s 预测状态序列
     * @return 概率的对数
     */
    public abstract float predict(int[] o, int[] s);

    /**
     * 预测（维特比算法）
     * @param o 观测序列
     * @param s 预测状态序列（需要预先分配）
     * @return 概率的对数
     */
    public float predict(int[] o, Integer[] s) {
        int[] states = new int[s.length];
        float p = predict(o, states);
        for (int i = 0; i < states.length; i++){
            s[i] = states[i];
        }
        return p;
    }

    /**
     * 比对模型的相似性
     * @param model
     * @return
     */
    public boolean similar(HiddenMarkovModel model) {
        if (!similar(start_probability, model.start_probability)){
            return false;
        }
        for (int i = 0; i < transition_probability.length; i++){
            if (!similar(transition_probability[i], model.transition_probability[i])){
                return false;
            }
            if (!similar(emission_probability[i], model.emission_probability[i])){
                return false;
            }
        }
        return true;
    }

    protected static boolean similar(float[] A, float[] B) {
        final float eta = 1e-2f;
        for (int i = 0; i < A.length; i++) {
            if (Math.abs(A[i] - B[i]) > eta) {
                return false;
            }
        }
        return true;
    }

    /**
     * 依据给定的数据集进行HMM模型的训练
     * @param samples 数据集 int[i][j] i=0为观测，i=1为状态，j为时序轴
     */
    public void train(Collection<int[][]> samples) {
        if (samples.isEmpty()){
            return;
        }
        int max_state = 0;
        int max_obser = 0;
        for (int[][] sample : samples) {
            if (sample.length != 2 || sample[0].length != sample[1].length){
                throw new IllegalArgumentException("非法样本");
            }
            for (int o: sample[0]){
                max_obser = Math.max(max_obser, o);
            }
            for (int s: sample[1]) {
                max_state = Math.max(max_state, s);
            }
        }
        estimateStartProbability(samples, max_state);
        estimateTransitionProbability(samples, max_state);
        estimateEmissionProbability(samples, max_state, max_obser);
        toLog();
    }

    /**
     * 极大似然估计发射矩阵概率
     * @param samples
     * @param max_state
     * @param max_obser
     */
    protected void estimateEmissionProbability(Collection<int[][]> samples, int max_state, int max_obser) {
        emission_probability = new float[max_state+1][max_obser];
        for (int[][] sample: samples) {
            for (int i = 0; i < sample[0].length; i++) {
                int o = sample[0][i];
                int s = sample[1][i];
                ++emission_probability[s][o];
            }
        }
        for (int i = 0; i < emission_probability.length; i++) {
            normalize(emission_probability[i]);
        }
    }

    /**
     * 极大似然估计状态转移矩阵
     * @param samples
     * @param max_state
     */
    protected void estimateTransitionProbability(Collection<int[][]> samples, int max_state) {
        transition_probability = new float[max_state + 1][max_state + 1];
        for (int[][] sample: samples) {
            int pre_s = sample[1][0];
            for (int i = 1; i < sample[0].length; i++) {
                int s = sample[1][i];
                ++transition_probability[pre_s][s];
                pre_s = s;
            }
        }
        for (int i = 0; i < transition_probability.length; i++) {
            normalize(transition_probability[i]);
        }
    }

    /**
     * 极大似然估计初始状态概率向量
     * @param samples
     * @param max_state
     */
    protected void estimateStartProbability(Collection<int[][]> samples, int max_state) {
        start_probability = new float[max_state + 1];
        for (int[][] sample: samples) {
            int s = sample[1][0];
            ++start_probability[s];
        }
        normalize(start_probability);
    }
}
