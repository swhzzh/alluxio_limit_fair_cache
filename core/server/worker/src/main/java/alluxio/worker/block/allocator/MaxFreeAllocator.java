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

package alluxio.worker.block.allocator;

import alluxio.worker.block.BlockMetadataView;
import alluxio.worker.block.BlockStoreLocation;
import alluxio.worker.block.meta.StorageDir;
import alluxio.worker.block.meta.StorageDirView;
import alluxio.worker.block.meta.StorageTier;
import alluxio.worker.block.meta.StorageTierView;

import com.google.common.base.Preconditions;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * An allocator that allocates a block in the storage dir with most free space. It always allocates
 * to the highest tier if the requested block store location is any tier.
 */
@NotThreadSafe
public final class MaxFreeAllocator implements Allocator {
  private BlockMetadataView mMetadataView;

  /**
   * Creates a new instance of {@link MaxFreeAllocator}.
   *
   * @param view {@link BlockMetadataView} to pass to the allocator
   */
  public MaxFreeAllocator(BlockMetadataView view) {
    mMetadataView = Preconditions.checkNotNull(view, "view");
  }

  @Override
  public StorageDirView allocateBlockWithView(long sessionId, long blockSize,
      BlockStoreLocation location, BlockMetadataView metadataView) {
    mMetadataView = Preconditions.checkNotNull(metadataView, "view");
    return allocateBlock(sessionId, blockSize, location);
  }

  /**
   * Allocates a block from the given block store location. The location can be a specific location,
   * or {@link BlockStoreLocation#anyTier()} or {@link BlockStoreLocation#anyDirInTier(String)}.
   *
   * @param sessionId the id of session to apply for the block allocation
   * @param blockSize the size of block in bytes
   * @param location the location in block store
   * @return a {@link StorageDirView} in which to create the temp block meta if success,
   *         null otherwise
   */
  private StorageDirView allocateBlock(long sessionId, long blockSize,
      BlockStoreLocation location) {
    Preconditions.checkNotNull(location, "location");
    StorageDirView candidateDirView = null;

    if (location.equals(BlockStoreLocation.anyTier())) {
      for (StorageTierView tierView : mMetadataView.getTierViews()) {
        candidateDirView = getCandidateDirInTier(tierView, blockSize,
            BlockStoreLocation.ANY_MEDIUM);
        if (candidateDirView != null) {
          break;
        }
      }
    } else if (location.equals(BlockStoreLocation.anyDirInTier(location.tierAlias()))) {
      StorageTierView tierView = mMetadataView.getTierView(location.tierAlias());
      candidateDirView = getCandidateDirInTier(tierView, blockSize, BlockStoreLocation.ANY_MEDIUM);
    } else if (location.equals(BlockStoreLocation.anyDirInTierWithMedium(location.mediumType()))) {
      for (StorageTierView tierView : mMetadataView.getTierViews()) {
        candidateDirView = getCandidateDirInTier(tierView, blockSize, location.mediumType());
        if (candidateDirView != null) {
          break;
        }
      }
    } else {
      StorageTierView tierView = mMetadataView.getTierView(location.tierAlias());
      StorageDirView dirView = tierView.getDirView(location.dir());
      if (dirView.getAvailableBytes() >= blockSize) {
        candidateDirView = dirView;
      }
    }

    return candidateDirView;
  }

  /**
   * Finds a directory view in a tier view that has max free space and is able to store the block.
   *
   * @param tierView the storage tier view
   * @param blockSize the size of block in bytes
   * @param mediumType the medium type that must match
   * @return the storage directory view if found, null otherwise
   */
  private StorageDirView getCandidateDirInTier(StorageTierView tierView,
      long blockSize, String mediumType) {
    StorageDirView candidateDirView = null;
    long maxFreeBytes = blockSize - 1;
    for (StorageDirView dirView : tierView.getDirViews()) {
      if ((mediumType.equals(BlockStoreLocation.ANY_MEDIUM)
          || dirView.getMediumType().equals(mediumType))
          && dirView.getAvailableBytes() > maxFreeBytes) {
        maxFreeBytes = dirView.getAvailableBytes();
        candidateDirView = dirView;
      }
    }
    return candidateDirView;
  }

  @Override
  public StorageDirView allocateUserBlockWithView(long sessionid, long userId, long blockSize,
      BlockStoreLocation location, BlockMetadataView view) {
    mMetadataView = Preconditions.checkNotNull(view, "view");
    return allocateUserBlock(sessionid, userId, blockSize, location);
  }

  private StorageDirView allocateUserBlock(long sessionId, long userId, long blockSize,
      BlockStoreLocation location) {
    Preconditions.checkNotNull(location, "location");
    StorageDirView candidateDirView = null;

    if (location.equals(BlockStoreLocation.anyTier())) {
      for (StorageTierView tierView : mMetadataView.getTierViews()) {
        candidateDirView = getUserCandidateDirInTier(tierView, blockSize, userId,
            BlockStoreLocation.ANY_MEDIUM);
        if (candidateDirView != null) {
          break;
        }
      }
    } else if (location.equals(BlockStoreLocation.anyDirInTier(location.tierAlias()))) {
      StorageTierView tierView = mMetadataView.getTierView(location.tierAlias());
      candidateDirView = getUserCandidateDirInTier(tierView, blockSize, userId, BlockStoreLocation.ANY_MEDIUM);
    } else if (location.equals(BlockStoreLocation.anyDirInTierWithMedium(location.mediumType()))) {
      for (StorageTierView tierView : mMetadataView.getTierViews()) {
        candidateDirView = getUserCandidateDirInTier(tierView, blockSize, userId, location.mediumType());
        if (candidateDirView != null) {
          break;
        }
      }
    } else {
      StorageTierView tierView = mMetadataView.getTierView(location.tierAlias());
      StorageDirView dirView = tierView.getDirView(location.dir());
      if (dirView.getUserAvailableBytes(userId) >= blockSize) {
        candidateDirView = dirView;
      }
    }

    return candidateDirView;
  }

  private StorageDirView getUserCandidateDirInTier(StorageTierView tierView,
      long blockSize, long userId, String mediumType){
    StorageDirView candidateDirView = null;
    long maxFreeBytes = blockSize - 1;
    for (StorageDirView dirView : tierView.getDirViews()) {
      if ((mediumType.equals(BlockStoreLocation.ANY_MEDIUM) || dirView.getMediumType().equals(mediumType))
      && dirView.getUserAvailableBytes(userId) > maxFreeBytes){
        maxFreeBytes = dirView.getUserAvailableBytes(userId);
        candidateDirView = dirView;
      }
    }
    return candidateDirView;
  }
}
