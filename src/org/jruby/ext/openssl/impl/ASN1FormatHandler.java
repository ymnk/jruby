package org.jruby.ext.openssl.impl;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidParameterSpecException;

import javax.crypto.spec.DHParameterSpec;

public interface ASN1FormatHandler {

    public static final String BEF = "-----";
    public static final String AFT = "-----";
    public static final String BEF_G = BEF + "BEGIN ";
    public static final String BEF_E = BEF + "END ";
    public static final String PEM_STRING_X509_OLD = "X509 CERTIFICATE";
    public static final String PEM_STRING_X509 = "CERTIFICATE";
    public static final String PEM_STRING_X509_PAIR = "CERTIFICATE PAIR";
    public static final String PEM_STRING_X509_TRUSTED = "TRUSTED CERTIFICATE";
    public static final String PEM_STRING_X509_REQ_OLD = "NEW CERTIFICATE REQUEST";
    public static final String PEM_STRING_X509_REQ = "CERTIFICATE REQUEST";
    public static final String PEM_STRING_X509_CRL = "X509 CRL";
    public static final String PEM_STRING_EVP_PKEY = "ANY PRIVATE KEY";
    public static final String PEM_STRING_PUBLIC = "PUBLIC KEY";
    public static final String PEM_STRING_RSA = "RSA PRIVATE KEY";
    public static final String PEM_STRING_RSA_PUBLIC = "RSA PUBLIC KEY";
    public static final String PEM_STRING_DSA = "DSA PRIVATE KEY";
    public static final String PEM_STRING_DSA_PUBLIC = "DSA PUBLIC KEY";
    public static final String PEM_STRING_PKCS7 = "PKCS7";
    public static final String PEM_STRING_PKCS8 = "ENCRYPTED PRIVATE KEY";
    public static final String PEM_STRING_PKCS8INF = "PRIVATE KEY";
    public static final String PEM_STRING_DHPARAMS = "DH PARAMETERS";
    public static final String PEM_STRING_SSL_SESSION = "SSL SESSION PARAMETERS";
    public static final String PEM_STRING_DSAPARAMS = "DSA PARAMETERS";
    public static final String PEM_STRING_ECDSA_PUBLIC = "ECDSA PUBLIC KEY";
    public static final String PEM_STRING_ECPARAMETERS = "EC PARAMETERS";
    public static final String PEM_STRING_ECPRIVATEKEY = "EC PRIVATE KEY";

    /**
     * c: PEM_X509_INFO_read_bio
     */
    public abstract Object readPEM(Reader in, char[] f) throws IOException;

    public abstract byte[] readPEMToDER(Reader in) throws IOException;

    /**
     * c: PEM_read_PrivateKey + PEM_read_bio_PrivateKey
     * CAUTION: KeyPair#getPublic() may be null.
     */
    public abstract KeyPair readPrivateKey(Reader in, char[] password) throws IOException;

    /*
     * c: PEM_read_bio_DSA_PUBKEY
     */
    public abstract DSAPublicKey readDSAPubKey(Reader in, char[] f) throws IOException;

    /*
     * c: PEM_read_bio_DSAPublicKey
     */
    public abstract DSAPublicKey readDSAPublicKey(Reader in, char[] f) throws IOException;

    /*
     * c: PEM_read_bio_DSAPrivateKey
     */
    public abstract KeyPair readDSAPrivateKey(Reader in, char[] f) throws IOException;

    /**
     * reads an RSA public key encoded in an SubjectPublicKeyInfo RSA structure.
     * c: PEM_read_bio_RSA_PUBKEY
     */
    public abstract RSAPublicKey readRSAPubKey(Reader in, char[] f) throws IOException;

    /**
     * reads an RSA public key encoded in an PKCS#1 RSA structure.
     * c: PEM_read_bio_RSAPublicKey
     */
    public abstract RSAPublicKey readRSAPublicKey(Reader in, char[] f) throws IOException;

    /**
     * c: PEM_read_bio_RSAPrivateKey
     */
    public abstract KeyPair readRSAPrivateKey(Reader in, char[] f) throws IOException;

    public abstract X509CRL readX509CRL(Reader in, char[] f) throws IOException;

    public abstract DHParameterSpec readDHParameters(Reader _in) throws IOException, InvalidParameterSpecException;

    public abstract void writeDSAPublicKey(Writer _out, DSAPublicKey obj) throws IOException;

    /** writes an RSA public key encoded in an PKCS#1 RSA structure. */
    public abstract void writeRSAPublicKey(Writer _out, RSAPublicKey obj) throws IOException;

    public abstract void writePKCS7(Writer _out, byte[] encoded) throws IOException;

    public abstract void writeX509Certificate(Writer _out, X509Certificate obj) throws IOException;

    public abstract void writeX509CRL(Writer _out, X509CRL obj) throws IOException;

    public abstract void writeDSAPrivateKey(Writer _out, DSAPrivateKey obj, String algo, char[] f) throws IOException;

    public abstract void writeRSAPrivateKey(Writer _out, RSAPrivateCrtKey obj, String algo, char[] f) throws IOException;

    public abstract void writeDHParameters(Writer _out, DHParameterSpec params) throws IOException;

    /* 
     * Handles PKey related ASN.1 handling.
     */
    
    // d2i_RSAPrivateKey_bio
    public abstract PrivateKey readRSAPrivateKey(String input) throws IOException, GeneralSecurityException;

    // d2i_RSAPublicKey_bio
    public abstract PublicKey readRSAPublicKey(String input) throws IOException, GeneralSecurityException;

    // d2i_DSAPrivateKey_bio
    public abstract KeyPair readDSAPrivateKey(String input) throws IOException, GeneralSecurityException;

    // d2i_DSA_PUBKEY_bio
    public abstract PublicKey readDSAPublicKey(String input) throws IOException, GeneralSecurityException;

    public abstract byte[] toDerRSAKey(RSAPublicKey pubKey, RSAPrivateCrtKey privKey) throws IOException;
    
    public abstract byte[] toDerDSAKey(DSAPublicKey pubKey, DSAPrivateKey privKey) throws IOException;

    public abstract byte[] toDerDHKey(BigInteger p, BigInteger g) throws IOException;
}