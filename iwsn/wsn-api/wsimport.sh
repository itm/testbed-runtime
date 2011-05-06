#!/bin/bash
echo "Generating classes for ControllerService..."
wsimport -b src/main/resources/CommonTypes.xjb -b src/main/resources/ControllerService.xjb -b src/main/resources/ControllerTypes.xjb -s src/main/java/ -keep -wsdllocation REPLACE_WITH_ACTUAL_URL -Xnocompile src/main/resources/ControllerService.wsdl
echo "Generating classes for SessionManagementService..."
wsimport -b src/main/resources/CommonTypes.xjb -b src/main/resources/SessionManagementService.xjb -b src/main/resources/SessionManagementTypes.xjb -s src/main/java/ -keep -wsdllocation REPLACE_WITH_ACTUAL_URL -Xnocompile src/main/resources/SessionManagementService.wsdl
echo "Generating classes for WSNService..."
wsimport -b src/main/resources/CommonTypes.xjb -b src/main/resources/WSNService.xjb -b src/main/resources/WSNTypes.xjb -s src/main/java/ -keep -wsdllocation REPLACE_WITH_ACTUAL_URL -Xnocompile src/main/resources/WSNService.wsdl
