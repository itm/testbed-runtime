#!/bin/bash
echo "Generating classes for ControllerService..."
wsimport -p eu.wisebed.testbed.api.wsn.v22 -s src/main/java/ -keep -wsdllocation REPLACE_WITH_ACTUAL_URL -Xnocompile src/main/resources/ControllerService.wsdl
echo "Generating classes for SessionManagementService..."
wsimport -p eu.wisebed.testbed.api.wsn.v22 -s src/main/java/ -keep -wsdllocation REPLACE_WITH_ACTUAL_URL -Xnocompile src/main/resources/SessionManagementService.wsdl
echo "Generating classes for WSNService..."
wsimport -p eu.wisebed.testbed.api.wsn.v22 -s src/main/java/ -keep -wsdllocation REPLACE_WITH_ACTUAL_URL -Xnocompile src/main/resources/WSNService.wsdl
