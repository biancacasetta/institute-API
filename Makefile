include .env
export

.PHONY: db-up db-down db-logs db-psql db-reset run build test

db-up:
	docker compose up -d --wait

db-down:
	docker compose down

db-logs:
	docker compose logs -f db

db-psql:
	docker compose exec db psql -U $(POSTGRES_USER) -d $(POSTGRES_DB)

db-reset:
	docker compose down -v
	db-up

run: db-up
	./mvnw spring-boot:run

build:
	./mvnw clean package

test:
	./mvnw test