package br.com.hadryan.agro.manager.domain.farm;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FarmActivityRepository extends JpaRepository<FarmActivity, UUID> {

    // Lista todas as atividades de uma lavoura com dados do usuário, mais recentes primeiro
    @Query("""
            SELECT a FROM FarmActivity a
            JOIN FETCH a.user
            WHERE a.farm.id = :farmId
            ORDER BY a.createdAt DESC
            """)
    List<FarmActivity> findByFarmIdOrderByCreatedAtDesc(UUID farmId);
}