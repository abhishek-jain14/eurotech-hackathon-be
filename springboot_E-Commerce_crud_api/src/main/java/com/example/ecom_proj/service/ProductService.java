package com.example.ecom_proj.service;
import com.example.ecom_proj.model.Product;
import com.example.ecom_proj.repository.ProductRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    @Autowired
    private ProductRepo repo;

    public List<Product> getAllProducts(){
            return repo.findAll();
    }

    public Product getProductById(int id){
        return repo.findById(id).orElse(null);
    }

    public Product addProduct(Product prod){
        return repo.save(prod);
    }

    public Product updateProduct(Product prod){
         // Fetch the existing product and update its fields
         Product existingProduct = repo.findById(prod.getId()).orElse(null);
         if(existingProduct == null) {
             throw new RuntimeException("Product not found with id: " + prod.getId());
         }

         // Update the existing product with new values
         existingProduct.setProductName(prod.getProductName());
         existingProduct.setDesc(prod.getDesc());
         existingProduct.setBrand(prod.getBrand());
         existingProduct.setPrice(prod.getPrice());
         existingProduct.setCategory(prod.getCategory());
         existingProduct.setReleaseDate(prod.getReleaseDate());
         existingProduct.setAvailable(prod.isAvailable());
         existingProduct.setQuantity(prod.getQuantity());

         return repo.save(existingProduct);
     }

    public void deleteProduct(int id)   {
            repo.deleteById(id);
    }

}
