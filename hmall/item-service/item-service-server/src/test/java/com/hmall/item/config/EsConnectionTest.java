package com.hmall.item.config;

import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.io.IOException;


@SpringBootTest
class EsConnectionTest {
    @Resource
    @SuppressWarnings("deprecation")
    private RestHighLevelClient restHighLevelClient;

    /*
    * 测试 Elasticsearch 连接是否成功
    * */
    @Test
    void contextLoads() throws IOException {
        boolean ping = restHighLevelClient.ping(RequestOptions.DEFAULT);
        System.out.println("Elasticsearch 连接状态: " + ping);
    }
}