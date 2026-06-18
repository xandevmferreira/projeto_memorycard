package com.memorycard.repository;

import com.memorycard.entity.Friendship;
import com.memorycard.entity.FriendshipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    @Query("""
            SELECT f FROM Friendship f
            WHERE f.status = :status
              AND (f.requesterId = :userId OR f.addresseeId = :userId)
            """)
    List<Friendship> findByUserAndStatus(@Param("userId") Long userId,
                                         @Param("status") FriendshipStatus status);

    List<Friendship> findByAddresseeIdAndStatus(Long addresseeId, FriendshipStatus status);

    List<Friendship> findByRequesterIdAndStatus(Long requesterId, FriendshipStatus status);

    @Query("""
            SELECT f FROM Friendship f
            WHERE (f.requesterId = :a AND f.addresseeId = :b)
               OR (f.requesterId = :b AND f.addresseeId = :a)
            """)
    Optional<Friendship> findBetweenUsers(@Param("a") Long userA, @Param("b") Long userB);

    @Query("""
            SELECT COUNT(f) FROM Friendship f
            WHERE f.status = 'ACCEPTED'
              AND (f.requesterId = :userId OR f.addresseeId = :userId)
            """)
    long countAcceptedFriends(@Param("userId") Long userId);
}
