package com.ecommerce.demo.dto;

// FRONTEND: returned by GET /api/users/me so the UI knows who is logged in.
// The JWT only carries the email, but cart / order / review requests need the
// numeric user id, and the UI needs the role to show the admin or shop screens.
public record CurrentUserDTO(Long id, String username, String email, String roles) {
}
