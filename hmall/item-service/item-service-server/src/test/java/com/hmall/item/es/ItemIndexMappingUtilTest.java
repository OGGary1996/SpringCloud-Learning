package com.hmall.item.es;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ItemIndexMappingUtilTest {
    @Resource
    private ItemIndexMappingUtil itemIndexMapping;

    /*
    * 测试新增 index
    * */
    @Test
    void initItemMappingIndex() {
        try {
            itemIndexMapping.initItemMappingIndex();
        } catch (Exception e) {
            fail("Initialization of item mapping index failed: " + e.getMessage());
        }
    }

    /*
    * 测试 index 是否存在
    * */
    @Test
    public void isIndexExistsTest() throws IOException {
        itemIndexMapping.isItemIndexExists();
    }

    /*
    * 测试删除 index
    * */
    @Test
    public void deleteIndexTest() throws IOException {
        itemIndexMapping.deleteItemIndex();
    }
}