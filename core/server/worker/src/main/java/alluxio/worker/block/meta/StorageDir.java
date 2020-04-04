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

package alluxio.worker.block.meta;

import alluxio.conf.PropertyKey;
import alluxio.conf.ServerConfiguration;
import alluxio.exception.BlockAlreadyExistsException;
import alluxio.exception.BlockDoesNotExistException;
import alluxio.exception.ExceptionMessage;
import alluxio.exception.InvalidWorkerStateException;
import alluxio.exception.UserOutOfSpaceException;
import alluxio.exception.WorkerOutOfSpaceException;
import alluxio.proto.meta.Block;
import alluxio.util.io.FileUtils;
import alluxio.util.io.PathUtils;
import alluxio.worker.block.BlockStoreLocation;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.sun.java.accessibility.util.EventID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.rmi.runtime.Log;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Represents a directory in a storage tier. It has a fixed capacity allocated to it on
 * instantiation. It contains the set of blocks currently in the storage directory.
 */
@NotThreadSafe
public final class StorageDir {
  private static final Logger LOG = LoggerFactory.getLogger(StorageDir.class);

  private final long mCapacityBytes;
  private final String mDirMedium;
  /** A map from block id to block metadata. */
  private Map<Long, BlockMeta> mBlockIdToBlockMap;
  /** A map from block id to temp block metadata. */
  private Map<Long, TempBlockMeta> mBlockIdToTempBlockMap;
  /** A map from session id to the set of temp blocks created by this session. */
  private Map<Long, Set<Long>> mSessionIdToTempBlockIdsMap;
  private AtomicLong mAvailableBytes;
  private AtomicLong mCommittedBytes;
  private String mDirPath;
  private int mDirIndex;
  private StorageTier mTier;

  // new for multi tenant cache isolation
  /** for test */
  private final long oneForthGB = 100 * 1024 * 1024;
  /** A map from user id to user storage info */
  private Map<Long, UserStorageInfo> mUserIdToUserStorageInfoMap;
  /** A map from block id to its use count, when the count decreases to 0, the block info can be deleted */
  private Map<Long, Long> mBlockIdToUseCountMap;
/*  private Map<Long, Set<Long>> mUserIdToBlockIdsMap;
  private Map<Long, Set<Long>> mUserIdToTempBlockIdsMap;
  private Map<Long, Long> mUserIdToCapacitiesMap;
  private Map<Long, Long> mUserIdToAvailableBytesMap;
  private Map<Long, Long> mUserIdToCommittedBytesMap;*/

  private StorageDir(StorageTier tier, int dirIndex, long capacityBytes, String dirPath,
      String dirMedium) {
    mTier = Preconditions.checkNotNull(tier, "tier");
    mDirIndex = dirIndex;
    mCapacityBytes = capacityBytes;
    mAvailableBytes = new AtomicLong(capacityBytes);
    mCommittedBytes = new AtomicLong(0);
    mDirPath = dirPath;
    mDirMedium = dirMedium;
    mBlockIdToBlockMap = new HashMap<>(200);
    mBlockIdToTempBlockMap = new HashMap<>(200);
    mSessionIdToTempBlockIdsMap = new HashMap<>(200);

    // preset some user information
    // total 1GB
    // four users(1,2,3,4) each 250MB
    mBlockIdToUseCountMap = new HashMap<>(200);
    mUserIdToUserStorageInfoMap = new HashMap<>();

    for (long i = 1L; i <= 4; i++) {
      mUserIdToUserStorageInfoMap.put(i, new UserStorageInfo(i, oneForthGB));
    }
  }

  /**
   * Factory method to create {@link StorageDir}.
   *
   * It will load metadata of existing committed blocks in the dirPath specified. Only files with
   * directory depth 1 under dirPath and whose file name can be parsed into {@code long} will be
   * considered as existing committed blocks, these files will be preserved, others files or
   * directories will be deleted.
   *
   * @param tier the {@link StorageTier} this dir belongs to
   * @param dirIndex the index of this dir in its tier
   * @param capacityBytes the initial capacity of this dir, can not be modified later
   * @param dirPath filesystem path of this dir for actual storage
   * @param dirMedium the medium type of the storage dir
   * @return the new created {@link StorageDir}
   * @throws BlockAlreadyExistsException when metadata of existing committed blocks already exists
   * @throws WorkerOutOfSpaceException when metadata can not be added due to limited left space
   */
  public static StorageDir newStorageDir(StorageTier tier, int dirIndex, long capacityBytes,
      String dirPath, String dirMedium)
      throws BlockAlreadyExistsException, IOException, WorkerOutOfSpaceException {
    StorageDir dir = new StorageDir(tier, dirIndex, capacityBytes, dirPath, dirMedium);
    dir.initializeMeta();
    return dir;
  }

  /**
   * Initializes metadata for existing blocks in this {@link StorageDir}.
   *
   * Only paths satisfying the contract defined in
   * {@link AbstractBlockMeta#commitPath(StorageDir, long)} are legal, should be in format like
   * {dir}/{blockId}. other paths will be deleted.
   *
   * @throws BlockAlreadyExistsException when metadata of existing committed blocks already exists
   * @throws WorkerOutOfSpaceException when metadata can not be added due to limited left space
   */
  private void initializeMeta() throws BlockAlreadyExistsException, IOException,
      WorkerOutOfSpaceException {
    // Create the storage directory path
    boolean isDirectoryNewlyCreated = FileUtils.createStorageDirPath(mDirPath,
        ServerConfiguration.get(PropertyKey.WORKER_DATA_FOLDER_PERMISSIONS));

    if (isDirectoryNewlyCreated) {
      LOG.info("Folder {} was created!", mDirPath);
    }

    File dir = new File(mDirPath);
    File[] paths = dir.listFiles();
    if (paths == null) {
      return;
    }
    for (File path : paths) {
      if (!path.isFile()) {
        //LOG.error("{} in StorageDir is not a file", path.getAbsolutePath());
        try {
          // TODO(calvin): Resolve this conflict in class names.
          org.apache.commons.io.FileUtils.deleteDirectory(path);
        } catch (IOException e) {
          LOG.error("can not delete directory {}", path.getAbsolutePath(), e);
        }
      } else {
        path.delete();
        /*try {
          path.delete();
          //long blockId = Long.parseLong(path.getName());
          //addBlockMeta(new BlockMeta(blockId, path.length(), this));
          // TODO: 2020/2/27 delete all or add as every user share
        } catch (NumberFormatException e) {
          LOG.error("filename of {} in StorageDir can not be parsed into long",
              path.getAbsolutePath(), e);
          if (path.delete()) {
            LOG.warn("file {} has been deleted", path.getAbsolutePath());
          } else {
            LOG.error("can not delete file {}", path.getAbsolutePath());
          }
        }*/
      }
    }

    /*for (File path : paths) {
      // test for multi tenant subdirs storage check
      // like {dir}/{userId}/{blockId}
      // TODO: 2020/2/26 add share block dir
      if (path.isDirectory()){
        // parse userId
        long userId;
        try {
          userId = Long.parseLong(path.getName());
          if (!mUserIdToUserStorageInfoMap.containsKey(userId)){
            mUserIdToUserStorageInfoMap.put(userId, new UserStorageInfo(userId, oneForthGB));
          }
        }
        catch (NumberFormatException e){
          LOG.error("filename of {} in StorageDir can not be parsed into long",
              path.getAbsolutePath(), e);
          if (path.delete()) {
            LOG.warn("file {} has been deleted", path.getAbsolutePath());
          } else {
            LOG.error("can not delete file {}", path.getAbsolutePath());
          }
          continue;
        }

        // parse and load blocks
        File[] subPaths = path.listFiles();
        if (subPaths == null){
          continue;
        }
        for (File subPath : subPaths) {
          if (!subPath.isFile()) {
            LOG.error("{} in StorageDir {} is not a file", subPath.getAbsolutePath(), path.getAbsolutePath());
            try {
              // TODO(calvin): Resolve this conflict in class names.
              org.apache.commons.io.FileUtils.deleteDirectory(subPath);
            } catch (IOException e) {
              LOG.error("can not delete directory {}", subPath.getAbsolutePath(), e);
            }
          } else {
            try {
              long blockId = Long.parseLong(path.getName());
              addUserBlockMeta(new BlockMeta(blockId, subPath.length(), this), userId);
              //addBlockMeta(new BlockMeta(blockId, path.length(), this));
            } catch (NumberFormatException e) {
              LOG.error("filename of {} in StorageDir can not be parsed into long",
                  path.getAbsolutePath(), e);
              if (path.delete()) {
                LOG.warn("file {} has been deleted", path.getAbsolutePath());
              } else {
                LOG.error("can not delete file {}", path.getAbsolutePath());
              }
            }
          }
        }

      }
      else{
        // TODO: 2020/2/26   handle those left by the old system by move it to some user
      }*/
  }

  public Set<Long> getAllUsers(){
    return mUserIdToUserStorageInfoMap.keySet();
  }

  public void addUser(long userId, long capacityBytes){
    if (!mUserIdToUserStorageInfoMap.containsKey(userId)){
      mUserIdToUserStorageInfoMap.put(userId, new UserStorageInfo(userId, capacityBytes));
    }
    else {
      // update user capacity
      // if capacityBytes decrease and result in Out Of Space, it should have been handled in the BlockStore
      // TODO: 2020/3/1 add
      mUserIdToUserStorageInfoMap.get(userId).capacityBytes = capacityBytes;
    }
  }

  public void removeUser(long userId){
    mUserIdToUserStorageInfoMap.remove(userId);
    // TODO: 2020/3/1 add
  }

  public long getBlockUseCount(long blockId){
    return mBlockIdToUseCountMap.getOrDefault(blockId, 0L);
  }

  /**
   * Gets the total capacity of this {@link StorageDir} in bytes, which is a constant once this
   * {@link StorageDir} has been initialized.
   *
   * @return the total capacity of this {@link StorageDir} in bytes
   */
  public long getCapacityBytes() {
    return mCapacityBytes;
  }

  /**
   * get total capacity for certain user
   *
   * @param userId
   * @return capacity for user
   */
  public long getUserCapacityBytes(long userId){
    if (mUserIdToUserStorageInfoMap.containsKey(userId)){
      return mUserIdToUserStorageInfoMap.get(userId).capacityBytes;
    }
    return 0L;
  }


  /**
   * Gets the total available capacity of this {@link StorageDir} in bytes. This value equals the
   * total capacity of this {@link StorageDir}, minus the used bytes by committed blocks and temp
   * blocks.
   *
   * @return available capacity in bytes
   */
  public long getAvailableBytes() {
    return mAvailableBytes.get();
  }

  /**
   * get available bytes for certain user
   *
   * @param userId
   * @return available bytes for user
   */
  public long getUserAvailableBytes(long userId){
    if (mUserIdToUserStorageInfoMap.containsKey(userId)){
      return mUserIdToUserStorageInfoMap.get(userId).availableBytes.get();
    }
    return 0L;
  }

  /**
   * Gets the total size of committed blocks in this StorageDir in bytes.
   *
   * @return number of committed bytes
   */
  public long getCommittedBytes() {
    return mCommittedBytes.get();
  }

  /**
   * get committed bytes for certain user
   *
   * @param userId
   * @return committed bytes for certain user
   */
  public long getUserCommittedBytes(long userId){
    if (mUserIdToUserStorageInfoMap.containsKey(userId)){
      return mUserIdToUserStorageInfoMap.get(userId).committedBytes.get();
    }
    return 0L;
  }

  /**
   * @return the path of the directory
   */
  public String getDirPath() {
    return mDirPath;
  }

  /**
   * get user dir
   *
   * @param userId
   * @return user dir
   */
  /*public String getUserDirPath(long userId){
    return PathUtils.concatPath(mDirPath, userId);
  }*/

  /**
   * @return the medium of the storage dir
   */
  public String getDirMedium() {
    return mDirMedium;
  }

  /**
   * Returns the {@link StorageTier} containing this {@link StorageDir}.
   *
   * @return {@link StorageTier}
   */
  public StorageTier getParentTier() {
    return mTier;
  }

  /**
   * Returns the zero-based index of this dir in its parent {@link StorageTier}.
   *
   * @return index
   */
  public int getDirIndex() {
    return mDirIndex;
  }

  /**
   * Returns the list of block ids in this dir.
   *
   * @return a list of block ids
   */
  public List<Long> getBlockIds() {
    return new ArrayList<>(mBlockIdToBlockMap.keySet());
  }

  /**
   * get blockIds for user
   *
   * @param userId
   * @return user blockIds
   */
  public List<Long> getUserBlockIds(long userId){
    UserStorageInfo userStorageInfo = Preconditions.checkNotNull(mUserIdToUserStorageInfoMap.get(userId), "user doesn't exist");
    return new ArrayList<>(userStorageInfo.blockIds);
  }

  /**
   * Returns the list of blocks stored in this dir.
   *
   * @return a list of blocks
   */
  public List<BlockMeta> getBlocks() {
    return new ArrayList<>(mBlockIdToBlockMap.values());
  }

  /**
   *
   *
   * @param userId
   * @return
   */
  public List<BlockMeta> getUserBlocks(long userId){
    UserStorageInfo userStorageInfo = Preconditions.checkNotNull(mUserIdToUserStorageInfoMap.get(userId), "user doesn't exist");
    return mBlockIdToBlockMap.entrySet().stream()
        .filter(entry -> userStorageInfo.blockIds.contains(entry.getKey()))
        .map(Map.Entry::getValue)
        .collect(Collectors.toList());
  }

  /**
   * Checks if a block is in this storage dir.
   *
   * @param blockId the block id
   * @return true if the block is in this storage dir, false otherwise
   */
  public boolean hasBlockMeta(long blockId) {
    return mBlockIdToBlockMap.containsKey(blockId);
  }

  /*public boolean hasBlockMetaForAllUsers(long blockId){
    for (UserStorageInfo userStorageInfo : mUserIdToUserStorageInfoMap.values()) {
      if (userStorageInfo.containsBlock(blockId)){
        return true;
      }
    }
    return false;
  }*/

  public boolean hasUserBlockMeta(long userId, long blockId){
    return mUserIdToUserStorageInfoMap.containsKey(userId)
        && mUserIdToUserStorageInfoMap.get(userId).containsBlock(blockId);
  }

  /**
   * Checks if a temp block is in this storage dir.
   *
   * @param blockId the block id
   * @return true if the block is in this storage dir, false otherwise
   */
  public boolean hasTempBlockMeta(long blockId) {
    return mBlockIdToTempBlockMap.containsKey(blockId);
  }

  /*public boolean hasTempBlockMetaForAllUsers(long blockId){
    for (UserStorageInfo userStorageInfo : mUserIdToUserStorageInfoMap.values()) {
      if (userStorageInfo.containsTempBlock(blockId)){
        return true;
      }
    }
    return false;
  }

  public boolean userHasTempBlockMeta(long userId, long blockId){
    return checkUser(userId)
        && mUserIdToUserStorageInfoMap.get(userId).containsTempBlock(blockId);
  }*/

  /**
   * Gets the {@link BlockMeta} from this storage dir by its block id.
   *
   * @param blockId the block id
   * @return {@link BlockMeta} of the given block or null
   * @throws BlockDoesNotExistException if no block is found
   */
  public BlockMeta getBlockMeta(long blockId) throws BlockDoesNotExistException {
    BlockMeta blockMeta = mBlockIdToBlockMap.get(blockId);
    if (blockMeta == null) {
      throw new BlockDoesNotExistException(ExceptionMessage.BLOCK_META_NOT_FOUND, blockId);
    }
    return blockMeta;
  }

  /**
   *
   * @param userId
   * @param blockId
   * @return
   * @throws BlockDoesNotExistException
   */
  public BlockMeta getUserBlockMeta(long userId, long blockId) throws BlockDoesNotExistException{
    UserStorageInfo userStorageInfo = Preconditions.checkNotNull(mUserIdToUserStorageInfoMap.get(userId), "user doesn't exist");
    if (!userStorageInfo.containsBlock(blockId)){
      // TODO: 2020/2/27 add user specific exception
      throw new BlockDoesNotExistException(ExceptionMessage.BLOCK_META_NOT_FOUND, blockId);
    }
    return getBlockMeta(blockId);
  }

/*  public BlockMeta getBlockMetaForAllUsers(long blockId){
    for (UserStorageInfo userStorageInfo : mUserIdToUserStorageInfoMap.values()) {
      if (userStorageInfo.containsBlock(blockId)){
        return userStorageInfo.getBlockMeta(blockId);
      }
    }
    return null;
  }*/

  /**
   * Gets the {@link BlockMeta} from this storage dir by its block id.
   *
   * @param blockId the block id
   * @return {@link TempBlockMeta} of the given block or null
   */
  public TempBlockMeta getTempBlockMeta(long blockId) {
    return mBlockIdToTempBlockMap.get(blockId);
  }


/*  public TempBlockMeta getUserTempBlockMeta(long userId, long blockId){
    if (!checkUser(userId)){
      // TODO: 2020/2/26 add user doesn't exist exception
      return null;
    }
    return mUserIdToUserStorageInfoMap.get(userId).blockIdToTempBlockMetaMap.get(blockId);
  }*/

  /**
   * Adds the metadata of a new block into this storage dir.
   *
   * @param blockMeta the metadata of the block
   * @throws BlockAlreadyExistsException if blockId already exists
   * @throws WorkerOutOfSpaceException when not enough space to hold block
   */
  public void addBlockMeta(BlockMeta blockMeta) throws WorkerOutOfSpaceException,
      BlockAlreadyExistsException {
    Preconditions.checkNotNull(blockMeta, "blockMeta");
    long blockId = blockMeta.getBlockId();
    long blockSize = blockMeta.getBlockSize();

    if (getAvailableBytes() < blockSize) {
      throw new WorkerOutOfSpaceException(ExceptionMessage.NO_SPACE_FOR_BLOCK_META, blockId,
          blockSize, getAvailableBytes(), blockMeta.getBlockLocation().tierAlias());
    }
    if (hasBlockMeta(blockId)) {
      throw new BlockAlreadyExistsException(ExceptionMessage.ADD_EXISTING_BLOCK, blockId, blockMeta
          .getBlockLocation().tierAlias());
    }
    mBlockIdToBlockMap.put(blockId, blockMeta);
    reserveSpace(blockSize, true);
  }

  /**
   * add the metadata of a new block into the user storage dir
   *
   * @param blockMeta
   * @param userId
   * @throws WorkerOutOfSpaceException
   * @throws BlockAlreadyExistsException
   */
  public void addUserBlockMeta(BlockMeta blockMeta, long userId) throws UserOutOfSpaceException,
      BlockAlreadyExistsException{
    UserStorageInfo userStorageInfo = Preconditions.checkNotNull(mUserIdToUserStorageInfoMap.get(userId), "user doesn't exist");
    long blockId = blockMeta.getBlockId();
    long blockSize = blockMeta.getBlockSize();

    long count = mBlockIdToUseCountMap.getOrDefault(blockId, 0L);
    long neededSpace = blockSize / (count + 1);
    if (getUserAvailableBytes(userId) < neededSpace){
      // TODO: 2020/2/26 add user sepcific exception of no space
      throw new UserOutOfSpaceException(ExceptionMessage.NO_USER_SPACE_FOR_BLOCK_META, blockId,
          blockSize, getUserAvailableBytes(userId), userId, blockMeta.getBlockLocation().tierAlias());
    }
    if (userStorageInfo.containsBlock(blockId)){
      // TODO: 2020/2/26 add user sepcific exception of add existing block
      throw new BlockAlreadyExistsException(ExceptionMessage.ADD_EXISTING_BLOCK, blockId, blockMeta
          .getBlockLocation().tierAlias());
    }
    userStorageInfo.addBlock(blockId);
    mBlockIdToBlockMap.put(blockId, blockMeta);
    // update the use count of the block
    mBlockIdToUseCountMap.put(blockId, count + 1);
    reserverUserSpace(blockSize, userId, blockId,true);

    LOG.info("user {} adds block {}, blockSize is {}mb, user capacity is {}mb, user available is {}mb", userId, blockId, blockSize / (1024*1024), userStorageInfo.capacityBytes/ (1024*1024), userStorageInfo.availableBytes.get()/ (1024*1024));
  }

  /**
   * Adds the metadata of a new block into this storage dir.
   *
   * @param tempBlockMeta the metadata of a temp block to add
   * @throws BlockAlreadyExistsException if blockId already exists
   * @throws WorkerOutOfSpaceException when not enough space to hold block
   */
  public void addTempBlockMeta(TempBlockMeta tempBlockMeta) throws WorkerOutOfSpaceException,
      BlockAlreadyExistsException {
    Preconditions.checkNotNull(tempBlockMeta, "tempBlockMeta");
    long sessionId = tempBlockMeta.getSessionId();
    long blockId = tempBlockMeta.getBlockId();
    long blockSize = tempBlockMeta.getBlockSize();

    if (getAvailableBytes() < blockSize) {
      throw new WorkerOutOfSpaceException(ExceptionMessage.NO_SPACE_FOR_BLOCK_META, blockId,
          blockSize, getAvailableBytes(), tempBlockMeta.getBlockLocation().tierAlias());
    }
    if (hasTempBlockMeta(blockId)) {
      throw new BlockAlreadyExistsException(ExceptionMessage.ADD_EXISTING_BLOCK, blockId,
          tempBlockMeta.getBlockLocation().tierAlias());
    }

    mBlockIdToTempBlockMap.put(blockId, tempBlockMeta);
    Set<Long> sessionTempBlocks = mSessionIdToTempBlockIdsMap.get(sessionId);
    if (sessionTempBlocks == null) {
      mSessionIdToTempBlockIdsMap.put(sessionId, Sets.newHashSet(blockId));
    } else {
      sessionTempBlocks.add(blockId);
    }
    reserveSpace(blockSize, false);
  }

  /**
   * add the metadata of the temp block for user storage dir
   *
   * @param tempBlockMeta
   * @throws WorkerOutOfSpaceException
   * @throws BlockAlreadyExistsException
   */
  public void addUserTempBlockMeta(TempBlockMeta tempBlockMeta) throws WorkerOutOfSpaceException,
      BlockAlreadyExistsException{
    long userId = tempBlockMeta.getUserId();
    UserStorageInfo userStorageInfo = Preconditions.checkNotNull(mUserIdToUserStorageInfoMap.get(userId), "user doesn't exist");
    long sessionId = tempBlockMeta.getSessionId();
    long blockId = tempBlockMeta.getBlockId();
    long blockSize = tempBlockMeta.getBlockSize();
    if (getUserAvailableBytes(userId) < blockSize){
      // TODO: 2020/2/26 add user specific exception
      throw new WorkerOutOfSpaceException(ExceptionMessage.NO_SPACE_FOR_BLOCK_META, blockId,
          blockSize, getUserAvailableBytes(userId), tempBlockMeta.getBlockLocation().tierAlias());
    }

    if (hasTempBlockMeta(blockId)){
      throw new BlockAlreadyExistsException(ExceptionMessage.ADD_EXISTING_BLOCK, blockId,
          tempBlockMeta.getBlockLocation().tierAlias());
    }

    userStorageInfo.addTempBlock(blockId);
    mBlockIdToTempBlockMap.put(blockId, tempBlockMeta);
    Set<Long> sessionTempBlocks = mSessionIdToTempBlockIdsMap.get(sessionId);
    if (sessionTempBlocks == null) {
      mSessionIdToTempBlockIdsMap.put(sessionId, Sets.newHashSet(blockId));
    } else {
      sessionTempBlocks.add(blockId);
    }
    reserverUserSpace(blockSize, userId, blockId,false);

    LOG.info("user {} adds temp block {}, blockSize is {}mb, user capacity is {}mb, user available is {}mb", userId, blockId, blockSize/ (1024*1024), userStorageInfo.capacityBytes/ (1024*1024), userStorageInfo.availableBytes.get()/ (1024*1024));
  }

  /**
   * Removes a block from this storage dir.
   *
   * @param blockMeta the metadata of the block
   * @throws BlockDoesNotExistException if no block is found
   */
  public void removeBlockMeta(BlockMeta blockMeta) throws BlockDoesNotExistException {
    Preconditions.checkNotNull(blockMeta, "blockMeta");
    long blockId = blockMeta.getBlockId();
    BlockMeta deletedBlockMeta = mBlockIdToBlockMap.remove(blockId);
    if (deletedBlockMeta == null) {
      throw new BlockDoesNotExistException(ExceptionMessage.BLOCK_META_NOT_FOUND, blockId);
    }
    reclaimSpace(blockMeta.getBlockSize(), true);
  }

  /**
   *
   * @param blockMeta
   * @param userId
   * @throws BlockDoesNotExistException
   */
  public void removeUserBlockMeta(BlockMeta blockMeta, long userId) throws BlockDoesNotExistException{
    Preconditions.checkNotNull(blockMeta, "blockMeta");
    UserStorageInfo userStorageInfo = Preconditions.checkNotNull(mUserIdToUserStorageInfoMap.get(userId), "user doesn't exist");
    long blockId = blockMeta.getBlockId();
    if (!userStorageInfo.removeBlock(blockId)){
      throw new BlockDoesNotExistException(ExceptionMessage.BLOCK_META_NOT_FOUND, blockId);
    }
    long useCount = mBlockIdToUseCountMap.get(blockId);
    // delete the block meta if no user use the block
    if (useCount == 1){
      mBlockIdToUseCountMap.remove(blockId);
      BlockMeta deletedBlockMeta = mBlockIdToBlockMap.remove(blockId);
      if (deletedBlockMeta == null) {
        throw new BlockDoesNotExistException(ExceptionMessage.BLOCK_META_NOT_FOUND, blockId);
      }
    }
    else {
      mBlockIdToUseCountMap.put(blockId, useCount - 1);
    }

    reclaimUserSpace(blockMeta.getBlockSize(), userId, blockId,true);

    LOG.info("user {} removes block {}, blockSize is {}mb, now user capacity is {}mb, user available is {}mb",
        userId, blockId, blockMeta.getBlockSize()/ (1024*1024), userStorageInfo.capacityBytes/ (1024*1024), userStorageInfo.availableBytes.get()/ (1024*1024));
  }

  /**
   * Removes a temp block from this storage dir.
   *
   * @param tempBlockMeta the metadata of the temp block to remove
   * @throws BlockDoesNotExistException if no temp block is found
   */
  public void removeTempBlockMeta(TempBlockMeta tempBlockMeta) throws BlockDoesNotExistException {
    Preconditions.checkNotNull(tempBlockMeta, "tempBlockMeta");
    final long blockId = tempBlockMeta.getBlockId();
    final long sessionId = tempBlockMeta.getSessionId();
    TempBlockMeta deletedTempBlockMeta = mBlockIdToTempBlockMap.remove(blockId);
    if (deletedTempBlockMeta == null) {
      throw new BlockDoesNotExistException(ExceptionMessage.BLOCK_META_NOT_FOUND, blockId);
    }
    Set<Long> sessionBlocks = mSessionIdToTempBlockIdsMap.get(sessionId);
    if (sessionBlocks == null || !sessionBlocks.contains(blockId)) {
      throw new BlockDoesNotExistException(ExceptionMessage.BLOCK_NOT_FOUND_FOR_SESSION, blockId,
          mTier.getTierAlias(), sessionId);
    }
    Preconditions.checkState(sessionBlocks.remove(blockId));
    if (sessionBlocks.isEmpty()) {
      mSessionIdToTempBlockIdsMap.remove(sessionId);
    }
    reclaimSpace(tempBlockMeta.getBlockSize(), false);
  }

  /**
   *
   *
   * @param tempBlockMeta
   * @throws BlockDoesNotExistException
   */
  public void removeUserTempBlockMeta(TempBlockMeta tempBlockMeta) throws BlockDoesNotExistException{
    Preconditions.checkNotNull(tempBlockMeta, "tempBlockMeta");
    long userId = tempBlockMeta.getUserId();
    long blockId = tempBlockMeta.getBlockId();
    long sessionId = tempBlockMeta.getSessionId();
    if (!mUserIdToUserStorageInfoMap.get(userId).removeTempBlock(blockId) ||
        mBlockIdToTempBlockMap.remove(blockId) == null){
      throw new BlockDoesNotExistException(ExceptionMessage.BLOCK_META_NOT_FOUND, blockId);
    }
    /*TempBlockMeta deletedTempBlockMeta = mBlockIdToTempBlockMap.remove(blockId);
    if (deletedTempBlockMeta == null){
      throw new BlockDoesNotExistException(ExceptionMessage.BLOCK_META_NOT_FOUND, blockId);
    }*/
    Set<Long> sessionBlocks = mSessionIdToTempBlockIdsMap.get(sessionId);
    if (sessionBlocks == null || !sessionBlocks.contains(blockId)) {
      throw new BlockDoesNotExistException(ExceptionMessage.BLOCK_NOT_FOUND_FOR_SESSION, blockId,
          mTier.getTierAlias(), sessionId);
    }
    Preconditions.checkState(sessionBlocks.remove(blockId));
    if (sessionBlocks.isEmpty()) {
      mSessionIdToTempBlockIdsMap.remove(sessionId);
    }
    // temp block is exclusive for a user
    reclaimUserSpace(tempBlockMeta.getBlockSize(), userId, blockId, false);
    LOG.info("user {} removes block {}, blockSize is {}mb, now user capacity is {}mb, user available is {}mb",
        userId, blockId, tempBlockMeta.getBlockSize()/ (1024*1024), mUserIdToUserStorageInfoMap.get(userId).capacityBytes/ (1024*1024), mUserIdToUserStorageInfoMap.get(userId).availableBytes.get()/ (1024*1024));
  }

  /**
   * Changes the size of a temp block.
   *
   * @param tempBlockMeta the metadata of the temp block to resize
   * @param newSize the new size after change in bytes
   * @throws InvalidWorkerStateException when newSize is smaller than oldSize
   */
  public void resizeTempBlockMeta(TempBlockMeta tempBlockMeta, long newSize)
      throws InvalidWorkerStateException {
    long oldSize = tempBlockMeta.getBlockSize();
    if (newSize > oldSize) {
      reserveSpace(newSize - oldSize, false);
      tempBlockMeta.setBlockSize(newSize);
    } else if (newSize < oldSize) {
      throw new InvalidWorkerStateException("Shrinking block, not supported!");
    }
  }

  /**
   *
   *
   * @param tempBlockMeta
   * @param newSize
   * @throws InvalidWorkerStateException
   */
  public void resizeUserTempBlockMeta(TempBlockMeta tempBlockMeta, long newSize)
      throws InvalidWorkerStateException {
    long userId = tempBlockMeta.getUserId();
    long oldSize = tempBlockMeta.getBlockSize();
    if (newSize < oldSize){
      throw new InvalidWorkerStateException("Shrinking block, not supported!");
    }
    reserverUserSpace(newSize - oldSize, userId, tempBlockMeta.getBlockId(), false);
    tempBlockMeta.setBlockSize(newSize);
    mBlockIdToTempBlockMap.get(tempBlockMeta.getBlockId()).setBlockSize(newSize);
  }

  /**
   * Cleans up the temp block metadata for each block id passed in.
   *
   * @param sessionId the id of the client associated with the temporary blocks
   * @param tempBlockIds the list of temporary blocks to clean up, non temporary blocks or
   *        nonexistent blocks will be ignored
   */
  public void cleanupSessionTempBlocks(long sessionId, List<Long> tempBlockIds) {
    Set<Long> sessionTempBlocks = mSessionIdToTempBlockIdsMap.get(sessionId);
    // The session's temporary blocks have already been removed.
    if (sessionTempBlocks == null) {
      return;
    }
    for (Long tempBlockId : tempBlockIds) {
      if (!mBlockIdToTempBlockMap.containsKey(tempBlockId)) {
        // This temp block does not exist in this dir, this is expected for some blocks since the
        // input list is across all dirs
        continue;
      }
      sessionTempBlocks.remove(tempBlockId);
      TempBlockMeta tempBlockMeta = mBlockIdToTempBlockMap.remove(tempBlockId);
      if (tempBlockMeta != null) {
        reclaimSpace(tempBlockMeta.getBlockSize(), false);
      } else {
        LOG.error("Cannot find blockId {} when cleanup sessionId {}", tempBlockId, sessionId);
      }
    }
    if (sessionTempBlocks.isEmpty()) {
      mSessionIdToTempBlockIdsMap.remove(sessionId);
    } else {
      // This may happen if the client comes back during clean up and creates more blocks or some
      // temporary blocks failed to be deleted
      LOG.warn("Blocks still owned by session {} after cleanup.", sessionId);
    }
  }

  /**
   * clean up session for all users since some users may share the same sessionId, e.g. async cache session id
   *
   * @param sessionId
   * @param tempBlockIds
   */
  public void cleanupSessionTempBlocksForAllUsers(long sessionId, List<Long> tempBlockIds)
      throws BlockDoesNotExistException {
    Set<Long> sessionTempBlocks = mSessionIdToTempBlockIdsMap.get(sessionId);
    // The session's temporary blocks have already been removed.
    if (sessionTempBlocks == null) {
      return;
    }
    // get the users who hold the temp block
    Map<Long, Long> blockIdToUserIdMap = new HashMap<>();
    for (UserStorageInfo userStorageInfo : mUserIdToUserStorageInfoMap.values()) {
      List<Long> userTempBlockIds = new ArrayList<>(userStorageInfo.tempBlockIds);
      if (userTempBlockIds.retainAll(tempBlockIds)){
        for (Long userTempBlockId : userTempBlockIds) {
          blockIdToUserIdMap.put(userTempBlockId, userStorageInfo.userId);
        }
      }
    }
    for (Long tempBlockId : tempBlockIds) {
      if (!mBlockIdToTempBlockMap.containsKey(tempBlockId)) {
        // This temp block does not exist in this dir, this is expected for some blocks since the
        // input list is across all dirs
        continue;
      }
      sessionTempBlocks.remove(tempBlockId);
      mUserIdToUserStorageInfoMap.get(blockIdToUserIdMap.get(tempBlockId)).removeTempBlock(tempBlockId);
      TempBlockMeta tempBlockMeta = mBlockIdToTempBlockMap.remove(tempBlockId);
      if (tempBlockMeta != null) {
        reclaimUserSpace(tempBlockMeta.getBlockSize(), blockIdToUserIdMap.get(tempBlockId), tempBlockId,false);
      } else {
        LOG.error("Cannot find blockId {} when cleanup sessionId {}", tempBlockId, sessionId);
      }
    }
    if (sessionTempBlocks.isEmpty()) {
      mSessionIdToTempBlockIdsMap.remove(sessionId);
    } else {
      // This may happen if the client comes back during clean up and creates more blocks or some
      // temporary blocks failed to be deleted
      LOG.warn("Blocks still owned by session {} after cleanup.", sessionId);
    }
  }

  /**
   * Gets the temporary blocks associated with a session in this {@link StorageDir}, an empty list
   * is returned if the session has no temporary blocks in this {@link StorageDir}.
   *
   * @param sessionId the id of the session
   * @return A list of temporary blocks the session is associated with in this {@link StorageDir}
   */
  public List<TempBlockMeta> getSessionTempBlocks(long sessionId) {
    Set<Long> sessionTempBlockIds = mSessionIdToTempBlockIdsMap.get(sessionId);

    if (sessionTempBlockIds == null || sessionTempBlockIds.isEmpty()) {
      return Collections.emptyList();
    }
    List<TempBlockMeta> sessionTempBlocks = new ArrayList<>();
    for (long blockId : sessionTempBlockIds) {
      sessionTempBlocks.add(mBlockIdToTempBlockMap.get(blockId));
    }
    return sessionTempBlocks;
  }

  /**
   * get temp blocks of a session
   *
   * @param sessionId
   * @return
   */
/*  public List<TempBlockMeta> getSessionTempBlocksForAllUsers(long sessionId){
    List<TempBlockMeta> result = new ArrayList<>();
    for (UserStorageInfo userStorageInfo : mUserIdToUserStorageInfoMap.values()){
      if (!userStorageInfo.containsSession(sessionId))
      result.addAll(userStorageInfo.blockIdToTempBlockMetaMap.entrySet().stream()
          .filter(entry -> userStorageInfo.sessionIdToTempBlockIdsMap.get(sessionId).contains(entry.getKey()))
          .map(Map.Entry::getValue).collect(Collectors.toList()));
    }
    return result;
  }*/

  /**
   * @return the block store location of this directory
   */
  public BlockStoreLocation toBlockStoreLocation() {
    return new BlockStoreLocation(mTier.getTierAlias(), mDirIndex, mDirMedium);
  }

  private void reclaimSpace(long size, boolean committed) {
    Preconditions.checkState(mCapacityBytes >= mAvailableBytes.get() + size,
        "Available bytes should always be less than total capacity bytes");
    mAvailableBytes.addAndGet(size);
    if (committed) {
      mCommittedBytes.addAndGet(-size);
    }
  }

  /**
   *
   *
   * @param size
   * @param userId
   * @param committed
   */
  private void reclaimUserSpace(long size, long userId, long blockId, boolean committed)
      throws BlockDoesNotExistException {
    // no user use the block
    if (!mBlockIdToUseCountMap.containsKey(blockId)){
      Preconditions.checkState(getUserCapacityBytes(userId) >= getUserAvailableBytes(userId) + size,
          "Available bytes should always be less than total capacity bytes");
      mAvailableBytes.addAndGet(size);
      mUserIdToUserStorageInfoMap.get(userId).availableBytes.addAndGet(size);
      if (committed){
        mCommittedBytes.addAndGet(-size);
        mUserIdToUserStorageInfoMap.get(userId).committedBytes.addAndGet(-size);
      }
    }
    else {
      long newSize = size / (mBlockIdToUseCountMap.get(blockId));
      long oldSize = size / (mBlockIdToUseCountMap.get(blockId) + 1);

      mAvailableBytes.addAndGet(oldSize);
      mUserIdToUserStorageInfoMap.get(userId).availableBytes.addAndGet(oldSize);
      if (committed){
        mCommittedBytes.addAndGet(-oldSize);
        mUserIdToUserStorageInfoMap.get(userId).committedBytes.addAndGet(-oldSize);
      }

      for (UserStorageInfo userInfo : mUserIdToUserStorageInfoMap.values()) {
        if (userInfo.containsBlock(blockId)){
          // user storage may be out of space, just let the available bytes minus to enable space evictor
          userInfo.availableBytes.addAndGet(oldSize - newSize);
          if (committed){
            userInfo.committedBytes.addAndGet(newSize - oldSize);
          }
        }

      }
    }
  }

  private void reserveSpace(long size, boolean committed) {
    Preconditions.checkState(size <= mAvailableBytes.get(),
        "Available bytes should always be non-negative");
    mAvailableBytes.addAndGet(-size);
    if (committed) {
      mCommittedBytes.addAndGet(size);
    }
  }

  /**
   *
   * @param size
   * @param userId
   * @param committed
   */
  private void reserverUserSpace(long size, long userId, long blockId, boolean committed){
    UserStorageInfo userStorageInfo = Preconditions.checkNotNull(mUserIdToUserStorageInfoMap.get(userId), "user doesn't exist");
    // new block or temp block add
    if (!mBlockIdToUseCountMap.containsKey(blockId) || mBlockIdToUseCountMap.get(blockId) == 1){
      Preconditions.checkState(size <= getUserAvailableBytes(userId));
      mAvailableBytes.addAndGet(-size);
      userStorageInfo.availableBytes.addAndGet(-size);
      if (committed){
        mCommittedBytes.addAndGet(size);
        userStorageInfo.committedBytes.addAndGet(size);
      }
    }
    // consider share the space
    else {
      long newSpace = size / mBlockIdToUseCountMap.get(blockId);
      Preconditions.checkState(newSpace <= getUserAvailableBytes(userId));
      long oldSpace = size / (mBlockIdToUseCountMap.get(blockId) - 1);
      mUserIdToUserStorageInfoMap.values().stream()
          .filter(userInfo -> userInfo.containsBlock(blockId))
          .forEach(userInfo -> {
            if (userInfo.userId == userId){
              userInfo.availableBytes.addAndGet(-newSpace);
              if (committed){
                userInfo.committedBytes.addAndGet(newSpace);
              }
            }
            else {
              userInfo.availableBytes.addAndGet(oldSpace - newSpace);
              if (committed){
                userInfo.committedBytes.addAndGet(newSpace - oldSpace);
              }
            }
          });
    }

  }


  public boolean hasUser(long userId){
    return mUserIdToUserStorageInfoMap.containsKey(userId);
  }

  /**
   *
   */
  class UserStorageInfo{
    final Long userId;
    Long capacityBytes;
    AtomicLong availableBytes;
    AtomicLong committedBytes;
    /*Map<Long, BlockMeta> blockIdToBlockMetaMap;
    Map<Long, TempBlockMeta> blockIdToTempBlockMetaMap;
    Map<Long, Set<Long>> sessionIdToTempBlockIdsMap;*/
    Set<Long> blockIds;
    Set<Long> tempBlockIds;

    public UserStorageInfo(long userId, long capacity){
      this.userId = userId;
      this.capacityBytes = capacity;
      this.availableBytes = new AtomicLong(capacity);
      this.committedBytes = new AtomicLong(0L);
      /*blockIdToBlockMetaMap = new HashMap<>(200);
      blockIdToTempBlockMetaMap = new HashMap<>(200);
      sessionIdToTempBlockIdsMap = new HashMap<>(200);*/
      blockIds = new HashSet<>();
      tempBlockIds = new HashSet<>();
    }

    public boolean containsBlock(long blockId){
      return blockIds.contains(blockId);
    }
    public boolean containsTempBlock(long blockId){
      return tempBlockIds.contains(blockId);
    }
    public void addBlock(long blockId){
      blockIds.add(blockId);
    }
    public void addTempBlock(long blockId){
      tempBlockIds.add(blockId);
    }
    public boolean removeBlock(long blockId){
      return blockIds.remove(blockId);
    }
    public boolean removeTempBlock(long blockId){
      return tempBlockIds.remove(blockId);
    }
    /*public boolean containsSession(long sessionId){
      return sessionIdToTempBlockIdsMap.containsKey(sessionId);
    }
    public void addBlockMeta(long blockId, BlockMeta blockMeta){
      blockIdToBlockMetaMap.put(blockId, blockMeta);
    }
    public void addTempBlockMeta(long sessionId, long blockId, TempBlockMeta tempBlockMeta){
      blockIdToTempBlockMetaMap.put(blockId, tempBlockMeta);
      if (containsSession(sessionId)){
        sessionIdToTempBlockIdsMap.get(sessionId).add(blockId);
      }
      else {
        sessionIdToTempBlockIdsMap.put(sessionId, Sets.newHashSet(blockId));
      }
    }
    public BlockMeta removeBlockMeta(long blockId){
      return blockIdToBlockMetaMap.remove(blockId);
    }
    public TempBlockMeta removeTempBlockMeta(long blockId){
      return blockIdToTempBlockMetaMap.remove(blockId);
    }
    public Set<Long> getSessionBlockIds(long sessionId){
      return sessionIdToTempBlockIdsMap.get(sessionId);
    }
    public void removeSessionBlockIds(long sessionId){
      sessionIdToTempBlockIdsMap.remove(sessionId);
    }

    public BlockMeta getBlockMeta(long blockId){
      return blockIdToBlockMetaMap.get(blockId);
    }
    public TempBlockMeta getTempBlockMeta(long blockId){
      return blockIdToTempBlockMetaMap.get(blockId);
    }*/
  }
}
