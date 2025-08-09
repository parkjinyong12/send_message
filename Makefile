PYTHON=python3
VENV=.venv
PIP=$(VENV)/bin/pip
UVICORN=$(VENV)/bin/uvicorn

APP_NAME=send-message
IMAGE_NAME=send-message:latest

.PHONY: build test run docker-build docker-run compose-up compose-down compose-logs deploy publish

build:
	mvn -q -DskipTests package

test:
	mvn -q test

run:
	mvn -q spring-boot:run

docker-build:
	docker build -t $(IMAGE_NAME) .

docker-run:
	docker run --rm -p 8080:8080 --env-file .env $(IMAGE_NAME)

compose-up:
	docker compose up -d --build

compose-down:
	docker compose down

compose-logs:
	docker compose logs -f

publish:
	bash scripts/publish.sh 