package com.hero.bikestore.repository;

import com.hero.bikestore.model.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {

    // All addresses for a user ordered oldest first
    List<UserAddress> findByUserIdOrderByCreatedAtAsc(Long userId);

    // Find the current default address for a user
    Optional<UserAddress> findByUserIdAndIsDefaultTrue(Long userId);

    // How many addresses does this user have
    int countByUserId(Long userId);

    // Clear all defaults for a user before setting a new one
    @Modifying
    @Query("UPDATE UserAddress a SET a.isDefault = false WHERE a.user.id = :userId")
    void clearDefaultForUser(@Param("userId") Long userId);
}
