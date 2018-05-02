package com.apitore.api.summarize.controller;


import java.util.Map;

import org.apache.log4j.Logger;
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
import com.apitore.banana.request.clustering.VecvecRequestEntity;
import com.apitore.banana.request.clustering.VecwordRequestEntity;
import com.apitore.banana.request.word2vec.WordsRequestEntity;
import com.apitore.banana.response.clustering.SimilarityResponseEntity;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import springfox.documentation.annotations.ApiIgnore;


/**
 * @author Keigo Hattori
 */
@RestController
@RequestMapping(value = "/cosine-similarity")
public class CosineSimilarityController extends Text2LabelBaseController {

  private final Logger LOG = Logger.getLogger(CosineSimilarityController.class);

  @Autowired
  Word2VecIntegration word2VecIntegration;

  final String NOTES = "Cosine similarity.<BR />"
      + "Response<BR />"
      + "&nbsp; Github: <a href=\"https://github.com/keigohtr/apitore-response-parent/tree/master/clustering-response\">clustering-response</a><BR />"
      + "&nbsp; Class: com.apitore.banana.response.clustering.SimilarityResponseEntity<BR />";

  /**
   * 実態
   *
   * @param text
   * @param num
   * @return
   */
  @RequestMapping(value="/open/vec-vec", method=RequestMethod.POST)
  @ApiIgnore
  public ResponseEntity<SimilarityResponseEntity> vecVec(
      @RequestBody
      VecvecRequestEntity req
      ) {

    SimilarityResponseEntity model = new SimilarityResponseEntity();
    Long startTime = System.currentTimeMillis();

    if (req.getVec1().length != req.getVec2().length) {
      LOG.error("length not match.");
      model.setError(true);
      model.setLog("Vector size is not matched.");
      return new ResponseEntity<SimilarityResponseEntity>(model,HttpStatus.OK);
    }
    INDArray arr1 = Nd4j.create(req.getVec1());
    INDArray arr2 = Nd4j.create(req.getVec2());
    double cos_distance = Transforms.cosineSim(arr1, arr2);

    model.setScore(cos_distance);
    Long endTime = System.currentTimeMillis();
    Long processTime = endTime-startTime;
    model.setStartTime(startTime.toString());
    model.setEndTime(endTime.toString());
    model.setProcessTime(processTime.toString());
    return new ResponseEntity<SimilarityResponseEntity>(model,HttpStatus.OK);
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
  @RequestMapping(value = {"/vec-vec"}, produces=MediaType.APPLICATION_JSON_VALUE, method=RequestMethod.POST)
  @ApiOperation(value="Calclate similarity", notes=NOTES)
  public SimilarityResponseEntity vecVec(
      @ApiParam(value = "Access Token", required = true)
      @RequestParam("access_token")  String access_token,
      @ApiParam(value = "Input two vectors; vec1, vec2", required = true)
      @RequestBody
      VecvecRequestEntity req)
  {
    return new SimilarityResponseEntity();
  }

  /**
   * 実態
   *
   * @param text
   * @param num
   * @return
   */
  @RequestMapping(value="/open/vec-word", method=RequestMethod.POST)
  @ApiIgnore
  public ResponseEntity<SimilarityResponseEntity> vecWord(
      @RequestBody
      VecwordRequestEntity req
      ) {

    SimilarityResponseEntity model = new SimilarityResponseEntity();
    Long startTime = System.currentTimeMillis();

    ResponseEntity<Boolean> hres = word2VecIntegration.hasWord(req.getWord());
    if (hres.getStatusCode() != HttpStatus.OK) {
      LOG.error("word2vec hasWord error.");
      model.setError(true);
      model.setLog("Internal Server Error: \"word2vec\"");
      return new ResponseEntity<SimilarityResponseEntity>(model,HttpStatus.OK);
    }
    if (!hres.getBody()) {
      model.setLog("Out-of-vocabulary: "+req.getWord());
      model.setScore(0);
      Long endTime = System.currentTimeMillis();
      Long processTime = endTime-startTime;
      model.setStartTime(startTime.toString());
      model.setEndTime(endTime.toString());
      model.setProcessTime(processTime.toString());
      return new ResponseEntity<SimilarityResponseEntity>(model,HttpStatus.OK);
    }

    WordsRequestEntity went = new WordsRequestEntity();
    went.getWords().add(req.getWord());
    ResponseEntity<Map<String, double[]>> vres = word2VecIntegration.getWordVectorMatrix(went);
    if (vres.getStatusCode() != HttpStatus.OK) {//FIXME どう処理すれば良いか・・・。
      LOG.error("word2vec getwordvector error.");
      model.setError(true);
      model.setLog("Internal Server Error: \"word2vec\"");
      return new ResponseEntity<SimilarityResponseEntity>(model,HttpStatus.OK);
    }
    Map<String,double[]> vecmap = vres.getBody();
    double[] vec2 = vecmap.get(req.getWord());

    if (req.getVec().length != vec2.length) {
      LOG.error("length not match.");
      model.setError(true);
      model.setLog("Vector size is not matched.");
      return new ResponseEntity<SimilarityResponseEntity>(model,HttpStatus.OK);
    }
    INDArray arr1 = Nd4j.create(req.getVec());
    INDArray arr2 = Nd4j.create(vec2);
    double cos_distance = Transforms.cosineSim(arr1, arr2);

    model.setScore(cos_distance);
    Long endTime = System.currentTimeMillis();
    Long processTime = endTime-startTime;
    model.setStartTime(startTime.toString());
    model.setEndTime(endTime.toString());
    model.setProcessTime(processTime.toString());
    return new ResponseEntity<SimilarityResponseEntity>(model,HttpStatus.OK);
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
  @RequestMapping(value = {"/vec-word"}, produces=MediaType.APPLICATION_JSON_VALUE, method=RequestMethod.POST)
  @ApiOperation(value="Calclate similarity by word", notes=NOTES)
  public SimilarityResponseEntity vecWord(
      @ApiParam(value = "Access Token", required = true)
      @RequestParam("access_token")  String access_token,
      @ApiParam(value = "Input vector and word. Word is transformed to wordvector.", required = true)
      @RequestBody
      VecwordRequestEntity req)
  {
    return new SimilarityResponseEntity();
  }

}
