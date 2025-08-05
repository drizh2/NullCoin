package com.dadry.Threads;

import com.dadry.Model.Block;
import com.dadry.Service.BlockchainData;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

public class PeerClient extends Thread {
    Logger LOG = Logger.getLogger(getClass().getName());

    private Queue<Integer> queue = new ConcurrentLinkedQueue<>();

    public PeerClient() {
        this.queue.add(6001);
        this.queue.add(6002);
    }

    @Override
    public void run() {
        while (true) {
            try (Socket socket = new Socket("localhost", queue.peek())) {
                LOG.info("Sending blockchain object on port: " + queue.peek());
                queue.add(queue.poll());
                socket.setSoTimeout(5000);

                ObjectOutputStream objectOutput = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream objectInput = new ObjectInputStream(socket.getInputStream());

                LinkedList<Block> blockchain = BlockchainData.getInstance().getCurrentBlockchain();
                objectOutput.writeObject(blockchain);

                LinkedList<Block> returnedBlockchain = (LinkedList<Block>) objectInput.readObject();
                LOG.info(" RETURNED BC LedgerId = " + returnedBlockchain.getLast().getLedgerId()  +
                        " Size= " + returnedBlockchain.getLast().getTransactionLedger().size());
                BlockchainData.getInstance().getBlockchainConsensus(returnedBlockchain);
                Thread.sleep(2000);
            } catch (SocketTimeoutException e) {
                LOG.info("The socket timed out");
                queue.add(queue.poll());
            } catch (IOException e) {
                LOG.info("Client Error: " + e.getMessage() + " -- Error on port: "+ queue.peek());
                queue.add(queue.poll());
            } catch (InterruptedException | ClassNotFoundException e) {
                e.printStackTrace();
                queue.add(queue.poll());
            }
        }
    }
}
