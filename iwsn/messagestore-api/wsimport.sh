#!/bin/bash
echo "Generating classes for MessageStoreService..."
wsimport -p eu.wisebed.testbed.api.messagestore.v1 -s src/main/java/ -keep -wsdllocation REPLACE_WITH_ACTUAL_URL -Xnocompile src/main/resources/MessageStoreService.wsdl
