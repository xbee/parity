#!/bin/sh

mkdir -p /opt/parity

cat > /opt/parity/parity-system.conf <<-EOF
market-data {
  session             = parity
  multicast-interface = eth0
  multicast-group     = 224.0.0.1
  multicast-port      = 5000
  request-port        = 5001
}

market-report {
  session             = parity
  multicast-interface = eth0
  multicast-group     = 224.0.0.1
  multicast-port      = 6000
  request-port        = 6001
}

order-entry {
  address = 0.0.0.0
  port    = 4000
}

instruments = [ ${PARITY_SYSTEM_INSTRUMENTS:-"FOO,BAR,BAZ"} ]
EOF

exec cp /parity/parity-system.jar /opt/parity/parity-system.jar
#exec cp /parity/parity-system.conf /opt/parity/parity-system.conf
exec chmod 644 /opt/parity/parity-system.jar
exec /usr/bin/java -jar /opt/parity/parity-system.jar /opt/parity/parity-system.conf
