package br.com.hadryan.agro.manager;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.thymeleaf.spring6.SpringTemplateEngine;

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
 *
 * JavaMailSender e SpringTemplateEngine são mockados para evitar dependência
 * de servidor SMTP real — o EmailService é instanciado normalmente, mas
 * nenhum e-mail é enviado durante os testes.
 */
@SpringBootTest
@ImportTestcontainers
public abstract class IntegrationTestBase {

    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    // Substitui o JavaMailSender real por um mock — sem este bean o contexto
    // falha ao tentar auto-configurar o EmailService sem spring.mail.host definido
    @MockitoBean
    JavaMailSender mailSender;

    // Substitui o SpringTemplateEngine para evitar processamento real de templates
    // durante os testes, onde os arquivos HTML podem não estar no classpath de teste
    @MockitoBean
    SpringTemplateEngine templateEngine;
}