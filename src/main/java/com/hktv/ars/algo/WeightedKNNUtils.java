package com.hktv.ars.algo;

import com.hktv.ars.data.EvaluationResult;
import com.hktv.ars.data.LabeledPoint;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 加權KNN的輔助工具類，實現評估和距離計算相關功能
 */
@Slf4j
public class WeightedKNNUtils {

    /**
     * 詳細更新混淆矩陣
     * 
     * @param confusionMatrix 混淆矩陣
     * @param actualLabel 實際標籤
     * @param predictedLabel 預測標籤
     * @param allLabels 所有可能的標籤
     */
    private static void updateConfusionMatrix(
            Map<String, Map<String, Integer>> confusionMatrix,
            String actualLabel, 
            String predictedLabel, 
            Set<String> allLabels) {
        
        // 確保實際標籤存在於混淆矩陣中
        if (!confusionMatrix.containsKey(actualLabel)) {
            confusionMatrix.put(actualLabel, new HashMap<>());
            for (String label : allLabels) {
                confusionMatrix.get(actualLabel).put(label, 0);
            }
        }
        
        // 確保預測標籤存在於實際標籤的預測分佈中
        Map<String, Integer> predictions = confusionMatrix.get(actualLabel);
        if (!predictions.containsKey(predictedLabel)) {
            predictions.put(predictedLabel, 0);
        }
        
        // 更新計數
        predictions.put(predictedLabel, predictions.get(predictedLabel) + 1);
    }

    /**
     * 為WeightedKNNClassifier執行評估操作
     * 使用交叉驗證評估模型性能
     * 
     * @param classifier 待評估的分類器
     * @param folds 交叉驗證的折數
     * @param maxTestSamplesPerFold 每折最大測試樣本數
     * @return 評估結果
     */
    public static EvaluationResult evaluateModel(WeightedKNNClassifier classifier, 
                                                int folds, 
                                                int maxTestSamplesPerFold) {
        if (!classifier.isTrained()) {
            throw new IllegalStateException("分類器尚未訓練");
        }
        
        // 獲取訓練數據和標籤
        List<LabeledPoint> trainingData = new ArrayList<>(classifier.getTrainingData());
        Map<String, List<LabeledPoint>> labelToPointsMap = classifier.getLabelToPointsMap();
        
        log.info("準備評估模型，訓練數據大小 = {}", trainingData.size());
        if (trainingData.size() < folds) {
            throw new IllegalStateException("訓練數據不足以進行指定折數的交叉驗證");
        }

        // 初始化評估結果
        EvaluationResult result = new EvaluationResult();

        // 計算每個類別的樣本數
        Map<String, Integer> classCounts = new HashMap<>();
        for (Map.Entry<String, List<LabeledPoint>> entry : labelToPointsMap.entrySet()) {
            classCounts.put(entry.getKey(), entry.getValue().size());
        }
        result.setClassCounts(classCounts);

        // 初始化混淆矩陣
        Map<String, Map<String, Integer>> confusionMatrix = initializeConfusionMatrix(labelToPointsMap.keySet());

        // 打亂訓練數據
        log.info("打亂數據進行交叉驗證");
        List<LabeledPoint> shuffledData = new ArrayList<>(trainingData);
        Collections.shuffle(shuffledData);

        // 準備交叉驗證
        int foldSize = trainingData.size() / folds;
        
        // 使用線程池並行處理交叉驗證
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        int threadPoolSize = Math.min(availableProcessors, folds);
        log.info("使用 {} 個線程進行交叉驗證處理", threadPoolSize);
        
        ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);

        try {
            // 創建並提交交叉驗證任務
            List<Future<FoldResult>> futures = IntStream.range(0, folds)
                    .mapToObj(i -> executor.submit(new CrossValidationTask(
                            i, folds, foldSize, shuffledData, classifier.getConfig(), maxTestSamplesPerFold
                    )))
                    .collect(Collectors.toList());
            
            // 收集結果
            int totalCorrect = 0;
            int totalSamples = 0;
            
            // 相關係數計算準備
            double sumActualY = 0;
            double sumPredictedY = 0;
            double sumActualYSquared = 0;
            double sumPredictedYSquared = 0;
            double sumActualPredictedY = 0;
            
            // 收集所有標籤
            Set<String> allLabels = new HashSet<>(labelToPointsMap.keySet());
            
            // 處理每個折的結果
            for (Future<FoldResult> future : futures) {
                FoldResult foldResult = future.get();
                
                // 添加新標籤到全局標籤集
                allLabels.addAll(foldResult.uniqueLabels);
                
                // 更新混淆矩陣
                for (Map.Entry<String, Map<String, Integer>> entry : foldResult.confusionMatrix.entrySet()) {
                    String actualLabel = entry.getKey();
                    for (Map.Entry<String, Integer> prediction : entry.getValue().entrySet()) {
                        String predictedLabel = prediction.getKey();
                        int count = prediction.getValue();
                        
                        // 安全更新混淆矩陣
                        for (int i = 0; i < count; i++) {
                            updateConfusionMatrix(confusionMatrix, actualLabel, predictedLabel, allLabels);
                        }
                    }
                }
                
                // 更新計數和R2計算數據
                totalCorrect += foldResult.correctPredictions;
                totalSamples += foldResult.totalSamples;
                
                sumActualY += foldResult.sumActualY;
                sumPredictedY += foldResult.sumPredictedY;
                sumActualYSquared += foldResult.sumActualYSquared;
                sumPredictedYSquared += foldResult.sumPredictedYSquared;
                sumActualPredictedY += foldResult.sumActualPredictedY;
            }
            
            // 計算準確率
            double accuracy = (double) totalCorrect / totalSamples;
            result.setAccuracy(accuracy);
            
            // 計算精確率、召回率和F1分數
            calculatePrecisionRecallF1(result, confusionMatrix, allLabels.size());
            
            // 計算R2分數（決定係數）
            double r2 = calculateR2Score(totalSamples, sumActualY, sumPredictedY, 
                    sumActualYSquared, sumPredictedYSquared, sumActualPredictedY);
            result.setR2Score(r2);
            
            // 設置混淆矩陣
            result.setConfusionMatrix(confusionMatrix);
            
        } catch (Exception e) {
            throw new RuntimeException("評估模型失敗", e);
        } finally {
            executor.shutdown();
        }
        
        return result;
    }
    
    /**
     * 初始化混淆矩陣
     * 
     * @param labels 所有類別標籤
     * @return 初始化的混淆矩陣
     */
    private static Map<String, Map<String, Integer>> initializeConfusionMatrix(Iterable<String> labels) {
        Map<String, Map<String, Integer>> confusionMatrix = new HashMap<>();
        for (String actualLabel : labels) {
            confusionMatrix.put(actualLabel, new HashMap<>());
            for (String predictedLabel : labels) {
                confusionMatrix.get(actualLabel).put(predictedLabel, 0);
            }
        }
        return confusionMatrix;
    }
    
    /**
     * 創建標籤到數字的映射
     * 
     * @param labels 所有類別標籤
     * @return 標籤到數字的映射
     */
    private static Map<String, Integer> createLabelNumberMapping(Iterable<String> labels) {
        Map<String, Integer> labelToNumber = new HashMap<>();
        int labelNumber = 0;
        for (String label : labels) {
            labelToNumber.put(label, labelNumber++);
        }
        return labelToNumber;
    }
    
    /**
     * 計算精確率、召回率和F1分數
     * 
     * @param result 評估結果對象
     * @param confusionMatrix 混淆矩陣
     * @param classCount 類別數量
     */
    private static void calculatePrecisionRecallF1(EvaluationResult result, 
                                                  Map<String, Map<String, Integer>> confusionMatrix,
                                                  int classCount) {
        double totalPrecision = 0;
        double totalRecall = 0;
        int validClassCount = 0;
        
        for (String label : confusionMatrix.keySet()) {
            Map<String, Integer> predictions = confusionMatrix.get(label);
            if (predictions == null || !predictions.containsKey(label)) {
                continue; // 跳過缺失的類別
            }
            
            int truePositives = predictions.get(label);

            // 計算假陽性（其他類被預測為此類）
            int falsePositives = 0;
            for (String otherLabel : confusionMatrix.keySet()) {
                if (!otherLabel.equals(label) && 
                    confusionMatrix.get(otherLabel) != null && 
                    confusionMatrix.get(otherLabel).containsKey(label)) {
                    falsePositives += confusionMatrix.get(otherLabel).get(label);
                }
            }

            // 計算假陰性（此類被預測為其他類）
            int falseNegatives = 0;
            for (String otherLabel : confusionMatrix.keySet()) {
                if (!otherLabel.equals(label) && predictions.containsKey(otherLabel)) {
                    falseNegatives += predictions.get(otherLabel);
                }
            }

            // 計算此類的精確率和召回率
            double precision = (truePositives + falsePositives) > 0 ?
                    (double) truePositives / (truePositives + falsePositives) : 0;
            double recall = (truePositives + falseNegatives) > 0 ?
                    (double) truePositives / (truePositives + falseNegatives) : 0;

            totalPrecision += precision;
            totalRecall += recall;
            validClassCount++;
        }

        // 計算宏平均 (使用有效類別數)
        double avgPrecision = validClassCount > 0 ? totalPrecision / validClassCount : 0;
        double avgRecall = validClassCount > 0 ? totalRecall / validClassCount : 0;

        result.setPrecision(avgPrecision);
        result.setRecall(avgRecall);

        // 計算F1分數
        double f1Score = (avgPrecision + avgRecall) > 0 ?
                2 * avgPrecision * avgRecall / (avgPrecision + avgRecall) : 0;
        result.setF1Score(f1Score);
    }
    
    /**
     * 計算R2分數
     * 
     * @param totalSamples 總樣本數
     * @param sumActualY 實際標籤值總和
     * @param sumPredictedY 預測標籤值總和
     * @param sumActualYSquared 實際標籤值平方和
     * @param sumPredictedYSquared 預測標籤值平方和
     * @param sumActualPredictedY 實際與預測標籤值乘積和
     * @return R2分數
     */
    private static double calculateR2Score(int totalSamples, double sumActualY, double sumPredictedY,
                                          double sumActualYSquared, double sumPredictedYSquared,
                                          double sumActualPredictedY) {
        double numerator = totalSamples * sumActualPredictedY - sumActualY * sumPredictedY;
        double denomPart1 = totalSamples * sumActualYSquared - sumActualY * sumActualY;
        double denomPart2 = totalSamples * sumPredictedYSquared - sumPredictedY * sumPredictedY;

        double r = denomPart1 > 0 && denomPart2 > 0 ?
                numerator / Math.sqrt(denomPart1 * denomPart2) : 0;
        return r * r;
    }
    
    /**
     * 計算歐氏距離
     *
     * @param a 第一個點的特徵向量
     * @param b 第二個點的特徵向量
     * @return 兩點之間的歐氏距離
     */
    public static double calculateDistance(double[] a, double[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("特徵維度不匹配");
        }

        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            sum += Math.pow(a[i] - b[i], 2);
        }
        return Math.sqrt(sum);
    }
    
    /**
     * 計算曼哈頓距離
     *
     * @param a 第一個點的特徵向量
     * @param b 第二個點的特徵向量
     * @return 兩點之間的曼哈頓距離
     */
    public static double calculateManhattanDistance(double[] a, double[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("特徵維度不匹配");
        }

        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            sum += Math.abs(a[i] - b[i]);
        }
        return sum;
    }
    
    /**
     * 計算切比雪夫距離
     *
     * @param a 第一個點的特徵向量
     * @param b 第二個點的特徵向量
     * @return 兩點之間的切比雪夫距離
     */
    public static double calculateChebyshevDistance(double[] a, double[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("特徵維度不匹配");
        }

        double maxDiff = 0.0;
        for (int i = 0; i < a.length; i++) {
            maxDiff = Math.max(maxDiff, Math.abs(a[i] - b[i]));
        }
        return maxDiff;
    }
    
    /**
     * 計算餘弦相似度
     * 
     * @param a 第一個點的特徵向量
     * @param b 第二個點的特徵向量
     * @return 兩點之間的餘弦相似度（越接近1表示越相似）
     */
    public static double calculateCosineSimilarity(double[] a, double[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("特徵維度不匹配");
        }
        
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += Math.pow(a[i], 2);
            normB += Math.pow(b[i], 2);
        }
        
        if (normA == 0 || normB == 0) {
            return 0; // 避免除零錯誤
        }
        
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
    
    /**
     * 計算餘弦距離（基於餘弦相似度）
     * 
     * @param a 第一個點的特徵向量
     * @param b 第二個點的特徵向量
     * @return 兩點之間的餘弦距離（1-餘弦相似度）
     */
    public static double calculateCosineDistance(double[] a, double[] b) {
        return 1 - calculateCosineSimilarity(a, b);
    }
    
    /**
     * 交叉驗證單折任務
     */
    private static class CrossValidationTask implements Callable<FoldResult> {
        private final int foldIndex;
        private final int totalFolds;
        private final int foldSize;
        private final List<LabeledPoint> shuffledData;
        private final WeightedKNNClassifier.Config config;
        private final int maxTestSamplesPerFold;
        
        public CrossValidationTask(int foldIndex, int totalFolds, int foldSize, 
                                  List<LabeledPoint> shuffledData, 
                                  WeightedKNNClassifier.Config config,
                                  int maxTestSamplesPerFold) {
            this.foldIndex = foldIndex;
            this.totalFolds = totalFolds;
            this.foldSize = foldSize;
            this.shuffledData = shuffledData;
            this.config = config;
            this.maxTestSamplesPerFold = maxTestSamplesPerFold;
        }
        
        @Override
        public FoldResult call() {
            // 計算測試集範圍
            int startIdx = foldIndex * foldSize;
            int endIdx = (foldIndex == totalFolds - 1) ? shuffledData.size() : (foldIndex + 1) * foldSize;

            // 提取測試集
            List<LabeledPoint> fullTestFold = new ArrayList<>(shuffledData.subList(startIdx, endIdx));
            List<LabeledPoint> testFold;

            if (fullTestFold.size() > maxTestSamplesPerFold) {
                // 從完整測試集中隨機抽樣
                Collections.shuffle(fullTestFold);
                testFold = new ArrayList<>(fullTestFold.subList(0, maxTestSamplesPerFold));
                log.debug("折 {} 縮減測試集大小從 {} 到 {}", foldIndex, fullTestFold.size(), testFold.size());
            } else {
                testFold = fullTestFold;
            }

            // 提取訓練集（排除測試集）
            List<LabeledPoint> trainFold = new ArrayList<>();
            for (int j = 0; j < shuffledData.size(); j++) {
                if (j < startIdx || j >= endIdx) {
                    trainFold.add(shuffledData.get(j));
                }
            }

            // 創建並訓練臨時分類器
            WeightedKNNClassifier tempClassifier = new WeightedKNNClassifier(config);
            tempClassifier.train(trainFold);
            
            // 在測試集上評估
            FoldResult foldResult = new FoldResult();
            foldResult.uniqueLabels = new HashSet<>(tempClassifier.getUniqueLabels());
            
            // 初始化混淆矩陣
            Map<String, Map<String, Integer>> confusionMatrix = initializeConfusionMatrix(
                    tempClassifier.getUniqueLabels());
            
            // 創建標籤到數字的映射
            Map<String, Integer> labelToNumber = createLabelNumberMapping(tempClassifier.getUniqueLabels());
            
            int correctPredictions = 0;
            int processedSamples = 0;
            
            for (LabeledPoint testPoint : testFold) {
                processedSamples++;
                String actualLabel = testPoint.getLabel();
                String predictedLabel = tempClassifier.predict(testPoint.getFeatures());
                
                // 添加實際標籤到唯一標籤集合
                foldResult.uniqueLabels.add(actualLabel);
                foldResult.uniqueLabels.add(predictedLabel);

                // 更新混淆矩陣（處理可能不在訓練集中的新標籤）
                updateConfusionMatrix(confusionMatrix, actualLabel, predictedLabel, foldResult.uniqueLabels);

                // 計算正確預測數
                if (predictedLabel.equals(actualLabel)) {
                    correctPredictions++;
                }

                // 如果標籤不在映射中，添加它
                if (!labelToNumber.containsKey(actualLabel)) {
                    labelToNumber.put(actualLabel, labelToNumber.size());
                }
                if (!labelToNumber.containsKey(predictedLabel)) {
                    labelToNumber.put(predictedLabel, labelToNumber.size());
                }
                
                // 為R2計算收集數據
                int actualY = labelToNumber.get(actualLabel);
                int predictedY = labelToNumber.get(predictedLabel);

                foldResult.sumActualY += actualY;
                foldResult.sumPredictedY += predictedY;
                foldResult.sumActualYSquared += actualY * actualY;
                foldResult.sumPredictedYSquared += predictedY * predictedY;
                foldResult.sumActualPredictedY += actualY * predictedY;
            }
            
            foldResult.confusionMatrix = confusionMatrix;
            foldResult.correctPredictions = correctPredictions;
            foldResult.totalSamples = processedSamples;
            
            return foldResult;
        }
    }
    
    /**
     * 單折交叉驗證結果
     */
    private static class FoldResult {
        Map<String, Map<String, Integer>> confusionMatrix = new HashMap<>();
        Set<String> uniqueLabels = new HashSet<>();
        int correctPredictions = 0;
        int totalSamples = 0;
        double sumActualY = 0;
        double sumPredictedY = 0;
        double sumActualYSquared = 0;
        double sumPredictedYSquared = 0;
        double sumActualPredictedY = 0;
    }
}