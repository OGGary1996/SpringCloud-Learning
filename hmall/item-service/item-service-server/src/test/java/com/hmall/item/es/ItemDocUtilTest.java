package com.hmall.item.es;

import com.hmall.item.domain.doc.ItemDoc;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.io.IOException;

@SpringBootTest
class ItemDocUtilTest {
    @Resource
    private ItemDocUtil itemDocUtil;

    @Test
    void saveItemDocTest() throws IOException {
        itemDocUtil.saveItemDoc("317580");
    }

    @Test
    void saveItemDocBatch() throws IOException {
        itemDocUtil.saveItemDocBatch();
    }

    @Test
    void getItemDocById() throws IOException {
        ItemDoc itemDoc = itemDocUtil.getItemDocById("317580");
    }

    @Test
    void deleteItemDocByIdTest() throws IOException {
        itemDocUtil.deleteItemDocById("317580");
    }
}