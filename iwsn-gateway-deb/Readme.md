
Uses [jDeb](https://github.com/tcurdt/jdeb) to build a Debian package.

Use ``mvn clean install`` to build the package.

Edit ``/etc/tr.iwsn-gateway/tr.iwsn-gateway.properties`` before starting the service.

Creates the following directory structure:
(``-|`` marks a file)
```
/
├── etc
│   ├── init.d
│   │   └──| tr.iwsn-gateway
│   └── tr.iwsn-gateway
│       ├──| log4j.properties
│       └──| tr.iwsn-gateway.properties
├── usr
│   ├── bin
│   │   └──| tr.iwsn-gateway
│   └── share
│       └── testbed-runtime
│           └── tr.iwsn-gateway
│               ├──| tr.iwsn-gateway-[version].jar
│               └──| tr.iwsn-gateway.jar -> tr.iwsn-gateway-[version].jar
└── var
    ├── lib
    │   └── tr.iwsn-gateway
    ├── log
    │   └── tr.iwsn-gateway
    └── run
        └── tr.iwsn-gateway

```