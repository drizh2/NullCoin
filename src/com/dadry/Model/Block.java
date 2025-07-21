package com.dadry.Model;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Block implements Serializable {
    private byte[] prevHash;
    private byte[] currHash;
    private String timeStamp;
    private byte[] minedBy;
    private Integer ledgerId;
    private Integer miningPoints;
    private Double luck = 0.0;

    private List<Transaction> transactionLedger = new ArrayList<>();

    // For retrieving from DB
    public Block(byte[] prevHash, byte[] currHash, String timeStamp, byte[] minedBy, Integer ledgerId, Integer miningPoints, Double luck, List<Transaction> transactionLedger) {
        this.prevHash = prevHash;
        this.currHash = currHash;
        this.timeStamp = timeStamp;
        this.minedBy = minedBy;
        this.ledgerId = ledgerId;
        this.miningPoints = miningPoints;
        this.luck = luck;
        this.transactionLedger = transactionLedger;
    }

    // For initiating after retrieve
    public Block(LinkedList<Block> currentBlockchain) {
        Block lastBlock = currentBlockchain.getLast();
        prevHash = lastBlock.getCurrHash();
        ledgerId = lastBlock.getLedgerId() + 1;
        luck = Math.random() * 1000000;
    }

    // For creating first block only
    public Block() {
        prevHash = new byte[]{0};
    }

    public Boolean isVerified(Signature signature)
            throws NoSuchAlgorithmException,
            InvalidKeySpecException,
            InvalidKeyException,
            SignatureException {
        KeyFactory keyFactory = KeyFactory.getInstance("DSA");
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(this.minedBy); // minedBy = byte[]
        PublicKey publicKey = keyFactory.generatePublic(keySpec);

        signature.initVerify(publicKey);
        signature.update(this.toString().getBytes());
        return signature.verify(this.currHash);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;

        if (!(obj instanceof Block)) return false;

        Block block = (Block) obj;
        return Arrays.equals(getPrevHash(), block.getPrevHash());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(getPrevHash());
    }

    public byte[] getPrevHash() {
        return prevHash;
    }

    public void setPrevHash(byte[] prevHash) {
        this.prevHash = prevHash;
    }

    public byte[] getCurrHash() {
        return currHash;
    }

    public void setCurrHash(byte[] currHash) {
        this.currHash = currHash;
    }

    public List<Transaction> getTransactionLedger() {
        return transactionLedger;
    }

    public void setTransactionLedger(List<Transaction> transactionLedger) {
        this.transactionLedger = transactionLedger;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public byte[] getMinedBy() {
        return minedBy;
    }

    public void setMinedBy(byte[] minedBy) {
        this.minedBy = minedBy;
    }

    public Integer getMiningPoints() {
        return miningPoints;
    }

    public void setMiningPoints(Integer miningPoints) {
        this.miningPoints = miningPoints;
    }

    public Double getLuck() {
        return luck;
    }

    public void setLuck(Double luck) {
        this.luck = luck;
    }

    public Integer getLedgerId() {
        return ledgerId;
    }

    public void setLedgerId(Integer ledgerId) {
        this.ledgerId = ledgerId;
    }

    @Override
    public String toString() {
        return "Block{" +
                "prevHash=" + Arrays.toString(prevHash) +
                ", timeStamp='" + timeStamp + '\'' +
                ", minedBy=" + Arrays.toString(minedBy) +
                ", ledgerId=" + ledgerId +
                ", miningPoints=" + miningPoints +
                ", luck=" + luck +
                '}';
    }
}

