
Uses [jDeb](https://github.com/tcurdt/jdeb) to build a Debian package.

Use ``mvn clean install`` to build the package.

Creates the following directory structure:
(``-|`` marks a file)
```
/
├── etc
│   ├── init.d
│   │   └──| tr.iwsn-portal
│   └── tr.iwsn-portal
│       └──| log4j.properties
├── usr
│   ├── bin
│   │   └──| tr.iwsn-portal
│   └── share
│       └── testbed-runtime
│           └── tr.iwsn-portal
│               ├──| tr.iwsn-portal-[version].jar
│               └──| tr.iwsn-portal.jar -> tr.iwsn-portal-[version].jar
└── var
    ├── lib
    │   └── tr.iwsn-portal
    ├── log
    │   └── tr.iwsn-portal
    └── run
        └── tr.iwsn-portal

```