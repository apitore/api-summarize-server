package com.apitore.api.summarize.controller;


import java.util.Arrays;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.apitore.banana.response.summarize.SentenceResponseEntity;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import springfox.documentation.annotations.ApiIgnore;


/**
 * @author Keigo Hattori
 */
@RestController
@RequestMapping(value = "/sentence-separate")
public class SentenceSeparatorController {

  final String NOTES = "Sentence separator.<BR />"
      + "Response<BR />"
      + "&nbsp; Github: <a href=\"https://github.com/keigohtr/apitore-response-parent/tree/master/summarize-response\">summarize-response</a><BR />"
      + "&nbsp; Class: com.apitore.banana.response.summarize.SentenceResponseEntity<BR />";

  /**
   * 実態
   *
   * @param text
   * @return
   */
  @RequestMapping(value="/open/heuristics", method=RequestMethod.GET)
  @ApiIgnore
  public ResponseEntity<SentenceResponseEntity> heuristics(
      @RequestParam("text") String text
      ) {

    SentenceResponseEntity model = new SentenceResponseEntity();
    Long startTime = System.currentTimeMillis();

    List<String> sentences = splitSentenceHeuristics(text);

    model.setText(text);
    model.setSentences(sentences);
    Long endTime = System.currentTimeMillis();
    Long processTime = endTime-startTime;
    model.setStartTime(startTime.toString());
    model.setEndTime(endTime.toString());
    model.setProcessTime(processTime.toString());
    return new ResponseEntity<SentenceResponseEntity>(model,HttpStatus.OK);
  }

  static public List<String> splitSentenceHeuristics(String text) {
    text = text.replaceAll("([！？。」』\\?!]+)", "$1\n");
    text = text.replaceAll("([「『]+)", "\n$1");
    return Arrays.asList(text.split("[\r\n\t]+"));
  }


  /**
   * 公開用API
   * Dummyメソッド
   *
   * @param access_token
   * @param text
   * @return
   */
  @RequestMapping(value = {"/heuristics"}, produces=MediaType.APPLICATION_JSON_VALUE, method=RequestMethod.GET)
  @ApiOperation(value="Separate from text to sentence.", notes=NOTES)
  public SentenceResponseEntity heuristics(
      @ApiParam(value = "Access Token", required = true)
      @RequestParam("access_token")  String access_token,
      @ApiParam(value = "text", required = true)
      @RequestParam("text")       String text)
  {
    return new SentenceResponseEntity();
  }

}
