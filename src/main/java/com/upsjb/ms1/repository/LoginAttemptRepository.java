package com.upsjb.ms1.repository;

import com.upsjb.ms1.domain.entity.LoginAttempt;
import com.upsjb.ms1.domain.enums.TipoLogin;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long>, JpaSpecificationExecutor<LoginAttempt> {

    Page<LoginAttempt> findByUsuario_Id(Long idUsuario, Pageable pageable);

    @Query(
            value = """
                    select loginAttempt
                    from LoginAttempt loginAttempt
                    where lower(loginAttempt.usernameOrEmail) = lower(:usernameOrEmail)
                    """,
            countQuery = """
                    select count(loginAttempt)
                    from LoginAttempt loginAttempt
                    where lower(loginAttempt.usernameOrEmail) = lower(:usernameOrEmail)
                    """
    )
    Page<LoginAttempt> findPageByUsernameOrEmailIgnoreCase(
            @Param("usernameOrEmail") String usernameOrEmail,
            Pageable pageable
    );

    @Query("""
            select loginAttempt
            from LoginAttempt loginAttempt
            where lower(loginAttempt.usernameOrEmail) = lower(:usernameOrEmail)
            order by loginAttempt.attemptedAt desc, loginAttempt.id desc
            """)
    List<LoginAttempt> findRecentByUsernameOrEmailIgnoreCase(
            @Param("usernameOrEmail") String usernameOrEmail,
            Pageable pageable
    );

    @Query("""
            select count(loginAttempt)
            from LoginAttempt loginAttempt
            where lower(loginAttempt.usernameOrEmail) = lower(:usernameOrEmail)
              and loginAttempt.exitoso = false
              and loginAttempt.attemptedAt > :attemptedAt
            """)
    long countFailedByUsernameOrEmailIgnoreCaseAfter(
            @Param("usernameOrEmail") String usernameOrEmail,
            @Param("attemptedAt") Instant attemptedAt
    );

    long countByIpAddress_ValueAndExitosoFalseAndAttemptedAtAfter(
            String ipAddress,
            Instant attemptedAt
    );

    long countByTipoLoginAndExitosoAndAttemptedAtBetween(
            TipoLogin tipoLogin,
            boolean exitoso,
            Instant fechaDesde,
            Instant fechaHasta
    );
}