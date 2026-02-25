.PHONY: run build docker-up docker-down fmt tidy

swag:
	swag init -g cmd/server/main.go

run:
	go mod tidy && \
	swag init -g ./cmd/server/main.go \
          -d ./ \
          --parseDependency \
          --parseInternal \
          --parseDepth 1 && \
	go run ./cmd/server/main.go

test:
	go test -v ./...

build:
	go build -o go-app ./cmd/server/main.go

docker-up:
	docker compose up -d --build

docker-down:
	docker compose down

fmt:
	go fmt ./...

tidy:
	go mod tidy
