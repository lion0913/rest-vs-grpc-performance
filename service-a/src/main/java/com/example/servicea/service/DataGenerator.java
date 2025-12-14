package com.example.servicea.service;

import com.example.servicea.model.DataItem;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class DataGenerator {

    /**
     * 약 2KB 크기의 DataItem을 생성합니다.
     */
    public DataItem generateDataItem() {
        String id = UUID.randomUUID().toString();
        long timestamp = System.currentTimeMillis();

        // 각 필드를 충분히 채워서 약 2KB 정도의 크기를 만듭니다
        return new DataItem(
            id,
            "Product-" + id.substring(0, 8),
            generateString(200), // description
            "Category-" + (timestamp % 10),
            generateString(500), // content
            timestamp,
            generateString(150), // metadata1
            generateString(150), // metadata2
            generateString(150), // metadata3
            generateString(150), // metadata4
            generateString(150), // metadata5
            generateString(200), // additionalInfo
            Math.random() * 1000,
            (int) (timestamp % 5),
            "tag1,tag2,tag3,tag4,tag5"
        );
    }

    /**
     * 지정된 개수만큼 DataItem을 생성합니다.
     */
    public List<DataItem> generateDataItems(int count) {
        List<DataItem> items = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            items.add(generateDataItem());
        }
        return items;
    }

    /**
     * 지정된 길이의 더미 문자열을 생성합니다.
     */
    private String generateString(int length) {
        StringBuilder sb = new StringBuilder(length);
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 ";
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return sb.toString();
    }
}