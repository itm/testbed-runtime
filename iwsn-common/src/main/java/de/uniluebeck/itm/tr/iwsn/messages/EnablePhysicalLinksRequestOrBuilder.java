// Generated by the protocol buffer compiler.  DO NOT EDIT!

package de.uniluebeck.itm.tr.iwsn.messages;

public interface EnablePhysicalLinksRequestOrBuilder
    extends com.google.protobuf.MessageOrBuilder {
  
  // required .de.uniluebeck.itm.tr.iwsn.messages.RequestResponseHeader header = 1;
  boolean hasHeader();
  de.uniluebeck.itm.tr.iwsn.messages.RequestResponseHeader getHeader();
  de.uniluebeck.itm.tr.iwsn.messages.RequestResponseHeaderOrBuilder getHeaderOrBuilder();
  
  // optional .de.uniluebeck.itm.tr.iwsn.messages.MessageType type = 2 [default = REQUEST_ENABLE_PHYSICAL_LINKS];
  boolean hasType();
  de.uniluebeck.itm.tr.iwsn.messages.MessageType getType();
  
  // repeated .de.uniluebeck.itm.tr.iwsn.messages.Link links = 3;
  java.util.List<de.uniluebeck.itm.tr.iwsn.messages.Link> 
      getLinksList();
  de.uniluebeck.itm.tr.iwsn.messages.Link getLinks(int index);
  int getLinksCount();
  java.util.List<? extends de.uniluebeck.itm.tr.iwsn.messages.LinkOrBuilder> 
      getLinksOrBuilderList();
  de.uniluebeck.itm.tr.iwsn.messages.LinkOrBuilder getLinksOrBuilder(
      int index);
}
