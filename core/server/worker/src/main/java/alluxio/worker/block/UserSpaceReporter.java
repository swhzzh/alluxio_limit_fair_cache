package alluxio.worker.block;

import alluxio.exception.BlockDoesNotExistException;
import alluxio.heartbeat.HeartbeatExecutor;
import alluxio.util.IdUtils;
import alluxio.wire.FileInfo;

import com.google.common.io.Closer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserSpaceReporter implements HeartbeatExecutor {
  private static final Logger LOG = LoggerFactory.getLogger(UserSpaceReporter.class);
  /** The block worker the space reserver monitors. */
  private final BlockWorker mBlockWorker;

  private FileWriter mFileWriter;
  private String mCurDir = System.getProperty("user.dir");
  private Closer mCloser;
  private Map<Long, String> mBlockIdToFileNameMap = new HashMap<>();

  public UserSpaceReporter(BlockWorker blockWorker){
    mBlockWorker = blockWorker;
    mCloser = Closer.create();
    try {
      //LOG.info(mCurDir + "/logs/user/user_space_report.log");
      File file = new File(mCurDir + "/logs/user/user_space_report.log");
      if (!file.exists()){
        file.createNewFile();
      }
      mFileWriter = mCloser.register(new FileWriter(file));
      LOG.info("user_space_report.log was created successfully!");
    } catch (IOException e) {
      e.printStackTrace();
    }

    //preload
/*    mBlockIdToFileNameMap.put(50415534080L, "test1_10M.txt");
    mBlockIdToFileNameMap.put(50432311296L, "test1_20M.txt");
    mBlockIdToFileNameMap.put(50449088512L, "test1_30M.txt");
    mBlockIdToFileNameMap.put(50465865728L, "test1_40M.txt");
    mBlockIdToFileNameMap.put(50482642944L, "test1_50M.txt");
    mBlockIdToFileNameMap.put(117440512000L, "test2_10M.txt");
    mBlockIdToFileNameMap.put(134217728000L, "test2_20M.txt");
    mBlockIdToFileNameMap.put(100663296000L, "test2_30M.txt");
    mBlockIdToFileNameMap.put(201343369216L, "test2_40M.txt");
    mBlockIdToFileNameMap.put(201360146432L, "test2_50M.txt");
    mBlockIdToFileNameMap.put(201376923648L, "test3_10M.txt");
    mBlockIdToFileNameMap.put(201393700864L, "test3_20M.txt");
    mBlockIdToFileNameMap.put(150994944000L, "test3_30M.txt");
    mBlockIdToFileNameMap.put(201410478080L, "test3_40M.txt");
    mBlockIdToFileNameMap.put(201427255296L, "test3_50M.txt");
    mBlockIdToFileNameMap.put(201444032512L, "test4_10M.txt");
    mBlockIdToFileNameMap.put(201460809728L, "test4_20M.txt");
    mBlockIdToFileNameMap.put(201477586944L, "test4_30M.txt");
    mBlockIdToFileNameMap.put(201494364160L, "test4_40M.txt");
    mBlockIdToFileNameMap.put(201511141376L, "test4_50M.txt");*/
    /*test1_10M 50415534080
    test1_20M 50432311296
    test1_30M 50449088512
    test1_40M 50465865728
    test1_50M 50482642944

    test2_10M 117440512000
    test2_20M 134217728000
    test2_30M 100663296000
    test2_40M 201343369216
    test2_50M 201360146432

    test3_10M 201376923648
    test3_20M 201393700864
    test3_30M 150994944000
    test3_40M 201410478080
    test3_50M 201427255296

    test4_10M 201444032512
    test4_20M 201460809728
    test4_30M 201477586944
    test4_40M 201494364160
    test4_50M 201511141376*/
  }

  public String generateUserSpaceReport(){
    StringBuilder result = new StringBuilder();
    BlockStoreMeta storeMeta = mBlockWorker.getStoreMetaFull();
    Map<Long, Long> userCapacities = storeMeta.getAllUserCapacityBytes();
    Map<Long, Long> userUsedBytes = storeMeta.getAllUserUsedBytes();
    Map<Long, Integer> userNumOfBlocks = storeMeta.getAllUserNumberOfBlocks();
    Map<Long, Map<String, List<Long>>> userBlockIdsOnTiers = storeMeta.getAllBlockList();
    for (Map.Entry<Long, Long> entry : userCapacities.entrySet()) {
      long userId = entry.getKey();
      /*result.append("user ").append(String.valueOf(userId)).append(" capacity is ")
          .append(String.valueOf(entry.getValue() / (1024 * 1024))).append("mb, used space is ")
          .append(String.valueOf(userUsedBytes.get(userId) / (1024 * 1024))).append("mb\n");
      result.append("user ").append(String.valueOf(userId)).append(" has ")
          .append(String.valueOf(userNumOfBlocks.get(userId))).append(" blocks.\n").append("They are:\n");*/
      result.append("swh").append(userId).append("\t").append(entry.getValue() / (1024 * 1024))
          .append("\t").append(userUsedBytes.get(userId) / (1024 * 1024)).append("\n");

      for (Map.Entry<String, List<Long>> listEntry : userBlockIdsOnTiers.get(userId).entrySet()) {
        String tierAlias = listEntry.getKey();
        List<Long> blockIds = listEntry.getValue();
        //result.append(tierAlias).append(" : ");
        for (Long blockId : blockIds) {
          try {
            String fileName = mBlockIdToFileNameMap.get(blockId);
            if (fileName == null){
              FileInfo fileInfo = mBlockWorker.getFileInfo(IdUtils.fileIdFromBlockId(blockId));
              fileName = fileInfo.getName();
              mBlockIdToFileNameMap.put(blockId, fileName);
            }
            //result.append(fileName).append("(").append(mBlockWorker.getBlockUsedSpace(blockId) / (1024 * 1024)).append("mb), ");
            result.append(fileName).append("-").append(mBlockWorker.getBlockUsedSpace(blockId) / (1024 * 1024)).append("\t");
          } catch (IOException | BlockDoesNotExistException e) {
            e.printStackTrace();
          }
        }
        //result.append("\n");
      }
      result.append("\n");
    }

    return result.toString();
  }


  @Override
  public void heartbeat() throws InterruptedException {
    // log info some information
    BlockStoreMeta storeMeta = mBlockWorker.getStoreMetaFull();
    Map<Long, Long> userCapacities = storeMeta.getAllUserCapacityBytes();
    Map<Long, Long> userUsedBytes = storeMeta.getAllUserUsedBytes();
    Map<Long, Integer> userNumOfBlocks = storeMeta.getAllUserNumberOfBlocks();
    Map<Long, Map<String, List<Long>>> userBlockIdsOnTiers = storeMeta.getAllBlockList();
    for (Map.Entry<Long, Long> entry : userCapacities.entrySet()) {
      long userId = entry.getKey();
      try {
        mFileWriter.append("user ").append(String.valueOf(userId)).append(" capacity is ")
            .append(String.valueOf(entry.getValue() / (1024 * 1024))).append("mb, used space is ")
            .append(String.valueOf(userUsedBytes.get(userId) / (1024 * 1024))).append("mb\n");
        mFileWriter.append("user ").append(String.valueOf(userId)).append(" has ")
            .append(String.valueOf(userNumOfBlocks.get(userId))).append(" blocks.\n").append("They are\n");
        for (Map.Entry<String, List<Long>> listEntry : userBlockIdsOnTiers.get(userId).entrySet()) {
          String tierAlias = listEntry.getKey();
          List<Long> blockIds = listEntry.getValue();
          mFileWriter.append(tierAlias).append(" : ");
          for (Long blockId : blockIds) {
            mFileWriter.append(String.valueOf(blockId)).append(", ");
          }
          mFileWriter.append("\n");
        }
        mFileWriter.append("\n");
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    try {
      mFileWriter.flush();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void close() {
    try {
      mCloser.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
