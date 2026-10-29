package com.ecommerce.demo.dto;


public class CartItemResponseDTO {

    // FRONTEND: cart item id + product id so the UI can remove / update / check out items.
    private Long id;
    private Long productId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    private ProductDTO product;

	private int quantity;
    
    
	public ProductDTO getProduct() {
		return product;
	}
	public void setProduct(ProductDTO product) {
		this.product = product;
	}
	public int getQuantity() {
		return quantity;
	}
	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public CartItemResponseDTO(ProductDTO product, int quantity) {
		super();
		this.product = product;
		this.quantity = quantity;
	}

	public CartItemResponseDTO() {
		super();
	}

    
    
}
