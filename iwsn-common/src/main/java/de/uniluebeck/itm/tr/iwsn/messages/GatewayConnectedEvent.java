// Generated by the protocol buffer compiler.  DO NOT EDIT!

package de.uniluebeck.itm.tr.iwsn.messages;

public  final class GatewayConnectedEvent extends
    com.google.protobuf.GeneratedMessage
    implements GatewayConnectedEventOrBuilder {
  // Use GatewayConnectedEvent.newBuilder() to construct.
  private GatewayConnectedEvent(Builder builder) {
    super(builder);
  }
  private GatewayConnectedEvent(boolean noInit) {}
  
  private static final GatewayConnectedEvent defaultInstance;
  public static GatewayConnectedEvent getDefaultInstance() {
    return defaultInstance;
  }
  
  public GatewayConnectedEvent getDefaultInstanceForType() {
    return defaultInstance;
  }
  
  public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
    return de.uniluebeck.itm.tr.iwsn.messages.Messages.internal_static_de_uniluebeck_itm_tr_iwsn_messages_GatewayConnectedEvent_descriptor;
  }
  
  protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return de.uniluebeck.itm.tr.iwsn.messages.Messages.internal_static_de_uniluebeck_itm_tr_iwsn_messages_GatewayConnectedEvent_fieldAccessorTable;
  }
  
  private int bitField0_;
  // required .de.uniluebeck.itm.tr.iwsn.messages.EventHeader header = 1;
  public static final int HEADER_FIELD_NUMBER = 1;
  private de.uniluebeck.itm.tr.iwsn.messages.EventHeader header_;
  public boolean hasHeader() {
    return ((bitField0_ & 0x00000001) == 0x00000001);
  }
  public de.uniluebeck.itm.tr.iwsn.messages.EventHeader getHeader() {
    return header_;
  }
  public de.uniluebeck.itm.tr.iwsn.messages.EventHeaderOrBuilder getHeaderOrBuilder() {
    return header_;
  }
  
  // optional .de.uniluebeck.itm.tr.iwsn.messages.MessageType type = 2 [default = EVENT_GATEWAY_CONNECTED];
  public static final int TYPE_FIELD_NUMBER = 2;
  private de.uniluebeck.itm.tr.iwsn.messages.MessageType type_;
  public boolean hasType() {
    return ((bitField0_ & 0x00000002) == 0x00000002);
  }
  public de.uniluebeck.itm.tr.iwsn.messages.MessageType getType() {
    return type_;
  }
  
  // required string hostname = 3;
  public static final int HOSTNAME_FIELD_NUMBER = 3;
  private java.lang.Object hostname_;
  public boolean hasHostname() {
    return ((bitField0_ & 0x00000004) == 0x00000004);
  }
  public String getHostname() {
    java.lang.Object ref = hostname_;
    if (ref instanceof String) {
      return (String) ref;
    } else {
      com.google.protobuf.ByteString bs = 
          (com.google.protobuf.ByteString) ref;
      String s = bs.toStringUtf8();
      if (com.google.protobuf.Internal.isValidUtf8(bs)) {
        hostname_ = s;
      }
      return s;
    }
  }
  private com.google.protobuf.ByteString getHostnameBytes() {
    java.lang.Object ref = hostname_;
    if (ref instanceof String) {
      com.google.protobuf.ByteString b = 
          com.google.protobuf.ByteString.copyFromUtf8((String) ref);
      hostname_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }
  
  private void initFields() {
    header_ = de.uniluebeck.itm.tr.iwsn.messages.EventHeader.getDefaultInstance();
    type_ = de.uniluebeck.itm.tr.iwsn.messages.MessageType.EVENT_GATEWAY_CONNECTED;
    hostname_ = "";
  }
  private byte memoizedIsInitialized = -1;
  public final boolean isInitialized() {
    byte isInitialized = memoizedIsInitialized;
    if (isInitialized != -1) return isInitialized == 1;
    
    if (!hasHeader()) {
      memoizedIsInitialized = 0;
      return false;
    }
    if (!hasHostname()) {
      memoizedIsInitialized = 0;
      return false;
    }
    if (!getHeader().isInitialized()) {
      memoizedIsInitialized = 0;
      return false;
    }
    memoizedIsInitialized = 1;
    return true;
  }
  
  public void writeTo(com.google.protobuf.CodedOutputStream output)
                      throws java.io.IOException {
    getSerializedSize();
    if (((bitField0_ & 0x00000001) == 0x00000001)) {
      output.writeMessage(1, header_);
    }
    if (((bitField0_ & 0x00000002) == 0x00000002)) {
      output.writeEnum(2, type_.getNumber());
    }
    if (((bitField0_ & 0x00000004) == 0x00000004)) {
      output.writeBytes(3, getHostnameBytes());
    }
    getUnknownFields().writeTo(output);
  }
  
  private int memoizedSerializedSize = -1;
  public int getSerializedSize() {
    int size = memoizedSerializedSize;
    if (size != -1) return size;
  
    size = 0;
    if (((bitField0_ & 0x00000001) == 0x00000001)) {
      size += com.google.protobuf.CodedOutputStream
        .computeMessageSize(1, header_);
    }
    if (((bitField0_ & 0x00000002) == 0x00000002)) {
      size += com.google.protobuf.CodedOutputStream
        .computeEnumSize(2, type_.getNumber());
    }
    if (((bitField0_ & 0x00000004) == 0x00000004)) {
      size += com.google.protobuf.CodedOutputStream
        .computeBytesSize(3, getHostnameBytes());
    }
    size += getUnknownFields().getSerializedSize();
    memoizedSerializedSize = size;
    return size;
  }
  
  private static final long serialVersionUID = 0L;
  @java.lang.Override
  protected java.lang.Object writeReplace()
      throws java.io.ObjectStreamException {
    return super.writeReplace();
  }
  
  public static de.uniluebeck.itm.tr.iwsn.messages.GatewayConnectedEvent parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return newBuilder().mergeFrom(data).buildParsed();
  }
  public static de.uniluebeck.itm.tr.iwsn.messages.GatewayConnectedEvent parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return newBuilder().mergeFrom(data, extensionRegistry)
             .buildParsed();
  }
  public static de.uniluebeck.itm.tr.iwsn.messages.GatewayConnectedEvent parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return newBuilder().mergeFrom(data).buildParsed();
  }
  public static de.uniluebeck.itm.tr.iwsn.messages.GatewayConnectedEvent parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return newBuilder().mergeFrom(data, extensionRegistry)
             .buildParsed();
  }
  public static de.uniluebeck.itm.tr.iwsn.messages.GatewayConnectedEvent parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return newBuilder().mergeFrom(input).buildParsed();
  }
  public static de.uniluebeck.itm.tr.iwsn.messages.GatewayConnectedEvent parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return newBuilder().mergeFrom(input, extensionRegistry)
             .buildParsed();
  }
  public static de.uniluebeck.itm.tr.iwsn.messages.GatewayConnectedEvent parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    Builder builder = newBuilder();
    if (builder.mergeDelimitedFrom(input)) {
      return builder.buildParsed();
    } else {
      return null;
    }
  }
  public static de.uniluebeck.itm.tr.iwsn.messages.GatewayConnectedEvent parseDelimitedFrom(
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
  public static de.uniluebeck.itm.tr.iwsn.messages.GatewayConnectedEvent parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return newBuilder().mergeFrom(input).buildParsed();
  }
  public static de.uniluebeck.itm.tr.iwsn.messages.GatewayConnectedEvent parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return newBuilder().mergeFrom(input, extensionRegistry)
             .buildParsed();
  }
  
  public static Builder newBuilder() { return Builder.create(); }
  public Builder newBuilderForType() { return newBuilder(); }
  public static Builder newBuilder(de.uniluebeck.itm.tr.iwsn.messages.GatewayConnectedEvent prototype) {
    return newBuilder().mergeFrom(prototype);
  }
  public Builder toBuilder() { return newBuilder(this); }
  
  @java.lang.Override
  protected Builder newBuilderForType(
      com.google.protobuf.GeneratedMessage.BuilderParent parent) {
    Builder builder = new Builder(parent);
    return builder;
  }
  public static final class Builder extends
      com.google.protobuf.GeneratedMessage.Builder<Builder>
     implements de.uniluebeck.itm.tr.iwsn.messages.GatewayConnectedEventOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return de.uniluebeck.itm.tr.iwsn.messages.Messages.internal_static_de_uniluebeck_itm_tr_iwsn_messages_GatewayConnectedEvent_descriptor;
    }
    
    protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return de.uniluebeck.itm.tr.iwsn.messages.Messages.internal_static_de_uniluebeck_itm_tr_iwsn_messages_GatewayConnectedEvent_fieldAccessorTable;
    }
    
    // Construct using de.uniluebeck.itm.tr.iwsn.messages.GatewayConnectedEvent.newBuilder()
    private Builder() {
      maybeForceBuilderInitialization();
    }
    
    private Builder(BuilderParent parent) {
      super(parent);
      maybeForceBuilderInitialization();
    }
    private void maybeForceBuilderInitialization() {
      if (com.google.protobuf.GeneratedMessage.alwaysUseFieldBuilders) {
        getHeaderFieldBuilder();
      }
    }
    private static Builder create() {
      return new Builder();
    }
    
    public Builder clear() {
      super.clear();
      if (headerBuilder_ == null) {
        header_ = de.uniluebeck.itm.tr.iwsn.messages.EventHeader.getDefaultInstance();
      } else {
        headerBuilder_.clear();
      }
      bitField0_ = (bitField0_ & ~0x00000001);
      type_ = de.uniluebeck.itm.tr.iwsn.messages.MessageType.EVENT_GATEWAY_CONNECTED;
      bitField0_ = (bitField0_ & ~0x00000002);
      hostname_ = "";
      bitField0_ = (bitField0_ & ~0x00000004);
      return this;
    }
    
    public Builder clone() {
      return create().mergeFrom(buildPartial());
    }
    
    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return de.uniluebeck.itm.tr.iwsn.messages.GatewayConnectedEvent.getDescriptor();
    }
    
    public de.uniluebeck.itm.tr.iwsn.messages.GatewayConnectedEvent getDefaultInstanceForType() {
      return de.uniluebeck.itm.tr.iwsn.messages.GatewayConnectedEvent.getDefaultInstance();
    }
    
    public de.uniluebeck.itm.tr.iwsn.messages.GatewayConnectedEvent build() {
      de.uniluebeck.itm.tr.iwsn.messages.GatewayConnectedEvent result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }
    
    private de.uniluebeck.itm.tr.iwsn.messages.GatewayConnectedEvent buildParsed()
        throws com.google.protobuf.InvalidProtocolBufferException {
      de.uniluebeck.itm.tr.iwsn.messages.GatewayConnectedEvent result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(
          result).asInvalidProtocolBufferException();
      }
      return result;
    }
    
    public de.uniluebeck.itm.tr.iwsn.messages.GatewayConnectedEvent buildPartial() {
      de.uniluebeck.itm.tr.iwsn.messages.GatewayConnectedEvent result = new de.uniluebeck.itm.tr.iwsn.messages.GatewayConnectedEvent(this);
      int from_bitField0_ = bitField0_;
      int to_bitField0_ = 0;
      if (((from_bitField0_ & 0x00000001) == 0x00000001)) {
        to_bitField0_ |= 0x00000001;
      }
      if (headerBuilder_ == null) {
        result.header_ = header_;
      } else {
        result.header_ = headerBuilder_.build();
      }
      if (((from_bitField0_ & 0x00000002) == 0x00000002)) {
        to_bitField0_ |= 0x00000002;
      }
      result.type_ = type_;
      if (((from_bitField0_ & 0x00000004) == 0x00000004)) {
        to_bitField0_ |= 0x00000004;
      }
      result.hostname_ = hostname_;
      result.bitField0_ = to_bitField0_;
      onBuilt();
      return result;
    }
    
    public Builder mergeFrom(com.google.protobuf.Message other) {
      if (other instanceof de.uniluebeck.itm.tr.iwsn.messages.GatewayConnectedEvent) {
        return mergeFrom((de.uniluebeck.itm.tr.iwsn.messages.GatewayConnectedEvent)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }
    
    public Builder mergeFrom(de.uniluebeck.itm.tr.iwsn.messages.GatewayConnectedEvent other) {
      if (other == de.uniluebeck.itm.tr.iwsn.messages.GatewayConnectedEvent.getDefaultInstance()) return this;
      if (other.hasHeader()) {
        mergeHeader(other.getHeader());
      }
      if (other.hasType()) {
        setType(other.getType());
      }
      if (other.hasHostname()) {
        setHostname(other.getHostname());
      }
      this.mergeUnknownFields(other.getUnknownFields());
      return this;
    }
    
    public final boolean isInitialized() {
      if (!hasHeader()) {
        
        return false;
      }
      if (!hasHostname()) {
        
        return false;
      }
      if (!getHeader().isInitialized()) {
        
        return false;
      }
      return true;
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
            onChanged();
            return this;
          default: {
            if (!parseUnknownField(input, unknownFields,
                                   extensionRegistry, tag)) {
              this.setUnknownFields(unknownFields.build());
              onChanged();
              return this;
            }
            break;
          }
          case 10: {
            de.uniluebeck.itm.tr.iwsn.messages.EventHeader.Builder subBuilder = de.uniluebeck.itm.tr.iwsn.messages.EventHeader.newBuilder();
            if (hasHeader()) {
              subBuilder.mergeFrom(getHeader());
            }
            input.readMessage(subBuilder, extensionRegistry);
            setHeader(subBuilder.buildPartial());
            break;
          }
          case 16: {
            int rawValue = input.readEnum();
            de.uniluebeck.itm.tr.iwsn.messages.MessageType value = de.uniluebeck.itm.tr.iwsn.messages.MessageType.valueOf(rawValue);
            if (value == null) {
              unknownFields.mergeVarintField(2, rawValue);
            } else {
              bitField0_ |= 0x00000002;
              type_ = value;
            }
            break;
          }
          case 26: {
            bitField0_ |= 0x00000004;
            hostname_ = input.readBytes();
            break;
          }
        }
      }
    }
    
    private int bitField0_;
    
    // required .de.uniluebeck.itm.tr.iwsn.messages.EventHeader header = 1;
    private de.uniluebeck.itm.tr.iwsn.messages.EventHeader header_ = de.uniluebeck.itm.tr.iwsn.messages.EventHeader.getDefaultInstance();
    private com.google.protobuf.SingleFieldBuilder<
        de.uniluebeck.itm.tr.iwsn.messages.EventHeader, de.uniluebeck.itm.tr.iwsn.messages.EventHeader.Builder, de.uniluebeck.itm.tr.iwsn.messages.EventHeaderOrBuilder> headerBuilder_;
    public boolean hasHeader() {
      return ((bitField0_ & 0x00000001) == 0x00000001);
    }
    public de.uniluebeck.itm.tr.iwsn.messages.EventHeader getHeader() {
      if (headerBuilder_ == null) {
        return header_;
      } else {
        return headerBuilder_.getMessage();
      }
    }
    public Builder setHeader(de.uniluebeck.itm.tr.iwsn.messages.EventHeader value) {
      if (headerBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        header_ = value;
        onChanged();
      } else {
        headerBuilder_.setMessage(value);
      }
      bitField0_ |= 0x00000001;
      return this;
    }
    public Builder setHeader(
        de.uniluebeck.itm.tr.iwsn.messages.EventHeader.Builder builderForValue) {
      if (headerBuilder_ == null) {
        header_ = builderForValue.build();
        onChanged();
      } else {
        headerBuilder_.setMessage(builderForValue.build());
      }
      bitField0_ |= 0x00000001;
      return this;
    }
    public Builder mergeHeader(de.uniluebeck.itm.tr.iwsn.messages.EventHeader value) {
      if (headerBuilder_ == null) {
        if (((bitField0_ & 0x00000001) == 0x00000001) &&
            header_ != de.uniluebeck.itm.tr.iwsn.messages.EventHeader.getDefaultInstance()) {
          header_ =
            de.uniluebeck.itm.tr.iwsn.messages.EventHeader.newBuilder(header_).mergeFrom(value).buildPartial();
        } else {
          header_ = value;
        }
        onChanged();
      } else {
        headerBuilder_.mergeFrom(value);
      }
      bitField0_ |= 0x00000001;
      return this;
    }
    public Builder clearHeader() {
      if (headerBuilder_ == null) {
        header_ = de.uniluebeck.itm.tr.iwsn.messages.EventHeader.getDefaultInstance();
        onChanged();
      } else {
        headerBuilder_.clear();
      }
      bitField0_ = (bitField0_ & ~0x00000001);
      return this;
    }
    public de.uniluebeck.itm.tr.iwsn.messages.EventHeader.Builder getHeaderBuilder() {
      bitField0_ |= 0x00000001;
      onChanged();
      return getHeaderFieldBuilder().getBuilder();
    }
    public de.uniluebeck.itm.tr.iwsn.messages.EventHeaderOrBuilder getHeaderOrBuilder() {
      if (headerBuilder_ != null) {
        return headerBuilder_.getMessageOrBuilder();
      } else {
        return header_;
      }
    }
    private com.google.protobuf.SingleFieldBuilder<
        de.uniluebeck.itm.tr.iwsn.messages.EventHeader, de.uniluebeck.itm.tr.iwsn.messages.EventHeader.Builder, de.uniluebeck.itm.tr.iwsn.messages.EventHeaderOrBuilder> 
        getHeaderFieldBuilder() {
      if (headerBuilder_ == null) {
        headerBuilder_ = new com.google.protobuf.SingleFieldBuilder<
            de.uniluebeck.itm.tr.iwsn.messages.EventHeader, de.uniluebeck.itm.tr.iwsn.messages.EventHeader.Builder, de.uniluebeck.itm.tr.iwsn.messages.EventHeaderOrBuilder>(
                header_,
                getParentForChildren(),
                isClean());
        header_ = null;
      }
      return headerBuilder_;
    }
    
    // optional .de.uniluebeck.itm.tr.iwsn.messages.MessageType type = 2 [default = EVENT_GATEWAY_CONNECTED];
    private de.uniluebeck.itm.tr.iwsn.messages.MessageType type_ = de.uniluebeck.itm.tr.iwsn.messages.MessageType.EVENT_GATEWAY_CONNECTED;
    public boolean hasType() {
      return ((bitField0_ & 0x00000002) == 0x00000002);
    }
    public de.uniluebeck.itm.tr.iwsn.messages.MessageType getType() {
      return type_;
    }
    public Builder setType(de.uniluebeck.itm.tr.iwsn.messages.MessageType value) {
      if (value == null) {
        throw new NullPointerException();
      }
      bitField0_ |= 0x00000002;
      type_ = value;
      onChanged();
      return this;
    }
    public Builder clearType() {
      bitField0_ = (bitField0_ & ~0x00000002);
      type_ = de.uniluebeck.itm.tr.iwsn.messages.MessageType.EVENT_GATEWAY_CONNECTED;
      onChanged();
      return this;
    }
    
    // required string hostname = 3;
    private java.lang.Object hostname_ = "";
    public boolean hasHostname() {
      return ((bitField0_ & 0x00000004) == 0x00000004);
    }
    public String getHostname() {
      java.lang.Object ref = hostname_;
      if (!(ref instanceof String)) {
        String s = ((com.google.protobuf.ByteString) ref).toStringUtf8();
        hostname_ = s;
        return s;
      } else {
        return (String) ref;
      }
    }
    public Builder setHostname(String value) {
      if (value == null) {
    throw new NullPointerException();
  }
  bitField0_ |= 0x00000004;
      hostname_ = value;
      onChanged();
      return this;
    }
    public Builder clearHostname() {
      bitField0_ = (bitField0_ & ~0x00000004);
      hostname_ = getDefaultInstance().getHostname();
      onChanged();
      return this;
    }
    void setHostname(com.google.protobuf.ByteString value) {
      bitField0_ |= 0x00000004;
      hostname_ = value;
      onChanged();
    }
    
    // @@protoc_insertion_point(builder_scope:de.uniluebeck.itm.tr.iwsn.messages.GatewayConnectedEvent)
  }
  
  static {
    defaultInstance = new GatewayConnectedEvent(true);
    defaultInstance.initFields();
  }
  
  // @@protoc_insertion_point(class_scope:de.uniluebeck.itm.tr.iwsn.messages.GatewayConnectedEvent)
}

