package com.impetus.product_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.impetus.product_service.dto.ProductRequestDto;
import com.impetus.product_service.dto.ProductResponseDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester.*;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

import java.math.BigDecimal;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
class ProductServiceApplicationTests {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:4.4.2").withReplicaSet();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry dynamicPropertyRegistry){
        dynamicPropertyRegistry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }
//    @Test
//	void shouldCreateProducts() throws Exception {
//        ProductRequestDto productRequest = getProductRequest();
//        String productRequestString = objectMapper.writeValueAsString(productRequest);
//        mockMvc.perform(MockMvcRequestBuilder.post("/api/product")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(productRequestString)
//                .andExpect(status().isCreated());
//
//	}

    private ProductRequestDto getProductRequest(){
        return ProductRequestDto.builder()
                .name("IPhone 13")
                .description("Iphone 13")
                .price(BigDecimal.valueOf(120000))
                .build();
    }

}
