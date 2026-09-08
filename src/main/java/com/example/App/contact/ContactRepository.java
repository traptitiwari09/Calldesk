package com.example.App.contact;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactRepository extends JpaRepository<Contact, Long> {

	List<Contact> findByUserIdOrderByNameAsc(Long userId);

	Optional<Contact> findByIdAndUserId(Long id, Long userId);

	boolean existsByUserIdAndPhoneNumber(Long userId, String phoneNumber);

	Optional<Contact> findFirstByPhoneNumber(String phoneNumber);
}
