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

package alluxio.worker.block.evictor;

import alluxio.Sessions;
import alluxio.collections.Pair;
import alluxio.exception.BlockDoesNotExistException;
import alluxio.worker.block.AbstractBlockStoreEventListener;
import alluxio.worker.block.BlockMetadataEvictorView;
import alluxio.worker.block.BlockStoreLocation;
import alluxio.worker.block.allocator.Allocator;
import alluxio.worker.block.meta.BlockMeta;
import alluxio.worker.block.meta.StorageDirEvictorView;
import alluxio.worker.block.meta.StorageTierView;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * Provides the basic implementation for every evictor.
 */
@NotThreadSafe
public abstract class AbstractEvictor extends AbstractBlockStoreEventListener implements Evictor {
  private static final Logger LOG = LoggerFactory.getLogger(AbstractEvictor.class);
  protected final Allocator mAllocator;
  protected BlockMetadataEvictorView mMetadataView;

  /**
   * Creates a new instance of {@link AbstractEvictor}.
   *
   * @param view a view of block metadata information
   * @param allocator an allocation policy
   */
  public AbstractEvictor(BlockMetadataEvictorView view, Allocator allocator) {
    mMetadataView = Preconditions.checkNotNull(view, "view");
    mAllocator = Preconditions.checkNotNull(allocator, "allocator");
  }


  @Override
  public EvictionPlan freeSpaceWithView(long bytesToBeAvailable, BlockStoreLocation location,
      BlockMetadataEvictorView view) {
    return freeSpaceWithView(bytesToBeAvailable, location, view, Mode.GUARANTEED);
  }

  @Override
  public EvictionPlan freeSpaceWithView(long bytesToBeAvailable, BlockStoreLocation location,
      BlockMetadataEvictorView view, Mode mode) {
    mMetadataView = view;

    List<BlockTransferInfo> toMove = new ArrayList<>();
    List<Pair<Long, BlockStoreLocation>> toEvict = new ArrayList<>();
    EvictionPlan plan = new EvictionPlan(toMove, toEvict);
    StorageDirEvictorView candidateDir = cascadingEvict(bytesToBeAvailable, location, plan, mode);

    mMetadataView.clearBlockMarks();
    if (candidateDir == null) {
      return null;
    }

    return plan;
  }

  /**
   * A recursive implementation of cascading eviction.
   *
   * This method uses a specific eviction strategy to find blocks to evict in the requested
   * location. After eviction, one {@link alluxio.worker.block.meta.StorageDir} in the location has
   * the specific amount of free space. It then uses an allocation strategy to allocate space in the
   * next tier to move each evicted blocks. If the next tier fails to allocate space for the evicted
   * blocks, the next tier will continue to evict its blocks to free space.
   *
   * This method is only used in
   * {@link #freeSpaceWithView(long, BlockStoreLocation, BlockMetadataEvictorView)}.
   *
   * @param bytesToBeAvailable bytes to be available after eviction
   * @param location target location to evict blocks from
   * @param plan the plan to be recursively updated, is empty when first called in
   *        {@link #freeSpaceWithView(long, BlockStoreLocation, BlockMetadataEvictorView)}
   * @param mode the eviction mode
   * @return the first {@link StorageDirEvictorView} in the range of location
   *         to evict/move bytes from, or null if there is no plan
   */
  protected StorageDirEvictorView cascadingEvict(long bytesToBeAvailable,
      BlockStoreLocation location, EvictionPlan plan, Mode mode) {
    location = updateBlockStoreLocation(bytesToBeAvailable, location);

    // 1. If bytesToBeAvailable can already be satisfied without eviction, return the eligible
    // StorageDirView
    StorageDirEvictorView candidateDirView = (StorageDirEvictorView)
        EvictorUtils.selectDirWithRequestedSpace(bytesToBeAvailable, location, mMetadataView);
    if (candidateDirView != null) {
      return candidateDirView;
    }

    // 2. Iterate over blocks in order until we find a StorageDirEvictorView that is
    // in the range of location and can satisfy bytesToBeAvailable
    // after evicting its blocks iterated so far
    EvictionDirCandidates dirCandidates = new EvictionDirCandidates();
    Iterator<Long> it = getBlockIterator();
    while (it.hasNext() && dirCandidates.candidateSize() < bytesToBeAvailable) {
      long blockId = it.next();
      try {
        BlockMeta block = mMetadataView.getBlockMeta(blockId);
        if (block != null) { // might not present in this view
          if (block.getBlockLocation().belongsTo(location)) {
            String tierAlias = block.getParentDir().getParentTier().getTierAlias();
            int dirIndex = block.getParentDir().getDirIndex();
            dirCandidates.add((StorageDirEvictorView) mMetadataView.getTierView(tierAlias)
                .getDirView(dirIndex), blockId, block.getBlockSize());
          }
        }
      } catch (BlockDoesNotExistException e) {
        LOG.warn("Remove block {} from evictor cache because {}", blockId, e);
        it.remove();
        onRemoveBlockFromIterator(blockId);
      }
    }

    // 3. If there is no eligible StorageDirEvictorView, return null
    if (mode == Mode.GUARANTEED && dirCandidates.candidateSize() < bytesToBeAvailable) {
      return null;
    }

    // 4. cascading eviction: try to allocate space in the next tier to move candidate blocks
    // there. If allocation fails, the next tier will continue to evict its blocks to free space.
    // Blocks are only evicted from the last tier or it can not be moved to the next tier.
    candidateDirView = dirCandidates.candidateDir();
    if (candidateDirView == null) {
      return null;
    }
    List<Long> candidateBlocks = dirCandidates.candidateBlocks();
    StorageTierView nextTierView
        = mMetadataView.getNextTier(candidateDirView.getParentTierView());
    if (nextTierView == null) {
      // This is the last tier, evict all the blocks.
      for (Long blockId : candidateBlocks) {
        try {
          BlockMeta block = mMetadataView.getBlockMeta(blockId);
          if (block != null) {
            candidateDirView.markBlockMoveOut(blockId, block.getBlockSize());
            plan.toEvict().add(new Pair<>(blockId, candidateDirView.toBlockStoreLocation()));
          }
        } catch (BlockDoesNotExistException e) {
          continue;
        }
      }
    } else {
      for (Long blockId : candidateBlocks) {
        try {
          BlockMeta block = mMetadataView.getBlockMeta(blockId);
          if (block == null) {
            continue;
          }
          StorageDirEvictorView nextDirView
              = (StorageDirEvictorView) mAllocator.allocateBlockWithView(
                  Sessions.MIGRATE_DATA_SESSION_ID, block.getBlockSize(),
                  BlockStoreLocation.anyDirInTier(nextTierView.getTierViewAlias()), mMetadataView);
          if (nextDirView == null) {
            nextDirView = cascadingEvict(block.getBlockSize(),
                BlockStoreLocation.anyDirInTier(nextTierView.getTierViewAlias()), plan, mode);
          }
          if (nextDirView == null) {
            // If we failed to find a dir in the next tier to move this block, evict it and
            // continue. Normally this should not happen.
            plan.toEvict().add(new Pair<>(blockId, block.getBlockLocation()));
            candidateDirView.markBlockMoveOut(blockId, block.getBlockSize());
            continue;
          }
          plan.toMove().add(new BlockTransferInfo(blockId, block.getBlockLocation(),
              nextDirView.toBlockStoreLocation()));
          candidateDirView.markBlockMoveOut(blockId, block.getBlockSize());
          nextDirView.markBlockMoveIn(blockId, block.getBlockSize());
        } catch (BlockDoesNotExistException e) {
          continue;
        }
      }
    }

    return candidateDirView;
  }

  @Override
  public EvictionPlan freeUserSpaceWithView(long userId, long bytesToBeAvailable, BlockStoreLocation location,
      BlockMetadataEvictorView view) {
    return freeUserSpaceWithView(userId, bytesToBeAvailable, location, view, Mode.GUARANTEED);
  }

  @Override
  public EvictionPlan freeUserSpaceWithView(long userId, long bytesToBeAvailable, BlockStoreLocation location,
      BlockMetadataEvictorView view, Mode mode) {
    mMetadataView = view;

    List<BlockTransferInfo> toMove = new ArrayList<>();
    List<Pair<Long, BlockStoreLocation>> toEvict = new ArrayList<>();
    EvictionPlan plan = new EvictionPlan(toMove, toEvict, userId);
    StorageDirEvictorView candidateDir = cascadingEvictUser(userId, bytesToBeAvailable, location, plan, mode);

    mMetadataView.clearUserBlockMarks(userId);
    if (candidateDir == null) {
      return null;
    }

    return plan;
  }

  protected StorageDirEvictorView cascadingEvictUser(long userId, long bytesToBeAvailable,
      BlockStoreLocation location, EvictionPlan plan, Mode mode) {
    location = updateBlockStoreLocation(bytesToBeAvailable, location);

    // 1. If bytesToBeAvailable can already be satisfied without eviction, return the eligible
    // StorageDirView
    StorageDirEvictorView candidateDirView = (StorageDirEvictorView)
        EvictorUtils.selectUserDirWithRequestedSpace(userId, bytesToBeAvailable, location, mMetadataView);
    if (candidateDirView != null) {
      return candidateDirView;
    }

    // 2. Iterate over blocks in order until we find a StorageDirEvictorView that is
    // in the range of location and can satisfy bytesToBeAvailable
    // after evicting its blocks iterated so far
    EvictionDirCandidates dirCandidates = new EvictionDirCandidates();
    Iterator<Long> it = getUserBlockIterator(userId);
    while (it.hasNext() && dirCandidates.candidateSize() < bytesToBeAvailable) {
      long blockId = it.next();
      try {
        BlockMeta block = mMetadataView.getUserBlockMeta(userId, blockId);
        if (block != null) { // might not present in this view
          if (block.getBlockLocation().belongsTo(location)) {
            String tierAlias = block.getParentDir().getParentTier().getTierAlias();
            int dirIndex = block.getParentDir().getDirIndex();
            long blockUseCount =  mMetadataView.getBlockUseCount(block);
            if (blockUseCount > 0){
              dirCandidates.add((StorageDirEvictorView) mMetadataView.getTierView(tierAlias)
                  .getDirView(dirIndex), blockId, block.getBlockSize() / blockUseCount);
            }
          }
        }
      } catch (BlockDoesNotExistException e) {
        LOG.warn("Remove block {} from evictor cache because {}", blockId, e);
        it.remove();
        onRemoveUserBlockFromIterator(userId, blockId);
      }
    }

    // 3. If there is no eligible StorageDirEvictorView, return null
    if (mode == Mode.GUARANTEED && dirCandidates.candidateSize() < bytesToBeAvailable) {
      return null;
    }

    // 4. cascading eviction: try to allocate space in the next tier to move candidate blocks
    // there. If allocation fails, the next tier will continue to evict its blocks to free space.
    // Blocks are only evicted from the last tier or it can not be moved to the next tier.
    candidateDirView = dirCandidates.candidateDir();
    if (candidateDirView == null) {
      return null;
    }
    List<Long> candidateBlocks = dirCandidates.candidateBlocks();
    StorageTierView nextTierView
        = mMetadataView.getNextTier(candidateDirView.getParentTierView());
    if (nextTierView == null) {
      // This is the last tier, evict all the blocks.
      for (Long blockId : candidateBlocks) {
        try {
          BlockMeta block = mMetadataView.getUserBlockMeta(userId, blockId);
          if (block != null) {
            long blockUseCount =  mMetadataView.getBlockUseCount(block);
            if (blockUseCount > 0) {
              candidateDirView.markUserBlockMoveOut(userId, blockId, block.getBlockSize() / blockUseCount);
              plan.toEvict().add(new Pair<>(blockId, candidateDirView.toBlockStoreLocation()));
            }
          }
        } catch (BlockDoesNotExistException e) {
          continue;
        }
      }
    } else {
      for (Long blockId : candidateBlocks) {
        try {
          BlockMeta block = mMetadataView.getBlockMeta(blockId);
          if (block == null) {
            continue;
          }
          long blockUseCount =  mMetadataView.getBlockUseCount(block);

          if (blockUseCount <= 0){
            continue;
          }
          long blockSpace = block.getBlockSize() / blockUseCount;
          StorageDirEvictorView nextDirView
              = (StorageDirEvictorView) mAllocator.allocateUserBlockWithView(
              Sessions.MIGRATE_DATA_SESSION_ID, userId, blockSpace,
              BlockStoreLocation.anyDirInTier(nextTierView.getTierViewAlias()), mMetadataView);
          if (nextDirView == null) {
            nextDirView = cascadingEvictUser(userId, blockSpace,
                BlockStoreLocation.anyDirInTier(nextTierView.getTierViewAlias()), plan, mode);
          }
          if (nextDirView == null) {
            // If we failed to find a dir in the next tier to move this block, evict it and
            // continue. Normally this should not happen.
            plan.toEvict().add(new Pair<>(blockId, block.getBlockLocation()));
            candidateDirView.markUserBlockMoveOut(userId, blockId, blockSpace);
            continue;
          }
          plan.toMove().add(new BlockTransferInfo(blockId, block.getBlockLocation(),
              nextDirView.toBlockStoreLocation()));
          candidateDirView.markUserBlockMoveOut(userId, blockId, blockSpace);
          nextDirView.markUserBlockMoveIn(userId, blockId, blockSpace);
        } catch (BlockDoesNotExistException e) {
          continue;
        }
      }
    }

    return candidateDirView;
  }



  /**
   * Returns an iterator for evictor cache blocks. The evictor is responsible for specifying the
   * iteration order using its own strategy. For example, {@link LRUEvictor} returns an iterator
   * that iterates through the block ids in LRU order.
   *
   * @return an iterator over the ids of the blocks in the evictor cache
   */
  protected abstract Iterator<Long> getBlockIterator();

  protected abstract Iterator<Long> getUserBlockIterator(long userId);

  /**
   * Performs additional cleanup when a block is removed from the iterator returned by
   * {@link #getBlockIterator()}.
   */
  protected void onRemoveBlockFromIterator(long blockId) {}
  protected void onRemoveUserBlockFromIterator(long userId, long blockId) {}
  /**
   * Updates the block store location if the evictor wants to free space in a specific location. For
   * example, {@link PartialLRUEvictor} always evicts blocks from a dir with max free space.
   *
   * @param bytesToBeAvailable bytes to be available after eviction
   * @param location the original block store location
   * @return the updated block store location
   */
  protected BlockStoreLocation updateBlockStoreLocation(long bytesToBeAvailable,
      BlockStoreLocation location) {
    return location;
  }
}
