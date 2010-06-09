#!/bin/bash
wsimport -p eu.wisebed.testbed.api.wsn.v211 -s src/main/java/ -keep -wsdllocation REPLACE_WITH_ACTUAL_URL -Xnocompile src/main/resources/ControllerService.wsdl
wsimport -p eu.wisebed.testbed.api.wsn.v211 -s src/main/java/ -keep -wsdllocation REPLACE_WITH_ACTUAL_URL -Xnocompile src/main/resources/SessionManagementService.wsdl
wsimport -p eu.wisebed.testbed.api.wsn.v211 -s src/main/java/ -keep -wsdllocation REPLACE_WITH_ACTUAL_URL -Xnocompile src/main/resources/WSNService.wsdl
