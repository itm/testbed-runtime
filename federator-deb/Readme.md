
Uses [jDeb](https://github.com/tcurdt/jdeb) to build a Debian package.

Use ``mvn clean install`` to build the package.

Edit ``/etc/tr.federator/tr.federator.properties`` before starting the service.

Creates the following directory structure:
(``-|`` marks a file)
```
/
├── etc
│   ├── init.d
│   │   └──| tr.federator
│   └── tr.federator
│       ├──| tr.federator.log4j.properties
│       └──| tr.federator.properties
├── usr
│   ├── bin
│   │   └──| tr.federator
│   └── share
│       └── testbed-runtime
│           └── tr.federator
│               ├──| tr.federator-[version].jar
│               └──| tr.federator.jar -> tr.federator-[version].jar
└── var
    ├── lib
    │   └── tr.federator
    ├── log
    │   └── tr.federator
    └── run
        └── tr.federator

```