package com.taha.catalog;

import com.taha.catalog.dto.ProductRequest;
import com.taha.catalog.dto.ProductResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductApiIT {
    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");
    @Autowired TestRestTemplate http;

    @Test void completeCrudLifecycleAgainstPostgres() {
        String base = "/api/products";
        var created = http.postForEntity(base,
                new ProductRequest("Keyboard", new BigDecimal("49.95"), 4), ProductResponse.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getHeaders().getLocation()).isNotNull();
        assertThat(created.getBody()).isNotNull();
        String path = base + "/" + created.getBody().id();

        var fetched = http.getForEntity(path, ProductResponse.class);
        assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fetched.getBody().price()).isEqualByComparingTo("49.95");
        assertThat(http.getForObject(base, ProductResponse[].class)).hasSize(1);

        var updated = http.exchange(path, HttpMethod.PUT,
                new HttpEntity<>(new ProductRequest("Mouse", new BigDecimal("12.50"), 0)), ProductResponse.class);
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        // A separate HTTP request proves JPA committed the update (dirty checking).
        var persisted = http.getForObject(path, ProductResponse.class);
        assertThat(persisted.name()).isEqualTo("Mouse");
        assertThat(persisted.price()).isEqualByComparingTo("12.50");
        assertThat(persisted.stockQuantity()).isZero();

        assertThat(http.postForEntity(base, new ProductRequest("", BigDecimal.ONE, -1), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(http.exchange(path, HttpMethod.DELETE, HttpEntity.EMPTY, Void.class)
                .getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(http.getForEntity(path, String.class).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(http.exchange(path, HttpMethod.PUT,
                new HttpEntity<>(new ProductRequest("Mouse", BigDecimal.ONE, 1)), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(http.exchange(path, HttpMethod.DELETE, HttpEntity.EMPTY, String.class)
                .getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(http.getForObject(base, ProductResponse[].class)).isEmpty();
    }
}
