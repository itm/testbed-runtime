#!/bin/bash
protoc --java_out=src/main/java/ src/main/resources/overlay-messages.proto
