package com.impetus.product_service.service;

import com.impetus.product_service.dto.*;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService {

    ProductResponseDto create(CreateProductRequest req);
    ProductResponseDto getById(String id);
    ProductResponseDto update(String id, UpdateProductRequest req);
    void delete(String id);

    Page<ProductResponseDto> search(ProductSearchRequest req);
    Page<ProductResponseDto> list(int page, int size);

    ProductResponseDto updateInventory(String id, int delta);
    ProductResponseDto setPrice(String id, BigDecimal price);

    List<ProductResponseDto> getDetailsOfIds(List<String> productIds);

    //For order service
    UpdateInventoryResponse updateInventory(List<UpdateInventoryRequest> listOfProducts);
}
