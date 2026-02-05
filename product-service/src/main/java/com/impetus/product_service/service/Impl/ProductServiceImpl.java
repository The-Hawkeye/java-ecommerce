package com.impetus.product_service.service.Impl;

import com.impetus.product_service.dto.*;
import com.impetus.product_service.entity.Product;
import com.impetus.product_service.repository.ProductRepository;
import com.impetus.product_service.service.ProductService;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final MongoTemplate mongoTemplate;

    private ProductResponseDto toResponse(Product p){
        ProductResponseDto r = new ProductResponseDto();
        r.setId(p.getId());
        r.setName(p.getName());
        r.setDescription(p.getDescription());
        r.setSku(p.getSku());
        r.setPrice(p.getPrice());
        r.setInventoryQuantity(p.getInventoryQuantity());
        r.setAttributes(p.getAttributes());
        return r;
    }

    @Override
    public ProductResponseDto create(CreateProductRequest req) {
        Product p = new Product();
        p.setName(req.getName());
        p.setDescription(req.getDescription());
        p.setSku(req.getSku());
        p.setPrice(req.getPrice());
        p.setInventoryQuantity(req.getInventoryQuantity() != null ? req.getInventoryQuantity() : 0);
        if(req.getAttributes() != null){
            p.setAttributes(req.getAttributes());
        }
        p.setCreatedAt(Instant.now());

        Product saved = productRepository.save(p);
        return toResponse(saved);
    }

    @Override
    public ProductResponseDto getById(String id) {
        Product p = productRepository.findById(id).orElseThrow(()-> new NoSuchElementException("Product not found"));
        return toResponse(p);
    }

    @Override
    public ProductResponseDto update(String id, UpdateProductRequest req) {
        Product p = productRepository.findById(id).orElseThrow(()-> new NoSuchElementException("Product not found"));
        if(req.name != null) p.setName(req.name);
        if(req.description != null) p.setDescription(req.description);
        if(req.price != null) p.setPrice(req.price);
        if(req.inventoryQuantity != null) p.setInventoryQuantity(req.inventoryQuantity);
        if(req.attributes != null) p.setAttributes(req.attributes);

        Product saved = productRepository.save(p);
        return toResponse(saved);
    }

    @Override
    public void delete(String id) {
        if(!productRepository.existsById(id)){
            throw new NotFoundException("Product not found");
        }
        productRepository.deleteById(id);
    }

    @Override
    public Page<ProductResponseDto> search(ProductSearchRequest req) {
        Pageable pageable = PageRequest.of(req.page != null ? req.page : 0, req.size != null ? req.size : 20);

        Page<Product> page;
        if(req.query != null && !req.query.isBlank()){
            page = productRepository.textSearch(req.query, pageable);
        } else if (req.name != null) {
            page = productRepository.findByNameContainingIgnoreCase(req.name, pageable);
        } else if (req.minPrice != null || req.maxPrice != null) {
            BigDecimal min = req.minPrice != null? req.minPrice:BigDecimal.ZERO;
            BigDecimal max = req.maxPrice != null ? req.maxPrice : BigDecimal.valueOf(Long.MAX_VALUE);
            page = productRepository.findByPriceBetween(min, max, pageable);
        }else {
            page = productRepository.findAll(pageable);
        }
        return page.map(this::toResponse);
    }

    @Override
    public Page<ProductResponseDto> list(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return productRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    public ProductResponseDto updateInventory(String id, int delta) {
        Product p = productRepository.findById(id).orElseThrow(()-> new NoSuchElementException("Product not found"));
        int newQty = Math.max(0, p.getInventoryQuantity()+delta);
        p.setInventoryQuantity(newQty);
        Product saved = productRepository.save(p);
        return toResponse(saved);
    }

    @Override
    public ProductResponseDto setPrice(String id, BigDecimal price) {
        Product p = productRepository.findById(id).orElseThrow(()-> new NotFoundException("Product not found"));
        p.setPrice(price);
        Product saved = productRepository.save(p);
        return toResponse(saved);
    }

    @Override
    public List<ProductResponseDto> getDetailsOfIds(List<String> productIds) {
        List<Product> products = productRepository.findAllById(productIds);
        log.info(products.stream().findFirst().get().getName() + " products fetched ");
        return products.stream().map(this::toResponse).toList();
    }

    @Override
    public UpdateInventoryResponse updateInventory(List<UpdateInventoryRequest> items){
        List<UpdateInventoryRequest> succeeded = new ArrayList<>();
        List<UpdateInventoryResponse.FailedItem> failed = new ArrayList<>();

        for(UpdateInventoryRequest it : items){
            String productId = it.getProductId();
            int qty = it.getQuantity();

            Query query = new Query(Criteria.where("_id").is(productId).andOperator(Criteria.where("inventoryQuantity").gte(qty)));
            Update update = new Update().inc("inventoryQuantity", -qty);

            try{
                Product before = mongoTemplate.findAndModify(query, update, FindAndModifyOptions.options().returnNew(false), Product.class, "product");
                if(before == null){
                    Optional<Product> mayBe = productRepository.findById(productId);
                    if(mayBe.isEmpty()){
                        failed.add(new UpdateInventoryResponse.FailedItem(productId, "NOT_FOUND", 0));
                    }else {
                        Product p = mayBe.get();
                        int availableQty = p.getInventoryQuantity() == null ? 0 : p.getInventoryQuantity();
                        failed.add(new UpdateInventoryResponse.FailedItem(productId, "INSUFFICIENT_STOCK", availableQty));

                    }
                    break;
                }else {
                    succeeded.add(it);
                }
            } catch (Exception e) {
                log.error("Error updating inventory for {} {}", productId, e.getMessage());
                int availability = productRepository.findById(productId).map(Product::getInventoryQuantity).orElse(0);
                failed.add(new UpdateInventoryResponse.FailedItem(productId, "ERROR", availability));
                break;
            }

        }
        if(!failed.isEmpty()){
            for (UpdateInventoryRequest it: succeeded){
                try{
                    mongoTemplate.findAndModify(Query.query(Criteria.where("_id").is(it.getProductId())),new Update().inc("inventoryQuantity", it.getQuantity()), Product.class, "product");
                }catch (Exception e){
                    log.error("Rollback failed for {} {}", it.getProductId(), e.getMessage());
                    //Add this rollback in queue so it can rollback later
                }
            }
            return new UpdateInventoryResponse(false, failed);
        }
        return new UpdateInventoryResponse(true, List.of());
    }
}
