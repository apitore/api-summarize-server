package com.apitore.api.summarize.controller;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.deeplearning4j.clustering.cluster.Cluster;
import org.deeplearning4j.clustering.cluster.ClusterSet;
import org.deeplearning4j.clustering.cluster.Point;
import org.deeplearning4j.clustering.kmeans.KMeansClustering;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.ops.transforms.Transforms;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.apitore.api.summarize.service.Word2VecIntegration;
import com.apitore.banana.request.clustering.ClusteringRequestEntity;
import com.apitore.banana.request.word2vec.WordsRequestEntity;
import com.apitore.banana.response.clustering.ClusterEntity;
import com.apitore.banana.response.clustering.ClusterResponseEntity;
import com.apitore.banana.response.clustering.FactorEntity;
import com.apitore.banana.response.clustering.FactorEntityComparator;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import springfox.documentation.annotations.ApiIgnore;


/**
 * @author Keigo Hattori
 */
@RestController
@RequestMapping(value = "/kmeans-wordvector")
public class KmeansByWord2VecController extends Text2LabelBaseController {

  private final Logger LOG = Logger.getLogger(KmeansByWord2VecController.class);

  @Autowired
  Word2VecIntegration word2VecIntegration;

  final String NOTES = "kmeans clustering by word2vec.<BR />"
      + "Response<BR />"
      + "&nbsp; Github: <a href=\"https://github.com/keigohtr/apitore-response-parent/tree/master/clustering-response\">clustering-response</a><BR />"
      + "&nbsp; Class: com.apitore.banana.response.clustering.ClusterResponseEntity<BR />";

  /**
   * 実態
   *
   * @param text
   * @param num
   * @return
   */
  @RequestMapping(value="/open/cluster", method=RequestMethod.POST)
  @ApiIgnore
  public ResponseEntity<ClusterResponseEntity> cluster(
      @RequestBody
      ClusteringRequestEntity req
      ) {

    ClusterResponseEntity model = new ClusterResponseEntity();
    Long startTime = System.currentTimeMillis();
    int num = req.getNum();
    if (num<0)
      num=1;
    else if (num>100)
      num=100;
    int iter = req.getIter();
    if (iter<1)
      iter=2;
    else if (iter>100)
      iter=100;

    /* word2vec */
    WordsRequestEntity went = new WordsRequestEntity();
    went.setWords(req.getWords());
    ResponseEntity<Map<String, double[]>> vres = word2VecIntegration.getWordVectorMatrix(went);
    if (vres.getStatusCode() != HttpStatus.OK) {//FIXME どう処理すれば良いか・・・。
      LOG.error("word2vec getwordvector error.");
      model.setError(true);
      model.setLog("Internal Server Error: \"word2vec\"");
      return new ResponseEntity<ClusterResponseEntity>(model,HttpStatus.OK);
    }
    Map<String,double[]> vecmap = vres.getBody();

    List<INDArray> vectors = new ArrayList<>();
    List<String> contents = new ArrayList<>();
    int count=0;
    for (String word: req.getWords()) {
      count++;
      if (count>1000)
        break;
      ResponseEntity<Boolean> hres = word2VecIntegration.hasWord(word);
      if (hres.getStatusCode() != HttpStatus.OK)
        continue;
      if (!hres.getBody())
        continue;
      double[] vec = vecmap.get(word.toLowerCase());
      vectors.add(Nd4j.create(vec));
      contents.add(word);
    }
    List<Point> pointsLst = Point.toPoints(vectors);
    for (int i=0; i<pointsLst.size(); i++) {
      Point pt = pointsLst.get(i);
      String label = contents.get(i);
      pt.setLabel(label);
    }

    /* clustering, then output */
    KMeansClustering kmc = KMeansClustering.setup(num, iter, "euclidean");
    ClusterSet cs = kmc.applyTo(pointsLst);
    pointsLst = null;
    List<Cluster> clsterLst = cs.getClusters();
    List<ClusterEntity> clusters = new ArrayList<>();
    for(Cluster c: clsterLst) {
      ClusterEntity cluster = new ClusterEntity();
      Point center = c.getCenter();
      INDArray arr1 = center.getArray();
      cluster.setCenter(center.getArray().data().asDouble());
      List<Point> facts = c.getPoints();
      List<FactorEntity> wdlist = new ArrayList<>();
      for (Point pt: facts) {
        FactorEntity ent = new FactorEntity();
        ent.setWord(pt.getLabel());
        INDArray arr2 = pt.getArray();
        double cos_distance = Transforms.cosineSim(arr1, arr2);
        ent.setSimilarity(cos_distance);
        wdlist.add(ent);
      }
      Collections.sort(wdlist, new FactorEntityComparator());
      cluster.setWords(wdlist);
      clusters.add(cluster);
    }

    model.setClusters(clusters);
    Long endTime = System.currentTimeMillis();
    Long processTime = endTime-startTime;
    model.setStartTime(startTime.toString());
    model.setEndTime(endTime.toString());
    model.setProcessTime(processTime.toString());
    return new ResponseEntity<ClusterResponseEntity>(model,HttpStatus.OK);
  }


  /**
   * 公開用API
   * Dummyメソッド
   *
   * @param access_token
   * @param text
   * @param num
   * @return
   */
  @RequestMapping(value = {"/cluster"}, produces=MediaType.APPLICATION_JSON_VALUE, method=RequestMethod.POST)
  @ApiOperation(value="Words clustering by word2vec", notes=NOTES)
  public ClusterResponseEntity cluster(
      @ApiParam(value = "Access Token", required = true)
      @RequestParam("access_token")  String access_token,
      @ApiParam(value = "Clustering request entity", required = true)
      @RequestBody
      ClusteringRequestEntity req)
  {
    return new ClusterResponseEntity();
  }

}
