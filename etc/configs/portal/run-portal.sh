#!/bin/bash

if [ "$#" -lt 1 ]
then
    echo "Usage: $0 PROPERTIES_FILE"
    exit 1
fi

CONF_DIR=`dirname $0`
CONF_FILE=$CONF_DIR/`basename $1`
TR_DIR=`dirname $CONF_DIR/../../../../`

java -jar $TR_DIR/iwsn-portal/target/tr.iwsn-portal-0.9-SNAPSHOT.jar --logLevel TRACE --config $CONF_FILE
