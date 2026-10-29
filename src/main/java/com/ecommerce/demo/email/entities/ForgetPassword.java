package com.ecommerce.demo.email.entities;

import java.util.Date;

import com.ecommerce.demo.email.dto.MailBody.MailBodyBuilder;
import com.ecommerce.demo.model.User;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

//import lombok.Builder;
//
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ForgetPassword {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int forgetID;

	@Column(nullable = false)
	private int otp;

	@Column(nullable = false)
	private Date expirationTime;


	public int getForgetID() {
		return forgetID;
	}

	public void setForgetID(int forgetID) {
		this.forgetID = forgetID;
	}


	public int getOtp() {
		return otp;
	}


	public void setOtp(int otp) {
		this.otp = otp;
	}


	public Date getExpirationTime() {
		return expirationTime;
	}


	public void setExpirationTime(Date expirationTime) {
		this.expirationTime = expirationTime;
	}


	public User getUser() {
		return user;
	}


	public void setUser(User user) {
		this.user = user;
	}


	@OneToOne
	private User user;

}


// above is with @Builder. before is @Builder not working code

//@Entity
//public class ForgetPassword {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private int forgetID;
//
//    @Column(nullable = false)
//    private int otp;
//
//    @Column(nullable = false)
//    private Date expirationTime;
//
//    @OneToOne
//    private User user;
//
//    public static ForgetPasswordBuilder builder() {
//        return new ForgetPasswordBuilder();
//    }
//
//    // Private constructor to enforce the use of the builder
//    private ForgetPassword(ForgetPasswordBuilder builder) {
//        this.forgetID = builder.forgetID;
//        this.otp = builder.otp;
//        this.expirationTime = builder.expirationTime;
//        this.user = builder.user;
//    }
//
//    // Static inner class for the builder
//    public static class ForgetPasswordBuilder {
//        private int forgetID;
//        private int otp;
//        private Date expirationTime;
//        private User user;
//
//        public ForgetPasswordBuilder forgetID(int forgetID) {
//            this.forgetID = forgetID;
//            return this;
//        }
//
//        public ForgetPasswordBuilder otp(int otp) {
//            this.otp = otp;
//            return this;
//        }
//
//        public ForgetPasswordBuilder expirationTime(Date expirationTime) {
//            this.expirationTime = expirationTime;
//            return this;
//        }
//
//        public ForgetPasswordBuilder user(User user) {
//            this.user = user;
//            return this;
//        }
//
//        public ForgetPassword build() {
//            return new ForgetPassword(this);
//        }
//    }
//
//    // Getters and Setters
//    public int getForgetID() {
//        return forgetID;
//    }
//
//    public void setForgetID(int forgetID) {
//        this.forgetID = forgetID;
//    }
//
//    public int getOtp() {
//        return otp;
//    }
//
//    public void setOtp(int otp) {
//        this.otp = otp;
//    }
//
//    public Date getExpirationTime() {
//        return expirationTime;
//    }
//
//    public void setExpirationTime(Date expirationTime) {
//        this.expirationTime = expirationTime;
//    }
//
//    public User getUser() {
//        return user;
//    }
//
//    public void setUser(User user) {
//        this.user = user;
//    }
//}
