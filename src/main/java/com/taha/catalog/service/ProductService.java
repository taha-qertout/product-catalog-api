package com.taha.catalog.service;

import com.taha.catalog.dto.ProductRequest;
import com.taha.catalog.dto.ProductResponse;
import com.taha.catalog.entity.Product;
import com.taha.catalog.exception.ProductNotFoundException;
import com.taha.catalog.repository.ProductRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductService {
    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product = new Product(request.name().strip(), request.price(), request.stockQuantity());
        return ProductResponse.from(repository.save(product));
    }

    public List<ProductResponse> findAll() {
        return repository.findAll(Sort.by("id")).stream().map(ProductResponse::from).toList();
    }

    public ProductResponse findById(Long id) {
        return ProductResponse.from(requireProduct(id));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = requireProduct(id);
        product.update(request.name().strip(), request.price(), request.stockQuantity());
        return ProductResponse.from(product);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(requireProduct(id));
    }

    private Product requireProduct(Long id) {
        return repository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }
}
