// Generated by the protocol buffer compiler.  DO NOT EDIT!

package de.uniluebeck.itm.tr.iwsn.messages;

public  final class NotificationEvent extends
    com.google.protobuf.GeneratedMessage {
  // Use NotificationEvent.newBuilder() to construct.
  private NotificationEvent() {
    initFields();
  }
  private NotificationEvent(boolean noInit) {}
  
  private static final NotificationEvent defaultInstance;
  public static NotificationEvent getDefaultInstance() {
    return defaultInstance;
  }
  
  public NotificationEvent getDefaultInstanceForType() {
    return defaultInstance;
  }
  
  public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
    return de.uniluebeck.itm.tr.iwsn.messages.Messages.internal_static_de_uniluebeck_itm_tr_iwsn_messages_NotificationEvent_descriptor;
  }
  
  protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return de.uniluebeck.itm.tr.iwsn.messages.Messages.internal_static_de_uniluebeck_itm_tr_iwsn_messages_NotificationEvent_fieldAccessorTable;
  }
  
  // optional string nodeUrn = 1;
  public static final int NODEURN_FIELD_NUMBER = 1;
  private boolean hasNodeUrn;
  private java.lang.String nodeUrn_ = "";
  public boolean hasNodeUrn() { return hasNodeUrn; }
  public java.lang.String getNodeUrn() { return nodeUrn_; }
  
  // required uint64 timestamp = 2;
  public static final int TIMESTAMP_FIELD_NUMBER = 2;
  private boolean hasTimestamp;
  private long timestamp_ = 0L;
  public boolean hasTimestamp() { return hasTimestamp; }
  public long getTimestamp() { return timestamp_; }
  
  // required string message = 3;
  public static final int MESSAGE_FIELD_NUMBER = 3;
  private boolean hasMessage;
  private java.lang.String message_ = "";
  public boolean hasMessage() { return hasMessage; }
  public java.lang.String getMessage() { return message_; }
  
  private void initFields() {
  }
  public final boolean isInitialized() {
    if (!hasTimestamp) return false;
    if (!hasMessage) return false;
    return true;
  }
  
  public void writeTo(com.google.protobuf.CodedOutputStream output)
                      throws java.io.IOException {
    getSerializedSize();
    if (hasNodeUrn()) {
      output.writeString(1, getNodeUrn());
    }
    if (hasTimestamp()) {
      output.writeUInt64(2, getTimestamp());
    }
    if (hasMessage()) {
      output.writeString(3, getMessage());
    }
    getUnknownFields().writeTo(output);
  }
  
  private int memoizedSerializedSize = -1;
  public int getSerializedSize() {
    int size = memoizedSerializedSize;
    if (size != -1) return size;
  
    size = 0;
    if (hasNodeUrn()) {
      size += com.google.protobuf.CodedOutputStream
        .computeStringSize(1, getNodeUrn());
    }
    if (hasTimestamp()) {
      size += com.google.protobuf.CodedOutputStream
        .computeUInt64Size(2, getTimestamp());
    }
    if (hasMessage()) {
      size += com.google.protobuf.CodedOutputStream
        .computeStringSize(3, getMessage());
    }
    size += getUnknownFields().getSerializedSize();
    memoizedSerializedSize = size;
    return size;
  }
  
  public static de.uniluebeck.itm.tr.iwsn.messages.NotificationEvent parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return newBuilder().mergeFrom(data).buildParsed();
  }
  public static de.uniluebeck.itm.tr.iwsn.messages.NotificationEvent parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return newBuilder().mergeFrom(data, extensionRegistry)
             .buildParsed();
  }
  public static de.uniluebeck.itm.tr.iwsn.messages.NotificationEvent parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return newBuilder().mergeFrom(data).buildParsed();
  }
  public static de.uniluebeck.itm.tr.iwsn.messages.NotificationEvent parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return newBuilder().mergeFrom(data, extensionRegistry)
             .buildParsed();
  }
  public static de.uniluebeck.itm.tr.iwsn.messages.NotificationEvent parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return newBuilder().mergeFrom(input).buildParsed();
  }
  public static de.uniluebeck.itm.tr.iwsn.messages.NotificationEvent parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return newBuilder().mergeFrom(input, extensionRegistry)
             .buildParsed();
  }
  public static de.uniluebeck.itm.tr.iwsn.messages.NotificationEvent parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    Builder builder = newBuilder();
    if (builder.mergeDelimitedFrom(input)) {
      return builder.buildParsed();
    } else {
      return null;
    }
  }
  public static de.uniluebeck.itm.tr.iwsn.messages.NotificationEvent parseDelimitedFrom(
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
  public static de.uniluebeck.itm.tr.iwsn.messages.NotificationEvent parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return newBuilder().mergeFrom(input).buildParsed();
  }
  public static de.uniluebeck.itm.tr.iwsn.messages.NotificationEvent parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return newBuilder().mergeFrom(input, extensionRegistry)
             .buildParsed();
  }
  
  public static Builder newBuilder() { return Builder.create(); }
  public Builder newBuilderForType() { return newBuilder(); }
  public static Builder newBuilder(de.uniluebeck.itm.tr.iwsn.messages.NotificationEvent prototype) {
    return newBuilder().mergeFrom(prototype);
  }
  public Builder toBuilder() { return newBuilder(this); }
  
  public static final class Builder extends
      com.google.protobuf.GeneratedMessage.Builder<Builder> {
    private de.uniluebeck.itm.tr.iwsn.messages.NotificationEvent result;
    
    // Construct using de.uniluebeck.itm.tr.iwsn.messages.NotificationEvent.newBuilder()
    private Builder() {}
    
    private static Builder create() {
      Builder builder = new Builder();
      builder.result = new de.uniluebeck.itm.tr.iwsn.messages.NotificationEvent();
      return builder;
    }
    
    protected de.uniluebeck.itm.tr.iwsn.messages.NotificationEvent internalGetResult() {
      return result;
    }
    
    public Builder clear() {
      if (result == null) {
        throw new IllegalStateException(
          "Cannot call clear() after build().");
      }
      result = new de.uniluebeck.itm.tr.iwsn.messages.NotificationEvent();
      return this;
    }
    
    public Builder clone() {
      return create().mergeFrom(result);
    }
    
    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return de.uniluebeck.itm.tr.iwsn.messages.NotificationEvent.getDescriptor();
    }
    
    public de.uniluebeck.itm.tr.iwsn.messages.NotificationEvent getDefaultInstanceForType() {
      return de.uniluebeck.itm.tr.iwsn.messages.NotificationEvent.getDefaultInstance();
    }
    
    public boolean isInitialized() {
      return result.isInitialized();
    }
    public de.uniluebeck.itm.tr.iwsn.messages.NotificationEvent build() {
      if (result != null && !isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return buildPartial();
    }
    
    private de.uniluebeck.itm.tr.iwsn.messages.NotificationEvent buildParsed()
        throws com.google.protobuf.InvalidProtocolBufferException {
      if (!isInitialized()) {
        throw newUninitializedMessageException(
          result).asInvalidProtocolBufferException();
      }
      return buildPartial();
    }
    
    public de.uniluebeck.itm.tr.iwsn.messages.NotificationEvent buildPartial() {
      if (result == null) {
        throw new IllegalStateException(
          "build() has already been called on this Builder.");
      }
      de.uniluebeck.itm.tr.iwsn.messages.NotificationEvent returnMe = result;
      result = null;
      return returnMe;
    }
    
    public Builder mergeFrom(com.google.protobuf.Message other) {
      if (other instanceof de.uniluebeck.itm.tr.iwsn.messages.NotificationEvent) {
        return mergeFrom((de.uniluebeck.itm.tr.iwsn.messages.NotificationEvent)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }
    
    public Builder mergeFrom(de.uniluebeck.itm.tr.iwsn.messages.NotificationEvent other) {
      if (other == de.uniluebeck.itm.tr.iwsn.messages.NotificationEvent.getDefaultInstance()) return this;
      if (other.hasNodeUrn()) {
        setNodeUrn(other.getNodeUrn());
      }
      if (other.hasTimestamp()) {
        setTimestamp(other.getTimestamp());
      }
      if (other.hasMessage()) {
        setMessage(other.getMessage());
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
            setNodeUrn(input.readString());
            break;
          }
          case 16: {
            setTimestamp(input.readUInt64());
            break;
          }
          case 26: {
            setMessage(input.readString());
            break;
          }
        }
      }
    }
    
    
    // optional string nodeUrn = 1;
    public boolean hasNodeUrn() {
      return result.hasNodeUrn();
    }
    public java.lang.String getNodeUrn() {
      return result.getNodeUrn();
    }
    public Builder setNodeUrn(java.lang.String value) {
      if (value == null) {
    throw new NullPointerException();
  }
  result.hasNodeUrn = true;
      result.nodeUrn_ = value;
      return this;
    }
    public Builder clearNodeUrn() {
      result.hasNodeUrn = false;
      result.nodeUrn_ = getDefaultInstance().getNodeUrn();
      return this;
    }
    
    // required uint64 timestamp = 2;
    public boolean hasTimestamp() {
      return result.hasTimestamp();
    }
    public long getTimestamp() {
      return result.getTimestamp();
    }
    public Builder setTimestamp(long value) {
      result.hasTimestamp = true;
      result.timestamp_ = value;
      return this;
    }
    public Builder clearTimestamp() {
      result.hasTimestamp = false;
      result.timestamp_ = 0L;
      return this;
    }
    
    // required string message = 3;
    public boolean hasMessage() {
      return result.hasMessage();
    }
    public java.lang.String getMessage() {
      return result.getMessage();
    }
    public Builder setMessage(java.lang.String value) {
      if (value == null) {
    throw new NullPointerException();
  }
  result.hasMessage = true;
      result.message_ = value;
      return this;
    }
    public Builder clearMessage() {
      result.hasMessage = false;
      result.message_ = getDefaultInstance().getMessage();
      return this;
    }
    
    // @@protoc_insertion_point(builder_scope:de.uniluebeck.itm.tr.iwsn.messages.NotificationEvent)
  }
  
  static {
    defaultInstance = new NotificationEvent(true);
    de.uniluebeck.itm.tr.iwsn.messages.Messages.internalForceInit();
    defaultInstance.initFields();
  }
  
  // @@protoc_insertion_point(class_scope:de.uniluebeck.itm.tr.iwsn.messages.NotificationEvent)
}

