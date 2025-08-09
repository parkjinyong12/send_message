PYTHON=python3
VENV=.venv
PIP=$(VENV)/bin/pip
UVICORN=$(VENV)/bin/uvicorn

APP_NAME=send-message
IMAGE_NAME=send-message:latest

.PHONY: venv install run docker-build docker-run deploy compose-up compose-down compose-logs

# 로컬 venv는 유지하지만, 기본 워크플로우는 docker compose를 권장
venv:
	$(PYTHON) -m venv $(VENV)
	@echo "Run 'source $(VENV)/bin/activate' to activate the venv"

install: venv
	$(PIP) install --upgrade pip
	$(PIP) install -r requirements.txt

run:
	$(UVICORN) app.main:app --host 0.0.0.0 --port 8000 --reload

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

deploy:
	bash scripts/ship.sh 