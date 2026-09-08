package com.example.App.contact;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "contacts")
public class Contact {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userId;

	@Column(nullable = false)
	private String name;

	
	@Column(nullable = false)
	private String phoneNumber;

	@Column(nullable = false)
	private Instant createdAt = Instant.now();

	protected Contact() {
	}

	public Contact(Long userId, String name, String phoneNumber) {
		this.userId = userId;
		this.name = name;
		this.phoneNumber = phoneNumber;
	}

	public Long getId() { return id; }

	public Long getUserId() { return userId; }

	public String getName() { return name; }
	public void setName(String name) { this.name = name; }

	public String getPhoneNumber() { return phoneNumber; }
	public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

	public Instant getCreatedAt() { return createdAt; }
}
