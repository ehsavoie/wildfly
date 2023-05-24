#!/bin/bash



rm ca-key.pem  ca.pem  client-cert.pem  client-key.pem  server-cert.pem  server-key.pem client-expired-cert.pem truststore.p12
 
# Generate CA private key and certificate
openssl genpkey -algorithm RSA -out ca-key.pem
openssl req -new -x509 -key ca-key.pem -out ca.pem -subj "/C=CZ/ST=Brno/L=Brno/O=Certificate Authority/OU=IT Department/CN=127.0.0.1"

# Generate server private key and CSR
openssl genpkey -algorithm RSA -out server-key.pem
openssl req -new -key server-key.pem -out server.csr -subj "/C=CZ/ST=Brno/L=Brno/O=Server/OU=IT Department/CN=127.0.0.1"
echo subjectAltName = DNS:localhost,IP:127.0.0.1 > extfile.cnf

# Sign server CSR with CA and generate server certificate
openssl x509 -req -in server.csr -CA ca.pem -CAkey ca-key.pem -CAcreateserial -out server-cert.pem -days 3650 -extfile extfile.cnf

# Generate client private key and CSR
openssl genpkey -algorithm RSA -out client-key.pem
openssl req -new -key client-key.pem -out client.csr -subj "/C=CZ/ST=Brno/L=Brno/O=Client/OU=IT Department/CN=127.0.0.1"

# Sign client CSR with CA and generate client certificate
openssl x509 -req -in client.csr -CA ca.pem -CAkey ca-key.pem -CAcreateserial -out client-cert.pem -days 3650
openssl x509 -req -in client.csr -CA ca.pem -CAkey ca-key.pem -CAcreateserial -out client-expired-cert.pem -days -1

# Convert private keys to unencrypted PKCS#8 format in PEM
openssl pkcs8 -in server-key.pem -topk8 -out server-key.pem.pkcs8 -nocrypt
openssl pkcs8 -in client-key.pem -topk8 -out client-key.pem.pkcs8 -nocrypt

# Clean up intermediate files
rm ca.srl server.csr client.csr client-key.pem.pkcs8 server-key.pem.pkcs8

# Create the truststore
keytool -genkeypair -alias myalias -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore truststore.p12 -storepass changeit -validity 365 -dname "CN=mydomain.com, OU=My Org Unit, O=My Organization, L=My City, S=My State, C=My Country"  && keytool -delete -alias myalias -keystore truststore.p12 -storepass changeit

keytool -importcert -file ca.pem -alias customca -keystore truststore.p12 -storepass changeit -noprompt

echo "Certificates and private keys generated successfully!"
