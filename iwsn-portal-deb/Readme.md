
Uses [jDeb](https://github.com/tcurdt/jdeb) to build a Debian package.

Use ``mvn clean install`` to build the package.

Edit ``/etc/tr.iwsn-portal.properties`` before starting the service.

Creates the following directory structure:
(``-|`` marks a file)
```
/
├── etc
│   ├── init.d
│   │   └──| tr.iwsn-portal
│   │
│   ├──| tr.iwsn-portal.log4j.properties
│	├──| tr.iwsn-portal.properties.example
│   └──| tr.iwsn-portal.properties
├── usr
│   ├── bin
│   │   └──| tr.iwsn-portal
│   └── share
│       └── tr.iwsn-portal
│           ├──| tr.iwsn-portal-[version].jar
│           └──| tr.iwsn-portal.jar -> tr.iwsn-portal-[version].jar
└── var
    ├── lib
    │   └── tr.iwsn-portal
    ├── log
    │   └── tr.iwsn-portal
    └── run
        └── tr.iwsn-portal

```