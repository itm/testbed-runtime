#!/bin/bash
SCRIPT_DIR=`dirname $0`
TR_DIR=$SCRIPT_DIR/..
TR_VERSION=0.5.4-SNAPSHOT

if [ ! -d $SCRIPT_DIR/logs ]; then
	mkdir -p $SCRIPT_DIR/logs
fi

java -jar $TR_DIR/iwsn/runtime.cmdline/target/tr.runtime.cmdline-$TR_VERSION.one-jar.jar -f $TR_DIR/iwsn/runtime.cmdline/configs/various/local-logcontroller.xml -l DEBUG -n local-logcontroller | tee $SCRIPT_DIR/logs/local-logcontroller-iwsn.log
