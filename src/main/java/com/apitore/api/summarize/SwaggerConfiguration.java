package com.apitore.api.summarize;


import static com.google.common.base.Predicates.or;
import static springfox.documentation.builders.PathSelectors.regex;

import java.util.Date;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;


/**
 * @author Keigo Hattori
 */
@Configuration
@EnableSwagger2
public class SwaggerConfiguration {

  final String PATH = "com.apitore.api.summarize.controller";

  @SuppressWarnings("unchecked")
  @Bean
  public Docket dfAPI() {
    return new Docket(DocumentationType.SWAGGER_2)
        .groupName("documentfrequency")
        .select()
        .apis(RequestHandlerSelectors.basePackage(PATH))
        .paths(or(
            regex(".*/documentfrequency.*")
            ))
        .build()
        .apiInfo(
            new ApiInfoBuilder()
            .title("Document frequency APIs")
            .description("Document frequency of Wikipedia.")
            .version("0.0.1")
            .build()
            )
        .directModelSubstitute(Date.class, Long.class);
  }

  @SuppressWarnings("unchecked")
  @Bean
  public Docket sentenceSeparateAPI() {
    return new Docket(DocumentationType.SWAGGER_2)
        .groupName("sentence-separate")
        .select()
        .apis(RequestHandlerSelectors.basePackage(PATH))
        .paths(or(
            regex(".*/sentence-separate.*")
            ))
        .build()
        .apiInfo(
            new ApiInfoBuilder()
            .title("Sentence separator APIs")
            .description("Simple sentence separator.")
            .version("0.0.1")
            .build()
            )
        .directModelSubstitute(Date.class, Long.class);
  }

  @SuppressWarnings("unchecked")
  @Bean
  public Docket text2labelTfidfAPI() {
    return new Docket(DocumentationType.SWAGGER_2)
        .groupName("text2label-tfidf")
        .select()
        .apis(RequestHandlerSelectors.basePackage(PATH))
        .paths(or(
            regex(".*/text2label-tfidf.*")
            ))
        .build()
        .apiInfo(
            new ApiInfoBuilder()
            .title("Text2Label by tfidf APIs")
            .description("Text to label by tfidf of contents.")
            .version("0.0.1")
            .build()
            )
        .directModelSubstitute(Date.class, Long.class);
  }

  @SuppressWarnings("unchecked")
  @Bean
  public Docket text2labelWordvectorAPI() {
    return new Docket(DocumentationType.SWAGGER_2)
        .groupName("text2label-wordvector")
        .select()
        .apis(RequestHandlerSelectors.basePackage(PATH))
        .paths(or(
            regex(".*/text2label-wordvector.*")
            ))
        .build()
        .apiInfo(
            new ApiInfoBuilder()
            .title("Text2Label by word vector APIs")
            .description("Text to label by word2vec of contents.")
            .version("0.0.1")
            .build()
            )
        .directModelSubstitute(Date.class, Long.class);
  }

  @SuppressWarnings("unchecked")
  @Bean
  public Docket url2labelTfidfAPI() {
    return new Docket(DocumentationType.SWAGGER_2)
        .groupName("url2label-tfidf")
        .select()
        .apis(RequestHandlerSelectors.basePackage(PATH))
        .paths(or(
            regex(".*/url2label-tfidf.*")
            ))
        .build()
        .apiInfo(
            new ApiInfoBuilder()
            .title("Url2Label by tfidf APIs")
            .description("Url to label by tfidf of contents.")
            .version("0.0.1")
            .build()
            )
        .directModelSubstitute(Date.class, Long.class);
  }

  @SuppressWarnings("unchecked")
  @Bean
  public Docket url2labelWordvectorAPI() {
    return new Docket(DocumentationType.SWAGGER_2)
        .groupName("url2label-wordvector")
        .select()
        .apis(RequestHandlerSelectors.basePackage(PATH))
        .paths(or(
            regex(".*/url2label-wordvector.*")
            ))
        .build()
        .apiInfo(
            new ApiInfoBuilder()
            .title("Url2Label by word vector APIs")
            .description("Url to label by word2vec of contents.")
            .version("0.0.1")
            .build()
            )
        .directModelSubstitute(Date.class, Long.class);
  }

  @SuppressWarnings("unchecked")
  @Bean
  public Docket kmeansWordVectorAPI() {
    return new Docket(DocumentationType.SWAGGER_2)
        .groupName("kmeans-wordvector")
        .select()
        .apis(RequestHandlerSelectors.basePackage(PATH))
        .paths(or(
            regex(".*/kmeans-wordvector.*")
            ))
        .build()
        .apiInfo(
            new ApiInfoBuilder()
            .title("Kmeans clustering by word2vec")
            .description("Kmeans clustering by word2vec.")
            .version("0.0.1")
            .build()
            )
        .directModelSubstitute(Date.class, Long.class);
  }

  @SuppressWarnings("unchecked")
  @Bean
  public Docket cosineSimilarityAPI() {
    return new Docket(DocumentationType.SWAGGER_2)
        .groupName("cosine-similarity")
        .select()
        .apis(RequestHandlerSelectors.basePackage(PATH))
        .paths(or(
            regex(".*/cosine-similarity.*")
            ))
        .build()
        .apiInfo(
            new ApiInfoBuilder()
            .title("Cosine Similarity")
            .description("Cosine Similarity.")
            .version("0.0.1")
            .build()
            )
        .directModelSubstitute(Date.class, Long.class);
  }

  @SuppressWarnings("unchecked")
  @Bean
  public Docket sentenceSimilarityAPI() {
    return new Docket(DocumentationType.SWAGGER_2)
        .groupName("sentence-similarity")
        .select()
        .apis(RequestHandlerSelectors.basePackage(PATH))
        .paths(or(
            regex(".*/sentence-similarity.*")
            ))
        .build()
        .apiInfo(
            new ApiInfoBuilder()
            .title("Sentence Similarity")
            .description("Sentence Similarity.")
            .version("0.0.1")
            .build()
            )
        .directModelSubstitute(Date.class, Long.class);
  }

}