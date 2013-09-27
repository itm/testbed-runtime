// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: src/main/resources/iwsn-messages.proto

package de.uniluebeck.itm.tr.iwsn.messages;

/**
 * Protobuf type {@code de.uniluebeck.itm.tr.iwsn.messages.DevicesDetachedEvent}
 */
public  final class DevicesDetachedEvent extends
    com.google.protobuf.GeneratedMessage
    implements DevicesDetachedEventOrBuilder {
  // Use DevicesDetachedEvent.newBuilder() to construct.
  private DevicesDetachedEvent(com.google.protobuf.GeneratedMessage.Builder<?> builder) {
    super(builder);
    this.unknownFields = builder.getUnknownFields();
  }
  private DevicesDetachedEvent(boolean noInit) { this.unknownFields = com.google.protobuf.UnknownFieldSet.getDefaultInstance(); }

  private static final DevicesDetachedEvent defaultInstance;
  public static DevicesDetachedEvent getDefaultInstance() {
    return defaultInstance;
  }

  public DevicesDetachedEvent getDefaultInstanceForType() {
    return defaultInstance;
  }

  private final com.google.protobuf.UnknownFieldSet unknownFields;
  @java.lang.Override
  public final com.google.protobuf.UnknownFieldSet
      getUnknownFields() {
    return this.unknownFields;
  }
  private DevicesDetachedEvent(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    initFields();
    int mutable_bitField0_ = 0;
    com.google.protobuf.UnknownFieldSet.Builder unknownFields =
        com.google.protobuf.UnknownFieldSet.newBuilder();
    try {
      boolean done = false;
      while (!done) {
        int tag = input.readTag();
        switch (tag) {
          case 0:
            done = true;
            break;
          default: {
            if (!parseUnknownField(input, unknownFields,
                                   extensionRegistry, tag)) {
              done = true;
            }
            break;
          }
          case 10: {
            if (!((mutable_bitField0_ & 0x00000001) == 0x00000001)) {
              nodeUrns_ = new com.google.protobuf.LazyStringArrayList();
              mutable_bitField0_ |= 0x00000001;
            }
            nodeUrns_.add(input.readBytes());
            break;
          }
          case 16: {
            bitField0_ |= 0x00000001;
            timestamp_ = input.readUInt64();
            break;
          }
        }
      }
    } catch (com.google.protobuf.InvalidProtocolBufferException e) {
      throw e.setUnfinishedMessage(this);
    } catch (java.io.IOException e) {
      throw new com.google.protobuf.InvalidProtocolBufferException(
          e.getMessage()).setUnfinishedMessage(this);
    } finally {
      if (((mutable_bitField0_ & 0x00000001) == 0x00000001)) {
        nodeUrns_ = new com.google.protobuf.UnmodifiableLazyStringList(nodeUrns_);
      }
      this.unknownFields = unknownFields.build();
      makeExtensionsImmutable();
    }
  }
  public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
    return de.uniluebeck.itm.tr.iwsn.messages.Messages.internal_static_de_uniluebeck_itm_tr_iwsn_messages_DevicesDetachedEvent_descriptor;
  }

  protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return de.uniluebeck.itm.tr.iwsn.messages.Messages.internal_static_de_uniluebeck_itm_tr_iwsn_messages_DevicesDetachedEvent_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            de.uniluebeck.itm.tr.iwsn.messages.DevicesDetachedEvent.class, de.uniluebeck.itm.tr.iwsn.messages.DevicesDetachedEvent.Builder.class);
  }

  public static com.google.protobuf.Parser<DevicesDetachedEvent> PARSER =
      new com.google.protobuf.AbstractParser<DevicesDetachedEvent>() {
    public DevicesDetachedEvent parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return new DevicesDetachedEvent(input, extensionRegistry);
    }
  };

  @java.lang.Override
  public com.google.protobuf.Parser<DevicesDetachedEvent> getParserForType() {
    return PARSER;
  }

  private int bitField0_;
  // repeated string nodeUrns = 1;
  public static final int NODEURNS_FIELD_NUMBER = 1;
  private com.google.protobuf.LazyStringList nodeUrns_;
  /**
   * <code>repeated string nodeUrns = 1;</code>
   */
  public java.util.List<java.lang.String>
      getNodeUrnsList() {
    return nodeUrns_;
  }
  /**
   * <code>repeated string nodeUrns = 1;</code>
   */
  public int getNodeUrnsCount() {
    return nodeUrns_.size();
  }
  /**
   * <code>repeated string nodeUrns = 1;</code>
   */
  public java.lang.String getNodeUrns(int index) {
    return nodeUrns_.get(index);
  }
  /**
   * <code>repeated string nodeUrns = 1;</code>
   */
  public com.google.protobuf.ByteString
      getNodeUrnsBytes(int index) {
    return nodeUrns_.getByteString(index);
  }

  // required uint64 timestamp = 2;
  public static final int TIMESTAMP_FIELD_NUMBER = 2;
  private long timestamp_;
  /**
   * <code>required uint64 timestamp = 2;</code>
   */
  public boolean hasTimestamp() {
    return ((bitField0_ & 0x00000001) == 0x00000001);
  }
  /**
   * <code>required uint64 timestamp = 2;</code>
   */
  public long getTimestamp() {
    return timestamp_;
  }

  private void initFields() {
    nodeUrns_ = com.google.protobuf.LazyStringArrayList.EMPTY;
    timestamp_ = 0L;
  }
  private byte memoizedIsInitialized = -1;
  public final boolean isInitialized() {
    byte isInitialized = memoizedIsInitialized;
    if (isInitialized != -1) return isInitialized == 1;

    if (!hasTimestamp()) {
      memoizedIsInitialized = 0;
      return false;
    }
    memoizedIsInitialized = 1;
    return true;
  }

  public void writeTo(com.google.protobuf.CodedOutputStream output)
                      throws java.io.IOException {
    getSerializedSize();
    for (int i = 0; i < nodeUrns_.size(); i++) {
      output.writeBytes(1, nodeUrns_.getByteString(i));
    }
    if (((bitField0_ & 0x00000001) == 0x00000001)) {
      output.writeUInt64(2, timestamp_);
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
      for (int i = 0; i < nodeUrns_.size(); i++) {
        dataSize += com.google.protobuf.CodedOutputStream
          .computeBytesSizeNoTag(nodeUrns_.getByteString(i));
      }
      size += dataSize;
      size += 1 * getNodeUrnsList().size();
    }
    if (((bitField0_ & 0x00000001) == 0x00000001)) {
      size += com.google.protobuf.CodedOutputStream
        .computeUInt64Size(2, timestamp_);
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

  public static de.uniluebeck.itm.tr.iwsn.messages.DevicesDetachedEvent parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static de.uniluebeck.itm.tr.iwsn.messages.DevicesDetachedEvent parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static de.uniluebeck.itm.tr.iwsn.messages.DevicesDetachedEvent parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static de.uniluebeck.itm.tr.iwsn.messages.DevicesDetachedEvent parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static de.uniluebeck.itm.tr.iwsn.messages.DevicesDetachedEvent parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return PARSER.parseFrom(input);
  }
  public static de.uniluebeck.itm.tr.iwsn.messages.DevicesDetachedEvent parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return PARSER.parseFrom(input, extensionRegistry);
  }
  public static de.uniluebeck.itm.tr.iwsn.messages.DevicesDetachedEvent parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return PARSER.parseDelimitedFrom(input);
  }
  public static de.uniluebeck.itm.tr.iwsn.messages.DevicesDetachedEvent parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return PARSER.parseDelimitedFrom(input, extensionRegistry);
  }
  public static de.uniluebeck.itm.tr.iwsn.messages.DevicesDetachedEvent parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return PARSER.parseFrom(input);
  }
  public static de.uniluebeck.itm.tr.iwsn.messages.DevicesDetachedEvent parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return PARSER.parseFrom(input, extensionRegistry);
  }

  public static Builder newBuilder() { return Builder.create(); }
  public Builder newBuilderForType() { return newBuilder(); }
  public static Builder newBuilder(de.uniluebeck.itm.tr.iwsn.messages.DevicesDetachedEvent prototype) {
    return newBuilder().mergeFrom(prototype);
  }
  public Builder toBuilder() { return newBuilder(this); }

  @java.lang.Override
  protected Builder newBuilderForType(
      com.google.protobuf.GeneratedMessage.BuilderParent parent) {
    Builder builder = new Builder(parent);
    return builder;
  }
  /**
   * Protobuf type {@code de.uniluebeck.itm.tr.iwsn.messages.DevicesDetachedEvent}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessage.Builder<Builder>
     implements de.uniluebeck.itm.tr.iwsn.messages.DevicesDetachedEventOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return de.uniluebeck.itm.tr.iwsn.messages.Messages.internal_static_de_uniluebeck_itm_tr_iwsn_messages_DevicesDetachedEvent_descriptor;
    }

    protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return de.uniluebeck.itm.tr.iwsn.messages.Messages.internal_static_de_uniluebeck_itm_tr_iwsn_messages_DevicesDetachedEvent_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              de.uniluebeck.itm.tr.iwsn.messages.DevicesDetachedEvent.class, de.uniluebeck.itm.tr.iwsn.messages.DevicesDetachedEvent.Builder.class);
    }

    // Construct using de.uniluebeck.itm.tr.iwsn.messages.DevicesDetachedEvent.newBuilder()
    private Builder() {
      maybeForceBuilderInitialization();
    }

    private Builder(
        com.google.protobuf.GeneratedMessage.BuilderParent parent) {
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
      nodeUrns_ = com.google.protobuf.LazyStringArrayList.EMPTY;
      bitField0_ = (bitField0_ & ~0x00000001);
      timestamp_ = 0L;
      bitField0_ = (bitField0_ & ~0x00000002);
      return this;
    }

    public Builder clone() {
      return create().mergeFrom(buildPartial());
    }

    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return de.uniluebeck.itm.tr.iwsn.messages.Messages.internal_static_de_uniluebeck_itm_tr_iwsn_messages_DevicesDetachedEvent_descriptor;
    }

    public de.uniluebeck.itm.tr.iwsn.messages.DevicesDetachedEvent getDefaultInstanceForType() {
      return de.uniluebeck.itm.tr.iwsn.messages.DevicesDetachedEvent.getDefaultInstance();
    }

    public de.uniluebeck.itm.tr.iwsn.messages.DevicesDetachedEvent build() {
      de.uniluebeck.itm.tr.iwsn.messages.DevicesDetachedEvent result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    public de.uniluebeck.itm.tr.iwsn.messages.DevicesDetachedEvent buildPartial() {
      de.uniluebeck.itm.tr.iwsn.messages.DevicesDetachedEvent result = new de.uniluebeck.itm.tr.iwsn.messages.DevicesDetachedEvent(this);
      int from_bitField0_ = bitField0_;
      int to_bitField0_ = 0;
      if (((bitField0_ & 0x00000001) == 0x00000001)) {
        nodeUrns_ = new com.google.protobuf.UnmodifiableLazyStringList(
            nodeUrns_);
        bitField0_ = (bitField0_ & ~0x00000001);
      }
      result.nodeUrns_ = nodeUrns_;
      if (((from_bitField0_ & 0x00000002) == 0x00000002)) {
        to_bitField0_ |= 0x00000001;
      }
      result.timestamp_ = timestamp_;
      result.bitField0_ = to_bitField0_;
      onBuilt();
      return result;
    }

    public Builder mergeFrom(com.google.protobuf.Message other) {
      if (other instanceof de.uniluebeck.itm.tr.iwsn.messages.DevicesDetachedEvent) {
        return mergeFrom((de.uniluebeck.itm.tr.iwsn.messages.DevicesDetachedEvent)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(de.uniluebeck.itm.tr.iwsn.messages.DevicesDetachedEvent other) {
      if (other == de.uniluebeck.itm.tr.iwsn.messages.DevicesDetachedEvent.getDefaultInstance()) return this;
      if (!other.nodeUrns_.isEmpty()) {
        if (nodeUrns_.isEmpty()) {
          nodeUrns_ = other.nodeUrns_;
          bitField0_ = (bitField0_ & ~0x00000001);
        } else {
          ensureNodeUrnsIsMutable();
          nodeUrns_.addAll(other.nodeUrns_);
        }
        onChanged();
      }
      if (other.hasTimestamp()) {
        setTimestamp(other.getTimestamp());
      }
      this.mergeUnknownFields(other.getUnknownFields());
      return this;
    }

    public final boolean isInitialized() {
      if (!hasTimestamp()) {
        
        return false;
      }
      return true;
    }

    public Builder mergeFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      de.uniluebeck.itm.tr.iwsn.messages.DevicesDetachedEvent parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (de.uniluebeck.itm.tr.iwsn.messages.DevicesDetachedEvent) e.getUnfinishedMessage();
        throw e;
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }
    private int bitField0_;

    // repeated string nodeUrns = 1;
    private com.google.protobuf.LazyStringList nodeUrns_ = com.google.protobuf.LazyStringArrayList.EMPTY;
    private void ensureNodeUrnsIsMutable() {
      if (!((bitField0_ & 0x00000001) == 0x00000001)) {
        nodeUrns_ = new com.google.protobuf.LazyStringArrayList(nodeUrns_);
        bitField0_ |= 0x00000001;
       }
    }
    /**
     * <code>repeated string nodeUrns = 1;</code>
     */
    public java.util.List<java.lang.String>
        getNodeUrnsList() {
      return java.util.Collections.unmodifiableList(nodeUrns_);
    }
    /**
     * <code>repeated string nodeUrns = 1;</code>
     */
    public int getNodeUrnsCount() {
      return nodeUrns_.size();
    }
    /**
     * <code>repeated string nodeUrns = 1;</code>
     */
    public java.lang.String getNodeUrns(int index) {
      return nodeUrns_.get(index);
    }
    /**
     * <code>repeated string nodeUrns = 1;</code>
     */
    public com.google.protobuf.ByteString
        getNodeUrnsBytes(int index) {
      return nodeUrns_.getByteString(index);
    }
    /**
     * <code>repeated string nodeUrns = 1;</code>
     */
    public Builder setNodeUrns(
        int index, java.lang.String value) {
      if (value == null) {
    throw new NullPointerException();
  }
  ensureNodeUrnsIsMutable();
      nodeUrns_.set(index, value);
      onChanged();
      return this;
    }
    /**
     * <code>repeated string nodeUrns = 1;</code>
     */
    public Builder addNodeUrns(
        java.lang.String value) {
      if (value == null) {
    throw new NullPointerException();
  }
  ensureNodeUrnsIsMutable();
      nodeUrns_.add(value);
      onChanged();
      return this;
    }
    /**
     * <code>repeated string nodeUrns = 1;</code>
     */
    public Builder addAllNodeUrns(
        java.lang.Iterable<java.lang.String> values) {
      ensureNodeUrnsIsMutable();
      super.addAll(values, nodeUrns_);
      onChanged();
      return this;
    }
    /**
     * <code>repeated string nodeUrns = 1;</code>
     */
    public Builder clearNodeUrns() {
      nodeUrns_ = com.google.protobuf.LazyStringArrayList.EMPTY;
      bitField0_ = (bitField0_ & ~0x00000001);
      onChanged();
      return this;
    }
    /**
     * <code>repeated string nodeUrns = 1;</code>
     */
    public Builder addNodeUrnsBytes(
        com.google.protobuf.ByteString value) {
      if (value == null) {
    throw new NullPointerException();
  }
  ensureNodeUrnsIsMutable();
      nodeUrns_.add(value);
      onChanged();
      return this;
    }

    // required uint64 timestamp = 2;
    private long timestamp_ ;
    /**
     * <code>required uint64 timestamp = 2;</code>
     */
    public boolean hasTimestamp() {
      return ((bitField0_ & 0x00000002) == 0x00000002);
    }
    /**
     * <code>required uint64 timestamp = 2;</code>
     */
    public long getTimestamp() {
      return timestamp_;
    }
    /**
     * <code>required uint64 timestamp = 2;</code>
     */
    public Builder setTimestamp(long value) {
      bitField0_ |= 0x00000002;
      timestamp_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>required uint64 timestamp = 2;</code>
     */
    public Builder clearTimestamp() {
      bitField0_ = (bitField0_ & ~0x00000002);
      timestamp_ = 0L;
      onChanged();
      return this;
    }

    // @@protoc_insertion_point(builder_scope:de.uniluebeck.itm.tr.iwsn.messages.DevicesDetachedEvent)
  }

  static {
    defaultInstance = new DevicesDetachedEvent(true);
    defaultInstance.initFields();
  }

  // @@protoc_insertion_point(class_scope:de.uniluebeck.itm.tr.iwsn.messages.DevicesDetachedEvent)
}

