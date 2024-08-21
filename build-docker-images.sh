#!/bin/bash

services=(
  ts-assurance-service
  ts-auth-service
  ts-basic-service
  ts-contacts-service
  ts-config-service
  ts-consign-service
  ts-consign-price-service
  ts-inside-payment-service
  ts-food-service
  ts-food-map-service
  ts-order-service
  ts-order-other-service
  ts-payment-service
  ts-price-service
  ts-route-plan-service
  ts-route-service
  ts-seat-service
  ts-security-service
  ts-station-service
  ts-ticketinfo-service
  ts-train-service
  ts-travel-service
  ts-travel2-service
  ts-user-service
)

for service in "${services[@]}"; do
  (
    cd "$service" || exit
    docker build -t "local/$service:0.1" .
    echo "Finished building Docker image for $service."
  )
done