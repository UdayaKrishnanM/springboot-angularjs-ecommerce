package com.ecommerce.demo.dto;

import com.ecommerce.demo.model.SizeType;

public class ProductSizeDTO {
    private String size;
    private int stockQuantity;

    public ProductSizeDTO(String size, int stockQuantity) {
        this.size = size;
        this.stockQuantity = stockQuantity;
    }

    public ProductSizeDTO() {
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
    }
}
