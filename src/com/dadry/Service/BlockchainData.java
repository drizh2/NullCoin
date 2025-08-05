package com.dadry.Service;

import com.dadry.Model.Block;
import com.dadry.Model.Transaction;
import com.dadry.Model.Wallet;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.logging.Logger;

public class BlockchainData {
    Logger LOG = Logger.getLogger(getClass().getName());

    private ObservableList<Transaction> newBlockTransactionsFX;
    private ObservableList<Transaction> newBlockTransactions;
    private LinkedList<Block> currentBlockchain = new LinkedList<>();
    private Block latestBlock;
    private boolean exit = false;
    private  int miningPoints;

    public static final int TIMEOUT_INTERVAL = 65;
    public static final int MINING_INTERVAL = 60;

    private Signature signing = Signature.getInstance("SHA256withDSA");
    private static BlockchainData instance;

    static {
        try {
            instance = new BlockchainData();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public BlockchainData() throws NoSuchAlgorithmException {
        newBlockTransactions = FXCollections.observableArrayList();
        newBlockTransactionsFX = FXCollections.observableArrayList();
    }

    public static BlockchainData getInstance() {
        return instance;
    }

    Comparator<Transaction> transactionComparator = Comparator.comparing(Transaction::getTimestamp);
    public ObservableList<Transaction> getTransactionLedgerFX() {
        newBlockTransactionsFX.clear();
        newBlockTransactions.sort(transactionComparator);
        newBlockTransactionsFX.addAll(newBlockTransactions);
        return FXCollections.observableArrayList(newBlockTransactionsFX);
    }

    public String getWalletBalanceFX() {
        return getBalance(currentBlockchain, newBlockTransactions,
                WalletData.getInstance().getWallet().getPublicKey()).toString();
    }

    private Integer getBalance(LinkedList<Block> blockchain, ObservableList<Transaction> currentLedger, PublicKey walletAddress) {
        Integer balance = 0;
        for (Block block : blockchain) {
            for (Transaction transaction : block.getTransactionLedger()) {
                if (Arrays.equals(transaction.getFrom(), walletAddress.getEncoded())) {
                    balance -= transaction.getValue();
                }
                if (Arrays.equals(transaction.getTo(), walletAddress.getEncoded())) {
                    balance += transaction.getValue();
                }
            }
        }
        for (Transaction transaction : currentLedger) {
            if (Arrays.equals(transaction.getFrom(), walletAddress.getEncoded())) {
                balance -= transaction.getValue();
            }
        }
        return balance;
    }

    private void verifyBlockchain(LinkedList<Block> currentBlockchain) throws GeneralSecurityException {
        for (Block block : currentBlockchain) {
            if (!block.isVerified(signing)) {
                throw new GeneralSecurityException("Block validation failed!");
            }
            List<Transaction> transactions = block.getTransactionLedger();
            for (Transaction transaction : transactions) {
                if (!transaction.isVerified(signing)) {
                    throw new GeneralSecurityException("Transaction validation failed!");
                }
            }
        }
    }

    public void addTransactionState(Transaction transaction) {
        newBlockTransactions.add(transaction);
        newBlockTransactions.sort(transactionComparator);
    }

    public void addTransaction(Transaction transaction, boolean blockReward) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("DSA");
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(transaction.getFrom());
            PublicKey publicKey = keyFactory.generatePublic(keySpec);

            if (getBalance(currentBlockchain, newBlockTransactions, publicKey) < transaction.getValue() && !blockReward) {
                throw new GeneralSecurityException("Not enough funds by sender to record transaction!");
            } else {
                Connection connection = DriverManager.getConnection
                        ("jdbc:sqlite:/home/dadry/Projects/NullCoin/NullCoin/db/blockchain");

                PreparedStatement pstmt;
                pstmt = connection.prepareStatement("INSERT INTO TRANSACTIONS" +
                        "(\"FROM\", \"TO\", LEDGER_ID, VALUE, SIGNATURE, CREATED_ON) " +
                        " VALUES (?,?,?,?,?,?) ");
                pstmt.setBytes(1, transaction.getFrom());
                pstmt.setBytes(2, transaction.getTo());
                pstmt.setInt(3, transaction.getLedgerId());
                pstmt.setInt(4, transaction.getValue());
                pstmt.setBytes(5, transaction.getSignature());
                pstmt.setString(6, transaction.getTimestamp());
                pstmt.executeUpdate();

                pstmt.close();
                connection.close();
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            System.out.println("Problem with DB: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private List<Transaction> loadTransactionLedger(Integer ledgerId) {
        List<Transaction> transactions = new ArrayList<>();
        try {
            Connection connection = DriverManager.getConnection
                    ("jdbc:sqlite:/home/dadry/Projects/NullCoin/NullCoin/db/blockchain");
            PreparedStatement pstmt = connection.prepareStatement
                    ("SELECT * FROM TRANSACTIONS WHERE LEDGER_ID = ?");
            pstmt.setInt(1, ledgerId);
            ResultSet resultSet = pstmt.executeQuery();
            while (resultSet.next()) {
                transactions.add(new Transaction(
                        resultSet.getBytes("FROM"),
                        resultSet.getBytes("TO"),
                        resultSet.getInt("VALUE"),
                        resultSet.getBytes("SIGNATURE"),
                        resultSet.getInt("LEDGER_ID"),
                        resultSet.getString("CREATED_ON")
                ));
            }
            resultSet.close();
            pstmt.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return transactions;
    }

    public void loadBlockchain() {
        try {
            Connection connection = DriverManager.getConnection
                    ("jdbc:sqlite:/home/dadry/Projects/NullCoin/NullCoin/db/blockchain");
            Statement stmt = connection.createStatement();
            ResultSet resultSet = stmt.executeQuery(" SELECT * FROM BLOCKCHAIN ");
            while (resultSet.next()) {
                this.currentBlockchain.add(new Block(
                        resultSet.getBytes("PREVIOUS_HASH"),
                        resultSet.getBytes("CURRENT_HASH"),
                        resultSet.getString("CREATED_ON"),
                        resultSet.getBytes("CREATED_BY"),
                        resultSet.getInt("LEDGER_ID"),
                        resultSet.getInt("MINING_POINTS"),
                        resultSet.getDouble("LUCK"),
                        loadTransactionLedger(resultSet.getInt("LEDGER_ID"))
                ));
            }

            latestBlock = currentBlockchain.getLast();
            Transaction transaction = new Transaction(new Wallet(),
                    WalletData.getInstance().getWallet().getPublicKey().getEncoded(),
                    100, latestBlock.getLedgerId() + 1, signing);
            newBlockTransactions.clear();
            newBlockTransactions.add(transaction);
            verifyBlockchain(currentBlockchain);
            resultSet.close();
            stmt.close();
            connection.close();
        } catch (SQLException | NoSuchAlgorithmException e) {
            LOG.info("Problem with DB: " + e.getMessage());
            e.printStackTrace();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
    }

    public void mineBlock() {
        try {
            finalizeBlock(WalletData.getInstance().getWallet());
            addBlock(latestBlock);
        } catch (SQLException | GeneralSecurityException e) {
            LOG.info("Problem with DB: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void finalizeBlock(Wallet minersWallet) throws SQLException, GeneralSecurityException {
        latestBlock = new Block(BlockchainData.getInstance().currentBlockchain);
        latestBlock.setTransactionLedger(new ArrayList<>(newBlockTransactions));
        latestBlock.setTimeStamp(LocalDateTime.now().toString());
        latestBlock.setMinedBy(minersWallet.getPublicKey().getEncoded());
        latestBlock.setMiningPoints(miningPoints);
        signing.initSign(minersWallet.getPrivateKey());
        signing.update(latestBlock.toString().getBytes());
        latestBlock.setCurrHash(signing.sign());
        currentBlockchain.add(latestBlock);
        miningPoints = 0;
        latestBlock.getTransactionLedger().sort(transactionComparator);
        addTransaction(latestBlock.getTransactionLedger().get(0), true);
        Transaction transaction = new Transaction(new Wallet(), minersWallet.getPublicKey().getEncoded(),
                100, latestBlock.getLedgerId() + 1, signing);
        newBlockTransactions.clear();
        newBlockTransactions.add(transaction);
    }

    private void addBlock(Block block) {
        try {
            Connection connection = DriverManager.getConnection
                    ("jdbc:sqlite:/home/dadry/Projects/NullCoin/NullCoin/db/blockchain");
            PreparedStatement pstmt;
            pstmt = connection.prepareStatement
                    ("INSERT INTO BLOCKCHAIN(PREVIOUS_HASH, CURRENT_HASH, LEDGER_ID, CREATED_ON," +
                            " CREATED_BY, MINING_POINTS, LUCK) VALUES (?,?,?,?,?,?,?) ");
            pstmt.setBytes(1, block.getPrevHash());
            pstmt.setBytes(2, block.getCurrHash());
            pstmt.setInt(3, block.getLedgerId());
            pstmt.setString(4, block.getTimeStamp());
            pstmt.setBytes(5, block.getMinedBy());
            pstmt.setInt(6, block.getMiningPoints());
            pstmt.setDouble(7, block.getLuck());
            pstmt.executeUpdate();
            pstmt.close();
            connection.close();
        } catch (SQLException e) {
            LOG.info("Problem with DB: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void replaceBlockchainInDatabase(LinkedList<Block> receivedBC) {
        try {
            Connection connection = DriverManager.getConnection
                    ("jdbc:sqlite:/home/dadry/Projects/NullCoin/NullCoin/db/blockchain");
            Statement clearDBStatement = connection.createStatement();
            clearDBStatement.executeUpdate(" DELETE FROM BLOCKCHAIN ");
            clearDBStatement.executeUpdate(" DELETE FROM TRANSACTIONS ");
            clearDBStatement.close();
            connection.close();

            for (Block block : receivedBC) {
                addBlock(block);
                boolean rewardTransaction = true;
                block.getTransactionLedger().sort(transactionComparator);
                for (Transaction transaction : block.getTransactionLedger()) {
                    addTransaction(transaction, rewardTransaction);
                    rewardTransaction = false;
                }
            }
        } catch (SQLException e) {
            LOG.info("Problem with DB: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public LinkedList<Block> getBlockchainConsensus(LinkedList<Block> receivedBC) {
        try{
            verifyBlockchain(receivedBC);
            if (!Arrays.equals(receivedBC.getLast().getCurrHash(), getCurrentBlockchain().getLast().getCurrHash())) {
                if (checkIfOutdated(receivedBC) != null) {
                    return getCurrentBlockchain();
                } else {
                    if (checkWhichIsCreatedFirst(receivedBC) != null) {
                        return getCurrentBlockchain();
                    } else {
                        if (compareMiningPointsAndLuck(receivedBC) != null) {
                            return getCurrentBlockchain();
                        }
                    }
                }
            } else if (!receivedBC.getLast().getTransactionLedger().equals(getCurrentBlockchain()
                    .getLast().getTransactionLedger())) {
                updateTransactionLedgers(receivedBC);
                LOG.info("Transaction ledgers updated");
                return receivedBC;
            } else {
                LOG.info("Blockchains are identical");
            }
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }

        return receivedBC;
    }

    private LinkedList<Block> checkIfOutdated(LinkedList<Block> receivedBC) {
        long lastMinedLocalBlock = LocalDateTime.parse
                (getCurrentBlockchain().getLast().getTimeStamp()).toEpochSecond(ZoneOffset.UTC);
        long lastMinedReceivedBlock = LocalDateTime.parse
                (receivedBC.getLast().getTimeStamp()).toEpochSecond(ZoneOffset.UTC);
        if ((lastMinedLocalBlock + TIMEOUT_INTERVAL) < LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
                && (lastMinedReceivedBlock +  TIMEOUT_INTERVAL) < LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)) {
            LOG.info("Both are old check other peers");
        } else if ((lastMinedLocalBlock + TIMEOUT_INTERVAL) < LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
                && (lastMinedReceivedBlock +  TIMEOUT_INTERVAL) >= LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)) {
            setMiningPoints(0);
            replaceBlockchainInDatabase(receivedBC);
            setCurrentBlockchain(receivedBC);
            loadBlockchain();
            LOG.info("Received blockchain won! Local blockchain was old");
        } else if ((lastMinedLocalBlock + TIMEOUT_INTERVAL) > LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
                && (lastMinedReceivedBlock + TIMEOUT_INTERVAL) <  LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)) {
            return getCurrentBlockchain();
        }
        
        return null;
    }

    private LinkedList<Block> checkWhichIsCreatedFirst(LinkedList<Block> receivedBC) {
        long initReceiveBlockTime = LocalDateTime.parse(receivedBC.getFirst().getTimeStamp())
                .toEpochSecond(ZoneOffset.UTC);
        long initLocalBlockTime = LocalDateTime.parse(getCurrentBlockchain().getFirst().getTimeStamp()).toEpochSecond(ZoneOffset.UTC);
        if (initReceiveBlockTime < initLocalBlockTime) {
            setMiningPoints(0);
            replaceBlockchainInDatabase(receivedBC);
            setCurrentBlockchain(new LinkedList<>());
            loadBlockchain();
            LOG.info("PeerClient blockchain won! PeerServer's blockchain was old");
        } else if (initLocalBlockTime < initReceiveBlockTime) {
            return getCurrentBlockchain();
        }

        return null;
    }

    private LinkedList<Block> compareMiningPointsAndLuck(LinkedList<Block> receivedBC) throws GeneralSecurityException {
        if (receivedBC.equals(getCurrentBlockchain())) {
            if (receivedBC.getLast().getMiningPoints() > getCurrentBlockchain()
                    .getLast().getMiningPoints() || receivedBC.getLast().getMiningPoints()
                    .equals(getCurrentBlockchain().getLast().getMiningPoints()) &&
                    receivedBC.getLast().getLuck() > getCurrentBlockchain().getLast().getLuck()) {
                getCurrentBlockchain().getLast().getTransactionLedger().remove(0);
                for (Transaction transaction : getCurrentBlockchain().getLast().getTransactionLedger()) {
                    if (!receivedBC.getLast().getTransactionLedger().contains(transaction)) {
                        receivedBC.getLast().getTransactionLedger().add(transaction);
                    }
                }
                receivedBC.getLast().getTransactionLedger().sort(transactionComparator);
                setMiningPoints(BlockchainData.getInstance().getMiningPoints() +
                        getCurrentBlockchain().getLast().getMiningPoints());
                replaceBlockchainInDatabase(receivedBC);
                setCurrentBlockchain(new LinkedList<>());
                loadBlockchain();
                LOG.info("Received blockchain won!");
            } else {
                receivedBC.getLast().getTransactionLedger().remove(0);
                for (Transaction transaction : receivedBC.getLast().getTransactionLedger()) {
                    if (!getCurrentBlockchain().getLast().getTransactionLedger().contains(transaction)) {
                        getCurrentBlockchain().getLast().getTransactionLedger().add(transaction);
                        addTransaction(transaction, false);
                    }
                }
                getCurrentBlockchain().getLast().getTransactionLedger().sort(transactionComparator);
                return getCurrentBlockchain();
            }
        }
        return null;
    }

    private void updateTransactionLedgers(LinkedList<Block> receivedBC) throws GeneralSecurityException {
        for (Transaction transaction : receivedBC.getLast().getTransactionLedger()) {
            if (!getCurrentBlockchain().getLast().getTransactionLedger().contains(transaction) ) {
                getCurrentBlockchain().getLast().getTransactionLedger().add(transaction);
                LOG.info("current ledger id = " + getCurrentBlockchain().getLast().getLedgerId() + " transaction id = " + transaction.getLedgerId());
                addTransaction(transaction, false);
            }
        }
        getCurrentBlockchain().getLast().getTransactionLedger().sort(transactionComparator);
        for (Transaction transaction : getCurrentBlockchain().getLast().getTransactionLedger()) {
            if (!receivedBC.getLast().getTransactionLedger().contains(transaction) ) {
                getCurrentBlockchain().getLast().getTransactionLedger().add(transaction);
            }
        }
        receivedBC.getLast().getTransactionLedger().sort(transactionComparator);
    }

    public LinkedList<Block> getCurrentBlockchain() {
        return currentBlockchain;
    }

    public void setCurrentBlockchain(LinkedList<Block> currentBlockChain) {
        this.currentBlockchain = currentBlockChain;
    }

    public static int getTimeoutInterval() { return TIMEOUT_INTERVAL; }

    public static int getMiningInterval() { return MINING_INTERVAL; }

    public int getMiningPoints() {
        return miningPoints;
    }

    public void setMiningPoints(int miningPoints) {
        this.miningPoints = miningPoints;
    }

    public boolean isExit() {
        return exit;
    }

    public void setExit(boolean exit) {
        this.exit = exit;
    }
}
