package com.apitore.api.summarize;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.annotation.Bean;


/**
 * @author Keigo Hattori
 */
@EnableFeignClients
@EnableDiscoveryClient
@EnableCircuitBreaker
@EnableZuulProxy
@SpringBootApplication
public class SummarizeAppMain {

  public static void main(String[] args) {
    SpringApplication.run(SummarizeAppMain.class, args);
  }

  @Bean(name="documentFrequency")
  public Map<String,Double> documentFrequency() throws IOException, ClassNotFoundException {
    Map<String,Double> map = new HashMap<>();
    BufferedReader br = new BufferedReader(new FileReader(new File("./jawiki-corpus.df.txt")));
    String str;
    while((str = br.readLine()) != null) {
      String[] arr = str.split("\t");
      map.put(arr[0], Double.valueOf(arr[1]));
    }
    br.close();
    return map;
  }

}