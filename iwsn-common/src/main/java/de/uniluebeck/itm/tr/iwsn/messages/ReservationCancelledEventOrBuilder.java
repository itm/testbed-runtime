// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: src/main/resources/iwsn-messages.proto

package de.uniluebeck.itm.tr.iwsn.messages;

public interface ReservationCancelledEventOrBuilder
    extends com.google.protobuf.MessageOrBuilder {

  // required string serializedKey = 1;
  /**
   * <code>required string serializedKey = 1;</code>
   */
  boolean hasSerializedKey();
  /**
   * <code>required string serializedKey = 1;</code>
   */
  java.lang.String getSerializedKey();
  /**
   * <code>required string serializedKey = 1;</code>
   */
  com.google.protobuf.ByteString
      getSerializedKeyBytes();

  // required uint64 timestamp = 2;
  /**
   * <code>required uint64 timestamp = 2;</code>
   */
  boolean hasTimestamp();
  /**
   * <code>required uint64 timestamp = 2;</code>
   */
  long getTimestamp();
}
