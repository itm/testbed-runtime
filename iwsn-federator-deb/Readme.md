
Uses [jDeb](https://github.com/tcurdt/jdeb) to build a Debian package.

Use ``mvn clean install`` to build the package.

Edit ``/etc/tr.iwsn-federator/tr.iwsn-federator.properties`` before starting the service.

Creates the following directory structure:
(``-|`` marks a file)
```
/
├── etc
│   ├── init.d
│   │   └──| tr.iwsn-federator
│   └── tr.iwsn-federator
│       ├──| log4j.properties
│       └──| tr.iwsn-federator.properties
├── usr
│   ├── bin
│   │   └──| tr.iwsn-federator
│   └── share
│       └── testbed-runtime
│           └── tr.iwsn-federator
│               ├──| tr.iwsn-federator-[version].jar
│               └──| tr.iwsn-federator.jar -> tr.iwsn-federator-[version].jar
└── var
    ├── lib
    │   └── tr.iwsn-federator
    ├── log
    │   └── tr.iwsn-federator
    └── run
        └── tr.iwsn-federator

```