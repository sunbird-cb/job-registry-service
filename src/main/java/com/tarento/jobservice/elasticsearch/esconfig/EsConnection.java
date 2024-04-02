package com.tarento.jobservice.elasticsearch.esconfig;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.util.ResourceUtils;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
public class EsConnection {

    @Value("${elastic_certificate}")
    private String certificatePath;

    @Value("${elastic_use_ssl:true}")
    private Boolean useSSL;

    @Value("${elasticsearch.port:0}")
    private Integer esPort;

    @Value("${elasticsearch.username}")
    private String userName;

    @Value("${elasticsearch.password}")
    private String password;

    @Value("${elasticsearch.path_prefix}")
    private String pathPrefix;
    @Value("${elasticsearch.host}")
    private String esHost;

    @Value("${prod}")
    private boolean prod;

    private RestHighLevelClient restHighLevelClient;

    public EsConnection() {

    }

    @Bean(destroyMethod = "close")
    RestHighLevelClient client() throws Exception {
    if(prod) {
        RestClientBuilder.HttpClientConfigCallback clientConfigCallback = new RestClientBuilder.HttpClientConfigCallback() {
            @Override
            public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
                return httpClientBuilder.setConnectionTimeToLive(5, TimeUnit.MINUTES)
                        .setDefaultIOReactorConfig(
                                IOReactorConfig.custom()
                                        .setSoKeepAlive(true)
                                        .setIoThreadCount(1)
                                        .build());
            }
        };

        ClientConfiguration.MaybeSecureClientConfigurationBuilder builder = ClientConfiguration.builder()
                .connectedTo(esHost + ":" + esPort);

        if (useSSL)
            builder.usingSsl();

        log.info("ES ***************************");

        log.info("Host:{}, Port:{}", esHost, esPort);

        if (StringUtils.isNotEmpty(pathPrefix))
            builder.withPathPrefix(pathPrefix);
        else
            log.info("Not setting pathPrefix");

        if (StringUtils.isNotEmpty(userName) && StringUtils.isNotEmpty(password))
            builder.withBasicAuth(userName, password);
        else
            log.info("not setting password");

        builder.withConnectTimeout(10_000)
                .withSocketTimeout(60_000)
                .withHttpClientConfigurer(clientConfigCallback);

        // certificatePath
        if (StringUtils.isNotEmpty(certificatePath) && StringUtils.isNotBlank(certificatePath)) {
            log.info("Using certificate: '{}'", certificatePath);
            KeyStore trustStore = KeyStore.getInstance("jks");

            File file = new File(certificatePath);
            if (!file.exists()) {
                file = ResourceUtils.getFile("classpath:" + certificatePath);
            }

            InputStream is = new FileInputStream(file);
            trustStore.load(is, "changeit".toCharArray());

            SSLContextBuilder sslContextBuilder = SSLContexts.custom().loadTrustMaterial(trustStore, null);

            SSLContext sslContext = sslContextBuilder.build();
            builder.usingSsl(sslContext);
        }


        ClientConfiguration clientConfiguration = builder.build();

        log.info("ES ***************************");

        return RestClients.create(clientConfiguration)
                .rest();
    }
        log.info("ES ***************************");
        log.info("Host:{}, Port:{}", esHost, esPort);
        return new RestHighLevelClient(
                RestClient.builder(new HttpHost(esHost, esPort)));
    }


    public RestHighLevelClient getRestHighLevelClient() throws Exception {
        return client();
    }
    @Bean
    public ElasticsearchOperations elasticsearchTemplate() throws Exception {
        return new ElasticsearchRestTemplate(client());
    }

}
