package com.apitore.api.summarize.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.apitore.banana.request.com.atilika.kuromoji.KuromojiRequestEntity;
import com.apitore.banana.response.com.atilika.kuromoji.TokensResponseEntity;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;


@FeignClient("api-kuromoji-server")
interface KuromojiClient {
  @RequestMapping(method = RequestMethod.GET, value="/kuromoji-ipadic/open/tokenize")
  ResponseEntity<TokensResponseEntity> kuromojiIpadicTokenize(
      @RequestBody   KuromojiRequestEntity req);

  @RequestMapping(method = RequestMethod.GET, value="/kuromoji-ipadic-neologd/open/tokenize")
  ResponseEntity<TokensResponseEntity> kuromojiIpadicNeologdTokenize(
      @RequestBody   KuromojiRequestEntity req);
}

@Component
public class KuromojiIntegration {
  private final String ERROR_SERVER_DOWN = "Server down.";

  @Autowired
  KuromojiClient kuromojiClient;

  public ResponseEntity<TokensResponseEntity> kuromojiTokenizeFallback(KuromojiRequestEntity req) {
    TokensResponseEntity map = new TokensResponseEntity();
    map.setLog("Error: "+ERROR_SERVER_DOWN);
    return new ResponseEntity<TokensResponseEntity>(map,HttpStatus.INTERNAL_SERVER_ERROR);
  }

  public ResponseEntity<TokensResponseEntity> kuromojiTokenizeFallback2(KuromojiRequestEntity req) {
    TokensResponseEntity map = new TokensResponseEntity();
    map.setLog("Error: "+ERROR_SERVER_DOWN);
    return new ResponseEntity<TokensResponseEntity>(map,HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @HystrixCommand(fallbackMethod = "kuromojiTokenizeFallback")
  public ResponseEntity<TokensResponseEntity> kuromojiIpadicTokenize(KuromojiRequestEntity req) {
    return kuromojiClient.kuromojiIpadicTokenize(req);
  }

  @HystrixCommand(fallbackMethod = "kuromojiTokenizeFallback2")
  public ResponseEntity<TokensResponseEntity> kuromojiIpadicNeologdTokenize(KuromojiRequestEntity req) {
    return kuromojiClient.kuromojiIpadicNeologdTokenize(req);
  }
}

