package com.ecommerce.demo.dto;

import com.ecommerce.demo.model.Product;
import com.ecommerce.demo.model.SizeType;

public class UpdateProductDTO {
    private Product productDetails;
    private SizeType requestedSize;
    private int newStockQuantity;

    // Getters and Setters


    public UpdateProductDTO() {
    }

    public UpdateProductDTO(Product productDetails, SizeType requestedSize, int newStockQuantity) {
        this.productDetails = productDetails;
        this.requestedSize = requestedSize;
        this.newStockQuantity = newStockQuantity;
    }

    public Product getProductDetails() {
        return productDetails;
    }

    public void setProductDetails(Product productDetails) {
        this.productDetails = productDetails;
    }

    public SizeType getRequestedSize() {
        return requestedSize;
    }

    public void setRequestedSize(SizeType requestedSize) {
        this.requestedSize = requestedSize;
    }

    public int getNewStockQuantity() {
        return newStockQuantity;
    }

    public void setNewStockQuantity(int newStockQuantity) {
        this.newStockQuantity = newStockQuantity;
    }
}
