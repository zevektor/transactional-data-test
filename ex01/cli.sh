#!/usr/bin/env bash

set -e

COMMAND=${1}

SBT_IMAGE_NAME="hseeberger/scala-sbt"
SBT_IMAGE_TAG="8u302_1.5.5_2.12.15"

check_input_files() {
  if [[ -e "./input/input_dataset.json" ]]; then
    echo "Checking input file... OK"
  else
    echo "Checking input file... Input file does not exist. Downloading it"
    wget https://data.cdc.gov/api/views/cjae-szjv/rows.json\?accessType\=DOWNLOAD -O ./input/input_dataset.json
  fi
}

docker_run_sbt_build() {
  echo "Building Data Ingestion Jar Inside SBT Container..."
  docker run -v $(pwd):/app -w /app "${SBT_IMAGE_NAME}:${SBT_IMAGE_TAG}" bash -c "cd /app && sbt clean test assembly"
  echo "Building Data Ingestion Jar Inside SBT Container... OK"
}

docker_compose_up() {
  echo "Starting Docker Stack if not already running..."
  docker-compose up -d
  echo -e "Starting Docker Stack if not already running... OK"
}

docker_compose_down() {
  echo "Stopping Docker Stack if running..."
  docker-compose down
  echo -e "Stopping Docker Stack if running... OK"
}

docker_compose_logs() {
  echo "Showing Docker Stack logs when running:"
  docker-compose logs -f -t
}

process_data() {
  docker exec -it -w /app spark-master bash -c '/spark/bin/spark-submit --master local[*] de-test.jar --class it.zevektor.datatest.MainApp'
}

test_code() {
  echo "Running Unit Tests..."
  docker run -v $(pwd):/app -w /app "${SBT_IMAGE_NAME}:${SBT_IMAGE_TAG}" bash -c "sbt test"
  echo "Running Unit Tests... OK"
}

case "${COMMAND}" in
build)
  docker_run_sbt_build
  ;;
start)
  docker_compose_up
  ;;
stop)
  docker_compose_down
  ;;
logs)
  docker_compose_logs
  ;;
process_data)
  check_input_files
  process_data
  ;;
test)
  test_code
  ;;
*)
  echo "Usage: cli.sh (build|start|stop|logs|process_data|test)"
  exit 1
  ;;
esac