package com.apitore.api.summarize.controller;


import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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

import com.apitore.api.summarize.service.DocumentFrequencyService;
import com.apitore.api.summarize.service.KuromojiIntegration;
import com.apitore.api.summarize.service.Word2VecIntegration;
import com.apitore.banana.request.com.atilika.kuromoji.KuromojiRequestEntity;
import com.apitore.banana.request.textsimilarity.TextRequestEntity;
import com.apitore.banana.request.word2vec.WordsRequestEntity;
import com.apitore.banana.response.com.atilika.kuromoji.TokenEntity;
import com.apitore.banana.response.com.atilika.kuromoji.TokensResponseEntity;
import com.apitore.banana.response.textsimilarity.TextSimilarityResponseEntity;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import springfox.documentation.annotations.ApiIgnore;


/**
 * @author Keigo Hattori
 */
@RestController
@RequestMapping(value = "/sentence-similarity")
public class SentenceSimilarityController extends Text2LabelBaseController {

  private final Logger LOG = Logger.getLogger(SentenceSimilarityController.class);

  @Autowired
  KuromojiIntegration kuromojiIntegration;
  @Autowired
  DocumentFrequencyService  documentFrequencyService;
  @Autowired
  Word2VecIntegration word2VecIntegration;

  final String NOTES = "Sentence similarity.<BR />"
      + "Response<BR />"
      + "&nbsp; Github: <a href=\"https://github.com/keigohtr/apitore-response-parent/tree/master/text-similarity-response\">text-similarity-response</a><BR />"
      + "&nbsp; Class: com.apitore.banana.response.textsimilarity.TextSimilarityResponseEntity<BR />";


  /**
   * 実態
   *
   * @param text
   * @param num
   * @return
   */
  @SuppressWarnings("rawtypes")
  @RequestMapping(value="/open/eval", method=RequestMethod.POST)
  @ApiIgnore
  public ResponseEntity<TextSimilarityResponseEntity> eval(
      @RequestBody
      TextRequestEntity req
      ) {

    TextSimilarityResponseEntity model = new TextSimilarityResponseEntity();
    Long startTime = System.currentTimeMillis();

    /* 形態素解析 */
    KuromojiRequestEntity kent = new KuromojiRequestEntity();
    kent.getTexts().add(req.getText1());
    kent.getTexts().add(req.getText2());
    ResponseEntity<TokensResponseEntity> kres = kuromojiIntegration.kuromojiIpadicNeologdTokenize(kent);
    if (kres.getStatusCode() != HttpStatus.OK) {
      LOG.error("Internal Server Error: \"kuromoji\"");
      model.setError(true);
      model.setLog("Internal Server Error: \"kuromoji\"");
      return new ResponseEntity<TextSimilarityResponseEntity>(model,HttpStatus.OK);
    }

    /* word2vec */
    WordsRequestEntity went = new WordsRequestEntity();
    for (List<TokenEntity> tokens: kres.getBody().getTokens()) {
      for (TokenEntity token: tokens) {
        went.getWords().add(token.getSurface());
      }
    }
    ResponseEntity<Map<String, double[]>> vres = word2VecIntegration.getWordVectorMatrix(went);
    if (vres.getStatusCode() != HttpStatus.OK) {//FIXME どう処理すれば良いか・・・。
      LOG.error("word2vec getwordvector error.");
      model.setError(true);
      model.setLog("Internal Server Error: \"word2vec\"");
      return new ResponseEntity<TextSimilarityResponseEntity>(model,HttpStatus.OK);
    }
    Map<String,double[]> vecmap = vres.getBody();

    /* df */
    Map<String,Double> idfmap = new HashMap<>();
    Double tdf = documentFrequencyService.getDocumentFrequency("。");
    for (String str: went.getWords()) {
      Double df = documentFrequencyService.getDocumentFrequency(str);
      double idf = Math.log10((tdf+1)/(df+1));
      idfmap.put(str, idf);
    }

    /* 類似度計算の下準備 */
    Map<String, Double> tf1map = new HashMap<>();
    List<TokenEntity> toks = kres.getBody().getTokens().get(0);
    for (TokenEntity tok: toks) {
      String key = tok.getSurface();
      if (tf1map.containsKey(key))
        tf1map.put(key, tf1map.get(key)+1);
      else
        tf1map.put(key, 1D);
    }
    Map<String, Double> tf2map = new HashMap<>();
    toks = kres.getBody().getTokens().get(1);
    for (TokenEntity tok: toks) {
      String key = tok.getSurface();
      if (tf2map.containsKey(key))
        tf2map.put(key, tf2map.get(key)+1);
      else
        tf2map.put(key, 1D);
    }

    Map<String,Double> tmp = new HashMap<>();
    for (Iterator it2 = tf2map.entrySet().iterator(); it2.hasNext();) {
      Map.Entry entry2 = (Map.Entry)it2.next();
      String key2 = (String) entry2.getKey();
      if (!tf1map.containsKey(key2)) {
        Double tf2 = (Double) entry2.getValue();
        double[] vec2 = vecmap.get(key2);
        if (vec2==null)
          continue;
        double maxscore = 0.6; //FIXME
        String maxkey = null;
        for (Iterator it1 = tf1map.entrySet().iterator(); it1.hasNext();) {
          Map.Entry entry1 = (Map.Entry)it1.next();
          String key1 = (String) entry1.getKey();
          double[] vec1 = vecmap.get(key1);
          if (vec1==null)
            continue;
          INDArray arr1 = Nd4j.create(vec1);
          INDArray arr2 = Nd4j.create(vec2);
          double cos_distance = Transforms.cosineSim(arr1, arr2);
          if (maxscore<cos_distance) {
            maxscore=cos_distance;
            maxkey = key1;
          }
        }
        if (maxkey == null)
          continue;
        if (tmp.containsKey(maxkey))
          tmp.put(maxkey, tmp.get(maxkey)+maxscore*tf2);
        else
          tmp.put(maxkey, maxscore*tf2);
        tf2map.put(key2, 0D);
      }
    }
    for (Iterator it3 = tmp.entrySet().iterator(); it3.hasNext();) {
      Map.Entry entry = (Map.Entry)it3.next();
      String key = (String) entry.getKey();
      Double val = (Double) entry.getValue();
      if (tf2map.containsKey(key))
        tf2map.put(key, tf2map.get(key)+val);
      else
        tf2map.put(key, val);
    }
    tmp=null;

    /* 類似度計算 */
    int len = went.getWords().size();
    double[] nvec1 = new double[len];
    double[] nvec2 = new double[len];
    int idx=-1;
    for (String str: went.getWords()) {
      idx++;
      Double idf = idfmap.get(str);
      Double tf1 = tf1map.get(str);
      Double tf2 = tf2map.get(str);
      if (tf1==null)
        nvec1[idx]=0;
      else
        nvec1[idx]=tf1*idf;
      if (tf2==null)
        nvec2[idx]=0;
      else
        nvec2[idx]=tf2*idf;
    }
    INDArray arr1 = Nd4j.create(nvec1);
    INDArray arr2 = Nd4j.create(nvec2);
    double cos_distance = Transforms.cosineSim(arr1, arr2);

    model.setText1(req.getText1());
    model.setText2(req.getText2());
    model.setSimilarity(cos_distance);
    Long endTime = System.currentTimeMillis();
    Long processTime = endTime-startTime;
    model.setStartTime(startTime.toString());
    model.setEndTime(endTime.toString());
    model.setProcessTime(processTime.toString());
    return new ResponseEntity<TextSimilarityResponseEntity>(model,HttpStatus.OK);
  }


  /**
   * 公開用API
   * Dummyメソッド
   *
   * @param access_token
   * @param req
   * @return
   */
  @RequestMapping(value = {"/eval"}, produces=MediaType.APPLICATION_JSON_VALUE, method=RequestMethod.POST)
  @ApiOperation(value="Text similarity using word2vec", notes=NOTES)
  public TextSimilarityResponseEntity eval(
      @ApiParam(value = "Access Token", required = true)
      @RequestParam("access_token")  String access_token,
      @ApiParam(value = "Input texts. Text must be a sentence.", required = true)
      @RequestBody
      TextRequestEntity req)
  {
    return new TextSimilarityResponseEntity();
  }

}
