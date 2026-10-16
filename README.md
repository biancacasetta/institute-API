# Institute API

REST API for managing an English institute: students, courses, and payments. Built as a real project for a family business, and as a way to learn Spring Boot.

## Tech stack

- Java 21 and Spring Boot 4
- Spring Data JPA with PostgreSQL
- Spring Security
- Bean Validation and Lombok
- Docker Compose for the database
- Maven

## Features

- Student and course management (full CRUD)
- Enroll students in courses
- Request validation with clear error responses
- DTO-based API: entities are never exposed directly

## Getting started

This project requires JDK 21 and Docker.

```bash
cp .env.example .env
cp src/main/resources/application.example.yaml src/main/resources/application.yaml
# edit both files with your database credentials

make run
```
## Roadmap for v1

- [x] Global error handling
- [x] Basic Student CRUD operations
- [x] Basic Course CRUD operations
- [ ] Basic Payment CRUD operations
- [ ] Admin authentication
- [ ] API docs
- [ ] Database migrations
- [ ] Automated tests fully covering the API
- [ ] CI: GitHub Actions running tests on push/PR
- [ ] Basic React frontend functionality
- [ ] Deployment

## Status

Work in progress.