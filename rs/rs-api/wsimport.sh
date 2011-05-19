#!/bin/bash
wsimport -b src/main/resources/RSService.xjb -b src/main/resources/RSTypes.xjb -s src/main/java/ -keep -wsdllocation REPLACE_WITH_ACTUAL_URL -Xnocompile src/main/resources/RSService.wsdl
