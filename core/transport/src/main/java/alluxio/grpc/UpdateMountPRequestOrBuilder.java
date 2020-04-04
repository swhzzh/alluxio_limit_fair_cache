// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: grpc/file_system_master.proto

package alluxio.grpc;

public interface UpdateMountPRequestOrBuilder extends
    // @@protoc_insertion_point(interface_extends:alluxio.grpc.file.UpdateMountPRequest)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <pre>
   ** the path of alluxio mount point 
   * </pre>
   *
   * <code>optional string alluxioPath = 1;</code>
   */
  boolean hasAlluxioPath();
  /**
   * <pre>
   ** the path of alluxio mount point 
   * </pre>
   *
   * <code>optional string alluxioPath = 1;</code>
   */
  java.lang.String getAlluxioPath();
  /**
   * <pre>
   ** the path of alluxio mount point 
   * </pre>
   *
   * <code>optional string alluxioPath = 1;</code>
   */
  com.google.protobuf.ByteString
      getAlluxioPathBytes();

  /**
   * <code>optional .alluxio.grpc.file.MountPOptions options = 3;</code>
   */
  boolean hasOptions();
  /**
   * <code>optional .alluxio.grpc.file.MountPOptions options = 3;</code>
   */
  alluxio.grpc.MountPOptions getOptions();
  /**
   * <code>optional .alluxio.grpc.file.MountPOptions options = 3;</code>
   */
  alluxio.grpc.MountPOptionsOrBuilder getOptionsOrBuilder();
}
