package com.impetus.product_service.controller;

import com.impetus.product_service.dto.*;
import com.impetus.product_service.response.ApiResponse;
import com.impetus.product_service.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponseDto>> create(@Valid @RequestBody CreateProductRequest req){
        ProductResponseDto res = productService.create(req);
        return ResponseEntity.ok(new ApiResponse<ProductResponseDto>("Product created successfully", res));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponseDto>> getProduct(@PathVariable String id){
        return ResponseEntity.ok(new ApiResponse<>("Fetched product successfully", productService.getById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponseDto>> update(@PathVariable String id, @Valid @RequestBody UpdateProductRequest req){
        ProductResponseDto res = productService.update(id, req);
        return ResponseEntity.ok(new ApiResponse<>("Product updated successfully", res));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id){
        productService.delete(id);
        return ResponseEntity.ok(new ApiResponse<>("Product deleted successfully", null));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<ProductResponseDto>>> search(@RequestBody ProductSearchRequest req){
        Page<ProductResponseDto> res = productService.search(req);
        return ResponseEntity.ok(new ApiResponse<>("Product fetched successfully", res));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductResponseDto>>> list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size){
        Page<ProductResponseDto> res = productService.list(page, size);
        return ResponseEntity.ok(new ApiResponse<>("Products fetched successfully", res));
    }

    //For Order Service
    @Tag(name = "For inter service communication", description = "Used for internal service communication only by order service")
    @PostMapping("/detailsOfIds")
    public ResponseEntity<ApiResponse<List<ProductResponseDto>>> getProductFromIds(@RequestBody List<String> productIds) throws InterruptedException {
        List<ProductResponseDto> productResponseDtoList = productService.getDetailsOfIds(productIds);
        //For testing retry
        Thread.sleep(3000);
        return ResponseEntity.ok(new ApiResponse<>("Products fetched successfully", productResponseDtoList));
    }

    //for order service
    @Tag(name = "For inter service communication", description = "Used for internal service communication only by order service")
    @PostMapping(value = "/updateInventory", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UpdateInventoryResponse> updateInventory(@RequestBody UpdateInventoryDto req){
        UpdateInventoryResponse response = productService.updateInventory(req.getItem());
        return ResponseEntity.ok(response);
    }

}
