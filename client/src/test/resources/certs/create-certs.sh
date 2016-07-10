#!/bin/bash

# see https://docs.docker.com/articles/https/

echo 01 > ca.srl
openssl genrsa -des3 -out ca-key.pem 2048
openssl req -new -x509 -days 365 -key ca-key.pem -out ca.pem

openssl genrsa -des3 -out server-key.pem 2048
openssl req -subj '/CN=localhost' -new -key server-key.pem -out server.csr

openssl x509 -req -days 365 -in server.csr -CA ca.pem -CAkey ca-key.pem -out server-cert.pem

openssl genrsa -des3 -out key.pem 2048
openssl req -subj '/CN=client' -new -key key.pem -out client.csr

echo extendedKeyUsage = clientAuth > extfile.cnf
openssl x509 -req -days 365 -in client.csr -CA ca.pem -CAkey ca-key.pem -out cert.pem -extfile extfile.cnf

openssl rsa -in server-key.pem -out server-key.pem
openssl rsa -in key.pem -out key.pem
