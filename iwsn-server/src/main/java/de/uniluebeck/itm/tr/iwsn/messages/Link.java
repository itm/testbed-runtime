// Generated by the protocol buffer compiler.  DO NOT EDIT!

package de.uniluebeck.itm.tr.iwsn.messages;

public  final class Link extends
    com.google.protobuf.GeneratedMessage {
  // Use Link.newBuilder() to construct.
  private Link() {
    initFields();
  }
  private Link(boolean noInit) {}
  
  private static final Link defaultInstance;
  public static Link getDefaultInstance() {
    return defaultInstance;
  }
  
  public Link getDefaultInstanceForType() {
    return defaultInstance;
  }
  
  public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
    return de.uniluebeck.itm.tr.iwsn.messages.Messages.internal_static_de_uniluebeck_itm_tr_iwsn_messages_Link_descriptor;
  }
  
  protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return de.uniluebeck.itm.tr.iwsn.messages.Messages.internal_static_de_uniluebeck_itm_tr_iwsn_messages_Link_fieldAccessorTable;
  }
  
  // required string sourceNodeUrn = 1;
  public static final int SOURCENODEURN_FIELD_NUMBER = 1;
  private boolean hasSourceNodeUrn;
  private java.lang.String sourceNodeUrn_ = "";
  public boolean hasSourceNodeUrn() { return hasSourceNodeUrn; }
  public java.lang.String getSourceNodeUrn() { return sourceNodeUrn_; }
  
  // required string targetNodeUrn = 2;
  public static final int TARGETNODEURN_FIELD_NUMBER = 2;
  private boolean hasTargetNodeUrn;
  private java.lang.String targetNodeUrn_ = "";
  public boolean hasTargetNodeUrn() { return hasTargetNodeUrn; }
  public java.lang.String getTargetNodeUrn() { return targetNodeUrn_; }
  
  private void initFields() {
  }
  public final boolean isInitialized() {
    if (!hasSourceNodeUrn) return false;
    if (!hasTargetNodeUrn) return false;
    return true;
  }
  
  public void writeTo(com.google.protobuf.CodedOutputStream output)
                      throws java.io.IOException {
    getSerializedSize();
    if (hasSourceNodeUrn()) {
      output.writeString(1, getSourceNodeUrn());
    }
    if (hasTargetNodeUrn()) {
      output.writeString(2, getTargetNodeUrn());
    }
    getUnknownFields().writeTo(output);
  }
  
  private int memoizedSerializedSize = -1;
  public int getSerializedSize() {
    int size = memoizedSerializedSize;
    if (size != -1) return size;
  
    size = 0;
    if (hasSourceNodeUrn()) {
      size += com.google.protobuf.CodedOutputStream
        .computeStringSize(1, getSourceNodeUrn());
    }
    if (hasTargetNodeUrn()) {
      size += com.google.protobuf.CodedOutputStream
        .computeStringSize(2, getTargetNodeUrn());
    }
    size += getUnknownFields().getSerializedSize();
    memoizedSerializedSize = size;
    return size;
  }
  
  public static de.uniluebeck.itm.tr.iwsn.messages.Link parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return newBuilder().mergeFrom(data).buildParsed();
  }
  public static de.uniluebeck.itm.tr.iwsn.messages.Link parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return newBuilder().mergeFrom(data, extensionRegistry)
             .buildParsed();
  }
  public static de.uniluebeck.itm.tr.iwsn.messages.Link parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return newBuilder().mergeFrom(data).buildParsed();
  }
  public static de.uniluebeck.itm.tr.iwsn.messages.Link parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return newBuilder().mergeFrom(data, extensionRegistry)
             .buildParsed();
  }
  public static de.uniluebeck.itm.tr.iwsn.messages.Link parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return newBuilder().mergeFrom(input).buildParsed();
  }
  public static de.uniluebeck.itm.tr.iwsn.messages.Link parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return newBuilder().mergeFrom(input, extensionRegistry)
             .buildParsed();
  }
  public static de.uniluebeck.itm.tr.iwsn.messages.Link parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    Builder builder = newBuilder();
    if (builder.mergeDelimitedFrom(input)) {
      return builder.buildParsed();
    } else {
      return null;
    }
  }
  public static de.uniluebeck.itm.tr.iwsn.messages.Link parseDelimitedFrom(
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
  public static de.uniluebeck.itm.tr.iwsn.messages.Link parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return newBuilder().mergeFrom(input).buildParsed();
  }
  public static de.uniluebeck.itm.tr.iwsn.messages.Link parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return newBuilder().mergeFrom(input, extensionRegistry)
             .buildParsed();
  }
  
  public static Builder newBuilder() { return Builder.create(); }
  public Builder newBuilderForType() { return newBuilder(); }
  public static Builder newBuilder(de.uniluebeck.itm.tr.iwsn.messages.Link prototype) {
    return newBuilder().mergeFrom(prototype);
  }
  public Builder toBuilder() { return newBuilder(this); }
  
  public static final class Builder extends
      com.google.protobuf.GeneratedMessage.Builder<Builder> {
    private de.uniluebeck.itm.tr.iwsn.messages.Link result;
    
    // Construct using de.uniluebeck.itm.tr.iwsn.messages.Link.newBuilder()
    private Builder() {}
    
    private static Builder create() {
      Builder builder = new Builder();
      builder.result = new de.uniluebeck.itm.tr.iwsn.messages.Link();
      return builder;
    }
    
    protected de.uniluebeck.itm.tr.iwsn.messages.Link internalGetResult() {
      return result;
    }
    
    public Builder clear() {
      if (result == null) {
        throw new IllegalStateException(
          "Cannot call clear() after build().");
      }
      result = new de.uniluebeck.itm.tr.iwsn.messages.Link();
      return this;
    }
    
    public Builder clone() {
      return create().mergeFrom(result);
    }
    
    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return de.uniluebeck.itm.tr.iwsn.messages.Link.getDescriptor();
    }
    
    public de.uniluebeck.itm.tr.iwsn.messages.Link getDefaultInstanceForType() {
      return de.uniluebeck.itm.tr.iwsn.messages.Link.getDefaultInstance();
    }
    
    public boolean isInitialized() {
      return result.isInitialized();
    }
    public de.uniluebeck.itm.tr.iwsn.messages.Link build() {
      if (result != null && !isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return buildPartial();
    }
    
    private de.uniluebeck.itm.tr.iwsn.messages.Link buildParsed()
        throws com.google.protobuf.InvalidProtocolBufferException {
      if (!isInitialized()) {
        throw newUninitializedMessageException(
          result).asInvalidProtocolBufferException();
      }
      return buildPartial();
    }
    
    public de.uniluebeck.itm.tr.iwsn.messages.Link buildPartial() {
      if (result == null) {
        throw new IllegalStateException(
          "build() has already been called on this Builder.");
      }
      de.uniluebeck.itm.tr.iwsn.messages.Link returnMe = result;
      result = null;
      return returnMe;
    }
    
    public Builder mergeFrom(com.google.protobuf.Message other) {
      if (other instanceof de.uniluebeck.itm.tr.iwsn.messages.Link) {
        return mergeFrom((de.uniluebeck.itm.tr.iwsn.messages.Link)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }
    
    public Builder mergeFrom(de.uniluebeck.itm.tr.iwsn.messages.Link other) {
      if (other == de.uniluebeck.itm.tr.iwsn.messages.Link.getDefaultInstance()) return this;
      if (other.hasSourceNodeUrn()) {
        setSourceNodeUrn(other.getSourceNodeUrn());
      }
      if (other.hasTargetNodeUrn()) {
        setTargetNodeUrn(other.getTargetNodeUrn());
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
            setSourceNodeUrn(input.readString());
            break;
          }
          case 18: {
            setTargetNodeUrn(input.readString());
            break;
          }
        }
      }
    }
    
    
    // required string sourceNodeUrn = 1;
    public boolean hasSourceNodeUrn() {
      return result.hasSourceNodeUrn();
    }
    public java.lang.String getSourceNodeUrn() {
      return result.getSourceNodeUrn();
    }
    public Builder setSourceNodeUrn(java.lang.String value) {
      if (value == null) {
    throw new NullPointerException();
  }
  result.hasSourceNodeUrn = true;
      result.sourceNodeUrn_ = value;
      return this;
    }
    public Builder clearSourceNodeUrn() {
      result.hasSourceNodeUrn = false;
      result.sourceNodeUrn_ = getDefaultInstance().getSourceNodeUrn();
      return this;
    }
    
    // required string targetNodeUrn = 2;
    public boolean hasTargetNodeUrn() {
      return result.hasTargetNodeUrn();
    }
    public java.lang.String getTargetNodeUrn() {
      return result.getTargetNodeUrn();
    }
    public Builder setTargetNodeUrn(java.lang.String value) {
      if (value == null) {
    throw new NullPointerException();
  }
  result.hasTargetNodeUrn = true;
      result.targetNodeUrn_ = value;
      return this;
    }
    public Builder clearTargetNodeUrn() {
      result.hasTargetNodeUrn = false;
      result.targetNodeUrn_ = getDefaultInstance().getTargetNodeUrn();
      return this;
    }
    
    // @@protoc_insertion_point(builder_scope:de.uniluebeck.itm.tr.iwsn.messages.Link)
  }
  
  static {
    defaultInstance = new Link(true);
    de.uniluebeck.itm.tr.iwsn.messages.Messages.internalForceInit();
    defaultInstance.initFields();
  }
  
  // @@protoc_insertion_point(class_scope:de.uniluebeck.itm.tr.iwsn.messages.Link)
}

