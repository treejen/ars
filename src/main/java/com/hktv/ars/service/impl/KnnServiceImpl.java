package com.hktv.ars.service.impl;

import com.hktv.ars.data.AddressData;
import com.hktv.ars.data.LabeledPoint;
import com.hktv.ars.repository.EstateDao;
import com.hktv.ars.repository.StreetDao;
import com.hktv.ars.repository.StreetNumberDao;
import com.hktv.ars.service.KnnService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class KnnServiceImpl implements KnnService {

    @Value("${ars.classifier.model-path:knn_classifier.ser}")
    private String modelFilePath;

    @Value("${ars.classifier.need-train:true}")
    private boolean isNeedTrain;

    @Value("${ars.classifier.k:10}")
    private int k;

    private KNNClassifier classifier;

    public void initModel(List<AddressData> addressDataList) {
        if (isNeedTrain) {
            createAndTrainNewModel(addressDataList);
        } else {
            File modelFile = new File(modelFilePath);
            if (modelFile.exists()) {
                try {
                    classifier = KNNClassifier.loadModel(modelFilePath);
                    log.info("成功載入已訓練的KNN分類器");
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

    private void createAndTrainNewModel(List<AddressData> addressDataList) {
        List<LabeledPoint> trainingData = new ArrayList<>();
        addressDataList.stream()
                .map(data -> new LabeledPoint(new double[]{data.getLatitude().doubleValue(), data.getLongitude().doubleValue()}, data.getDeliveryZoneCode()))
                .forEach(trainingData::add);

        // 如果未配置 k 值，則定義為訓練數據的開平方根
        if (k <= 0) {
            k = (int) Math.sqrt(trainingData.size());
        }
        log.info("k值: {}", k);
        classifier = new KNNClassifier(k);

        // train
        classifier.train(trainingData);

        // save model
        try {
            classifier.saveModel(modelFilePath);
            log.info("成功訓練並保存新的KNN分類器");
        } catch (IOException e) {
            log.warn("保存模型失敗: {}", e.getMessage());
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
        return classifier.predict(x, y);
    }


}
