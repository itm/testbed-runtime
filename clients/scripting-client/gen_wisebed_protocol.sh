#!/bin/bash
protoc --java_out=src/main/java/ src/main/resources/wisebed_protocol.proto
