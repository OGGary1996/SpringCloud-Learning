package com.hmall.item.es;

import com.alibaba.fastjson.JSON;
import com.hmall.common.domain.PageDTO;
import com.hmall.item.api.dto.ItemDTO;
import com.hmall.item.domain.doc.ItemDoc;
import com.hmall.item.domain.query.ItemPageQuery;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/*
* 本类用于实现 ES 搜索相关功能
* */
@Service
@Slf4j
public class EsSearchService {
    @SuppressWarnings("deprecation")
    private final RestHighLevelClient client;
    @Autowired
    public EsSearchService(RestHighLevelClient client) {
        this.client = client;
    }

    /*
    * 实现复杂的搜索功能，组合查询
    * 比如：bool 查询、范围查询、分页、排序、聚合等
    * */
    public SearchResponse search(ItemPageQuery query) throws IOException {
        // 1. 构建搜索请求对象
        SearchRequest searchRequest = new SearchRequest(EsConstant.ITEM_INDEX);
        // 2. 构建复杂查询条件，对应 DSL 中的 bool 查询
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        // 2.1 构建 bool 查询
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        // 2.2 must 条件，全文检索，需要判断非空
        if(StringUtils.hasText(query.getKey())) {
            boolQuery.must(QueryBuilders.matchQuery("name", query.getKey()));
        }
        // 2.3 filter 条件
          // 2.3.1 分类过滤
        if (StringUtils.hasText(query.getCategory())) {
            boolQuery.filter(QueryBuilders.termQuery("category", query.getCategory()));
        }
          // 2.3.2 品牌过滤
        if (StringUtils.hasText(query.getBrand())) {
            boolQuery.filter(QueryBuilders.termQuery("brand", query.getBrand()));
        }
        // 2.3.3 价格区间过滤
        if (query.getMinPrice() != null || query.getMaxPrice() != null) { // 如果至少一个不为空就进入，如果都为空就跳过
            RangeQueryBuilder price = QueryBuilders.rangeQuery("price");
            if (query.getMinPrice() != null) {
                price.gte(query.getMinPrice());
            }
            if (query.getMaxPrice() != null) {
                price.lte(query.getMaxPrice());
            }
            boolQuery.filter(price);
        }

        // 3. 将 bool 查询设置到 sourceBuilder 中
        sourceBuilder.query(boolQuery);

        // 4. 设置排序和分页参数
        sourceBuilder.sort("updateTime", SortOrder.DESC);
          // 4.1 补充根据销量排序，sortBy字段来自于ItemPageQuery所继承的 PageQuery
        if (StringUtils.hasText(query.getSortBy())) {
            sourceBuilder.sort(query.getSortBy(), SortOrder.DESC);
        }

        // 5. 分页，pageNo, pageSize 来自于 ItemPageQuery 所继承的 PageQuery
        int from = (query.getPageNo() - 1) * query.getPageSize();
        int pageSize = query.getPageSize();
        sourceBuilder.from(from).size(pageSize);

        // 6.高亮显示
        if (StringUtils.hasText(query.getKey())) {
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.field("name")
                    .preTags("<span style='color:red;font-weight:bold;'>")
                    .postTags("</span>");
            sourceBuilder.highlighter(highlightBuilder);
        }

        // 7. 将 sourceBuilder 设置到 searchRequest 中
        searchRequest.source(sourceBuilder);

        // 8. 发送搜索请求，获取响应结果
        return client.search(searchRequest, RequestOptions.DEFAULT);
    }


    /*
    * 解析结果
    * SearchResponse
         └── hits
             ├── total（命中总数）
             └── hits[]（每一条文档）
                 ├── _id
                 ├── _score
                 ├── _source（JSON）
                 └── highlight
                    └── fieldName -> Text[]
    * */
    public PageDTO<ItemDTO> parseSearchResponse(SearchResponse response) {
        // 1. 获取命中结果
        SearchHits hits = response.getHits();
        long totalHits = hits.getTotalHits().value;
        long pages = totalHits / ItemPageQuery.DEFAULT_PAGE_SIZE;
        log.info("命中总数: {}", totalHits);

        // 2. 遍历命中结果，并且反序列化为 ItemDoc 对象
        List<ItemDoc> itemDocs = new ArrayList<>();
        hits.forEach(hit -> {
            // 原始 JSON
            String sourceAsString = hit.getSourceAsString();
            // 反序列化为 ItemDoc 对象
            ItemDoc itemDoc = JSON.parseObject(sourceAsString, ItemDoc.class);
            itemDocs.add(itemDoc);

            // 补充: 处理高亮结果
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            HighlightField highlightField = highlightFields.get("name");
            if (highlightField != null) {
                Text[] fragments = highlightField.getFragments();
                String highlightedField = fragments[0].string();
                // 将高亮结果设置到 itemDoc 中，结果包含了 <span> 标签，替换掉原有的 name 字段
                itemDoc.setName(highlightedField);
            }
        });

        // 3. Doc 转 DTO
        List<ItemDTO> itemDTOs = itemDocs.stream().map(itemDoc -> {
            ItemDTO itemDTO = new ItemDTO();
            BeanUtils.copyProperties(itemDoc, itemDTO);
            return itemDTO;
        }).toList();

        // 4. 返回 PageDTO 对象
        PageDTO<ItemDTO> pageDTO = new PageDTO<>();
        pageDTO.setTotal(totalHits);
        pageDTO.setPages(pages);
        pageDTO.setList(itemDTOs);
        return pageDTO;
    }

}
