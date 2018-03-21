rebuild:
	docker-compose down
	docker-compose build
	docker-compose up

status:
	docker-compose ps

crossbar:
	docker-compose run crossbar

up:
	docker-compose up

down:
	docker-compose down

buildall:
	docker-compose down
	mvn package
	docker-compose build
	docker-compose up

build:
	docker-compose build

restart:
	docker-compose down
	docker-compose up
