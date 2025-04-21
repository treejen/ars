package com.hktv.ars.algo;

import com.hktv.ars.data.EvaluationResult;
import com.hktv.ars.data.LabeledPoint;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 加權K最近鄰（KNN）分類器實現
 * 基於歐氏距離計算最近鄰，並使用加權投票進行分類
 * 權重基於距離的反比：1/(distance+epsilon)^distanceWeightFactor
 */
@Slf4j
public class WeightedKNNClassifier implements Serializable {
    @Serial
    private static final long serialVersionUID = 3L; // 增加版本號

    /**
     * 分類器配置類
     * 使用Builder模式提供靈活的配置選項
     */
    @Getter
    public static class Config implements Serializable {
        @Serial
        private static final long serialVersionUID = 2L; // 增加版本號

        private final int k;                        // 最近鄰居數量
        private final double epsilon;               // 防止除零錯誤的小值
        private final boolean useClassWeights;      // 是否使用類別權重來平衡類別
        private final double maxClassWeight;        // 類別權重的最大值
        private final double distanceWeightFactor;  // 距離權重因子
        private final DistanceFunction distanceFunction; // 距離計算函數，使用枚舉而非BiFunction

        @Builder
        public Config(
                int k,
                Double epsilon,
                Boolean useClassWeights,
                Double maxClassWeight,
                Double distanceWeightFactor,
                DistanceFunction distanceFunction) {
            this.k = k > 0 ? k : 5; // 默認為5
            this.epsilon = epsilon != null ? epsilon : 0.00001;
            this.useClassWeights = useClassWeights != null ? useClassWeights : true;
            this.maxClassWeight = maxClassWeight != null ? maxClassWeight : 50.0;
            this.distanceWeightFactor = distanceWeightFactor != null ? distanceWeightFactor : 2.0;

            // 如果沒有提供距離函數，使用默認的歐氏距離
            this.distanceFunction = distanceFunction != null ? 
                    distanceFunction : DistanceFunction.EUCLIDEAN;
        }
    }

    @Getter
    private final Config config;

    @Getter
    private final List<LabeledPoint> trainingData;

    private boolean isTrained = false;

    @Getter
    private EvaluationResult lastEvaluation;

    @Getter
    private final Set<String> uniqueLabels; // 存儲所有唯一標籤

    @Getter
    private final Map<String, List<LabeledPoint>> labelToPointsMap; // 按標籤存儲訓練數據的映射

    private Map<String, Double> classWeights; // 類別權重，用於處理類別不平衡

    /**
     * 使用默認配置構造函數
     *
     * @param k 最近鄰居數量
     */
    public WeightedKNNClassifier(int k) {
        this(Config.builder().k(k).build());
    }

    /**
     * 使用自定義配置構造函數
     *
     * @param config 配置對象
     */
    public WeightedKNNClassifier(Config config) {
        this.config = config;
        this.trainingData = new ArrayList<>();
        this.uniqueLabels = new HashSet<>();
        this.labelToPointsMap = new HashMap<>();
        this.classWeights = new HashMap<>();
    }

    /**
     * 訓練分類器
     *
     * @param labeledPoints 帶標籤的數據點列表
     * @return 當前實例，用於方法鏈式調用
     */
    public WeightedKNNClassifier train(List<LabeledPoint> labeledPoints) {
        if (labeledPoints == null || labeledPoints.isEmpty()) {
            throw new IllegalArgumentException("訓練數據不能為空");
        }

        // 清除舊數據
        resetTrainingData();

        // 處理訓練數據
        processTrainingData(labeledPoints);

        // 計算類別權重
        if (config.isUseClassWeights()) {
            calculateClassWeights();
        }

        // 標記為已訓練
        isTrained = true;

        // 記錄訓練信息
        logTrainingInfo();

        return this;
    }

    /**
     * 重置訓練數據
     */
    private void resetTrainingData() {
        trainingData.clear();
        uniqueLabels.clear();
        labelToPointsMap.clear();
        classWeights.clear();
    }

    /**
     * 處理訓練數據
     *
     * @param labeledPoints 帶標籤的數據點列表
     */
    private void processTrainingData(List<LabeledPoint> labeledPoints) {
        for (LabeledPoint point : labeledPoints) {
            trainingData.add(point);
            String label = point.getLabel();
            uniqueLabels.add(label);

            // 為每個標籤建立數據點列表
            labelToPointsMap.computeIfAbsent(label, k -> new ArrayList<>())
                    .add(point);
        }
    }

    /**
     * 記錄訓練信息
     */
    private void logTrainingInfo() {
        log.info("已完成訓練，共有 {} 個數據點、{} 個類別", trainingData.size(), uniqueLabels.size());

        // 輸出每個類別的樣本數和權重
        for (String label : uniqueLabels) {
            List<LabeledPoint> points = labelToPointsMap.get(label);
            int sampleCount = points.size();
            // 對於單樣本類別，提供額外信息並額外輸出其經緯度
            if (sampleCount < 4) {
                LabeledPoint point = points.getFirst();
                double[] features = point.getFeatures();
                if (features.length >= 2) {
                    log.info("注意: 類別 '{}' 只有1個樣本，將主要依賴距離權重進行預測。經緯度: ({}, {})",
                            label, features[0], features[1]);
                }
            }
        }
    }

    /**
     * 計算類別權重，處理類別不平衡問題
     * 權重與類別樣本數成反比：maxCount/count
     */
    private void calculateClassWeights() {
        // 獲取最大類別的樣本數
        int maxCount = labelToPointsMap.values().stream()
                .mapToInt(List::size)
                .max()
                .orElse(1);

        // 計算每個類別的權重
        for (String label : uniqueLabels) {
            int count = labelToPointsMap.get(label).size();
            // 對於樣本數極少的類別(如只有1個)，使用對數函數來平滑權重
            double rawWeight = (double) maxCount / count;
            // 使用對數平滑並設定上限
            double weight = Math.min(Math.log10(rawWeight * 10), config.getMaxClassWeight());
            classWeights.put(label, weight);
            log.debug("類別 '{}' 原始權重: {}, 平滑後權重: {}", label, rawWeight, weight);
        }
    }

    /**
     * 預測新點的標籤
     *
     * @param x 第一個特徵值
     * @param y 第二個特徵值
     * @return 預測的標籤
     */
    public String predict(double x, double y) {
        checkIsTrained();
        double[] features = {x, y};
        return predict(features);
    }

    /**
     * 預測新點的標籤
     *
     * @param features 特徵數組
     * @return 預測的標籤
     */
    public String predict(double[] features) {
        checkIsTrained();
        Map<String, Double> labelWeights = getWeightedVotes(features);
        return getMostWeightedLabel(labelWeights);
    }

    /**
     * 預測並返回各標籤的概率分佈
     *
     * @param features 特徵數組
     * @return 各標籤的概率分佈
     */
    public Map<String, Double> predictProbabilities(double[] features) {
        checkIsTrained();
        Map<String, Double> labelWeights = getWeightedVotes(features);

        // 計算總權重
        double totalWeight = labelWeights.values().stream().mapToDouble(Double::doubleValue).sum();

        // 將權重轉換為概率
        return labelWeights.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue() / totalWeight
                ));
    }

    /**
     * 從標籤權重中獲取權重最高的標籤
     *
     * @param labelWeights 標籤權重映射
     * @return 權重最高的標籤
     */
    private static String getMostWeightedLabel(Map<String, Double> labelWeights) {
        // 使用 Java 8 Stream 找出最權重最高的標籤
        return labelWeights.entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * 獲取特徵向量k個最近鄰中各標籤的加權投票
     *
     * @param features 特徵向量
     * @return 標籤權重映射
     */
    private Map<String, Double> getWeightedVotes(double[] features) {
        // 計算到所有訓練點的距離
        List<DistanceResult> distances = calculateDistances(features);

        // 根據距離排序
        Collections.sort(distances);

        // 取前k個點，計算加權投票
        return calculateWeightedVotes(distances);
    }

    /**
     * 計算到所有訓練點的距離
     *
     * @param features 特徵向量
     * @return 距離結果列表
     */
    private List<DistanceResult> calculateDistances(double[] features) {
        return trainingData.parallelStream()  // 使用並行流提高性能
                .map(point -> {
                    double dist = config.getDistanceFunction().calculate(features, point.getFeatures());
                    return new DistanceResult(dist, point.getLabel());
                })
                .collect(Collectors.toList());
    }

    /**
     * 基於距離結果計算加權投票
     *
     * @param distances 距離結果列表
     * @return 標籤權重映射
     */
    private Map<String, Double> calculateWeightedVotes(List<DistanceResult> distances) {
        Map<String, Double> labelWeights = new HashMap<>();
        int count = Math.min(config.getK(), distances.size());

        for (int i = 0; i < count; i++) {
            DistanceResult result = distances.get(i);
            String label = result.getLabel();
            double distance = result.getDistance();

            // 計算距離的權重: 1/(distance+epsilon)^distanceWeightFactor
            double distanceWeight = Math.pow(
                    1.0 / (distance + config.getEpsilon()),
                    config.getDistanceWeightFactor()
            );

            // 如果使用類別權重，則結合距離權重和類別權重
            double weight = distanceWeight;
            if (config.isUseClassWeights()) {
                weight *= classWeights.getOrDefault(label, 1.0);
            }

            // 累加該標籤的權重
            labelWeights.merge(label, weight, Double::sum);
        }

        return labelWeights;
    }

    /**
     * 評估模型性能
     * 使用交叉驗證
     *
     * @param folds                 交叉驗證的折數
     * @param maxTestSamplesPerFold 每折最大測試樣本數
     * @return 評估結果
     */
    public EvaluationResult evaluateModel(int folds, int maxTestSamplesPerFold) {
        checkIsTrained();
        this.lastEvaluation = WeightedKNNUtils.evaluateModel(this, folds, maxTestSamplesPerFold);
        return this.lastEvaluation;
    }

    /**
     * 保存模型到文件
     *
     * @param filePath 文件路徑
     * @throws IOException 如果保存失敗
     */
    public void saveModel(String filePath) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(this);
            log.info("模型已保存到: {}", filePath);
        }
    }

    /**
     * 從文件加載模型
     *
     * @param filePath 文件路徑
     * @return WeightedKNNClassifier模型實例
     * @throws IOException            如果加載失敗
     * @throws ClassNotFoundException 如果找不到類
     */
    public static WeightedKNNClassifier loadModel(String filePath) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath))) {
            WeightedKNNClassifier model = (WeightedKNNClassifier) ois.readObject();
            log.info("已從 {} 加載模型，訓練數據大小: {}", filePath, model.getTrainingDataSize());
            return model;
        }
    }

    /**
     * 檢查分類器是否已訓練
     */
    private void checkIsTrained() {
        if (!isTrained) {
            throw new IllegalStateException("分類器尚未訓練");
        }
    }

    /**
     * 獲取模型是否已訓練
     *
     * @return 是否已訓練
     */
    public boolean isTrained() {
        return isTrained;
    }

    /**
     * 獲取訓練數據大小
     *
     * @return 訓練數據點數量
     */
    public int getTrainingDataSize() {
        return trainingData.size();
    }
}
