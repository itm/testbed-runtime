#!/bin/bash
wsimport -s src/main/java/ -keep -wsdllocation REPLACE_WITH_ACTUAL_URL -Xnocompile src/main/resources/SNAA.wsdl
