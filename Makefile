rebuild:
	docker-compose build
	docker-compose restart

status:
	docker-compose ps

crossbar:
	docker-compose run crossbar

up:
	docker-compose up

restart:
	docker-compose down
	docker-compose up
