package org.sagebionetworks.bridge.dataUploadUtils;


import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;
import java.io.OutputStream;
import java.security.Security;
import java.security.cert.CertificateEncodingException;
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

public class StudyUploadEncryptorBC implements StudyUploadEncryptor {
    private static final Logger LOG = LoggerFactory.getLogger(StudyUploadEncryptorSC.class);

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
}
