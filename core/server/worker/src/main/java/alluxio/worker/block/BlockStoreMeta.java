/*
 * The Alluxio Open Foundation licenses this work under the Apache License, version 2.0
 * (the "License"). You may not use this work except in compliance with the License, which is
 * available at www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied, as more fully set forth in the License.
 *
 * See the NOTICE file distributed with this work for information regarding copyright ownership.
 */

package alluxio.worker.block;

import alluxio.StorageTierAssoc;
import alluxio.collections.Pair;

import java.util.List;
import java.util.Map;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Interface for the block store meta in Alluxio.
 */
public interface BlockStoreMeta {

  /**
   * Factory for {@link BlockStoreMeta}.
   */
  @ThreadSafe
  class Factory{

    private Factory() {}

    /**
     * Factory method to return a BlockStoreMeta instance without blockIds.
     *
     * @param manager the BlockMetadataManager
     * @return BlockStoreMeta instance
     */
    public static BlockStoreMeta create(BlockMetadataManager manager) {
      return new DefaultBlockStoreMeta(manager, false);
    }

    /**
     * Factory method to return a BlockStoreMeta instance with blockIds.
     *
     * @param manager the BlockMetadata Manager
     * @return BlockStoreMeta instance
     */
    public static BlockStoreMeta createFull(BlockMetadataManager manager) {
      return new DefaultBlockStoreMeta(manager, true);
    }
  }
  /**
   * Note: This is only available in {@link BlockStoreMeta.Factory#createFull}.
   *
   * @return A mapping from storage tier alias to blocks
   */
  Map<String, List<Long>> getBlockList();

  Map<Long, Map<String, List<Long>>> getAllBlockList();
  //Map<String, List<Long>> getUserBlockList(long userId);

  /**
   * Note: This is only available in {@link BlockStoreMeta.Factory#createFull}.
   *
   * @return A mapping from storage location alias to blocks
   */
  Map<BlockStoreLocation, List<Long>> getBlockListByStorageLocation();
  Map<Long, Map<BlockStoreLocation, List<Long>>> getAllUserBlockListByStorageLocation();

  /**
   * @return the capacity in bytes
   */
  long getCapacityBytes();

  Map<Long, Long> getAllUserCapacityBytes();
  //long getUserCapacityBytes(long userId);

  /**
   * @return a mapping from tier aliases to capacity in bytes
   */
  Map<String, Long> getCapacityBytesOnTiers();
  Map<Long, Map<String, Long>> getAllUserCapacityBytesOnTiers();

  /**
   * @return a mapping from tier directory-path pairs to capacity in bytes
   */
  Map<Pair<String, String>, Long> getCapacityBytesOnDirs();
  Map<Long, Map<Pair<String, String>, Long>> getAllUserCapacityBytesOnDirs();

  /**
   * @return a mapping from tier aliases to directory paths in that tier
   */
  Map<String, List<String>> getDirectoryPathsOnTiers();

  /**
   * @return a mapping from tier alias to lost storage paths
   */
  Map<String, List<String>> getLostStorage();

  /**
   * Note: This is only available in {@link BlockStoreMeta.Factory#createFull}.
   *
   * @return the number of blocks
   */
  int getNumberOfBlocks();
//  int getNumberOfUserBlocks(long userId);
  Map<Long, Integer> getAllUserNumberOfBlocks();

  /**
   * @return the used capacity in bytes
   */
  long getUsedBytes();
  //long getUserUsedBytes(long userId);
  Map<Long, Long> getAllUserUsedBytes();

  /**
   * @return a mapping from tier aliases to used capacity in bytes
   */
  Map<String, Long> getUsedBytesOnTiers();
  Map<Long, Map<String, Long>> getAllUserUsedBytesOnTiers();

  /**
   * @return a mapping from tier directory-path pairs to used capacity in bytes
   */
  Map<Pair<String, String>, Long> getUsedBytesOnDirs();
  Map<Long, Map<Pair<String, String>, Long>> getUserUsedBytesOnDirs();

  /**
   * @return the storage tier mapping
   */
  StorageTierAssoc getStorageTierAssoc();
}
