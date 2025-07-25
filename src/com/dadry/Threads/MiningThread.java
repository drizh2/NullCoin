package com.dadry.Threads;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.logging.Logger;

public class MiningThread extends Thread {

    Logger LOG = Logger.getLogger(getClass().getName());

    @Override
    public void run() {
        while (true) {
            long now = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
            long lastMinedBlock = LocalDateTime.parse(BlockchainData.getInstance()
                    .getCurrentBlockChain().getLast().getTimeStamp()).toEpochSecond(ZoneOffset.UTC);

            long timeSinceLastBlock = now - lastMinedBlock;
            long miningInterval = BlockchainData.getMiningInterval();
            long timeoutInterval = BlockchainData.getTimeoutInterval();

            if (timeSinceLastBlock > timeoutInterval) {
                LOG.info("BlockChain is too old for mining! Update it from peers");
                sleepSmart(5000);  // Check less often when outdated
            } else {
                long timeToNextMine = (lastMinedBlock + miningInterval) - now;

                if (timeToNextMine > 0) {
                    LOG.info("BlockChain is current, mining will commence in " + timeToNextMine + " seconds");
                    sleepSmart(Math.min(timeToNextMine * 1000L, 5000)); // Sleep until mining or up to 5s
                } else {
                    LOG.info("MINING NEW BLOCK");
                    BlockchainData.getInstance().mineBlock();
                    LOG.info(BlockchainData.getInstance().getWalletBallanceFX());

                    sleepSmart(1000);
                }
            }
            LOG.info(LocalDateTime.parse(BlockchainData.getInstance()
                    .getCurrentBlockChain().getLast().getTimeStamp()).toEpochSecond(ZoneOffset.UTC));

            BlockchainData.getInstance().setMiningPoints(BlockchainData.getInstance().getMiningPoints() + 2);

            if (BlockchainData.getInstance().isExit()) {
                break;
            }
        }
    }

    private void sleepSmart(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
