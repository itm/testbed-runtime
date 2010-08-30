#!/bin/bash
SCRIPT_DIR=`dirname $0`
TR_DIR=$SCRIPT_DIR/..
TR_VERSION=0.5.4-SNAPSHOT

if [ ! -d $SCRIPT_DIR/logs ]; then
	mkdir -p $SCRIPT_DIR/logs
fi

java -jar $TR_DIR/rs/rs-cmdline/target/tr.rs-cmdline-$TR_VERSION.one-jar.jar -f $TR_DIR/rs/rs-cmdline/configs/various/local-logcontroller.properties -v | tee $SCRIPT_DIR/logs/local-logcontroller-rs.log
