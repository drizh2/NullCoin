package com.dadry.Model;

import java.io.Serializable;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;

public class Transaction implements Serializable {
    private byte[] from;
    private String fromFx;
    private byte[] to;
    private String toFx;
    private Integer value;
    private String timestamp;
    private byte[] signature;
    private String signatureFx;
    private Integer ledgerId;

    // For retrieving from DB
    public Transaction(byte[] from, byte[] to, Integer value, byte[] signature, Integer ledgerId, String timestamp) {
        Base64.Encoder encoder = Base64.getEncoder();

        this.from = from;
        this.fromFx = encoder.encodeToString(from);
        this.to = to;
        this.toFx = encoder.encodeToString(to);
        this.value = value;
        this.signature = signature;
        this.signatureFx = encoder.encodeToString(signature);
        this.ledgerId = ledgerId;
        this.timestamp = timestamp;
    }

    // For creating a new transaction
    public Transaction(Wallet fromWallet, byte[] toAddress, Integer value, Integer ledgerId, Signature signature)
            throws SignatureException,
            InvalidKeyException {
        Base64.Encoder encoder = Base64.getEncoder();
        this.from = fromWallet.getPublicKey().getEncoded();
        this.fromFx = encoder.encodeToString(fromWallet.getPublicKey().getEncoded());
        this.to = toAddress;
        this.toFx = encoder.encodeToString(toAddress);
        this.value = value;
        this.ledgerId = ledgerId;
        this.timestamp = LocalDateTime.now().toString();
        signature.initSign(fromWallet.getPrivateKey());
        String sr = this.toString();
        signature.update(sr.getBytes());
        this.signature = signature.sign();
        this.signatureFx = encoder.encodeToString(this.signature);
    }

    public Boolean isVerified(Signature signature)
            throws NoSuchAlgorithmException,
            InvalidKeySpecException,
            InvalidKeyException,
            SignatureException {
        KeyFactory keyFactory = KeyFactory.getInstance("DSA");
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(this.getFrom());
        PublicKey publicKey = keyFactory.generatePublic(keySpec);

        signature.initVerify(publicKey);
        signature.update(this.toString().getBytes());
        return signature.verify(this.signature);
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "from=" + Arrays.toString(from) +
                ", to=" + Arrays.toString(to) +
                ", value=" + value +
                ", timestamp='" + timestamp + '\'' +
                ", ledgerId=" + ledgerId +
                '}';
    }

    public byte[] getFrom() {
        return from;
    }

    public void setFrom(byte[] from) {
        this.from = from;
    }

    public String getFromFx() {
        return fromFx;
    }

    public void setFromFx(String fromFx) {
        this.fromFx = fromFx;
    }

    public byte[] getTo() {
        return to;
    }

    public void setTo(byte[] to) {
        this.to = to;
    }

    public String getToFx() {
        return toFx;
    }

    public void setToFx(String toFx) {
        this.toFx = toFx;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public byte[] getSignature() {
        return signature;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

    public String getSignatureFx() {
        return signatureFx;
    }

    public void setSignatureFx(String signatureFx) {
        this.signatureFx = signatureFx;
    }

    public Integer getLedgerId() {
        return ledgerId;
    }

    public void setLedgerId(Integer ledgerId) {
        this.ledgerId = ledgerId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Transaction)) return false;

        Transaction that = (Transaction) o;
        return Arrays.equals(getSignature(), that.getSignature());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(getSignature());
    }
}
