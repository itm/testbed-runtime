// Generated by the protocol buffer compiler.  DO NOT EDIT!

package de.uniluebeck.itm.tr.iwsn.messages;

public interface GatewayDisconnectedEventOrBuilder
    extends com.google.protobuf.MessageOrBuilder {
  
  // required uint64 timestamp = 1;
  boolean hasTimestamp();
  long getTimestamp();
  
  // required string hostname = 2;
  boolean hasHostname();
  String getHostname();
  
  // repeated string nodeUrns = 3;
  java.util.List<String> getNodeUrnsList();
  int getNodeUrnsCount();
  String getNodeUrns(int index);
}
