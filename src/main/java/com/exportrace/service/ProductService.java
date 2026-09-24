package com.exportrace.service;

import com.exportrace.entity.Product;
import com.exportrace.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product getProductByCode(String codigo) {
        return productRepository.findByCodigo(codigo).orElse(null);
    }

    public Product saveProduct(Product product) {
        return productRepository.save(product);
    }
}
