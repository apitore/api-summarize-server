package com.apitore.api.summarize.controller;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.apitore.api.summarize.service.DocumentFrequencyService;
import com.apitore.api.summarize.service.KuromojiIntegration;
import com.apitore.api.summarize.service.Word2VecIntegration;
import com.apitore.banana.request.com.atilika.kuromoji.KuromojiRequestEntity;
import com.apitore.banana.request.word2vec.WordsRequestEntity;
import com.apitore.banana.response.com.atilika.kuromoji.TokensResponseEntity;
import com.apitore.banana.response.summarize.LabelEntity;
import com.apitore.banana.response.summarize.LabelResponseEntity;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import springfox.documentation.annotations.ApiIgnore;


/**
 * @author Keigo Hattori
 */
@RestController
@RequestMapping(value = "/text2label-wordvector")
public class Text2LabelByWordVectorController extends Text2LabelBaseController {

  private final Logger LOG = Logger.getLogger(Text2LabelByWordVectorController.class);

  @Autowired
  Word2VecIntegration word2VecIntegration;
  @Autowired
  KuromojiIntegration kuromojiIntegration;
  @Autowired
  DocumentFrequencyService  documentFrequencyService;

  final String NOTES = "Text2Label by kmeans of word vectors.<BR />"
      + "Response<BR />"
      + "&nbsp; Github: <a href=\"https://github.com/keigohtr/apitore-response-parent/tree/master/summarize-response\">summarize-response</a><BR />"
      + "&nbsp; Class: com.apitore.banana.response.summarize.LabelResponseEntity<BR />";

  /**
   * 実態
   *
   * @param text
   * @param num
   * @return
   */
  @RequestMapping(value="/open/get", method=RequestMethod.GET)
  @ApiIgnore
  public ResponseEntity<LabelResponseEntity> get(
      @RequestParam("text") String text,
      @RequestParam(name="num", required=false, defaultValue="1")
      int num
      ) {

    LabelResponseEntity model = new LabelResponseEntity();
    Long startTime = System.currentTimeMillis();
    if (num<0)
      num=1;
    else if (num>10)
      num=10;

    /* 形態素解析 */
    KuromojiRequestEntity kent = new KuromojiRequestEntity();
    kent.getTexts().add(text);
    ResponseEntity<TokensResponseEntity> kres = kuromojiIntegration.kuromojiIpadicNeologdTokenize(kent);
    if (kres.getStatusCode() != HttpStatus.OK) {
      model.setError(true);
      model.setLog("Internal Server Error: \"kuromoji\"");
      return new ResponseEntity<LabelResponseEntity>(model,HttpStatus.OK);
    }

    /* tfidf計算 */
    List<String> toks = filterMorpho(kres.getBody().getTokens().get(0));
    Map<String,Double> tfidf = new HashMap<>();
    Double tdf = documentFrequencyService.getDocumentFrequency("。");
    for (String str: toks) {
      Double df = documentFrequencyService.getDocumentFrequency(str);
      double idf = Math.log10((tdf+1)/(df+1));
      if (tfidf.containsKey(str)) {
        tfidf.put(str, tfidf.get(str)+idf);
      } else {
        tfidf.put(str, idf);
      }
    }
    List<Entry<String, Double>> entries = sortTfidf(tfidf);

    /* word2vec */
    WordsRequestEntity ent = new WordsRequestEntity();
    for (Entry<String,Double> entry: entries) {
      ent.getWords().add(entry.getKey());
    }
    ResponseEntity<Map<String, double[]>> vres = word2VecIntegration.getWordVectorMatrix(ent);
    if (vres.getStatusCode() != HttpStatus.OK) {//FIXME どう処理すれば良いか・・・。
      LOG.error("word2vec getwordvector error.");
      model.setError(true);
      model.setLog("Internal Server Error: \"word2vec\"");
      return new ResponseEntity<LabelResponseEntity>(model,HttpStatus.OK);
    }
    Map<String,double[]> vecmap = vres.getBody();

    List<INDArray> vectors = new ArrayList<>();
    List<Entry<String, Double>> contents = new ArrayList<>();
    for (Entry<String,Double> entry: entries) {
      ResponseEntity<Boolean> hres = word2VecIntegration.hasWord(entry.getKey());
      if (hres.getStatusCode() != HttpStatus.OK)
        continue;
      if (!hres.getBody())
        continue;
      double[] vec = vecmap.get(entry.getKey());
      vectors.add(Nd4j.create(vec));
      contents.add(entry);
    }

    /* clustering, then output */
    List<LabelEntity> labels = getWordvectorLabels(num,vectors,contents);

    model.setInput(text);
    model.setNum(String.valueOf(num));
    model.setLabels(labels);
    Long endTime = System.currentTimeMillis();
    Long processTime = endTime-startTime;
    model.setStartTime(startTime.toString());
    model.setEndTime(endTime.toString());
    model.setProcessTime(processTime.toString());
    return new ResponseEntity<LabelResponseEntity>(model,HttpStatus.OK);
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
  @RequestMapping(value = {"/get"}, produces=MediaType.APPLICATION_JSON_VALUE, method=RequestMethod.GET)
  @ApiOperation(value="Get labels from text", notes=NOTES)
  public LabelResponseEntity get(
      @ApiParam(value = "Access Token", required = true)
      @RequestParam("access_token")  String access_token,
      @ApiParam(value = "text", required = true)
      @RequestParam("text")       String text,
      @ApiParam(value = "num [max 10, default 1]", required = false, defaultValue="1")
      @RequestParam(name="num", required=false, defaultValue="1")
      int num)
  {
    return new LabelResponseEntity();
  }

}
