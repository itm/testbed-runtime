// Generated by the protocol buffer compiler.  DO NOT EDIT!

package de.uniluebeck.itm.tr.iwsn.messages;

public  final class AreNodesAliveRequest extends
    com.google.protobuf.GeneratedMessage {
  // Use AreNodesAliveRequest.newBuilder() to construct.
  private AreNodesAliveRequest() {
    initFields();
  }
  private AreNodesAliveRequest(boolean noInit) {}
  
  private static final AreNodesAliveRequest defaultInstance;
  public static AreNodesAliveRequest getDefaultInstance() {
    return defaultInstance;
  }
  
  public AreNodesAliveRequest getDefaultInstanceForType() {
    return defaultInstance;
  }
  
  public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
    return de.uniluebeck.itm.tr.iwsn.messages.Messages.internal_static_de_uniluebeck_itm_tr_iwsn_messages_AreNodesAliveRequest_descriptor;
  }
  
  protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return de.uniluebeck.itm.tr.iwsn.messages.Messages.internal_static_de_uniluebeck_itm_tr_iwsn_messages_AreNodesAliveRequest_fieldAccessorTable;
  }
  
  // repeated string nodeUrns = 1;
  public static final int NODEURNS_FIELD_NUMBER = 1;
  private java.util.List<java.lang.String> nodeUrns_ =
    java.util.Collections.emptyList();
  public java.util.List<java.lang.String> getNodeUrnsList() {
    return nodeUrns_;
  }
  public int getNodeUrnsCount() { return nodeUrns_.size(); }
  public java.lang.String getNodeUrns(int index) {
    return nodeUrns_.get(index);
  }
  
  private void initFields() {
  }
  public final boolean isInitialized() {
    return true;
  }
  
  public void writeTo(com.google.protobuf.CodedOutputStream output)
                      throws java.io.IOException {
    getSerializedSize();
    for (java.lang.String element : getNodeUrnsList()) {
      output.writeString(1, element);
    }
    getUnknownFields().writeTo(output);
  }
  
  private int memoizedSerializedSize = -1;
  public int getSerializedSize() {
    int size = memoizedSerializedSize;
    if (size != -1) return size;
  
    size = 0;
    {
      int dataSize = 0;
      for (java.lang.String element : getNodeUrnsList()) {
        dataSize += com.google.protobuf.CodedOutputStream
          .computeStringSizeNoTag(element);
      }
      size += dataSize;
      size += 1 * getNodeUrnsList().size();
    }
    size += getUnknownFields().getSerializedSize();
    memoizedSerializedSize = size;
    return size;
  }
  
  public static de.uniluebeck.itm.tr.iwsn.messages.AreNodesAliveRequest parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return newBuilder().mergeFrom(data).buildParsed();
  }
  public static de.uniluebeck.itm.tr.iwsn.messages.AreNodesAliveRequest parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return newBuilder().mergeFrom(data, extensionRegistry)
             .buildParsed();
  }
  public static de.uniluebeck.itm.tr.iwsn.messages.AreNodesAliveRequest parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return newBuilder().mergeFrom(data).buildParsed();
  }
  public static de.uniluebeck.itm.tr.iwsn.messages.AreNodesAliveRequest parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return newBuilder().mergeFrom(data, extensionRegistry)
             .buildParsed();
  }
  public static de.uniluebeck.itm.tr.iwsn.messages.AreNodesAliveRequest parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return newBuilder().mergeFrom(input).buildParsed();
  }
  public static de.uniluebeck.itm.tr.iwsn.messages.AreNodesAliveRequest parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return newBuilder().mergeFrom(input, extensionRegistry)
             .buildParsed();
  }
  public static de.uniluebeck.itm.tr.iwsn.messages.AreNodesAliveRequest parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    Builder builder = newBuilder();
    if (builder.mergeDelimitedFrom(input)) {
      return builder.buildParsed();
    } else {
      return null;
    }
  }
  public static de.uniluebeck.itm.tr.iwsn.messages.AreNodesAliveRequest parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    Builder builder = newBuilder();
    if (builder.mergeDelimitedFrom(input, extensionRegistry)) {
      return builder.buildParsed();
    } else {
      return null;
    }
  }
  public static de.uniluebeck.itm.tr.iwsn.messages.AreNodesAliveRequest parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return newBuilder().mergeFrom(input).buildParsed();
  }
  public static de.uniluebeck.itm.tr.iwsn.messages.AreNodesAliveRequest parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return newBuilder().mergeFrom(input, extensionRegistry)
             .buildParsed();
  }
  
  public static Builder newBuilder() { return Builder.create(); }
  public Builder newBuilderForType() { return newBuilder(); }
  public static Builder newBuilder(de.uniluebeck.itm.tr.iwsn.messages.AreNodesAliveRequest prototype) {
    return newBuilder().mergeFrom(prototype);
  }
  public Builder toBuilder() { return newBuilder(this); }
  
  public static final class Builder extends
      com.google.protobuf.GeneratedMessage.Builder<Builder> {
    private de.uniluebeck.itm.tr.iwsn.messages.AreNodesAliveRequest result;
    
    // Construct using de.uniluebeck.itm.tr.iwsn.messages.AreNodesAliveRequest.newBuilder()
    private Builder() {}
    
    private static Builder create() {
      Builder builder = new Builder();
      builder.result = new de.uniluebeck.itm.tr.iwsn.messages.AreNodesAliveRequest();
      return builder;
    }
    
    protected de.uniluebeck.itm.tr.iwsn.messages.AreNodesAliveRequest internalGetResult() {
      return result;
    }
    
    public Builder clear() {
      if (result == null) {
        throw new IllegalStateException(
          "Cannot call clear() after build().");
      }
      result = new de.uniluebeck.itm.tr.iwsn.messages.AreNodesAliveRequest();
      return this;
    }
    
    public Builder clone() {
      return create().mergeFrom(result);
    }
    
    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return de.uniluebeck.itm.tr.iwsn.messages.AreNodesAliveRequest.getDescriptor();
    }
    
    public de.uniluebeck.itm.tr.iwsn.messages.AreNodesAliveRequest getDefaultInstanceForType() {
      return de.uniluebeck.itm.tr.iwsn.messages.AreNodesAliveRequest.getDefaultInstance();
    }
    
    public boolean isInitialized() {
      return result.isInitialized();
    }
    public de.uniluebeck.itm.tr.iwsn.messages.AreNodesAliveRequest build() {
      if (result != null && !isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return buildPartial();
    }
    
    private de.uniluebeck.itm.tr.iwsn.messages.AreNodesAliveRequest buildParsed()
        throws com.google.protobuf.InvalidProtocolBufferException {
      if (!isInitialized()) {
        throw newUninitializedMessageException(
          result).asInvalidProtocolBufferException();
      }
      return buildPartial();
    }
    
    public de.uniluebeck.itm.tr.iwsn.messages.AreNodesAliveRequest buildPartial() {
      if (result == null) {
        throw new IllegalStateException(
          "build() has already been called on this Builder.");
      }
      if (result.nodeUrns_ != java.util.Collections.EMPTY_LIST) {
        result.nodeUrns_ =
          java.util.Collections.unmodifiableList(result.nodeUrns_);
      }
      de.uniluebeck.itm.tr.iwsn.messages.AreNodesAliveRequest returnMe = result;
      result = null;
      return returnMe;
    }
    
    public Builder mergeFrom(com.google.protobuf.Message other) {
      if (other instanceof de.uniluebeck.itm.tr.iwsn.messages.AreNodesAliveRequest) {
        return mergeFrom((de.uniluebeck.itm.tr.iwsn.messages.AreNodesAliveRequest)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }
    
    public Builder mergeFrom(de.uniluebeck.itm.tr.iwsn.messages.AreNodesAliveRequest other) {
      if (other == de.uniluebeck.itm.tr.iwsn.messages.AreNodesAliveRequest.getDefaultInstance()) return this;
      if (!other.nodeUrns_.isEmpty()) {
        if (result.nodeUrns_.isEmpty()) {
          result.nodeUrns_ = new java.util.ArrayList<java.lang.String>();
        }
        result.nodeUrns_.addAll(other.nodeUrns_);
      }
      this.mergeUnknownFields(other.getUnknownFields());
      return this;
    }
    
    public Builder mergeFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      com.google.protobuf.UnknownFieldSet.Builder unknownFields =
        com.google.protobuf.UnknownFieldSet.newBuilder(
          this.getUnknownFields());
      while (true) {
        int tag = input.readTag();
        switch (tag) {
          case 0:
            this.setUnknownFields(unknownFields.build());
            return this;
          default: {
            if (!parseUnknownField(input, unknownFields,
                                   extensionRegistry, tag)) {
              this.setUnknownFields(unknownFields.build());
              return this;
            }
            break;
          }
          case 10: {
            addNodeUrns(input.readString());
            break;
          }
        }
      }
    }
    
    
    // repeated string nodeUrns = 1;
    public java.util.List<java.lang.String> getNodeUrnsList() {
      return java.util.Collections.unmodifiableList(result.nodeUrns_);
    }
    public int getNodeUrnsCount() {
      return result.getNodeUrnsCount();
    }
    public java.lang.String getNodeUrns(int index) {
      return result.getNodeUrns(index);
    }
    public Builder setNodeUrns(int index, java.lang.String value) {
      if (value == null) {
    throw new NullPointerException();
  }
  result.nodeUrns_.set(index, value);
      return this;
    }
    public Builder addNodeUrns(java.lang.String value) {
      if (value == null) {
    throw new NullPointerException();
  }
  if (result.nodeUrns_.isEmpty()) {
        result.nodeUrns_ = new java.util.ArrayList<java.lang.String>();
      }
      result.nodeUrns_.add(value);
      return this;
    }
    public Builder addAllNodeUrns(
        java.lang.Iterable<? extends java.lang.String> values) {
      if (result.nodeUrns_.isEmpty()) {
        result.nodeUrns_ = new java.util.ArrayList<java.lang.String>();
      }
      super.addAll(values, result.nodeUrns_);
      return this;
    }
    public Builder clearNodeUrns() {
      result.nodeUrns_ = java.util.Collections.emptyList();
      return this;
    }
    
    // @@protoc_insertion_point(builder_scope:de.uniluebeck.itm.tr.iwsn.messages.AreNodesAliveRequest)
  }
  
  static {
    defaultInstance = new AreNodesAliveRequest(true);
    de.uniluebeck.itm.tr.iwsn.messages.Messages.internalForceInit();
    defaultInstance.initFields();
  }
  
  // @@protoc_insertion_point(class_scope:de.uniluebeck.itm.tr.iwsn.messages.AreNodesAliveRequest)
}

