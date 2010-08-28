#!/bin/bash
SCRIPT_DIR=`dirname $0`
TR_DIR=$SCRIPT_DIR/..
TR_VERSION=0.5.4-SNAPSHOT

if [ ! -d $SCRIPT_DIR/logs ]; then
	mkdir -p $SCRIPT_DIR/logs
fi

java -jar $TR_DIR/iwsn/logcontroller/target/tr.logcontroller-$TR_VERSION.one-jar.jar -f $TR_DIR/iwsn/logcontroller/configs/local-logcontroller.properties | tee $SCRIPT_DIR/logs/local-logcontroller-logcontroller.log
