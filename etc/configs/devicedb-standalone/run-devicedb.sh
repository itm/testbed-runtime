#!/bin/bash

if [ "$#" -lt 1 ]
then
    echo "Usage: $0 PROPERTIES_FILE"
    exit 1
fi

TR_DIR=`dirname ../../../../`

java -jar $TR_DIR/device-db/target/tr.device-db-0.9-SNAPSHOT.jar --logLevel TRACE --config $1