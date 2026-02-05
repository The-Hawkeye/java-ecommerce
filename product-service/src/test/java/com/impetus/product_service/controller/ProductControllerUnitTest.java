package com.impetus.product_service.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.impetus.product_service.dto.*;
import com.impetus.product_service.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Standalone MockMvc unit tests for ProductController.
 * ProductService is mocked; no Spring context required.
 *
 * DTOs used here align with your provided definitions:
 *  - UpdateInventoryDto { List<UpdateInventoryRequest> item; }
 *  - UpdateInventoryResponse { boolean success; List<FailedItem> failedItems; }
 *  - UpdateProductRequest { name, description, price, inventoryQuantity, attributes }
 */
class ProductControllerUnitTest {

    private MockMvc mockMvc;
    private ProductService productService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        productService = Mockito.mock(ProductService.class);
        ProductController controller = new ProductController(productService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                // If you have a @ControllerAdvice for error handling, add it here:
                // .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
    }

    // ---------- Helpers ----------

    private CreateProductRequest createReq(String name, String description, String sku,
                                           BigDecimal price, Integer inventoryQuantity,
                                           Map<String, Object> attributes) {
        CreateProductRequest req = new CreateProductRequest();
        req.setName(name);
        req.setDescription(description);
        req.setSku(sku);
        req.setPrice(price);
        req.setInventoryQuantity(inventoryQuantity);
        req.setAttributes(attributes);
        return req;
    }

    private UpdateProductRequest updateReq(String name, String description,
                                           BigDecimal price, Integer inventoryQuantity,
                                           Map<String, Object> attributes) {
        UpdateProductRequest req = new UpdateProductRequest();
        req.setName(name);
        req.setDescription(description);
        req.setPrice(price);
        req.setInventoryQuantity(inventoryQuantity);
        req.setAttributes(attributes);
        return req;
    }

    private ProductResponseDto productDto(String id, String name, String description, String sku,
                                          BigDecimal price, Integer inventoryQuantity,
                                          Map<String, Object> attributes,
                                          Instant createdAt, Instant updatedAt) {
        ProductResponseDto dto = new ProductResponseDto();
        dto.setId(id);
        dto.setName(name);
        dto.setDescription(description);
        dto.setSku(sku);
        dto.setPrice(price);
        dto.setInventoryQuantity(inventoryQuantity);
        dto.setAttributes(attributes);
//        dto.setCreatedAt(createdAt);
//        dto.setUpdatedAt(updatedAt);
        return dto;
    }

    private ProductSearchRequest searchReq(String query, String category,
                                           BigDecimal minPrice, BigDecimal maxPrice,
                                           int page, int size) {
        ProductSearchRequest req = new ProductSearchRequest();
        // Adjust to your actual fields if they differ
        req.setQuery(query);
        req.setMinPrice(minPrice);
        req.setMaxPrice(maxPrice);
        req.setPage(page);
        req.setSize(size);
        return req;
    }

    private UpdateInventoryDto updateInventoryDto(List<UpdateInventoryRequest> items) {
        UpdateInventoryDto req = new UpdateInventoryDto();
        req.setItem(items);
        return req;
    }

    private UpdateInventoryRequest invItem(String productId, int qty) {
        UpdateInventoryRequest r = new UpdateInventoryRequest();
        r.setProductId(productId);
        r.setQuantity(qty);
        return r;
    }

    private UpdateInventoryResponse successResponse() {
        UpdateInventoryResponse res = new UpdateInventoryResponse();
        res.setSuccess(true);
        res.setFailedItems(List.of());
        return res;
    }

    private UpdateInventoryResponse partialFailureResponse() {
        UpdateInventoryResponse.FailedItem f1 =
                new UpdateInventoryResponse.FailedItem("p-FAIL-1", "Insufficient stock", 1);
        UpdateInventoryResponse res = new UpdateInventoryResponse();
        res.setSuccess(false);
        res.setFailedItems(List.of(f1));
        return res;
    }

    // ---------- POST /product ----------

    @Test
    @DisplayName("POST /product: returns 200 OK with ApiResponse<ProductResponseDto>")
    void create_success() throws Exception {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("color", "blue");
        attrs.put("warrantyYears", 2);

        CreateProductRequest req = createReq(
                "iPhone 15", "Flagship smartphone", "SKU-IPH15-BLUE-128",
                new BigDecimal("79999.00"), 10, attrs
        );

        Instant now = Instant.now();
        ProductResponseDto dto = productDto(
                "p-100", "iPhone 15", "Flagship smartphone", "SKU-IPH15-BLUE-128",
                new BigDecimal("79999.00"), 10, attrs, now, now
        );

        when(productService.create(any(CreateProductRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Product created successfully"))
                .andExpect(jsonPath("$.data.id").value("p-100"))
                .andExpect(jsonPath("$.data.name").value("iPhone 15"))
                .andExpect(jsonPath("$.data.description").value("Flagship smartphone"))
                .andExpect(jsonPath("$.data.sku").value("SKU-IPH15-BLUE-128"))
                .andExpect(jsonPath("$.data.price").value(79999.00))
                .andExpect(jsonPath("$.data.inventoryQuantity").value(10))
                .andExpect(jsonPath("$.data.attributes.color").value("blue"))
                .andExpect(jsonPath("$.data.attributes.warrantyYears").value(2));

        verify(productService).create(any(CreateProductRequest.class));
    }

    // ---------- GET /product/{id} ----------

    @Test
    @DisplayName("GET /product/{id}: returns 200 OK with ApiResponse<ProductResponseDto>")
    void get_success() throws Exception {
        String id = "p-101";
        Map<String, Object> attrs = Map.of("size", "medium");

        ProductResponseDto dto = productDto(
                id, "Pixel 9", "Android smartphone", "SKU-PXL9-MED",
                new BigDecimal("69999.00"), 5, attrs, Instant.now(), Instant.now()
        );

        when(productService.getById(eq(id))).thenReturn(dto);

        mockMvc.perform(get("/product/{id}", id))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Fetched product successfully"))
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.name").value("Pixel 9"))
                .andExpect(jsonPath("$.data.sku").value("SKU-PXL9-MED"))
                .andExpect(jsonPath("$.data.price").value(69999.00))
                .andExpect(jsonPath("$.data.attributes.size").value("medium"));

        verify(productService).getById(eq(id));
    }

    // ---------- PUT /product/{id} ----------

    @Test
    @DisplayName("PUT /product/{id}: returns 200 OK with ApiResponse<ProductResponseDto>")
    void update_success() throws Exception {
        String id = "p-102";
        Map<String, Object> attrs = Map.of("color", "silver");

        UpdateProductRequest req = updateReq(
                "MacBook Pro", "Pro laptop",
                new BigDecimal("199999.00"), 3, attrs
        );

        ProductResponseDto dto = productDto(
                id, "MacBook Pro", "Pro laptop", "SKU-MBP-16-SLV",
                new BigDecimal("199999.00"), 3, attrs, Instant.now(), Instant.now()
        );

        when(productService.update(eq(id), any(UpdateProductRequest.class))).thenReturn(dto);

        mockMvc.perform(put("/product/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Product updated successfully"))
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.name").value("MacBook Pro"))
                .andExpect(jsonPath("$.data.description").value("Pro laptop"))
                .andExpect(jsonPath("$.data.price").value(199999.00))
                .andExpect(jsonPath("$.data.inventoryQuantity").value(3))
                .andExpect(jsonPath("$.data.attributes.color").value("silver"));

        verify(productService).update(eq(id), any(UpdateProductRequest.class));
    }

    // ---------- DELETE /product/{id} ----------

    @Test
    @DisplayName("DELETE /product/{id}: returns 200 OK with ApiResponse<Void>")
    void delete_success() throws Exception {
        String id = "p-103";
        doNothing().when(productService).delete(eq(id));

        mockMvc.perform(delete("/product/{id}", id))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Product deleted successfully"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(productService).delete(eq(id));
    }

    // ---------- GET /product/search (with JSON body) ----------

//    @Test
//    @DisplayName("GET /product/search: returns 200 OK with ApiResponse<Page<ProductResponseDto>>")
//    void search_success() throws Exception {
//        ProductSearchRequest req = searchReq(
//                "smartphone", "Phones",
//                new BigDecimal("50000.00"), new BigDecimal("90000.00"),
//                0, 2
//        );
//
//        Page<ProductResponseDto> page = new PageImpl<>(List.of(
//                productDto("p-201", "Galaxy S25", "Android flagship", "SKU-GS25-BLK",
//                        new BigDecimal("85999.00"), 8, Map.of("color", "black"),
//                        Instant.now(), Instant.now()),
//                productDto("p-202", "iPhone 15", "Flagship smartphone", "SKU-IPH15-WHT",
//                        new BigDecimal("79999.00"), 10, Map.of("color", "white"),
//                        Instant.now(), Instant.now())
//        ));
//
//        when(productService.search(any(ProductSearchRequest.class))).thenReturn(page);
//
//        mockMvc.perform(get("/product/search")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(req)))
//                .andExpect(status().isOk())
//                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
//                .andExpect(jsonPath("$.message").value("Product fetched successfully"))
//                .andExpect(jsonPath("$.data.content[0].id").value("p-201"))
//                .andExpect(jsonPath("$.data.content[0].name").value("Galaxy S25"))
//                .andExpect(jsonPath("$.data.content[0].sku").value("SKU-GS25-BLK"))
//                .andExpect(jsonPath("$.data.content[0].price").value(85999.00))
//                .andExpect(jsonPath("$.data.content[1].id").value("p-202"))
//                .andExpect(jsonPath("$.data.content[1].name").value("iPhone 15"))
//                .andExpect(jsonPath("$.data.content[1].sku").value("SKU-IPH15-WHT"))
//                .andExpect(jsonPath("$.data.content[1].price").value(79999.00));
//
//        verify(productService).search(any(ProductSearchRequest.class));
//    }

    // ---------- GET /product (list) ----------
//
//    @Test
//    @DisplayName("GET /product: returns 200 OK with ApiResponse<Page<ProductResponseDto>> (pagination)")
//    void list_success() throws Exception {
//        int pageNo = 1;
//        int size = 3;
//
//        Page<ProductResponseDto> page = new PageImpl<>(List.of(
//                productDto("p-301", "ThinkPad X1", "Business laptop", "SKU-TXPX1",
//                        new BigDecimal("149999.00"), 7, Map.of("color", "black"),
//                        Instant.now(), Instant.now()),
//                productDto("p-302", "Surface Laptop 6", "Windows laptop", "SKU-SL6",
//                        new BigDecimal("129999.00"), 9, Map.of("color", "platinum"),
//                        Instant.now(), Instant.now()),
//                productDto("p-303", "MacBook Air M3", "Lightweight laptop", "SKU-MBA-M3",
//                        new BigDecimal("119999.00"), 6, Map.of("color", "starlight"),
//                        Instant.now(), Instant.now())
//        ));
//
//        when(productService.list(eq(pageNo), eq(size))).thenReturn(page);
//
//        mockMvc.perform(get("/product")
//                        .param("page", String.valueOf(pageNo))
//                        .param("size", String.valueOf(size)))
//                .andExpect(status().isOk())
//                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
//                .andExpect(jsonPath("$.message").value("Products fetched successfully"))
//                .andExpect(jsonPath("$.data.content[0].id").value("p-301"))
//                .andExpect(jsonPath("$.data.content[1].id").value("p-302"))
//                .andExpect(jsonPath("$.data.content[2].id").value("p-303"));
//
//        verify(productService).list(eq(pageNo), eq(size));
//    }

    // ---------- POST /product/detailsOfIds ----------

    @Test
    @DisplayName("POST /product/detailsOfIds: returns 200 OK with ApiResponse<List<ProductResponseDto>>")
    void getProductFromIds_success() throws Exception {
        List<String> ids = List.of("p-401", "p-402");

        List<ProductResponseDto> dtos = List.of(
                productDto("p-401", "Echo Dot", "Smart speaker", "SKU-EDOT",
                        new BigDecimal("4999.00"), 50, Map.of("color", "black"),
                        Instant.now(), Instant.now()),
                productDto("p-402", "Fire TV Stick", "Streaming device", "SKU-FTVS",
                        new BigDecimal("3999.00"), 30, Map.of("color", "black"),
                        Instant.now(), Instant.now())
        );

        when(productService.getDetailsOfIds(eq(ids))).thenReturn(dtos);

        mockMvc.perform(post("/product/detailsOfIds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ids)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Products fetched successfully"))
                .andExpect(jsonPath("$.data[0].id").value("p-401"))
                .andExpect(jsonPath("$.data[0].sku").value("SKU-EDOT"))
                .andExpect(jsonPath("$.data[1].id").value("p-402"))
                .andExpect(jsonPath("$.data[1].sku").value("SKU-FTVS"));

        verify(productService).getDetailsOfIds(eq(ids));
    }

    // ---------- POST /product/updateInventory ----------

//    @Test
//    @DisplayName("POST /product/updateInventory: returns 200 OK with UpdateInventoryResponse (success, no failures)")
//    void updateInventory_success_allOk() throws Exception {
//        UpdateInventoryDto req = updateInventoryDto(List.of(
//                invItem("p-501", 2),
//                invItem("p-502", 1)
//        ));
//
//        UpdateInventoryResponse res = successResponse();
//
//        // Controller calls productService.updateInventory(req.getItem()) with a List<UpdateInventoryRequest>
//        when(productService.updateInventory(eq(req.getItem()))).thenReturn(res);
//
//        mockMvc.perform(post("/product/updateInventory")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(req)))
//                .andExpect(status().isOk())
//                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
//                .andExpect(jsonPath("$.success").value(true))
//                .andExpect(jsonPath("$.failedItems").isArray())
//                .andExpect(jsonPath("$.failedItems").isEmpty());
//
//        verify(productService).updateInventory(eq(req.getItem()));
//    }
//
//    @Test
//    @DisplayName("POST /product/updateInventory: returns 200 OK with UpdateInventoryResponse (partial failure)")
//    void updateInventory_partialFailure() throws Exception {
//        UpdateInventoryDto req = updateInventoryDto(List.of(
//                invItem("p-FAIL-1", 5),
//                invItem("p-OK-2", 1)
//        ));
//
//        UpdateInventoryResponse res = partialFailureResponse();
//
//        when(productService.updateInventory(eq(req.getItem()))).thenReturn(res);
//
//        mockMvc.perform(post("/product/updateInventory")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .accept(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(req)))
//                .andExpect(status().isOk())
//                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
//                .andExpect(jsonPath("$.success").value(false))
//                .andExpect(jsonPath("$.failedItems[0].productId").value("p-FAIL-1"))
//                .andExpect(jsonPath("$.failedItems[0].reason").value("Insufficient stock"))
//                .andExpect(jsonPath("$.failedItems[0].availableQuantity").value(1));
//
//        verify(productService).updateInventory(eq(req.getItem()));
//    }

    // ---------- Optional negative cases (enable if you add validation/advice) ----------

     @Test
     @DisplayName("POST /product: missing required fields -> 400 (if you add validation)")
     void create_missingFields() throws Exception {
         mockMvc.perform(post("/product")
                         .contentType(MediaType.APPLICATION_JSON)
                         .content("{}"))
                 .andExpect(status().isBadRequest());
         verify(productService, never()).create(any());
     }

//     @Test
//     @DisplayName("GET /product/search: invalid bounds -> 400 (if you add validation)")
//     void search_invalidBounds() throws Exception {
//         ProductSearchRequest req = searchReq("phone", "Phones",
//                 new BigDecimal("90000.00"), new BigDecimal("50000.00"), 0, 10); // min > max
//         mockMvc.perform(get("/product/search")
//                         .contentType(MediaType.APPLICATION_JSON)
//                         .content(objectMapper.writeValueAsString(req)))
//                 .andExpect(status().isBadRequest());
//         verify(productService, never()).search(any());
//     }
}
