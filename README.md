# debezium-test

A local playground for the [Debezium tutorial](https://debezium.io/documentation/reference/stable/kc-tutorial.html) (Debezium 3.6, Kafka in KRaft mode, MySQL).

## Quick start

```sh
make up          # start kafka, mysql, connect
make register    # register the MySQL connector
make watch       # stream change events for inventory.customers (Ctrl-C to stop)
```

In another terminal, make some changes:

```sh
make mysql
mysql> UPDATE customers SET first_name='Anne Marie' WHERE id=1004;
mysql> DELETE FROM addresses WHERE customer_id=1004;
mysql> DELETE FROM customers WHERE id=1004;
```

The create, update, and delete events show up in the `make watch` terminal.

## Other commands

| Command | What it does |
|---|---|
| `make status` | Connector status |
| `make topics` | List Kafka topics |
| `make watch TOPIC=dbserver1.inventory.orders` | Watch a different table |
| `make logs` | Follow Kafka Connect logs |
| `make down` | Stop everything and wipe data |

## Endpoints

- Kafka Connect REST API: http://localhost:8083
- MySQL: `localhost:3306` (`mysqluser` / `mysqlpw`, root / `debezium`)

Kafka is reachable only inside the compose network (`kafka:9092`). Use `docker compose exec kafka ...` to work with it.

## Differences from the tutorial

- **Docker Compose instead of `docker run`**: the tutorial starts each container by hand with `--link`. Here one compose file starts them all on a shared network, and Connect finds Kafka through `BOOTSTRAP_SERVERS=kafka:9092`.
- **`make watch` uses Kafka's console consumer**: the tutorial's `watch-topic` helper printed nothing when run inside the Kafka container. The console consumer shows each event with its key and starts from the beginning of the topic.
- **MySQL shell via `docker compose exec`**: no separate `mysql:8.2` client container is needed.
- **Kafka isn't reachable from the host**: it advertises `kafka:9092`, as in the tutorial. The Connect API (`localhost:8083`) and MySQL (`localhost:3306`) are reachable from the host.

To start over with fresh sample data, run `make down && make up && make register`.
