package com.hmall.item.es;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


/*
* 本类提供商品 index,mapping 初始化的方法
* */
@Component
@Slf4j
public class ItemIndexMappingUtil {
    @Resource
    @SuppressWarnings("deprecation")
    private RestHighLevelClient client;
    /*
    * 创建 Index + Mapping
    * 注意：不建议先设置空的 Index，再设置 Mapping 的方式创建索引，因为这样会导致索引分片数和副本数无法自定义
    * */
    public void initItemMappingIndex() throws IOException {
        // 1. 创建Request对象，设置Index名称
        CreateIndexRequest request = new CreateIndexRequest(EsConstant.ITEM_INDEX);
        // 2. 设置Mapping，通过Map的方式创建
        // 2.1 设置 properties 字段
        Map<String, Object> properties = new HashMap<>();
        properties.put("id", Map.of("type", "keyword"));
        properties.put("name", Map.of("type", "text",
                "analyzer", "ik_max_word",
                "search_analyzer", "ik_smart"));
        properties.put("price", Map.of("type", "integer"));
        properties.put("image", Map.of("type", "keyword"));
        properties.put("category", Map.of("type", "keyword"));
        properties.put("brand", Map.of("type", "keyword"));
        properties.put("sold", Map.of("type", "integer"));
        properties.put("comment_count", Map.of("type", "integer"));
        properties.put("isAD", Map.of("type", "boolean"));
        properties.put("create_time", Map.of(
                "type", "date",
                "format", "yyyy-MM-dd HH:mm:ss||epoch_millis"
        ));
        // 2.2 设置 mapping
        Map<String, Object> mapping = new HashMap<>();
        mapping.put("properties", properties);
        // 2.3 将 mapping 设置到 request 中
        request.mapping(mapping);

        // 3. 发送请求,获取响应
        CreateIndexResponse response = client.indices() // index 复数形式： indices()
                .create(request, RequestOptions.DEFAULT);
        if (response.isAcknowledged()) {
            log.info("商品索引创建成功");
        }
    }

    /*
    * 删除 index
    * */
    public void deleteItemIndex() throws IOException {
        DeleteIndexRequest deleteRequest = new DeleteIndexRequest(EsConstant.ITEM_INDEX);
        AcknowledgedResponse deleted = client.indices().delete(deleteRequest, RequestOptions.DEFAULT);
        if (deleted.isAcknowledged()) {
            log.info("商品索引删除成功");
        }
    }

    /*
    * 查看是否存在 index
    * */
    public void isItemIndexExists() throws IOException {
            GetIndexRequest getRequest = new GetIndexRequest(EsConstant.ITEM_INDEX);
        boolean exists = client.indices().exists(getRequest, RequestOptions.DEFAULT);
        if (exists) {
            log.info("商品索引存在");
        } else {
            log.info("商品索引不存在");
        }
    }
}



