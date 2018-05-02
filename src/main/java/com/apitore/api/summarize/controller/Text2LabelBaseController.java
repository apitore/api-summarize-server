package com.apitore.api.summarize.controller;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.deeplearning4j.clustering.cluster.Cluster;
import org.deeplearning4j.clustering.cluster.ClusterSet;
import org.deeplearning4j.clustering.cluster.Point;
import org.deeplearning4j.clustering.kmeans.KMeansClustering;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.ops.transforms.Transforms;
import org.springframework.web.bind.annotation.RestController;

import com.apitore.banana.response.com.atilika.kuromoji.TokenEntity;
import com.apitore.banana.response.summarize.LabelEntity;


/**
 * @author Keigo Hattori
 */
@RestController
public class Text2LabelBaseController {

  final protected List<String> TARGET_NOUN = Arrays.asList(new String[]{
      "サ変接続",
      "ナイ形容詞語幹",
      "形容動詞語幹",
      "一般",
      "固有名詞",
  });

  final protected List<String> TARGET_VERB = Arrays.asList(new String[]{
      "自立",
  });

  final protected List<String> TARGET_ADJ = Arrays.asList(new String[]{
      "自立",
  });

  final protected List<String> TARGET_OTH = Arrays.asList(new String[]{
      "未知語",
  });

  final protected List<String> IGNORE_WORDS = Arrays.asList(new String[]{
      "する",
      "なる",
      "ある",
      "*",
  });


  /**
   * 形態素解析結果のフィルタリング
   *
   * @param toks
   * @return
   */
  protected List<String> filterMorpho(List<TokenEntity> toks) {
    List<String> rtn = new ArrayList<>();
    for (TokenEntity tok: toks) {
      if (IGNORE_WORDS.contains(tok.getBaseForm())) {
        continue;
      } else if (tok.getPartOfSpeechLevel1().equals("名詞") &&
          TARGET_NOUN.contains(tok.getPartOfSpeechLevel2())) {
        rtn.add(tok.getBaseForm());
      } else if (tok.getPartOfSpeechLevel1().equals("動詞") &&
          TARGET_VERB.contains(tok.getPartOfSpeechLevel2())) {
        rtn.add(tok.getBaseForm());
      } else if (tok.getPartOfSpeechLevel1().equals("形容詞") &&
          TARGET_ADJ.contains(tok.getPartOfSpeechLevel2())) {
        rtn.add(tok.getBaseForm());
      } else if (TARGET_OTH.contains(tok.getPartOfSpeechLevel1())) {
        rtn.add(tok.getSurface());
      } else {
        continue;
      }
    }
    return rtn;
  }

  /**
   * TFIDF値でソート
   *
   * @param tfidf
   * @return
   */
  protected List<Entry<String, Double>> sortTfidf(Map<String,Double> tfidf) {
    List<Entry<String, Double>> entries = new ArrayList<Entry<String, Double>>(tfidf.entrySet());
    Collections.sort(entries, new Comparator<Entry<String, Double>>() {
      @Override
      public int compare(Entry<String, Double> o1, Entry<String, Double> o2) {
        return o2.getValue().compareTo(o1.getValue());
      }
    });
    return entries;
  }

  /**
   * TFIDFの上位語を取得
   *
   * @param num
   * @param entries
   * @return
   */
  protected List<LabelEntity> getTfidfLabels(int num, List<Entry<String, Double>> entries) {
    List<LabelEntity> labels = new ArrayList<>();
    int i=0;
    for (Entry<String,Double> entry: entries) {
      if (i>=num)
        break;
      LabelEntity ent = new LabelEntity();
      ent.setLabel(entry.getKey());
      ent.setScore(entry.getValue());
      labels.add(ent);
      i++;
    }
    return labels;
  }

  /**
   * Word vectorをクラスタリングして、各クラスタの代表語を取得
   *
   * @param num
   * @param vectors
   * @param contents
   * @return
   */
  protected List<LabelEntity> getWordvectorLabels(int num, List<INDArray> vectors, List<Entry<String, Double>> contents) {
    List<LabelEntity> labels = new ArrayList<>();
    KMeansClustering kmc = KMeansClustering.setup(num, 10, "euclidean");//FIXME ハードコーディング
    List<Point> pointsLst = Point.toPoints(vectors);
    ClusterSet cs = kmc.applyTo(pointsLst);
    pointsLst = null;
    List<Cluster> clsterLst = cs.getClusters();
    Set<String> chks = new HashSet<>();
    for(Cluster c: clsterLst) {
      Point center = c.getCenter();
      INDArray tmp = center.getArray();
      double chk = 0;
      String label = "";
      double score = 0;
      for (int j=0; j<vectors.size(); j++) {
        INDArray vector = vectors.get(j);
        Entry<String,Double> content = contents.get(j);
        double cos_distance = Transforms.cosineSim(vector, tmp);
        if (cos_distance>chk && !chks.contains(content.getKey())) {
          chk = cos_distance;
          label = content.getKey();
          score = content.getValue();
        }
      }
      if (label.isEmpty())
        continue;
      chks.add(label);
      LabelEntity ent = new LabelEntity();
      ent.setLabel(label);
      ent.setScore(score);
      labels.add(ent);
    }
    return labels;
  }

}
