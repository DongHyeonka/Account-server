package com.synapse.account_service.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.synapse.account_service.domain.entity.Member;

public interface MemberRepository extends JpaRepository<Member, UUID> {
    Optional<Member> findByEmail(String email);
    Optional<Member> findByUsername(String username);
    Optional<Member> findByProviderAndRegistrationId(String provider, String registrationId);

    @Query("SELECT m FROM Member m LEFT JOIN FETCH m.subscription WHERE m.id = :memberId")
    Optional<Member> findByIdWithSubscription(@Param("memberId") UUID memberId);

    @Query("SELECT m FROM Member m WHERE m.username = :username AND m.email = :email")
    Optional<Member> findByUsernameAndEmail(@Param("username") String username, @Param("email") String email);

    @Query("SELECT m FROM Member m WHERE (m.provider = :provider AND m.registrationId = :registrationId) OR m.email = :email OR m.username = :username")
    Optional<Member> findBySocialIdOrEmailOrUsername(@Param("provider") String provider, @Param("registrationId") String registrationId, @Param("email") String email, @Param("username") String username);
}
