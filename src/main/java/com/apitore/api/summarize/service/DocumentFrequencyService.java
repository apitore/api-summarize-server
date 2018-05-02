package com.apitore.api.summarize.service;


import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;


/**
 * @author Keigo Hattori
 */
@Service
public class DocumentFrequencyService {

  @Autowired
  @Qualifier("documentFrequency")
  Map<String,Double> map;


  public Double getDocumentFrequency (String word) {
    if (map.containsKey(word))
      return map.get(word);
    else
      return 0D;
  }

}
