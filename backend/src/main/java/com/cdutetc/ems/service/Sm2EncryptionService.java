package com.cdutetc.ems.service;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.crypto.engines.SM2Engine;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.Security;
import java.security.spec.X509EncodedKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * SM2 加密服务
 * 用于四川协议数据上报的加密
 */
@Service
@Slf4j
public class Sm2EncryptionService {

    static {
        // 注册 BouncyCastle 提供者
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * 使用 SM2 公钥加密数据
     *
     * @param plaintext 明文
     * @param publicKeyHex 公钥（Hex编码）
     * @return 密文（Base64编码）
     */
    public String encrypt(String plaintext, String publicKeyHex) {
        try {
            log.debug("🔒 开始SM2加密，明文长度: {}", plaintext.length());

            // 1. 解码公钥
            byte[] publicKeyBytes = Hex.decode(publicKeyHex);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME);
            BCECPublicKey publicKey = (BCECPublicKey) keyFactory.generatePublic(keySpec);

            // 2. 创建 SM2 引擎
            SM2Engine engine = new SM2Engine();

            // 构建ECPublicKeyParameters，需要ECPoint和ECDomainParameters
            ECPoint ecPoint = publicKey.getQ();
            ECDomainParameters domainParams = new ECDomainParameters(
                publicKey.getParameters().getCurve(),
                publicKey.getParameters().getG(),
                publicKey.getParameters().getN(),
                publicKey.getParameters().getH()
            );
            ECPublicKeyParameters publicKeyParams = new ECPublicKeyParameters(ecPoint, domainParams);
            engine.init(true, publicKeyParams);

            // 3. 加密
            byte[] plaintextBytes = plaintext.getBytes(StandardCharsets.UTF_8);
            byte[] ciphertext = engine.processBlock(plaintextBytes, 0, plaintextBytes.length);

            // 4. Base64 编码
            String result = Base64.toBase64String(ciphertext);

            log.debug("✅ SM2加密完成，密文长度: {}", result.length());
            return result;

        } catch (Exception e) {
            log.error("❌ SM2加密失败: {}", e.getMessage(), e);
            throw new RuntimeException("SM2加密失败", e);
        }
    }

    /**
     * 使用 SM2 公钥加密数据（默认公钥）
     *
     * @param plaintext 明文
     * @return 密文（Base64编码）
     */
    public String encrypt(String plaintext) {
        // 注意：实际使用时应从配置中读取公钥
        // 这里为了演示，使用空公钥，实际需要配置
        throw new UnsupportedOperationException("请使用 encrypt(plaintext, publicKey) 方法并提供公钥");
    }

    /**
     * 检查公钥格式是否正确
     *
     * @param publicKeyHex 公钥（Hex编码）
     * @return 是否有效
     */
    public boolean isValidPublicKey(String publicKeyHex) {
        try {
            if (publicKeyHex == null || publicKeyHex.isEmpty()) {
                return false;
            }

            // 尝试解码公钥
            byte[] publicKeyBytes = Hex.decode(publicKeyHex);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME);
            keyFactory.generatePublic(keySpec);

            return true;

        } catch (Exception e) {
            log.warn("⚠️ 无效的SM2公钥: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 格式化公钥（去除空格、换行等）
     *
     * @param publicKey 原始公钥
     * @return 格式化后的公钥（Hex编码，无前缀）
     */
    public String formatPublicKey(String publicKey) {
        if (publicKey == null) {
            return null;
        }

        // 去除空格、换行等
        String formatted = publicKey.replaceAll("\\s+", "");

        // 如果是 Hex 格式（04开头），直接返回
        if (formatted.startsWith("04")) {
            return formatted;
        }

        // 如果是 PEM 格式，需要解析
        if (formatted.contains("BEGIN PUBLIC KEY")) {
            log.warn("⚠️ 暂不支持 PEM 格式公钥，请使用 Hex 格式");
            return null;
        }

        return formatted;
    }
}
