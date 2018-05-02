package com.apitore.api.summarize.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.apitore.api.summarize.service.DocumentFrequencyService;
import com.apitore.banana.response.summarize.DocumentFrequencyResponseEntity;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import springfox.documentation.annotations.ApiIgnore;


/**
 * @author Keigo Hattori
 */
@RestController
@RequestMapping(value = "/documentfrequency")
public class DocumentFrequencyController {

  @Autowired
  DocumentFrequencyService  documentFrequencyService;

  final String NOTES = "Document Frequency by JaWikipedia 2016-9-15 dump.<BR />"
      + "Response<BR />"
      + "&nbsp; Github: <a href=\"https://github.com/keigohtr/apitore-response-parent/tree/master/summarize-response\">summarize-response</a><BR />"
      + "&nbsp; Class: com.apitore.banana.response.summarize.DocumentFrequencyResponseEntity<BR />";

  /**
   * 実態
   *
   * @param word
   * @return
   */
  @RequestMapping(value="/open/get", method=RequestMethod.GET)
  @ApiIgnore
  public ResponseEntity<DocumentFrequencyResponseEntity> get(
      @RequestParam("word") String word
      ) {

    DocumentFrequencyResponseEntity model = new DocumentFrequencyResponseEntity();
    Long startTime = System.currentTimeMillis();
    model.setWord(word);

    Double df = documentFrequencyService.getDocumentFrequency(word);
    model.setDocumentFrequency(df);

    Long endTime = System.currentTimeMillis();
    Long processTime = endTime-startTime;
    model.setStartTime(startTime.toString());
    model.setEndTime(endTime.toString());
    model.setProcessTime(processTime.toString());
    return new ResponseEntity<DocumentFrequencyResponseEntity>(model,HttpStatus.OK);
  }


  /**
   * 公開用API
   * Dummyメソッド
   *
   * @param access_token
   * @param word
   * @return
   */
  @RequestMapping(value = {"/get"}, produces=MediaType.APPLICATION_JSON_VALUE, method=RequestMethod.GET)
  @ApiOperation(value="Get document frequency", notes=NOTES)
  public DocumentFrequencyResponseEntity get(
      @ApiParam(value = "Access Token", required = true)
      @RequestParam("access_token")  String access_token,
      @ApiParam(value = "word", required = true)
      @RequestParam("word")       String word)
  {
    return new DocumentFrequencyResponseEntity();
  }

}
