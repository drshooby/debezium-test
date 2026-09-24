TOPIC ?= dbserver1.inventory.customers

.PHONY: up down register status topics watch mysql logs

up:        ## Start Kafka, MySQL and Kafka Connect
	docker compose up -d

down:      ## Stop everything and delete data
	docker compose down -v

register:  ## Register the MySQL connector (waits for Connect to be ready)
	@until curl -sf localhost:8083/ >/dev/null; do echo "waiting for Kafka Connect..."; sleep 3; done
	curl -i -X POST -H "Accept:application/json" -H "Content-Type:application/json" \
		localhost:8083/connectors/ -d @register-mysql.json

status:    ## Show connector status
	curl -s localhost:8083/connectors/inventory-connector/status

topics:    ## List Kafka topics
	docker compose exec kafka /kafka/bin/kafka-topics.sh --bootstrap-server kafka:9092 --list

watch:     ## Watch change events, e.g. make watch TOPIC=dbserver1.inventory.orders
	docker compose exec kafka /kafka/bin/kafka-console-consumer.sh --bootstrap-server kafka:9092 \
		--from-beginning --property print.key=true --topic $(TOPIC)

mysql:     ## Open a MySQL shell on the inventory database
	docker compose exec mysql mysql -umysqluser -pmysqlpw inventory

logs:      ## Follow Kafka Connect logs
	docker compose logs -f connect
