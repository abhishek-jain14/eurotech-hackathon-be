package com.example.ecom_proj.controller;

import com.example.ecom_proj.model.Product;
import com.example.ecom_proj.service.ProductService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ProductController {

        private final ProductService service;

        // Constructor injection (Best practice over @Autowired field injection)
        public ProductController(ProductService service) {
                this.service = service;
        }

        // Helper method to build standard response headers
        private HttpHeaders createResponseHeaders(String correlationId, String channelId) {
                HttpHeaders headers = new HttpHeaders();
                headers.set("X-Correlation-ID", correlationId);
                headers.set("X-Channel-ID", channelId);
                return headers;
        }

        // Helper method to ensure Correlation ID is never null
        private String resolveCorrelationId(String correlationId) {
                return (correlationId != null && !correlationId.isBlank())
                        ? correlationId
                        : UUID.randomUUID().toString();
        }

        @GetMapping("/products")
        public ResponseEntity<List<Product>> getAllProducts(
                @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
                @RequestHeader(value = "X-User-ID", required = true) String userId,
                @RequestHeader(value = "X-Channel-ID", defaultValue = "WEB") String channelId) {

                String activeCorrelationId = resolveCorrelationId(correlationId);
                HttpHeaders responseHeaders = createResponseHeaders(activeCorrelationId, channelId);

                List<Product> products = service.getAllProducts(); //[cite: 4]
                return new ResponseEntity<>(products, responseHeaders, HttpStatus.OK);
        }

        @GetMapping("/product/{id}")
        public ResponseEntity<Product> getProductById(
                @PathVariable int id, //[cite: 4]
                @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
                @RequestHeader(value = "X-Channel-ID", defaultValue = "WEB") String channelId) {

                String activeCorrelationId = resolveCorrelationId(correlationId);
                HttpHeaders responseHeaders = createResponseHeaders(activeCorrelationId, channelId);

                Product prod = service.getProductById(id); //[cite: 4]

                if (prod != null) { //[cite: 4]
                        return new ResponseEntity<>(prod, responseHeaders, HttpStatus.OK); //[cite: 4]
                } else {
                        return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND); //[cite: 4]
                }
        }

        @PostMapping("/products")
        public ResponseEntity<?> addProduct(
                @RequestBody Product prod, //[cite: 4]
                @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
                @RequestHeader(value = "X-Channel-ID", defaultValue = "WEB") String channelId,
                @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey) {

                String activeCorrelationId = resolveCorrelationId(correlationId);
                HttpHeaders responseHeaders = createResponseHeaders(activeCorrelationId, channelId);
                if (idempotencyKey != null) {
                        responseHeaders.set("X-Idempotency-Key", idempotencyKey);
                }

                try {
                        Product product1 = service.addProduct(prod); //[cite: 4]
                        return new ResponseEntity<>(product1, responseHeaders, HttpStatus.CREATED); //[cite: 4]
                } catch (Exception e) { //[cite: 4]
                        return new ResponseEntity<>(e.getMessage(), responseHeaders, HttpStatus.INTERNAL_SERVER_ERROR); //[cite: 4]
                }
        }

        @PutMapping("/product")
        public ResponseEntity<?> updateProduct(
                @RequestBody Product prod, //[cite: 4]
                @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
                @RequestHeader(value = "X-Channel-ID", defaultValue = "WEB") String channelId,
                @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey) {

                String activeCorrelationId = resolveCorrelationId(correlationId);
                HttpHeaders responseHeaders = createResponseHeaders(activeCorrelationId, channelId);
                if (idempotencyKey != null) {
                        responseHeaders.set("X-Idempotency-Key", idempotencyKey);
                }

                try {
                        Product prod1 = service.updateProduct(prod); //[cite: 4]
                        return new ResponseEntity<>(prod1, responseHeaders, HttpStatus.OK); //[cite: 4]
                } catch (Exception e) { //[cite: 4]
                        return new ResponseEntity<>(e.getMessage(), responseHeaders, HttpStatus.INTERNAL_SERVER_ERROR); //[cite: 4]
                }
        }

        @DeleteMapping("/product/{id}")
        public ResponseEntity<?> deleteProduct(
                @PathVariable int id, //[cite: 4]
                @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
                @RequestHeader(value = "X-Channel-ID", defaultValue = "WEB") String channelId) {

                String activeCorrelationId = resolveCorrelationId(correlationId);
                HttpHeaders responseHeaders = createResponseHeaders(activeCorrelationId, channelId);

                Product prod1 = service.getProductById(id); //[cite: 4]
                if (prod1 != null) { //[cite: 4]
                        service.deleteProduct(id); //[cite: 4]
                        return new ResponseEntity<>("Deleted", responseHeaders, HttpStatus.OK); //[cite: 4]
                } else {
                        return new ResponseEntity<>("Product Not Found", responseHeaders, HttpStatus.NOT_FOUND); //[cite: 4]
                }
        }
}