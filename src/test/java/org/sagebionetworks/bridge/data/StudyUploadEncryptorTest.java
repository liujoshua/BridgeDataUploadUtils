package org.sagebionetworks.bridge.data;

import static junit.framework.TestCase.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.bouncycastle.cms.CMSException;
import org.testng.annotations.Test;

import org.sagebionetworks.bridge.crypto.BcCertificateFactory;
import org.sagebionetworks.bridge.crypto.BcCmsEncryptor;
import org.sagebionetworks.bridge.crypto.CertificateFactory;
import org.sagebionetworks.bridge.crypto.CertificateInfo;
import org.sagebionetworks.bridge.crypto.KeyPairFactory;
import org.sagebionetworks.bridge.crypto.PemUtils;

public class StudyUploadEncryptorTest {
    private static final String TEST_DATA = "this is a test data";

    @Test
    public void test() throws IOException, CertificateEncodingException, CMSException {
        // first generate pub and priv keys
        CertificateFactory certFactory = new BcCertificateFactory();
        KeyPair keyPair = KeyPairFactory.newRsa2048();
        X509Certificate cert = certFactory.newCertificate(keyPair, new CertificateInfo.Builder().build());
        PrivateKey privateKey = keyPair.getPrivate();

        StudyUploadEncryptor encryptor = new StudyUploadEncryptor(cert);
        BcCmsEncryptor correctEncryptor = new BcCmsEncryptor(cert, privateKey);

        // then test encrypt
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                OutputStream os = encryptor.encrypt(bos)) {
            os.write(TEST_DATA.getBytes(Charset.forName("UTF-8")));

            os.close();
            bos.close();

            byte[] encryptedData = bos.toByteArray();

            String decryptedData = new String(correctEncryptor.decrypt(encryptedData), Charset.forName("UTF-8"));

            assertEquals(TEST_DATA, decryptedData);
        }
    }

    @Test
    public void testWriteTo() throws CertificateException, CMSException, IOException {
        // first write a test input file
        try(PrintWriter out = new PrintWriter( "./resource/inputFile" )){
            out.println(TEST_DATA);
        }

        // encrypt
        String cert = new String(Files.readAllBytes(Paths.get("./src/test/java/resources/cms/rsacert.pem")), Charset.forName("UTF-8"));
        StudyUploadEncryptor.writeTo(cert, "./resource/inputFile", "./resource/outputFile");

        // verify by decrypting
        byte[] output = Files.readAllBytes(Paths.get("./resource/outputFile"));
        String privkey = new String(Files.readAllBytes(Paths.get("./src/test/java/resources/cms/rsaprivkey.pem")), Charset.forName("UTF-8"));

        X509Certificate actualCert = PemUtils.loadCertificateFromPem(cert);
        PrivateKey privateKey = PemUtils.loadPrivateKeyFromPem(privkey);

        BcCmsEncryptor encryptor = new BcCmsEncryptor(actualCert, privateKey);
        String decryptedData = new String(encryptor.decrypt(output), Charset.forName("UTF-8"));

        assertEquals(TEST_DATA, decryptedData.trim());
    }
}
