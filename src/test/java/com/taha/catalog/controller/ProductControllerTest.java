package com.taha.catalog.controller;

import com.taha.catalog.dto.ProductRequest;
import com.taha.catalog.dto.ProductResponse;
import com.taha.catalog.exception.ProductNotFoundException;
import com.taha.catalog.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.util.List;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
class ProductControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean ProductService service;
    private static final String VALID = "{\"name\":\"Keyboard\",\"price\":49.95,\"stockQuantity\":4}";

    @Test void createReturns201AndLocation() throws Exception {
        when(service.create(any(ProductRequest.class))).thenReturn(response());
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isCreated()).andExpect(header().string("Location", "http://localhost/api/products/1"))
                .andExpect(jsonPath("$.id").value(1)).andExpect(jsonPath("$.price").value(49.95));
    }

    @Test void listReturns200() throws Exception {
        when(service.findAll()).thenReturn(List.of(response()));
        mvc.perform(get("/api/products")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Keyboard"));
    }

    @Test void getReturns200() throws Exception {
        when(service.findById(1L)).thenReturn(response());
        mvc.perform(get("/api/products/1")).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1));
    }

    @Test void missingProductReturns404() throws Exception {
        when(service.findById(99L)).thenThrow(new ProductNotFoundException(99L));
        mvc.perform(get("/api/products/99")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Product 99 was not found"));
    }

    @Test void updateReturns200() throws Exception {
        when(service.update(eq(1L), any())).thenReturn(response());
        mvc.perform(put("/api/products/1").contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1));
    }

    @Test void deleteReturns204() throws Exception {
        mvc.perform(delete("/api/products/1")).andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(service).delete(1L);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"name\":\" \",\"price\":1,\"stockQuantity\":1}",
            "{\"name\":\"X\",\"price\":-1,\"stockQuantity\":1}",
            "{\"name\":\"X\",\"price\":1.001,\"stockQuantity\":1}",
            "{\"name\":\"X\",\"price\":10000000000,\"stockQuantity\":1}",
            "{\"name\":\"X\",\"price\":1,\"stockQuantity\":-1}",
            "{}"
    })
    void invalidInputReturns400WithoutCallingService(String body) throws Exception {
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors").isMap());
        verifyNoInteractions(service);
    }

    @Test void tooLongNameReturns400() throws Exception {
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                .content(VALID.replace("Keyboard", "x".repeat(121))))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test void invalidUpdateReturns400() throws Exception {
        mvc.perform(put("/api/products/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test void malformedJsonAndInvalidIdReturn400() throws Exception {
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/products/abc")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test void zeroPriceAndStockAreAllowed() throws Exception {
        when(service.create(any())).thenReturn(response());
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Sample\",\"price\":0,\"stockQuantity\":0}"))
                .andExpect(status().isCreated());
    }

    @Test void fractionalStockIsRejectedInsteadOfSilentlyTruncated() throws Exception {
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Sample\",\"price\":1,\"stockQuantity\":1.5}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    private ProductResponse response() {
        return new ProductResponse(1L, "Keyboard", new BigDecimal("49.95"), 4);
    }
}
