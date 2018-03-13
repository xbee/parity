rebuild:
	docker-compose build
	docker-compose restart

status:
	docker-compose ps

crossbar:
    docker-compose run crossbar