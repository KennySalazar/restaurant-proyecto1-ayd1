package com.restaurante.domain.repository;

import com.restaurante.domain.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

  List<Notification> findByRestaurantIdOrderByCreatedAtDesc(Long restaurantId);

  List<Notification> findByRestaurantIdAndReadFalseOrderByCreatedAtDesc(Long restaurantId);

  boolean existsByRestaurantIdAndTypeAndEntityAndEntityIdAndReadFalse(
      Long restaurantId, String type, String entity, String entityId);

  @Modifying
  @Query("""
      UPDATE Notification n
      SET n.read = true, n.readAt = :readAt
      WHERE n.restaurantId = :restaurantId
        AND n.type = :type
        AND n.entity = :entity
        AND n.entityId = :entityId
        AND n.read = false
      """)
  int markAsReadByEntity(Long restaurantId, String type, String entity, String entityId, Instant readAt);

  @Modifying
  @Query("""
      UPDATE Notification n
      SET n.read = true, n.readAt = :readAt
      WHERE n.restaurantId = :restaurantId
        AND n.read = false
      """)
  int markAllAsRead(Long restaurantId, Instant readAt);

  long countByRestaurantIdAndReadFalse(Long restaurantId);
}
