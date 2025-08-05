package com.dadry.Threads;

import com.dadry.Model.Block;
import com.dadry.Service.BlockchainData;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.LinkedList;
import java.util.logging.Logger;

public class PeerRequestThread extends Thread {
    Logger LOG = Logger.getLogger(getClass().getName());

    private Socket socket;

    public PeerRequestThread(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            ObjectOutputStream objectOutput = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream objectInput = new ObjectInputStream(socket.getInputStream());

            LinkedList<Block> blockchain = (LinkedList<Block>) objectInput.readObject();

            LOG.info("LedgerId = " + blockchain.getLast().getLedgerId()  +
                    " Size= " + blockchain.getLast().getTransactionLedger().size());
            objectOutput.writeObject(BlockchainData.getInstance().getBlockchainConsensus(blockchain));
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
