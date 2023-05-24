openssl ca -config /etc/pki/tls/openssl.cnf -revoke $PWD/client.crt -keyfile $PWD/ca.key -cert $PWD/ca.crt

openssl ca -gencrl -config /etc/pki/tls/openssl.cnf -keyfile $PWD/ca.key -cert $PWD/ca.crt -out $PWD/crl.pem

