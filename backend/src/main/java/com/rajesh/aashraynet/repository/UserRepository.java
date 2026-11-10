package com.rajesh.aashraynet.repository;

import com.rajesh.aashraynet.entity.Role;
import com.rajesh.aashraynet.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Query("""
           select u from User u left join u.district d
           where (:districtId is null or d.id = :districtId)
             and (:role is null or u.role = :role)
           """)
    Page<User> search(@Param("districtId") Long districtId, @Param("role") Role role, Pageable pageable);
}
