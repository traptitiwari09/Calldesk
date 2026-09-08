package com.example.App.auth;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String email;

	/** BCrypt hash. The plaintext password is never stored or logged. */
	@Column(nullable = false)
	private String passwordHash;

	private String name;

	/** The user's own phone in E.164 - this is the phone that rings first when they place a call. */
	@Column(nullable = false)
	private String phoneNumber;

	@Column(nullable = false)
	private Instant createdAt = Instant.now();

	protected User() {
	}

	public User(String email, String passwordHash, String name, String phoneNumber) {
		this.email = email;
		this.passwordHash = passwordHash;
		this.name = name;
		this.phoneNumber = phoneNumber;
	}

	public Long getId() { return id; }

	public String getEmail() { return email; }
	public void setEmail(String email) { this.email = email; }

	public String getPasswordHash() { return passwordHash; }
	public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

	public String getName() { return name; }
	public void setName(String name) { this.name = name; }

	public String getPhoneNumber() { return phoneNumber; }
	public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

	public Instant getCreatedAt() { return createdAt; }
}
