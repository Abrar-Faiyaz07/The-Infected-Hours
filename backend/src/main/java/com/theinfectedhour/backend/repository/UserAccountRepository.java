package com.theinfectedhour.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.theinfectedhour.backend.entity.UserAccount;

/** Spring Data access for users rows. */
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
}
