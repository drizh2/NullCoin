package com.dadry;

import java.util.logging.Logger;
import com.dadry.Model.Block;
import com.dadry.Service.BlockchainData;
import com.dadry.Service.WalletData;
import com.dadry.Model.Transaction;
import com.dadry.Model.Wallet;
import com.dadry.Threads.MiningThread;
import com.dadry.Threads.PeerClient;
import com.dadry.Threads.PeerServer;
import com.dadry.Threads.UI;
import javafx.application.Application;
import javafx.stage.Stage;

import java.security.*;
import java.sql.*;
import java.time.LocalDateTime;

public class NullCoin extends Application {

    Logger logger = Logger.getLogger(getClass().getName());

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        new UI().start(primaryStage);
        new PeerClient().start();
        new PeerServer(6000).start();
        new MiningThread().start();
    }

    @Override
    public void init() throws Exception {
        try {
            // -------- WALLET DB ------------
            Connection walletConnection = DriverManager
                    .getConnection("jdbc:sqlite:/home/dadry/Projects/NullCoin/NullCoin/db/wallet.db");
            Statement walletStatment = walletConnection.createStatement();
            walletStatment.executeUpdate("CREATE TABLE IF NOT EXISTS WALLET ( " +
                    " PRIVATE_KEY BLOB NOT NULL UNIQUE, " +
                    " PUBLIC_KEY BLOB NOT NULL UNIQUE, " +
                    " PRIMARY KEY (PRIVATE_KEY, PUBLIC_KEY)" +
                    ") "
            );
            ResultSet resultSet = walletStatment.executeQuery(" SELECT * FROM WALLET ");
            if (!resultSet.next()) {
                Wallet newWallet = new Wallet();
                byte[] pubBlob = newWallet.getPublicKey().getEncoded();
                byte[] prvBlob = newWallet.getPrivateKey().getEncoded();
                PreparedStatement pstmt = walletConnection
                        .prepareStatement("INSERT INTO WALLET(PRIVATE_KEY, PUBLIC_KEY) " +
                                " VALUES (?,?) ");
                pstmt.setBytes(1, prvBlob);
                pstmt.setBytes(2, pubBlob);
                pstmt.executeUpdate();
            }
            resultSet.close();
            walletStatment.close();
            walletConnection.close();
            WalletData.getInstance().loadWallet();

            // -------- BLOCKCHAIN DB ------------
            Connection blockchainConnection = DriverManager
                    .getConnection("jdbc:sqlite:/home/dadry/Projects/NullCoin/NullCoin/db/blockchain.db");
            Statement blockchainStmt = blockchainConnection.createStatement();
            blockchainStmt.executeUpdate("CREATE TABLE IF NOT EXISTS BLOCKCHAIN ( " +
                    " ID INTEGER NOT NULL UNIQUE, " +
                    " PREVIOUS_HASH BLOB UNIQUE, " +
                    " CURRENT_HASH BLOB UNIQUE, " +
                    " LEDGER_ID INTEGER NOT NULL UNIQUE, " +
                    " CREATED_ON  TEXT, " +
                    " CREATED_BY  BLOB, " +
                    " MINING_POINTS  TEXT, " +
                    " LUCK  NUMERIC, " +
                    " PRIMARY KEY( ID AUTOINCREMENT) " +
                    ")"
            );

            ResultSet resultSetBlockchain = blockchainStmt.executeQuery(" SELECT * FROM BLOCKCHAIN ");
            Transaction initBlockRewardTransaction = null;
            if (!resultSetBlockchain.next()) {
                Block firstBlock = new Block();
                firstBlock.setMinedBy(WalletData.getInstance().getWallet().getPublicKey().getEncoded());
                firstBlock.setTimeStamp(LocalDateTime.now().toString());
                Signature signing = Signature.getInstance("SHA256withDSA");
                signing.initSign(WalletData.getInstance().getWallet().getPrivateKey());
                signing.update(firstBlock.toString().getBytes());
                firstBlock.setCurrHash(signing.sign());
                PreparedStatement pstmt = blockchainConnection
                        .prepareStatement("INSERT INTO BLOCKCHAIN(PREVIOUS_HASH, CURRENT_HASH , LEDGER_ID," +
                                " CREATED_ON, CREATED_BY,MINING_POINTS,LUCK ) " +
                                " VALUES (?,?,?,?,?,?,?) ");
                pstmt.setBytes(1, firstBlock.getPrevHash());
                pstmt.setBytes(2, firstBlock.getCurrHash());
                pstmt.setInt(3, firstBlock.getLedgerId());
                pstmt.setString(4, firstBlock.getTimeStamp());
                pstmt.setBytes(5, WalletData.getInstance().getWallet().getPublicKey().getEncoded());
                pstmt.setInt(6, firstBlock.getMiningPoints());
                pstmt.setDouble(7, firstBlock.getLuck());
                pstmt.executeUpdate();
                Signature transSignature = Signature.getInstance("SHA256withDSA");
                initBlockRewardTransaction = new Transaction(WalletData.getInstance().getWallet(),WalletData.getInstance().getWallet().getPublicKey().getEncoded(),100,1,transSignature);
            }
            resultSetBlockchain.close();

            blockchainStmt.executeUpdate("CREATE TABLE IF NOT EXISTS TRANSACTIONS ( " +
                    " ID INTEGER NOT NULL UNIQUE, " +
                    " \"FROM\" BLOB, " +
                    " \"TO\" BLOB, " +
                    " LEDGER_ID INTEGER, " +
                    " VALUE INTEGER, " +
                    " SIGNATURE BLOB UNIQUE, " +
                    " CREATED_ON TEXT, " +
                    " PRIMARY KEY(ID AUTOINCREMENT) " +
                    ")"
            );
            if (initBlockRewardTransaction != null) {
                BlockchainData.getInstance().addTransaction(initBlockRewardTransaction,true);
                BlockchainData.getInstance().addTransactionState(initBlockRewardTransaction);
            }
            blockchainStmt.close();
            blockchainConnection.close();

            // -------- NEW NETWORK DB (PORTS) ------------
            Connection networkConnection = DriverManager
                    .getConnection("jdbc:sqlite:/home/dadry/Projects/NullCoin/NullCoin/db/network.db");
            Statement networkStmt = networkConnection.createStatement();
            networkStmt.executeUpdate("CREATE TABLE IF NOT EXISTS PORTS ( " +
                    " ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    " PORT INTEGER NOT NULL UNIQUE " +
                    ")");

            ResultSet portResultSet = networkStmt.executeQuery("SELECT COUNT(*) AS total FROM PORTS");
            if (portResultSet.next() && portResultSet.getInt("total") == 0) {
                PreparedStatement portInsert = networkConnection
                        .prepareStatement("INSERT INTO PORTS(PORT) VALUES (?)");
                int[] defaultPorts = {6001, 6002};
                for (int port : defaultPorts) {
                    portInsert.setInt(1, port);
                    portInsert.executeUpdate();
                }
                portInsert.close();
            }
            portResultSet.close();
            networkStmt.close();
            networkConnection.close();
        } catch (SQLException |
                NoSuchAlgorithmException |
                InvalidKeyException |
                SignatureException e
        ) {
            logger.info("db failed: " + e.getMessage());
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }

        BlockchainData.getInstance().loadBlockchain();
    }
}
