#!/bin/bash
SCRIPT_DIR=`dirname $0`
TR_DIR=$SCRIPT_DIR/..
TR_VERSION=0.5.4-SNAPSHOT

if [ ! -d $SCRIPT_DIR/logs ]; then
	mkdir -p $SCRIPT_DIR/logs
fi

java -jar $TR_DIR/iwsn/wsn-api-client/target/tr.wsn-api-client-$TR_VERSION.one-jar.jar -f $TR_DIR/iwsn/wsn-api-client/configs/local-logcontroller.properties | tee $SCRIPT_DIR/logs/local-logcontroller-wsn-api-client.log
