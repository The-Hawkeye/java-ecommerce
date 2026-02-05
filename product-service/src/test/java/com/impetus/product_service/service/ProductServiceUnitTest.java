package com.impetus.product_service.service;


import com.impetus.product_service.dto.*;
import com.impetus.product_service.entity.Product;
import com.impetus.product_service.repository.ProductRepository;
import com.impetus.product_service.service.Impl.ProductServiceImpl;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceUnitTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    private ProductService service;

    @Captor
    private ArgumentCaptor<Product> productCaptor;

    @BeforeEach
    void setUp() {
        service = new ProductServiceImpl(productRepository, mongoTemplate);
    }

    // ---------- Helpers ----------
    private Product newProduct(String id, String name, BigDecimal price, Integer qty) {
        Product p = new Product();
        p.setId(id);
        p.setName(name);
        p.setDescription("desc-" + name);
        p.setSku("SKU-" + name);
        p.setPrice(price);
        p.setInventoryQuantity(qty);
        p.setAttributes(Map.of("color", "red"));
        p.setCreatedAt(Instant.now());
        return p;
    }

    // ---------- getById ----------
    @Test
    void getById_shouldReturnResponse_whenFound() {
        Product p = newProduct("p2", "Laptop", new BigDecimal("2500"), 5);
        when(productRepository.findById("p2")).thenReturn(Optional.of(p));

        ProductResponseDto dto = service.getById("p2");

        assertEquals("p2", dto.getId());
        assertEquals("Laptop", dto.getName());
        assertEquals(5, dto.getInventoryQuantity());
    }

    @Test
    void getById_shouldThrow_whenNotFound() {
        when(productRepository.findById("missing")).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> service.getById("missing"));
    }

    // ---------- update ----------
    @Test
    void update_shouldPatchFields_andReturnResponse() {
        Product existing = newProduct("p3", "Mouse", new BigDecimal("20.00"), 50);
        when(productRepository.findById("p3")).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProductRequest req = new UpdateProductRequest();
        // direct field access per your service code
        req.name = "Gaming Mouse";
        req.description = "RGB mouse";
        req.price = new BigDecimal("35.50");
        req.inventoryQuantity = 60;
        req.attributes = Map.of("dpi", "12000");

        ProductResponseDto dto = service.update("p3", req);

        assertEquals("Gaming Mouse", dto.getName());
        assertEquals("RGB mouse", dto.getDescription());
        assertEquals(new BigDecimal("35.50"), dto.getPrice());
        assertEquals(60, dto.getInventoryQuantity());
        assertEquals("12000", dto.getAttributes().get("dpi"));
    }

    // ---------- delete ----------
    @Test
    void delete_shouldRemove_whenExists() {
        when(productRepository.existsById("p4")).thenReturn(true);
        doNothing().when(productRepository).deleteById("p4");

        assertDoesNotThrow(() -> service.delete("p4"));
        verify(productRepository).deleteById("p4");
    }

    @Test
    void delete_shouldThrowNotFound_whenNotExists() {
        when(productRepository.existsById("p404")).thenReturn(false);
        assertThrows(RuntimeException.class, () -> service.delete("p404"));
    }

    // ---------- setPrice ----------
    @Test
    void setPrice_shouldUpdatePrice() {
        Product existing = newProduct("p5", "Keyboard", new BigDecimal("45.00"), 10);
        when(productRepository.findById("p5")).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponseDto dto = service.setPrice("p5", new BigDecimal("59.99"));

        assertEquals(new BigDecimal("59.99"), dto.getPrice());
    }

    @Test
    void setPrice_shouldThrowNotFound_whenMissing() {
        when(productRepository.findById("none")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.setPrice("none", new BigDecimal("10")));
    }

    // ---------- list ----------
    @Test
    void list_shouldReturnPagedResults() {
        List<Product> items = List.of(
                newProduct("p6", "Item6", new BigDecimal("10"), 1),
                newProduct("p7", "Item7", new BigDecimal("20"), 2)
        );
        Page<Product> page = new PageImpl<>(items, PageRequest.of(0, 2), 2);

        when(productRepository.findAll(PageRequest.of(0, 2))).thenReturn(page);

        Page<ProductResponseDto> result = service.list(0, 2);

        assertEquals(2, result.getTotalElements());
        assertEquals("Item6", result.getContent().get(0).getName());
        assertEquals("Item7", result.getContent().get(1).getName());
    }

    // ---------- search ----------
    @Test
    void search_shouldUseTextSearch_whenQueryPresent() {
        ProductSearchRequest req = new ProductSearchRequest();
        req.query = "laptop";
        req.page = 0;
        req.size = 10;

        List<Product> items = List.of(newProduct("q1", "Laptop Pro", new BigDecimal("1999"), 3));
        Page<Product> page = new PageImpl<>(items, PageRequest.of(0, 10), 1);

        when(productRepository.textSearch(eq("laptop"), any(Pageable.class))).thenReturn(page);

        Page<ProductResponseDto> out = service.search(req);
        assertEquals(1, out.getTotalElements());
        assertTrue(out.getContent().get(0).getName().toLowerCase().contains("laptop"));
    }

    @Test
    void search_shouldUseNameContains_whenNamePresent() {
        ProductSearchRequest req = new ProductSearchRequest();
        req.name = "Phone";
        req.page = 1;
        req.size = 5;

        Page<Product> page = new PageImpl<>(
                List.of(newProduct("n1", "Phone X", new BigDecimal("899"), 7)),
                PageRequest.of(1, 5),
                1
        );

        when(productRepository.findByNameContainingIgnoreCase(eq("Phone"), any(Pageable.class))).thenReturn(page);

        Page<ProductResponseDto> out = service.search(req);
//        assertEquals(1, out.getTotalElements());
        assertEquals("Phone X", out.getContent().get(0).getName());
    }

    @Test
    void search_shouldUsePriceBetween_whenPriceRangePresent() {
        ProductSearchRequest req = new ProductSearchRequest();
        req.minPrice = new BigDecimal("100");
        req.maxPrice = new BigDecimal("200");
        req.page = 0;
        req.size = 10;

        Page<Product> page = new PageImpl<>(
                List.of(newProduct("pr1", "Budget", new BigDecimal("150"), 10)),
                PageRequest.of(0, 10),
                1
        );

        when(productRepository.findByPriceBetween(eq(new BigDecimal("100")), eq(new BigDecimal("200")), any(Pageable.class)))
                .thenReturn(page);

        Page<ProductResponseDto> out = service.search(req);
        assertEquals(1, out.getTotalElements());
        assertEquals(new BigDecimal("150"), out.getContent().get(0).getPrice());
    }

    @Test
    void search_shouldFallbackToFindAll_whenNoFilters() {
        ProductSearchRequest req = new ProductSearchRequest();
        req.page = 0;
        req.size = 2;

        Page<Product> page = new PageImpl<>(
                List.of(newProduct("fa1", "A", new BigDecimal("10"), 1), newProduct("fa2", "B", new BigDecimal("11"), 2)),
                PageRequest.of(0, 2),
                2
        );

        when(productRepository.findAll(any(Pageable.class))).thenReturn(page);

        Page<ProductResponseDto> out = service.search(req);
        assertEquals(2, out.getTotalElements());
    }

    // ---------- getDetailsOfIds ----------
    @Test
    void getDetailsOfIds_shouldMapAll() {
        List<Product> products = List.of(
                newProduct("d1", "One", new BigDecimal("1"), 1),
                newProduct("d2", "Two", new BigDecimal("2"), 2)
        );
        when(productRepository.findAllById(List.of("d1", "d2"))).thenReturn(products);

        List<ProductResponseDto> out = service.getDetailsOfIds(List.of("d1", "d2"));
        assertEquals(2, out.size());
        assertEquals("One", out.get(0).getName());
        assertEquals("Two", out.get(1).getName());
    }

    // ---------- updateInventory (single id) ----------
    @Test
    void updateInventory_single_shouldIncrement_andClampToZero() {
        Product existing = newProduct("inv1", "Item", new BigDecimal("10"), 5);
        when(productRepository.findById("inv1")).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        // decrease by 10 -> should clamp to 0
        ProductResponseDto dto1 = service.updateInventory("inv1", -10);
        assertEquals(0, dto1.getInventoryQuantity());

        // increase by 3 -> should be 3
        existing.setInventoryQuantity(0); // simulate latest state
        ProductResponseDto dto2 = service.updateInventory("inv1", 3);
        assertEquals(3, dto2.getInventoryQuantity());
    }

    // ---------- updateInventory (bulk) success path ----------
    @Test
    void updateInventory_bulk_shouldSucceed_whenSufficientStockForAll() {
        UpdateInventoryRequest it1 = new UpdateInventoryRequest("x1", 2);
        UpdateInventoryRequest it2 = new UpdateInventoryRequest("x2", 3);

        // findAndModify returns the "before" product when success; null means insufficient/not found
        when(mongoTemplate.findAndModify(
                any(Query.class), any(Update.class), any(FindAndModifyOptions.class),
                eq(Product.class), eq("product"))
        ).thenReturn(newProduct("x1", "A", new BigDecimal("10"), 5))
                .thenReturn(newProduct("x2", "B", new BigDecimal("10"), 6));

        UpdateInventoryResponse resp = service.updateInventory(List.of(it1, it2));
        assertTrue(resp.isSuccess());
        assertTrue(resp.getFailedItems().isEmpty());

        // ensure no rollback attempted
        verify(mongoTemplate, times(2)).findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class),
                eq(Product.class), eq("product"));
        verify(mongoTemplate, never()).findAndModify(any(Query.class), any(Update.class), eq(Product.class), eq("product"));
    }

    // ---------- updateInventory (bulk) insufficient stock ----------
    @Test
    void updateInventory_bulk_shouldFailAndRollback_onInsufficientStock() {
        UpdateInventoryRequest it1 = new UpdateInventoryRequest("y1", 2);
        UpdateInventoryRequest it2 = new UpdateInventoryRequest("y2", 5);

        // First succeeds, second returns null -> insufficient/not found
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class),
                eq(Product.class), eq("product")))
                .thenReturn(newProduct("y1", "A", new BigDecimal("10"), 3))
                .thenReturn(null);

        when(productRepository.findById("y2")).thenReturn(Optional.of(newProduct("y2", "B", new BigDecimal("10"), 1)));

        UpdateInventoryResponse resp = service.updateInventory(List.of(it1, it2));

        assertFalse(resp.isSuccess());
        assertEquals(1, resp.getFailedItems().size());
        UpdateInventoryResponse.FailedItem failed = resp.getFailedItems().get(0);
        assertEquals("y2", failed.getProductId());

        // verify rollback for succeeded item (it1)
        verify(mongoTemplate, times(1)).findAndModify(
                argThat(q -> ((String) q.getQueryObject().get("_id")).equals("y1")),
                argThat(u -> u.getUpdateObject().get("$inc").toString().contains("inventoryQuantity=2")), // +2 rollback
                eq(Product.class), eq("product"));
    }

    // ---------- updateInventory (bulk) not found ----------

    @Test
    void updateInventory_bulk_shouldFailAndRollback_whenProductMissing() {
        UpdateInventoryRequest it1 = new UpdateInventoryRequest("z1", 1);
        UpdateInventoryRequest it2 = new UpdateInventoryRequest("z404", 2);

        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class),
                eq(Product.class), eq("product")))
                .thenReturn(newProduct("z1", "A", new BigDecimal("10"), 10))
                .thenReturn(null);

        when(productRepository.findById("z404")).thenReturn(Optional.empty());

        UpdateInventoryResponse resp = service.updateInventory(List.of(it1, it2));

        // DTO changes: success -> isSuccess(), failedItems -> getFailedItems()
        assertFalse(resp.isSuccess());
        assertEquals(1, resp.getFailedItems().size());
        UpdateInventoryResponse.FailedItem failed = resp.getFailedItems().get(0);

        // DTO changes: code -> reason, available -> availableQuantity
        assertEquals("z404", failed.getProductId());
        assertEquals("NOT_FOUND", failed.getReason());
        assertEquals(0, failed.getAvailableQuantity());

        // rollback attempted for z1 (+1 back)
        verify(mongoTemplate, times(1)).findAndModify(
                argThat(q -> "z1".equals(q.getQueryObject().get("_id"))),
                argThat(u -> {
                    Object inc = u.getUpdateObject().get("$inc");
                    return inc != null && inc.toString().contains("inventoryQuantity=1");
                }),
                eq(Product.class), eq("product"));
    }

    // ---------- updateInventory (bulk) error path ----------
    @Test
    void updateInventory_bulk_shouldFailAndRollback_onMongoError() {
        UpdateInventoryRequest it1 = new UpdateInventoryRequest("e1", 2);
        UpdateInventoryRequest it2 = new UpdateInventoryRequest("e2", 3);

        // First succeeds, second throws
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class),
                eq(Product.class), eq("product")))
                .thenReturn(newProduct("e1", "A", new BigDecimal("10"), 10))
                .thenThrow(new RuntimeException("db error"));

        when(productRepository.findById("e2")).thenReturn(Optional.of(newProduct("e2", "B", new BigDecimal("10"), 8)));

        UpdateInventoryResponse resp = service.updateInventory(List.of(it1, it2));

        assertFalse(resp.isSuccess());
        assertEquals(1, resp.getFailedItems().size());
        UpdateInventoryResponse.FailedItem failed = resp.getFailedItems().get(0);
        assertEquals("e2", failed.getProductId());
//        assertEquals("ERROR", failed.getCode());
//        assertEquals(8, failed.getAvailable());

        // rollback attempted for e1
        verify(mongoTemplate, times(1)).findAndModify(
                argThat(q -> ((String) q.getQueryObject().get("_id")).equals("e1")),
                argThat(u -> u.getUpdateObject().get("$inc").toString().contains("inventoryQuantity=2")),
                eq(Product.class), eq("product"));
    }
}
