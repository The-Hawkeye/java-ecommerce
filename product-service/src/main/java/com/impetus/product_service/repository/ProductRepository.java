package com.impetus.product_service.repository;

import com.impetus.product_service.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {
    Optional<Product> findBySku(String sku);
    Page<Product> findByNameContainingIgnoreCase(String name , Pageable pageable);
    Page<Product> findByPriceBetween(BigDecimal min, BigDecimal max, Pageable pageable);

    @Query("{$text : { $search : ?0 } }")
    Page<Product> textSearch(String text, Pageable pageable);

    @Query("{ 'attribute.?0' : ?1")
    List<Product> findByAttributes(String key, Object value);
}
