package com.hktv.ars.service.impl;

import com.hktv.ars.algo.DistanceFunction;
import com.hktv.ars.algo.WeightedKNNClassifier;
import com.hktv.ars.data.AddressData;
import com.hktv.ars.data.LabeledPoint;
import com.hktv.ars.service.KnnService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class KnnServiceImpl implements KnnService {

    @Value("${ars.classifier.model-path:weighted_knn_classifier.ser}")
    private String modelFilePath;

    @Value("${ars.classifier.need-train:true}")
    private boolean isNeedTrain;

    @Value("${ars.classifier.k:10}")
    private int k;
    
    @Value("${ars.classifier.distance-function:euclidean}")
    private String distanceFunctionName;
    
    @Value("${ars.classifier.use-class-weights:true}")
    private boolean useClassWeights;
    
    @Value("${ars.classifier.max-class-weight:50.0}")
    private double maxClassWeight;
    
    @Value("${ars.classifier.distance-weight-factor:2.0}")
    private double distanceWeightFactor;
    
    @Value("${ars.classifier.epsilon:0.00001}")
    private double epsilon;

    private WeightedKNNClassifier classifier;

    /**
     * 初始化模型
     * 根據配置決定是創建新模型還是加載已有模型
     *
     * @param addressDataList 地址數據列表
     */
    public void initModel(List<AddressData> addressDataList) {
        if (isNeedTrain) {
            createAndTrainNewModel(addressDataList);
        } else {
            File modelFile = new File(modelFilePath);
            if (modelFile.exists()) {
                try {
                    classifier = WeightedKNNClassifier.loadModel(modelFilePath);
                    log.info("成功載入已訓練的加權KNN分類器");
                } catch (Exception e) {
                    log.warn("載入模型失敗，將創建新模型: {}", e.getMessage());
                    createAndTrainNewModel(addressDataList);
                }
            } else {
                log.info("未找到已訓練的模型，將創建新模型");
                createAndTrainNewModel(addressDataList);
            }
        }
    }

    /**
     * 創建並訓練新模型
     *
     * @param addressDataList 地址數據列表
     */
    private void createAndTrainNewModel(List<AddressData> addressDataList) {
        // 準備訓練數據
        List<LabeledPoint> trainingData = prepareTrainingData(addressDataList);

        // 如果未配置 k 值，則定義為訓練數據的開平方根
        if (k <= 0) {
            k = (int) Math.sqrt(trainingData.size());
        }
        
        // 獲取距離函數
        DistanceFunction distanceFunction = DistanceFunction.fromString(distanceFunctionName);
        log.info("使用距離函數: {}", distanceFunction);
        
        // 創建分類器配置
        WeightedKNNClassifier.Config config = WeightedKNNClassifier.Config.builder()
                .k(k)
                .distanceFunction(distanceFunction)
                .useClassWeights(useClassWeights)
                .maxClassWeight(maxClassWeight)
                .distanceWeightFactor(distanceWeightFactor)
                .epsilon(epsilon)
                .build();
                
        log.info("創建加權KNN分類器，k值: {}, 使用類別權重: {}", k, useClassWeights);
        classifier = new WeightedKNNClassifier(config);

        // 訓練
        classifier.train(trainingData);

        // 保存模型
        try {
            classifier.saveModel(modelFilePath);
            log.info("成功訓練並保存新的加權KNN分類器");
        } catch (IOException e) {
            log.warn("保存模型失敗: {}", e.getMessage());
        }
    }
    
    /**
     * 將地址數據轉換為訓練數據點
     *
     * @param addressDataList 地址數據列表
     * @return 帶標籤的數據點列表
     */
    private List<LabeledPoint> prepareTrainingData(List<AddressData> addressDataList) {
        List<LabeledPoint> trainingData = new ArrayList<>();
        addressDataList.stream()
                .map(data -> new LabeledPoint(
                        new double[]{data.getLatitude().doubleValue(), data.getLongitude().doubleValue()}, 
                        data.getDeliveryZoneCode()))
                .forEach(trainingData::add);
        
        log.info("準備了 {} 個訓練數據點", trainingData.size());
        return trainingData;
    }

    /**
     * 預測新點的標籤
     *
     * @param x 第一個特徵值（緯度）
     * @param y 第二個特徵值（經度）
     * @return 預測的標籤
     */
    public String predict(double x, double y) {
        return classifier.predict(x, y);
    }

    /**
     * 預測新點的標籤並附帶概率分佈
     *
     * @param x 第一個特徵值（緯度）
     * @param y 第二個特徵值（經度）
     * @return 各標籤的概率分佈
     */
    public Map<String, Double> predictWithProbabilities(double x, double y) {
        double[] features = {x, y};
        return classifier.predictProbabilities(features);
    }
}
