package org.sagebionetworks.bridge.dataUploadUtils;


import static com.google.common.base.Preconditions.checkState;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import org.bouncycastle.cms.CMSAlgorithm;
import org.bouncycastle.cms.CMSEnvelopedDataStreamGenerator;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.jcajce.JceCMSContentEncryptorBuilder;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientInfoGenerator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OutputEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encrypt data using a Study's public key under Bouncy Castle.
 */
public class StudyUploadEncryptorBC implements StudyUploadEncryptor {
    private static final Logger LOG = LoggerFactory.getLogger(StudyUploadEncryptorBC.class);

    private static final String JCE_PROVIDER = "BC"; // BouncyCastle

    private final Supplier<JceKeyTransRecipientInfoGenerator> recipientInfoGeneratorSupplier;

    static {
        // Dynamically register Cryptographic Service Provider
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
    }

    public StudyUploadEncryptorBC(X509Certificate publicKey) {
        this.recipientInfoGeneratorSupplier = Suppliers.memoize(() -> {
            try {
                return new JceKeyTransRecipientInfoGenerator(publicKey).setProvider(JCE_PROVIDER);
            } catch (CertificateEncodingException e) {
                LOG.error("Unable to create recipient archiveInfo generator from public key", e);
            }
            return null;
        });
    }

    /**
     * @param stream plaintext stream
     * @return encrypted stream
     * @throws CMSException problem with encryption
     * @throws IOException  problem with stream
     */
    public OutputStream encrypt(OutputStream stream) throws CMSException, IOException {
        JceKeyTransRecipientInfoGenerator recipientInfoGenerator = recipientInfoGeneratorSupplier
                .get();
        checkState(recipientInfoGenerator != null, "RecipientInfoGenerator was not initialized successfully");

        CMSEnvelopedDataStreamGenerator gen = new CMSEnvelopedDataStreamGenerator();
        gen.addRecipientInfoGenerator(recipientInfoGenerator);

        // Generate encrypted input stream in AES-256-CBC format, output is DER, not S/MIME or PEM
        OutputEncryptor encryptor =
                new JceCMSContentEncryptorBuilder(CMSAlgorithm.AES256_CBC).setProvider(JCE_PROVIDER)
                        .build();

        return gen.open(stream, encryptor);
    }

    /**
     * Util method to encrypt input file to given output file path using given public key
     * @param publicKey
     * @param inputFilePath
     * @param outputFilePath
     * @return
     * @throws CertificateException
     * @throws IOException
     * @throws CMSException
     */
    public static void writeTo(String publicKey, String inputFilePath, String outputFilePath)
            throws CertificateException, IOException, CMSException {
        InputStream in = new ByteArrayInputStream(publicKey.getBytes(StandardCharsets.UTF_8));
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) factory.generateCertificate(in);

        StudyUploadEncryptorBC encryptor = new StudyUploadEncryptorBC(cert);
        in.close();

        byte[] buffer = new byte[1024];
        File inputFile = new File(inputFilePath);
        try (FileOutputStream fos = new FileOutputStream(outputFilePath);
                OutputStream os = encryptor.encrypt(fos);
                FileInputStream fis = new FileInputStream(inputFile)) {

            int length;
            while ((length = fis.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
            os.flush();
        }
    }
}
