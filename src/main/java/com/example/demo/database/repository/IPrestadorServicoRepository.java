package com.example.demo.database.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.database.model.PrestadorServicoEntity;

@Repository
public interface IPrestadorServicoRepository extends JpaRepository<PrestadorServicoEntity, Integer> {

    /** Usado no login: o e-mail do prestador identifica o usuário. */
    Optional<PrestadorServicoEntity> findFirstByEmailIgnoreCase(String email);

}
