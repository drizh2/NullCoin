package com.dadry.Model;

import java.io.Serializable;
import java.security.*;

public class Wallet implements Serializable {
    private KeyPair keyPair;

    public Wallet() throws NoSuchAlgorithmException {
        this(2048, KeyPairGenerator.getInstance("DSA"));
    }

    public Wallet(Integer keySize, KeyPairGenerator keyPairGenerator) {
        keyPairGenerator.initialize(keySize);
        this.keyPair = keyPairGenerator.generateKeyPair();
    }

    public Wallet(PublicKey publicKey, PrivateKey privateKey) {
        this.keyPair = new KeyPair(publicKey, privateKey);
    }

    public KeyPair getKeyPair() {
        return keyPair;
    }

    public void setKeyPair(KeyPair keyPair) {
        this.keyPair = keyPair;
    }

    public PublicKey getPublicKey() {
        return keyPair.getPublic();
    }

    public PrivateKey getPrivateKey() {
        return keyPair.getPrivate();
    }
}
