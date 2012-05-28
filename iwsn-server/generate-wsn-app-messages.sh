#!/bin/bash
protoc --java_out=src/main/java/ src/main/resources/wsn-app-messages.proto
