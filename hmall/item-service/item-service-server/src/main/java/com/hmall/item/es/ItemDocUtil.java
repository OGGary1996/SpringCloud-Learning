package com.hmall.item.es;

import com.alibaba.fastjson.JSON;
import com.hmall.item.domain.doc.ItemDoc;
import com.hmall.item.domain.po.Item;
import com.hmall.item.service.IItemService;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
@Slf4j
public class ItemDocUtil {
    @Autowired
    private final IItemService itemService;
    @SuppressWarnings("deprecation")
    private final RestHighLevelClient client;
    public ItemDocUtil(IItemService itemService, RestHighLevelClient client) {
        this.itemService = itemService;
        this.client = client;
    }

    /*
    * 新增单个商品文档
    * 或者：
    * 全量更新单个商品文档
    * 本质上属于：
    *  1. 如果文档存在，则覆盖更新
    *  2. 如果文档不存在，则新增
    * */
    public void saveItemDoc(String id) throws IOException {
        // 1. 获取到商品数据
        Long longId = Long.valueOf(id);
        Item item = itemService.getById(longId);
        if (item == null) {
            throw new RuntimeException("未找到对应的商品，id=" + id);
        }
        // 2. 将 Item 转换为 ItemDoc
        ItemDoc itemDoc = new ItemDoc();
        BeanUtils.copyProperties(item, itemDoc);

        // 3. 创建 IndexRequest 对象
        IndexRequest indexRequest = new IndexRequest(EsConstant.ITEM_INDEX)
                .id(itemDoc.getId().toString())
                .source(JSON.toJSONString(itemDoc), XContentType.JSON);
        // 4. 发送请求
        IndexResponse indexResponse = client.index(indexRequest, RequestOptions.DEFAULT);
        DocWriteResponse.Result result = indexResponse.getResult();
        log.info("新增商品文档，id={}，结果={}", id, result);
    }

    /*
    * 批量新增商品文档
    * 数据库来自 MySQL 的 hmall-item 表
    * */
    public void saveItemDocBatch() throws IOException {
        // 1. 从数据库中获取所有商品数据
        List<Item> items = itemService.list();

        // 2. 将Item转换为ItemDoc对象列表
        List<ItemDoc> itemDocs = items.stream().map(item -> {
            ItemDoc itemDoc = new ItemDoc();
            BeanUtils.copyProperties(item, itemDoc);
            return itemDoc;
        }).toList();

        // 3. 批量保存到 Elasticsearch
        // 3.1 创建 批量请求对象
        BulkRequest bulkRequest = new BulkRequest();
        // 3.2 遍历 itemDocs 列表，添加到 bulkRequest
        itemDocs.forEach(itemDoc -> {
            IndexRequest indexRequest = new IndexRequest(EsConstant.ITEM_INDEX)
                    .id(itemDoc.getId().toString())
                    .source(JSON.toJSONString(itemDoc), XContentType.JSON);
            bulkRequest.add(indexRequest);
        });
        // 3.3 发送批量请求
        BulkResponse bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);

        // 4. 验证有无失败
        if (bulkResponse.hasFailures()) {
            throw new IOException("批量保存商品文档失败: " + bulkResponse.buildFailureMessage());
        }
    }

    /*
    * 根据 doc id 查询商品文档
    * */
    public ItemDoc getItemDocById(String id) throws IOException {
        // 1. 创建查询请求对象
        GetRequest getRequest = new GetRequest(EsConstant.ITEM_INDEX, id);
        // 2. 发送请求，获取响应
        GetResponse docResponse = client.get(getRequest, RequestOptions.DEFAULT);

        // 3. 验证是否存在
        if (!docResponse.isExists()) {
            log.warn("未找到对应的商品文档，id={}", id);
            return null;
        }
        // 4. 将结果转换为 ItemDoc 对象并返回
        log.info("查询到商品文档，id={}", id);
        String sourceJson = docResponse.getSourceAsString();
        ItemDoc itemDoc = JSON.parseObject(sourceJson, ItemDoc.class);
        log.info("查询到的商品文档: {}", itemDoc);
        return itemDoc;
    }

    /*
    * 根据 id 删除商品文档
    * */
    public void deleteItemDocById(String id) throws IOException {
        // 1. 创建删除请求对象
        DeleteRequest deleteRequest = new DeleteRequest(EsConstant.ITEM_INDEX, id);
        // 2. 发送请求
        DeleteResponse deleteResponse = client.delete(deleteRequest, RequestOptions.DEFAULT);
        // 3. 获取结果并打印日志
        DocWriteResponse.Result result = deleteResponse.getResult();
        log.info("删除商品文档，id={}，结果={}", id, result);
    }

}
