package com.example.App.call;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CallRepository extends JpaRepository<CallRecord, Long> {

	/** History for one agent: what they placed, plus inbound calls they handled. */
	List<CallRecord> findTop100ByUserIdOrDirectionOrderByCreatedAtDesc(Long userId, String direction);

	/** One agent's calls in a single direction. */
	List<CallRecord> findTop100ByUserIdAndDirectionOrderByCreatedAtDesc(Long userId, String direction);

	Optional<CallRecord> findByIdAndUserId(Long id, Long userId);

	/** Every call in a direction, regardless of agent (the shared inbound queue). */
	List<CallRecord> findTop100ByDirectionOrderByCreatedAtDesc(String direction);

	/** Used to mark earlier calls from the same number as handled. */
	List<CallRecord> findByDirectionAndToNumberAndStatusNot(String direction, String toNumber, String status);
}
