// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: grpc/block_worker.proto

package alluxio.grpc;

/**
 * <pre>
 * The read response.
 * next available id: 2
 * </pre>
 *
 * Protobuf type {@code alluxio.grpc.block.ReadResponse}
 */
public  final class ReadResponse extends
    com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:alluxio.grpc.block.ReadResponse)
    ReadResponseOrBuilder {
private static final long serialVersionUID = 0L;
  // Use ReadResponse.newBuilder() to construct.
  private ReadResponse(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private ReadResponse() {
  }

  @java.lang.Override
  public final com.google.protobuf.UnknownFieldSet
  getUnknownFields() {
    return this.unknownFields;
  }
  private ReadResponse(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    this();
    if (extensionRegistry == null) {
      throw new java.lang.NullPointerException();
    }
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
            if (!parseUnknownField(
                input, unknownFields, extensionRegistry, tag)) {
              done = true;
            }
            break;
          }
          case 10: {
            alluxio.grpc.Chunk.Builder subBuilder = null;
            if (((bitField0_ & 0x00000001) == 0x00000001)) {
              subBuilder = chunk_.toBuilder();
            }
            chunk_ = input.readMessage(alluxio.grpc.Chunk.PARSER, extensionRegistry);
            if (subBuilder != null) {
              subBuilder.mergeFrom(chunk_);
              chunk_ = subBuilder.buildPartial();
            }
            bitField0_ |= 0x00000001;
            break;
          }
        }
      }
    } catch (com.google.protobuf.InvalidProtocolBufferException e) {
      throw e.setUnfinishedMessage(this);
    } catch (java.io.IOException e) {
      throw new com.google.protobuf.InvalidProtocolBufferException(
          e).setUnfinishedMessage(this);
    } finally {
      this.unknownFields = unknownFields.build();
      makeExtensionsImmutable();
    }
  }
  public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
    return alluxio.grpc.BlockWorkerProto.internal_static_alluxio_grpc_block_ReadResponse_descriptor;
  }

  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return alluxio.grpc.BlockWorkerProto.internal_static_alluxio_grpc_block_ReadResponse_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            alluxio.grpc.ReadResponse.class, alluxio.grpc.ReadResponse.Builder.class);
  }

  private int bitField0_;
  public static final int CHUNK_FIELD_NUMBER = 1;
  private alluxio.grpc.Chunk chunk_;
  /**
   * <code>optional .alluxio.grpc.block.Chunk chunk = 1;</code>
   */
  public boolean hasChunk() {
    return ((bitField0_ & 0x00000001) == 0x00000001);
  }
  /**
   * <code>optional .alluxio.grpc.block.Chunk chunk = 1;</code>
   */
  public alluxio.grpc.Chunk getChunk() {
    return chunk_ == null ? alluxio.grpc.Chunk.getDefaultInstance() : chunk_;
  }
  /**
   * <code>optional .alluxio.grpc.block.Chunk chunk = 1;</code>
   */
  public alluxio.grpc.ChunkOrBuilder getChunkOrBuilder() {
    return chunk_ == null ? alluxio.grpc.Chunk.getDefaultInstance() : chunk_;
  }

  private byte memoizedIsInitialized = -1;
  public final boolean isInitialized() {
    byte isInitialized = memoizedIsInitialized;
    if (isInitialized == 1) return true;
    if (isInitialized == 0) return false;

    memoizedIsInitialized = 1;
    return true;
  }

  public void writeTo(com.google.protobuf.CodedOutputStream output)
                      throws java.io.IOException {
    if (((bitField0_ & 0x00000001) == 0x00000001)) {
      output.writeMessage(1, getChunk());
    }
    unknownFields.writeTo(output);
  }

  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    if (((bitField0_ & 0x00000001) == 0x00000001)) {
      size += com.google.protobuf.CodedOutputStream
        .computeMessageSize(1, getChunk());
    }
    size += unknownFields.getSerializedSize();
    memoizedSize = size;
    return size;
  }

  @java.lang.Override
  public boolean equals(final java.lang.Object obj) {
    if (obj == this) {
     return true;
    }
    if (!(obj instanceof alluxio.grpc.ReadResponse)) {
      return super.equals(obj);
    }
    alluxio.grpc.ReadResponse other = (alluxio.grpc.ReadResponse) obj;

    boolean result = true;
    result = result && (hasChunk() == other.hasChunk());
    if (hasChunk()) {
      result = result && getChunk()
          .equals(other.getChunk());
    }
    result = result && unknownFields.equals(other.unknownFields);
    return result;
  }

  @java.lang.Override
  public int hashCode() {
    if (memoizedHashCode != 0) {
      return memoizedHashCode;
    }
    int hash = 41;
    hash = (19 * hash) + getDescriptor().hashCode();
    if (hasChunk()) {
      hash = (37 * hash) + CHUNK_FIELD_NUMBER;
      hash = (53 * hash) + getChunk().hashCode();
    }
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static alluxio.grpc.ReadResponse parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static alluxio.grpc.ReadResponse parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static alluxio.grpc.ReadResponse parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static alluxio.grpc.ReadResponse parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static alluxio.grpc.ReadResponse parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static alluxio.grpc.ReadResponse parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static alluxio.grpc.ReadResponse parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static alluxio.grpc.ReadResponse parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static alluxio.grpc.ReadResponse parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input);
  }
  public static alluxio.grpc.ReadResponse parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static alluxio.grpc.ReadResponse parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static alluxio.grpc.ReadResponse parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }

  public Builder newBuilderForType() { return newBuilder(); }
  public static Builder newBuilder() {
    return DEFAULT_INSTANCE.toBuilder();
  }
  public static Builder newBuilder(alluxio.grpc.ReadResponse prototype) {
    return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
  }
  public Builder toBuilder() {
    return this == DEFAULT_INSTANCE
        ? new Builder() : new Builder().mergeFrom(this);
  }

  @java.lang.Override
  protected Builder newBuilderForType(
      com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
    Builder builder = new Builder(parent);
    return builder;
  }
  /**
   * <pre>
   * The read response.
   * next available id: 2
   * </pre>
   *
   * Protobuf type {@code alluxio.grpc.block.ReadResponse}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:alluxio.grpc.block.ReadResponse)
      alluxio.grpc.ReadResponseOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return alluxio.grpc.BlockWorkerProto.internal_static_alluxio_grpc_block_ReadResponse_descriptor;
    }

    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return alluxio.grpc.BlockWorkerProto.internal_static_alluxio_grpc_block_ReadResponse_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              alluxio.grpc.ReadResponse.class, alluxio.grpc.ReadResponse.Builder.class);
    }

    // Construct using alluxio.grpc.ReadResponse.newBuilder()
    private Builder() {
      maybeForceBuilderInitialization();
    }

    private Builder(
        com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
      super(parent);
      maybeForceBuilderInitialization();
    }
    private void maybeForceBuilderInitialization() {
      if (com.google.protobuf.GeneratedMessageV3
              .alwaysUseFieldBuilders) {
        getChunkFieldBuilder();
      }
    }
    public Builder clear() {
      super.clear();
      if (chunkBuilder_ == null) {
        chunk_ = null;
      } else {
        chunkBuilder_.clear();
      }
      bitField0_ = (bitField0_ & ~0x00000001);
      return this;
    }

    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return alluxio.grpc.BlockWorkerProto.internal_static_alluxio_grpc_block_ReadResponse_descriptor;
    }

    public alluxio.grpc.ReadResponse getDefaultInstanceForType() {
      return alluxio.grpc.ReadResponse.getDefaultInstance();
    }

    public alluxio.grpc.ReadResponse build() {
      alluxio.grpc.ReadResponse result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    public alluxio.grpc.ReadResponse buildPartial() {
      alluxio.grpc.ReadResponse result = new alluxio.grpc.ReadResponse(this);
      int from_bitField0_ = bitField0_;
      int to_bitField0_ = 0;
      if (((from_bitField0_ & 0x00000001) == 0x00000001)) {
        to_bitField0_ |= 0x00000001;
      }
      if (chunkBuilder_ == null) {
        result.chunk_ = chunk_;
      } else {
        result.chunk_ = chunkBuilder_.build();
      }
      result.bitField0_ = to_bitField0_;
      onBuilt();
      return result;
    }

    public Builder clone() {
      return (Builder) super.clone();
    }
    public Builder setField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        java.lang.Object value) {
      return (Builder) super.setField(field, value);
    }
    public Builder clearField(
        com.google.protobuf.Descriptors.FieldDescriptor field) {
      return (Builder) super.clearField(field);
    }
    public Builder clearOneof(
        com.google.protobuf.Descriptors.OneofDescriptor oneof) {
      return (Builder) super.clearOneof(oneof);
    }
    public Builder setRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        int index, java.lang.Object value) {
      return (Builder) super.setRepeatedField(field, index, value);
    }
    public Builder addRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        java.lang.Object value) {
      return (Builder) super.addRepeatedField(field, value);
    }
    public Builder mergeFrom(com.google.protobuf.Message other) {
      if (other instanceof alluxio.grpc.ReadResponse) {
        return mergeFrom((alluxio.grpc.ReadResponse)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(alluxio.grpc.ReadResponse other) {
      if (other == alluxio.grpc.ReadResponse.getDefaultInstance()) return this;
      if (other.hasChunk()) {
        mergeChunk(other.getChunk());
      }
      this.mergeUnknownFields(other.unknownFields);
      onChanged();
      return this;
    }

    public final boolean isInitialized() {
      return true;
    }

    public Builder mergeFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      alluxio.grpc.ReadResponse parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (alluxio.grpc.ReadResponse) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }
    private int bitField0_;

    private alluxio.grpc.Chunk chunk_ = null;
    private com.google.protobuf.SingleFieldBuilderV3<
        alluxio.grpc.Chunk, alluxio.grpc.Chunk.Builder, alluxio.grpc.ChunkOrBuilder> chunkBuilder_;
    /**
     * <code>optional .alluxio.grpc.block.Chunk chunk = 1;</code>
     */
    public boolean hasChunk() {
      return ((bitField0_ & 0x00000001) == 0x00000001);
    }
    /**
     * <code>optional .alluxio.grpc.block.Chunk chunk = 1;</code>
     */
    public alluxio.grpc.Chunk getChunk() {
      if (chunkBuilder_ == null) {
        return chunk_ == null ? alluxio.grpc.Chunk.getDefaultInstance() : chunk_;
      } else {
        return chunkBuilder_.getMessage();
      }
    }
    /**
     * <code>optional .alluxio.grpc.block.Chunk chunk = 1;</code>
     */
    public Builder setChunk(alluxio.grpc.Chunk value) {
      if (chunkBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        chunk_ = value;
        onChanged();
      } else {
        chunkBuilder_.setMessage(value);
      }
      bitField0_ |= 0x00000001;
      return this;
    }
    /**
     * <code>optional .alluxio.grpc.block.Chunk chunk = 1;</code>
     */
    public Builder setChunk(
        alluxio.grpc.Chunk.Builder builderForValue) {
      if (chunkBuilder_ == null) {
        chunk_ = builderForValue.build();
        onChanged();
      } else {
        chunkBuilder_.setMessage(builderForValue.build());
      }
      bitField0_ |= 0x00000001;
      return this;
    }
    /**
     * <code>optional .alluxio.grpc.block.Chunk chunk = 1;</code>
     */
    public Builder mergeChunk(alluxio.grpc.Chunk value) {
      if (chunkBuilder_ == null) {
        if (((bitField0_ & 0x00000001) == 0x00000001) &&
            chunk_ != null &&
            chunk_ != alluxio.grpc.Chunk.getDefaultInstance()) {
          chunk_ =
            alluxio.grpc.Chunk.newBuilder(chunk_).mergeFrom(value).buildPartial();
        } else {
          chunk_ = value;
        }
        onChanged();
      } else {
        chunkBuilder_.mergeFrom(value);
      }
      bitField0_ |= 0x00000001;
      return this;
    }
    /**
     * <code>optional .alluxio.grpc.block.Chunk chunk = 1;</code>
     */
    public Builder clearChunk() {
      if (chunkBuilder_ == null) {
        chunk_ = null;
        onChanged();
      } else {
        chunkBuilder_.clear();
      }
      bitField0_ = (bitField0_ & ~0x00000001);
      return this;
    }
    /**
     * <code>optional .alluxio.grpc.block.Chunk chunk = 1;</code>
     */
    public alluxio.grpc.Chunk.Builder getChunkBuilder() {
      bitField0_ |= 0x00000001;
      onChanged();
      return getChunkFieldBuilder().getBuilder();
    }
    /**
     * <code>optional .alluxio.grpc.block.Chunk chunk = 1;</code>
     */
    public alluxio.grpc.ChunkOrBuilder getChunkOrBuilder() {
      if (chunkBuilder_ != null) {
        return chunkBuilder_.getMessageOrBuilder();
      } else {
        return chunk_ == null ?
            alluxio.grpc.Chunk.getDefaultInstance() : chunk_;
      }
    }
    /**
     * <code>optional .alluxio.grpc.block.Chunk chunk = 1;</code>
     */
    private com.google.protobuf.SingleFieldBuilderV3<
        alluxio.grpc.Chunk, alluxio.grpc.Chunk.Builder, alluxio.grpc.ChunkOrBuilder> 
        getChunkFieldBuilder() {
      if (chunkBuilder_ == null) {
        chunkBuilder_ = new com.google.protobuf.SingleFieldBuilderV3<
            alluxio.grpc.Chunk, alluxio.grpc.Chunk.Builder, alluxio.grpc.ChunkOrBuilder>(
                getChunk(),
                getParentForChildren(),
                isClean());
        chunk_ = null;
      }
      return chunkBuilder_;
    }
    public final Builder setUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.setUnknownFields(unknownFields);
    }

    public final Builder mergeUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.mergeUnknownFields(unknownFields);
    }


    // @@protoc_insertion_point(builder_scope:alluxio.grpc.block.ReadResponse)
  }

  // @@protoc_insertion_point(class_scope:alluxio.grpc.block.ReadResponse)
  private static final alluxio.grpc.ReadResponse DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new alluxio.grpc.ReadResponse();
  }

  public static alluxio.grpc.ReadResponse getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  @java.lang.Deprecated public static final com.google.protobuf.Parser<ReadResponse>
      PARSER = new com.google.protobuf.AbstractParser<ReadResponse>() {
    public ReadResponse parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return new ReadResponse(input, extensionRegistry);
    }
  };

  public static com.google.protobuf.Parser<ReadResponse> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<ReadResponse> getParserForType() {
    return PARSER;
  }

  public alluxio.grpc.ReadResponse getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

