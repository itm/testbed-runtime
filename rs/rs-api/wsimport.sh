#!/bin/bash
wsimport -p eu.wisebed.testbed.api.rs.v1 -s src/main/java/ -keep -wsdllocation REPLACE_WITH_ACTUAL_URL -Xnocompile src/main/resources/RS.wsdl
