// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: src/main/resources/iwsn-messages.proto

package de.uniluebeck.itm.tr.iwsn.messages;

public interface NotificationEventOrBuilder
    extends com.google.protobuf.MessageOrBuilder {

  // optional string nodeUrn = 1;
  /**
   * <code>optional string nodeUrn = 1;</code>
   */
  boolean hasNodeUrn();
  /**
   * <code>optional string nodeUrn = 1;</code>
   */
  java.lang.String getNodeUrn();
  /**
   * <code>optional string nodeUrn = 1;</code>
   */
  com.google.protobuf.ByteString
      getNodeUrnBytes();

  // required uint64 timestamp = 2;
  /**
   * <code>required uint64 timestamp = 2;</code>
   */
  boolean hasTimestamp();
  /**
   * <code>required uint64 timestamp = 2;</code>
   */
  long getTimestamp();

  // required string message = 3;
  /**
   * <code>required string message = 3;</code>
   */
  boolean hasMessage();
  /**
   * <code>required string message = 3;</code>
   */
  java.lang.String getMessage();
  /**
   * <code>required string message = 3;</code>
   */
  com.google.protobuf.ByteString
      getMessageBytes();
}
