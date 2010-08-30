#!/bin/bash
SCRIPT_DIR=`dirname $0`
TR_DIR=$SCRIPT_DIR/..
TR_VERSION=0.5.4-SNAPSHOT

if [ ! -d $SCRIPT_DIR/logs ]; then
	mkdir -p $SCRIPT_DIR/logs
fi

java -jar $TR_DIR/snaa/snaa-cmdline-server/target/tr.snaa-cmdline-server-$TR_VERSION.one-jar.jar -f $TR_DIR/snaa/snaa-cmdline-server/configs/various/local-logcontroller.properties -v | tee $SCRIPT_DIR/logs/local-logcontroller-snaa.log
