// Generated by the protocol buffer compiler.  DO NOT EDIT!

package de.uniluebeck.itm.tr.iwsn.messages;

public  final class EventAck extends
    com.google.protobuf.GeneratedMessage
    implements EventAckOrBuilder {
  // Use EventAck.newBuilder() to construct.
  private EventAck(Builder builder) {
    super(builder);
  }
  private EventAck(boolean noInit) {}
  
  private static final EventAck defaultInstance;
  public static EventAck getDefaultInstance() {
    return defaultInstance;
  }
  
  public EventAck getDefaultInstanceForType() {
    return defaultInstance;
  }
  
  public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
    return de.uniluebeck.itm.tr.iwsn.messages.Messages.internal_static_de_uniluebeck_itm_tr_iwsn_messages_EventAck_descriptor;
  }
  
  protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return de.uniluebeck.itm.tr.iwsn.messages.Messages.internal_static_de_uniluebeck_itm_tr_iwsn_messages_EventAck_fieldAccessorTable;
  }
  
  private int bitField0_;
  // required int64 eventId = 1;
  public static final int EVENTID_FIELD_NUMBER = 1;
  private long eventId_;
  public boolean hasEventId() {
    return ((bitField0_ & 0x00000001) == 0x00000001);
  }
  public long getEventId() {
    return eventId_;
  }
  
  private void initFields() {
    eventId_ = 0L;
  }
  private byte memoizedIsInitialized = -1;
  public final boolean isInitialized() {
    byte isInitialized = memoizedIsInitialized;
    if (isInitialized != -1) return isInitialized == 1;
    
    if (!hasEventId()) {
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
      output.writeInt64(1, eventId_);
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
        .computeInt64Size(1, eventId_);
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
  
  public static de.uniluebeck.itm.tr.iwsn.messages.EventAck parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return newBuilder().mergeFrom(data).buildParsed();
  }
  public static de.uniluebeck.itm.tr.iwsn.messages.EventAck parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return newBuilder().mergeFrom(data, extensionRegistry)
             .buildParsed();
  }
  public static de.uniluebeck.itm.tr.iwsn.messages.EventAck parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return newBuilder().mergeFrom(data).buildParsed();
  }
  public static de.uniluebeck.itm.tr.iwsn.messages.EventAck parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return newBuilder().mergeFrom(data, extensionRegistry)
             .buildParsed();
  }
  public static de.uniluebeck.itm.tr.iwsn.messages.EventAck parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return newBuilder().mergeFrom(input).buildParsed();
  }
  public static de.uniluebeck.itm.tr.iwsn.messages.EventAck parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return newBuilder().mergeFrom(input, extensionRegistry)
             .buildParsed();
  }
  public static de.uniluebeck.itm.tr.iwsn.messages.EventAck parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    Builder builder = newBuilder();
    if (builder.mergeDelimitedFrom(input)) {
      return builder.buildParsed();
    } else {
      return null;
    }
  }
  public static de.uniluebeck.itm.tr.iwsn.messages.EventAck parseDelimitedFrom(
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
  public static de.uniluebeck.itm.tr.iwsn.messages.EventAck parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return newBuilder().mergeFrom(input).buildParsed();
  }
  public static de.uniluebeck.itm.tr.iwsn.messages.EventAck parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return newBuilder().mergeFrom(input, extensionRegistry)
             .buildParsed();
  }
  
  public static Builder newBuilder() { return Builder.create(); }
  public Builder newBuilderForType() { return newBuilder(); }
  public static Builder newBuilder(de.uniluebeck.itm.tr.iwsn.messages.EventAck prototype) {
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
     implements de.uniluebeck.itm.tr.iwsn.messages.EventAckOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return de.uniluebeck.itm.tr.iwsn.messages.Messages.internal_static_de_uniluebeck_itm_tr_iwsn_messages_EventAck_descriptor;
    }
    
    protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return de.uniluebeck.itm.tr.iwsn.messages.Messages.internal_static_de_uniluebeck_itm_tr_iwsn_messages_EventAck_fieldAccessorTable;
    }
    
    // Construct using de.uniluebeck.itm.tr.iwsn.messages.EventAck.newBuilder()
    private Builder() {
      maybeForceBuilderInitialization();
    }
    
    private Builder(BuilderParent parent) {
      super(parent);
      maybeForceBuilderInitialization();
    }
    private void maybeForceBuilderInitialization() {
      if (com.google.protobuf.GeneratedMessage.alwaysUseFieldBuilders) {
      }
    }
    private static Builder create() {
      return new Builder();
    }
    
    public Builder clear() {
      super.clear();
      eventId_ = 0L;
      bitField0_ = (bitField0_ & ~0x00000001);
      return this;
    }
    
    public Builder clone() {
      return create().mergeFrom(buildPartial());
    }
    
    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return de.uniluebeck.itm.tr.iwsn.messages.EventAck.getDescriptor();
    }
    
    public de.uniluebeck.itm.tr.iwsn.messages.EventAck getDefaultInstanceForType() {
      return de.uniluebeck.itm.tr.iwsn.messages.EventAck.getDefaultInstance();
    }
    
    public de.uniluebeck.itm.tr.iwsn.messages.EventAck build() {
      de.uniluebeck.itm.tr.iwsn.messages.EventAck result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }
    
    private de.uniluebeck.itm.tr.iwsn.messages.EventAck buildParsed()
        throws com.google.protobuf.InvalidProtocolBufferException {
      de.uniluebeck.itm.tr.iwsn.messages.EventAck result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(
          result).asInvalidProtocolBufferException();
      }
      return result;
    }
    
    public de.uniluebeck.itm.tr.iwsn.messages.EventAck buildPartial() {
      de.uniluebeck.itm.tr.iwsn.messages.EventAck result = new de.uniluebeck.itm.tr.iwsn.messages.EventAck(this);
      int from_bitField0_ = bitField0_;
      int to_bitField0_ = 0;
      if (((from_bitField0_ & 0x00000001) == 0x00000001)) {
        to_bitField0_ |= 0x00000001;
      }
      result.eventId_ = eventId_;
      result.bitField0_ = to_bitField0_;
      onBuilt();
      return result;
    }
    
    public Builder mergeFrom(com.google.protobuf.Message other) {
      if (other instanceof de.uniluebeck.itm.tr.iwsn.messages.EventAck) {
        return mergeFrom((de.uniluebeck.itm.tr.iwsn.messages.EventAck)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }
    
    public Builder mergeFrom(de.uniluebeck.itm.tr.iwsn.messages.EventAck other) {
      if (other == de.uniluebeck.itm.tr.iwsn.messages.EventAck.getDefaultInstance()) return this;
      if (other.hasEventId()) {
        setEventId(other.getEventId());
      }
      this.mergeUnknownFields(other.getUnknownFields());
      return this;
    }
    
    public final boolean isInitialized() {
      if (!hasEventId()) {
        
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
          case 8: {
            bitField0_ |= 0x00000001;
            eventId_ = input.readInt64();
            break;
          }
        }
      }
    }
    
    private int bitField0_;
    
    // required int64 eventId = 1;
    private long eventId_ ;
    public boolean hasEventId() {
      return ((bitField0_ & 0x00000001) == 0x00000001);
    }
    public long getEventId() {
      return eventId_;
    }
    public Builder setEventId(long value) {
      bitField0_ |= 0x00000001;
      eventId_ = value;
      onChanged();
      return this;
    }
    public Builder clearEventId() {
      bitField0_ = (bitField0_ & ~0x00000001);
      eventId_ = 0L;
      onChanged();
      return this;
    }
    
    // @@protoc_insertion_point(builder_scope:de.uniluebeck.itm.tr.iwsn.messages.EventAck)
  }
  
  static {
    defaultInstance = new EventAck(true);
    defaultInstance.initFields();
  }
  
  // @@protoc_insertion_point(class_scope:de.uniluebeck.itm.tr.iwsn.messages.EventAck)
}

