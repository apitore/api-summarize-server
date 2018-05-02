package com.apitore.api.summarize.controller;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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
import com.apitore.banana.request.com.atilika.kuromoji.KuromojiRequestEntity;
import com.apitore.banana.response.com.atilika.kuromoji.TokenEntity;
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
@RequestMapping(value = "/url2label-tfidf")
public class Url2LabelByTfidfController extends Text2LabelBaseController {

  private final Logger LOG = Logger.getLogger(Url2LabelByTfidfController.class);

  @Autowired
  KuromojiIntegration kuromojiIntegration;
  @Autowired
  DocumentFrequencyService  documentFrequencyService;

  final String NOTES = "Url2Label by tfidf.<BR />"
      + "Response<BR />"
      + "&nbsp; Github: <a href=\"https://github.com/keigohtr/apitore-response-parent/tree/master/summarize-response\">summarize-response</a><BR />"
      + "&nbsp; Class: com.apitore.banana.response.summarize.LabelResponseEntity<BR />";

  /**
   * 実態
   *
   * @param url
   * @param num
   * @return
   */
  @RequestMapping(value="/open/get", method=RequestMethod.GET)
  @ApiIgnore
  public ResponseEntity<LabelResponseEntity> get(
      @RequestParam("url") String url,
      @RequestParam(name="num", required=false, defaultValue="1")
      int num
      ) {

    LabelResponseEntity model = new LabelResponseEntity();
    Long startTime = System.currentTimeMillis();
    if (num<0)
      num=1;
    else if (num>10)
      num=10;

    /* Url2Text */
    List<String> sentences = new ArrayList<>();
    System.out.println(url);
    Document document = null;
    int i=0;
    while (i<5) {
      try {
        document = Jsoup.connect(url).get();
        break;
      } catch (Exception ex) {
        i++;
        System.err.println("  "+i+" try again.");
      }
    }
    if (document != null) {
      if (url.contains("ja.wikipedia.org")) {//FIXME ハードコーディング
        List<String> contents = Url2LabelByTfidfController.getTagText(document,"p");
        for (String content: contents) {
          sentences.addAll(SentenceSeparatorController.splitSentenceHeuristics(content));
        }
      } else {
        String content = document.text();
        sentences.addAll(SentenceSeparatorController.splitSentenceHeuristics(content));
      }
    }

    /* 強調タグ */
    List<String> importants=null;
    if (document != null) {
      importants = getTagText(document,"title,h1,em,strong,b,i");
    }
    int[] lens = new int[]{
        sentences.size(),importants.size()};
    sentences.addAll(importants);
    importants=null;

    /* 形態素解析 */
    KuromojiRequestEntity kent = new KuromojiRequestEntity();
    kent.setTexts(sentences);
    ResponseEntity<TokensResponseEntity> kres = kuromojiIntegration.kuromojiIpadicNeologdTokenize(kent);
    if (kres.getStatusCode() != HttpStatus.OK) {
      LOG.error("Internal Server Error: \"kuromoji\"");
      model.setError(true);
      model.setLog("Internal Server Error: \"kuromoji\"");
      return new ResponseEntity<LabelResponseEntity>(model,HttpStatus.OK);
    }

    List<TokenEntity> tokens = new ArrayList<>();
    int idx=-1;
    for (int j=0; j<lens.length; j++) {
      int len = lens[j];
      for (int k=0; k<len; k++) {
        idx++;
        tokens.addAll(kres.getBody().getTokens().get(idx));
        if (j>0) {
          tokens.addAll(kres.getBody().getTokens().get(idx));
        }
      }
    }

    /* tfidf計算 */
    List<String> toks = filterMorpho(tokens);
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
    toks = null;
    List<Entry<String, Double>> entries = sortTfidf(tfidf);

    /* output */
    List<LabelEntity> labels = getTfidfLabels(num,entries);

    model.setInput(url);
    model.setNum(String.valueOf(num));
    model.setLabels(labels);
    Long endTime = System.currentTimeMillis();
    Long processTime = endTime-startTime;
    model.setStartTime(startTime.toString());
    model.setEndTime(endTime.toString());
    model.setProcessTime(processTime.toString());
    return new ResponseEntity<LabelResponseEntity>(model,HttpStatus.OK);
  }

  static public List<String> getTagText(Document document, String tag) {
    List<String> texts = new ArrayList<>();
    Elements elm = document.select(tag);
    for (Element e: elm) {
      String text = e.text();
      if (!text.isEmpty())
        texts.add(text);
    }
    return texts;
  }


  /**
   * 公開用API
   * Dummyメソッド
   *
   * @param access_token
   * @param url
   * @param num
   * @return
   */
  @RequestMapping(value = {"/get"}, produces=MediaType.APPLICATION_JSON_VALUE, method=RequestMethod.GET)
  @ApiOperation(value="Get labels from URL", notes=NOTES)
  public LabelResponseEntity get(
      @ApiParam(value = "Access Token", required = true)
      @RequestParam("access_token")  String access_token,
      @ApiParam(value = "url", required = true)
      @RequestParam("url")       String url,
      @ApiParam(value = "num [max 10, default 1]", required = false, defaultValue="1")
      @RequestParam(name="num", required=false, defaultValue="1")
      int num)
  {
    return new LabelResponseEntity();
  }

}
