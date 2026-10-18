package com.taha.catalog.dto;

import com.taha.catalog.entity.Product;
import java.math.BigDecimal;

public record ProductResponse(Long id, String name, BigDecimal price, Integer stockQuantity) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(), product.getName(),
                product.getPrice(), product.getStockQuantity());
    }
}
