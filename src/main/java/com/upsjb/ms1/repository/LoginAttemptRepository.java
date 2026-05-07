package com.upsjb.ms1.repository;

import com.upsjb.ms1.domain.entity.LoginAttempt;
import com.upsjb.ms1.domain.enums.TipoLogin;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long>, JpaSpecificationExecutor<LoginAttempt> {

    Page<LoginAttempt> findByUsuario_Id(Long idUsuario, Pageable pageable);

    Page<LoginAttempt> findByUsernameOrEmailIgnoreCase(String usernameOrEmail, Pageable pageable);

    List<LoginAttempt> findTop10ByUsernameOrEmailIgnoreCaseOrderByAttemptedAtDesc(String usernameOrEmail);

    long countByUsernameOrEmailIgnoreCaseAndExitosoFalseAndAttemptedAtAfter(
            String usernameOrEmail,
            Instant attemptedAt
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