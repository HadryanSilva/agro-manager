<div align="center">

# 🌱 Agro Manager — Backend

**API REST para gestão agrícola de produtores de melancia**

[![Java](https://img.shields.io/badge/Java-25-007396?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=flat-square&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=flat-square&logo=docker&logoColor=white)](https://www.docker.com/)

</div>

---

## Sobre o projeto

O **Agro Manager** é uma plataforma de gestão agrícola desenvolvida para produtores de melancia. Este repositório contém a API REST que alimenta toda a aplicação — desde a autenticação até os relatórios financeiros por lavoura.

## Funcionalidades da API

| Módulo | Endpoints |
|---|---|
| **Autenticação** | Registro, login, refresh token, OAuth2 Google |
| **Usuários** | Perfil, alteração de senha |
| **Contas** | Multi-tenant, criação, listagem |
| **Membros** | Roles (OWNER/ADMIN/MEMBER), convites por link |
| **Lavouras** | CRUD completo, status dinâmico, filtros |
| **Despesas** | INSUMO/SERVIÇO, controle de pagamento |
| **Transações** | Listagem paginada com 5 filtros |
| **Dashboard** | Métricas agregadas, totais financeiros |
| **Relatório** | Financeiro por lavoura, evolução mensal |
| **Histórico** | Atividades automáticas + anotações manuais |
| **Cotações** | Comparação de preços de insumos, cálculo de economia |

## Stack

- **Java 25** + **Spring Boot 4**
- **Spring Security** — JWT stateless + OAuth2 Google
- **Spring Data JPA** + **Hibernate**
- **PostgreSQL 16** + **Flyway** (migrations V1–V8)
- **Lombok** + **MapStruct**
- **Docker** multi-stage (eclipse-temurin:25)

## Pré-requisitos

- Java 25+
- Gradle 9.4+
- Docker e Docker Compose
- PostgreSQL 16 (ou via Docker)

## Configuração e execução

### 1. Clonar o repositório

```bash
git clone https://github.com/HadryanSilva/agro-manager.git
cd agro-manager
```

### 2. Configurar variáveis de ambiente

| Variável | Descrição |
|---|---|
| `DB_HOST` | Host do PostgreSQL (padrão: `postgres`) |
| `DB_NAME` | Nome do banco (padrão: `agro_manager`) |
| `DB_USERNAME` | Usuário do banco |
| `DB_PASSWORD` | Senha do banco |
| `JWT_SECRET` | Chave secreta JWT (mín. 64 chars) |
| `GOOGLE_CLIENT_ID` | Client ID do Google OAuth2 |
| `GOOGLE_CLIENT_SECRET` | Client Secret do Google OAuth2 |
| `FRONTEND_URL` | URL do frontend (ex: `http://localhost:5173`) |

### 3. Subir com Docker Compose

```bash
docker compose up -d
```

A API estará disponível em `http://localhost:8080`.

### 4. Executar localmente (sem Docker)

```bash
# Subir apenas o banco
docker compose up postgres -d

# Executar a aplicação
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## Estrutura do projeto

```
src/main/java/br/com/hadryan/agro/manager/
├── domain/
│   ├── auth/          # Autenticação e tokens
│   ├── user/          # Usuários e perfil
│   ├── account/       # Contas, membros e convites
│   ├── farm/          # Lavouras, relatório e histórico
│   ├── expense/       # Despesas e transações
│   ├── quotation/     # Cotações de insumos
│   └── dashboard/     # Métricas agregadas
├── infra/
│   └── security/      # JWT, OAuth2, filtros
└── shared/
    ├── dto/           # Wrappers de resposta
    └── exception/     # Tratamento global de erros
```

## Repositório relacionado

Frontend: [agro-manager-frontend](https://github.com/HadryanSilva/agro-manager-web)