package com.taha.catalog.service;

import com.taha.catalog.dto.ProductRequest;
import com.taha.catalog.entity.Product;
import com.taha.catalog.exception.ProductNotFoundException;
import com.taha.catalog.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    @Mock ProductRepository repository;
    @InjectMocks ProductService service;

    @Test void createTrimsNameAndPreservesDecimalPrice() {
        when(repository.save(any(Product.class))).thenAnswer(call -> call.getArgument(0));
        var result = service.create(new ProductRequest("  Keyboard  ", new BigDecimal("49.95"), 4));
        assertThat(result.name()).isEqualTo("Keyboard");
        assertThat(result.price()).isEqualByComparingTo("49.95");
        assertThat(result.stockQuantity()).isEqualTo(4);
    }

    @Test void findAllReturnsProductsInRepositoryOrder() {
        when(repository.findAll(Sort.by("id"))).thenReturn(List.of(product()));
        assertThat(service.findAll()).extracting("name").containsExactly("Keyboard");
    }

    @Test void findByIdReturnsExistingProduct() {
        when(repository.findById(1L)).thenReturn(Optional.of(product()));
        assertThat(service.findById(1L).name()).isEqualTo("Keyboard");
    }

    @Test void findByIdRejectsMissingProduct() {
        assertThatThrownBy(() -> service.findById(99L)).isInstanceOf(ProductNotFoundException.class);
    }

    @Test void updateChangesTheManagedEntity() {
        var product = product();
        when(repository.findById(1L)).thenReturn(Optional.of(product));
        var result = service.update(1L, new ProductRequest(" Mouse ", new BigDecimal("12.50"), 0));
        assertThat(product.getName()).isEqualTo("Mouse");
        assertThat(result.price()).isEqualByComparingTo("12.50");
        assertThat(product.getStockQuantity()).isZero();
    }

    @Test void updateRejectsMissingProduct() {
        assertThatThrownBy(() -> service.update(99L,
                new ProductRequest("Mouse", BigDecimal.ONE, 1)))
                .isInstanceOf(ProductNotFoundException.class);
        verify(repository, never()).save(any());
    }

    @Test void deleteRemovesExistingProduct() {
        var product = product();
        when(repository.findById(1L)).thenReturn(Optional.of(product));
        service.delete(1L);
        verify(repository).delete(product);
    }

    @Test void deleteRejectsMissingProduct() {
        assertThatThrownBy(() -> service.delete(99L)).isInstanceOf(ProductNotFoundException.class);
        verify(repository, never()).delete(any());
    }

    private Product product() { return new Product("Keyboard", new BigDecimal("49.95"), 4); }
}
