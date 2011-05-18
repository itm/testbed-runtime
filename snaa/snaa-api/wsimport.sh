#!/bin/bash
wsimport -b src/main/resources/SNAATypes.xjb -b src/main/resources/SNAAService.xjb -s src/main/java/ -keep -wsdllocation REPLACE_WITH_ACTUAL_URL -Xnocompile src/main/resources/SNAAService.wsdl
