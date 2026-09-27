#!/usr/bin/env bash
# Floods the inventory database with inserts, updates and deletes so you can watch
# Debezium pick them up. Usage: ./generate-changes.sh [rounds]   (default 50)
set -euo pipefail
cd "$(dirname "$0")"

ROUNDS=${1:-50}
RUN=$(date +%s) # keeps emails unique across runs (customers.email is UNIQUE)

echo "Generating $ROUNDS rounds of changes in 2 seconds... switch terminals!"
sleep 2

for i in $(seq 1 "$ROUNDS"); do
  email="user-$RUN-$i@example.com"
  echo "INSERT INTO customers (first_name, last_name, email) VALUES ('User$i', 'Test', '$email');"
  echo "UPDATE customers SET last_name = 'Updated' WHERE email = '$email';"
  echo "UPDATE products_on_hand SET quantity = quantity + 1 WHERE product_id = $((101 + RANDOM % 9));"
  if (( i % 3 == 0 )); then
    echo "DELETE FROM customers WHERE email = '$email';"
  fi
done | docker compose exec -T -e MYSQL_PWD=mysqlpw mysql mysql -umysqluser inventory

echo "Done."
