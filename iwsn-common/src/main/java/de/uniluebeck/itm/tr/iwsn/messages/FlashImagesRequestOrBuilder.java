// Generated by the protocol buffer compiler.  DO NOT EDIT!

package de.uniluebeck.itm.tr.iwsn.messages;

public interface FlashImagesRequestOrBuilder
    extends com.google.protobuf.MessageOrBuilder {
  
  // required .de.uniluebeck.itm.tr.iwsn.messages.RequestResponseHeader header = 1;
  boolean hasHeader();
  de.uniluebeck.itm.tr.iwsn.messages.RequestResponseHeader getHeader();
  de.uniluebeck.itm.tr.iwsn.messages.RequestResponseHeaderOrBuilder getHeaderOrBuilder();
  
  // optional .de.uniluebeck.itm.tr.iwsn.messages.MessageType type = 2 [default = REQUEST_FLASH_IMAGES];
  boolean hasType();
  de.uniluebeck.itm.tr.iwsn.messages.MessageType getType();
  
  // required bytes image = 3;
  boolean hasImage();
  com.google.protobuf.ByteString getImage();
}
