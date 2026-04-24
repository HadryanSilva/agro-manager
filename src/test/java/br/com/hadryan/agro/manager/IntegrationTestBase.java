package br.com.hadryan.agro.manager;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Classe base para todos os testes de integração.
 *
 * Utiliza Testcontainers para subir um container PostgreSQL real,
 * garantindo que o ambiente de testes seja idêntico ao de produção.
 *
 * O container é compartilhado entre todos os testes que herdam esta classe
 * (campo static), evitando o custo de subir um novo container por classe.
 *
 * @ServiceConnection configura automaticamente o DataSource a partir
 * das propriedades do container, sem necessidade de application-test.yaml.
 */
@SpringBootTest
@ImportTestcontainers
public abstract class IntegrationTestBase {

    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");
}