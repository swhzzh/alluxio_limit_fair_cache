// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: grpc/table/table_master.proto

package alluxio.grpc.table;

public interface ColumnStatisticsDataOrBuilder extends
    // @@protoc_insertion_point(interface_extends:alluxio.grpc.table.ColumnStatisticsData)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>optional .alluxio.grpc.table.BooleanColumnStatsData boolean_stats = 1;</code>
   */
  boolean hasBooleanStats();
  /**
   * <code>optional .alluxio.grpc.table.BooleanColumnStatsData boolean_stats = 1;</code>
   */
  alluxio.grpc.table.BooleanColumnStatsData getBooleanStats();
  /**
   * <code>optional .alluxio.grpc.table.BooleanColumnStatsData boolean_stats = 1;</code>
   */
  alluxio.grpc.table.BooleanColumnStatsDataOrBuilder getBooleanStatsOrBuilder();

  /**
   * <code>optional .alluxio.grpc.table.LongColumnStatsData long_stats = 2;</code>
   */
  boolean hasLongStats();
  /**
   * <code>optional .alluxio.grpc.table.LongColumnStatsData long_stats = 2;</code>
   */
  alluxio.grpc.table.LongColumnStatsData getLongStats();
  /**
   * <code>optional .alluxio.grpc.table.LongColumnStatsData long_stats = 2;</code>
   */
  alluxio.grpc.table.LongColumnStatsDataOrBuilder getLongStatsOrBuilder();

  /**
   * <code>optional .alluxio.grpc.table.DoubleColumnStatsData double_stats = 3;</code>
   */
  boolean hasDoubleStats();
  /**
   * <code>optional .alluxio.grpc.table.DoubleColumnStatsData double_stats = 3;</code>
   */
  alluxio.grpc.table.DoubleColumnStatsData getDoubleStats();
  /**
   * <code>optional .alluxio.grpc.table.DoubleColumnStatsData double_stats = 3;</code>
   */
  alluxio.grpc.table.DoubleColumnStatsDataOrBuilder getDoubleStatsOrBuilder();

  /**
   * <code>optional .alluxio.grpc.table.StringColumnStatsData string_stats = 4;</code>
   */
  boolean hasStringStats();
  /**
   * <code>optional .alluxio.grpc.table.StringColumnStatsData string_stats = 4;</code>
   */
  alluxio.grpc.table.StringColumnStatsData getStringStats();
  /**
   * <code>optional .alluxio.grpc.table.StringColumnStatsData string_stats = 4;</code>
   */
  alluxio.grpc.table.StringColumnStatsDataOrBuilder getStringStatsOrBuilder();

  /**
   * <code>optional .alluxio.grpc.table.BinaryColumnStatsData binary_stats = 5;</code>
   */
  boolean hasBinaryStats();
  /**
   * <code>optional .alluxio.grpc.table.BinaryColumnStatsData binary_stats = 5;</code>
   */
  alluxio.grpc.table.BinaryColumnStatsData getBinaryStats();
  /**
   * <code>optional .alluxio.grpc.table.BinaryColumnStatsData binary_stats = 5;</code>
   */
  alluxio.grpc.table.BinaryColumnStatsDataOrBuilder getBinaryStatsOrBuilder();

  /**
   * <code>optional .alluxio.grpc.table.DecimalColumnStatsData decimal_stats = 6;</code>
   */
  boolean hasDecimalStats();
  /**
   * <code>optional .alluxio.grpc.table.DecimalColumnStatsData decimal_stats = 6;</code>
   */
  alluxio.grpc.table.DecimalColumnStatsData getDecimalStats();
  /**
   * <code>optional .alluxio.grpc.table.DecimalColumnStatsData decimal_stats = 6;</code>
   */
  alluxio.grpc.table.DecimalColumnStatsDataOrBuilder getDecimalStatsOrBuilder();

  /**
   * <code>optional .alluxio.grpc.table.DateColumnStatsData date_stats = 7;</code>
   */
  boolean hasDateStats();
  /**
   * <code>optional .alluxio.grpc.table.DateColumnStatsData date_stats = 7;</code>
   */
  alluxio.grpc.table.DateColumnStatsData getDateStats();
  /**
   * <code>optional .alluxio.grpc.table.DateColumnStatsData date_stats = 7;</code>
   */
  alluxio.grpc.table.DateColumnStatsDataOrBuilder getDateStatsOrBuilder();

  public alluxio.grpc.table.ColumnStatisticsData.DataCase getDataCase();
}
