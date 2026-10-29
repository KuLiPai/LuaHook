package com.kulipai.luahook.feature.project.pack.engine.signer

import java.io.ByteArrayInputStream
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64

/**
 * 内置默认测试签名密钥与证书（RSA 2048，PKCS#8 & X.509）。
 * 用户无需准备任何 Keystore 文件即可直接开箱完成 V1/V2/V3 签名。
 */
object TestKeys {

    private val PK8_BASE64 = """
        MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCictkc+0McEU9y
        D3Lapf7hwMLvq5nG/Vz7ADzuLakbnlkzkl5PVeTjgLOxdLayPVeTBxnpBq3uirJ1
        NQqMzvyFGWzKIb73SsDmZzSLjq4QlCKEfjKMGJTBgJdzc/0EQr9+g05KzZ0H61MP
        7/o7wC+ug/lqPiZEiv1o7VMdJg+W0wUcdcCT+s/mL36pmxdy4K7haym03/Y6DD/Z
        YXs/R/SwPl3tmGlGeSKhvRK+yoKFGDg9EjtEBgp/DUz2hn07ghmotb0PoZk/Flb6
        56mXd1Y/GFP1GvWBMIZBBLGHnTWtVrprY+6mpj5Iap7UHOnyfDHBRpatuTspq2NO
        s5cEhHV7AgMBAAECggEABKajHq2hmJPKTCqzEB2BNEzE3VXIEPwUsGmrqj9hFcBX
        2h55AsPOdNECKV99EjyoCDaY1aTHpV+6h5ky1HXh9vGTvXRv4jv1VTi5NrxUBszR
        nCzSHP611kBslZ/0K4NkYNE46Z1aFJhL3YCdD8mLKUTwBsxWuM796Ft7GLI2mgvO
        7p8fWJeI/XmhwA79I5sDw+O00pgp7PLOsG8WQaiWFtPPyyJcCZ5UeKoF5TrxVwvn
        5BP5REk1EYW/N2OSj+06IAyd2xklLc932uebmHAYrOnwwOZXeRAeBWX7h4kTq83T
        iYxshHXMr/ioBsnhI//mp3pBtrsVAyiaryY4pBnvOQKBgQDMXHWBUTPTSK8EVEhj
        z7gPmCewVV89eb/HLL9bQvgJs263JuwZl3EYmJoGdsz5+wTWRC9Z8zy2GAV6nScN
        czdA3GWPITJn3rZt80zST89YV7unb/N4L817x142dVLpYXCmqVzhJOvTElgNeNDo
        aMu8kYCMoa/pj5/UDMczRe1GjQKBgQDLfy+vlj8+gzii2gUtzZ1oFokv1p0TMXDb
        vVrD+KZcrGsPBVJr+Xm5T8n4PGpU7CPPH5tucj1X2amIypkn5z8m2PIwD/O5xfoV
        XblVnRt+D8ELo1bT2rKeB2g669LMEAa611zFcE/jK4UhDeek3l119B4bAyaBdUJ5
        Uyw5lk8OJwKBgHbP0mkqoqzV9xrXPXb6DxLR7VExcTu2iu7rbx1J5BcPwPCuqll+
        mHwqTHIxo546eGdyIO104vkjIhxVPZ0uPRNyj4EVVc7ZxspJx4IGcI/2Ad2o0q4v
        bJHoKTgUIe4ZfnsGwD8NTiXa5mJ+uUZFlx28ojYg0rtBizSncTC4GOkJAoGBAKSy
        8bNZFSLRLZuQfTSO/+DorGvNdR+9miwe20FVE8BZw+RKWGUPaxcynrKMozrMTEcs
        u63uQ/vALNiLsN186b3VRF7l6krnEvsL+h5yo3ep5II41uNsU7hXImLdzqReLDQ4
        bdtWhTHFXWBdUqg7WMVIqL0QFbAdaYmsaVTKdH1tAoGABDzcjCz/dKT5Bu1NAQfo
        wHsrCx5OHd+fNhbjZDttYtdjLvz3I1QXfFZbb4jnvPa2Oc9EUUDc6jxkZKp6Z0fP
        XfzWSPJTUEvnlgUyN6frKU0xqu2zw38uiJkL6YQtm3GVXkh246K/CtBFQ0BynX69
        NFv8WnqkViHiPM87m8+Xt4c=
    """.trimIndent().replace("\\s+".toRegex(), "")

    private val X509_PEM = """
        -----BEGIN CERTIFICATE-----
        MIIDtzCCAp+gAwIBAgIUAsJnRtMB0BUK44NonK0reMCOsmMwDQYJKoZIhvcNAQEL
        BQAwajELMAkGA1UEBhMCQ04xEDAOBgNVBAgMB0FuZHJvaWQxEDAOBgNVBAcMB0x1
        YUhvb2sxEDAOBgNVBAoMB0x1YUhvb2sxEzARBgNVBAsMClN0YW5kYWxvbmUxEDAO
        BgNVBAMMB0x1YUhvb2swIBcNMjYxMDI3MTAyNDE3WhgPMjA1NjEwMTkxMDI0MTda
        MGoxCzAJBgNVBAYTAkNOMRAwDgYDVQQIDAdBbmRyb2lkMRAwDgYDVQQHDAdMdWFI
        b29rMRAwDgYDVQQKDAdMdWFIb29rMRMwEQYDVQQLDApTdGFuZGFsb25lMRAwDgYD
        VQQDDAdMdWFIb29rMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAonLZ
        HPtDHBFPcg9y2qX+4cDC76uZxv1c+wA87i2pG55ZM5JeT1Xk44CzsXS2sj1XkwcZ
        6Qat7oqydTUKjM78hRlsyiG+90rA5mc0i46uEJQihH4yjBiUwYCXc3P9BEK/foNO
        Ss2dB+tTD+/6O8AvroP5aj4mRIr9aO1THSYPltMFHHXAk/rP5i9+qZsXcuCu4Wsp
        tN/2Ogw/2WF7P0f0sD5d7ZhpRnkiob0SvsqChRg4PRI7RAYKfw1M9oZ9O4IZqLW9
        D6GZPxZW+uepl3dWPxhT9Rr1gTCGQQSxh501rVa6a2PupqY+SGqe1Bzp8nwxwUaW
        rbk7KatjTrOXBIR1ewIDAQABo1MwUTAdBgNVHQ4EFgQUEkn8PjXaA7EcIUdH7SfJ
        jYMUGSMwHwYDVR0jBBgwFoAUEkn8PjXaA7EcIUdH7SfJjYMUGSMwDwYDVR0TAQH/
        BAUwAwEB/zANBgkqhkiG9w0BAQsFAAOCAQEAaRmHA3sx1rdf5Uhr90KHgpkOZ6Xi
        pSFFrIsImpuBq0uN4PPyrb4et5KmSdW3H9trzyw0SQoqRsPWwbteSSPpkfcEANQg
        FhtvIcjWAHUePAvqKKfuHQbaPuAnT7jJxXK+9bOdsOlNcF1jKlMREPrpQGrrcjDM
        I9o8YsIIX5MknVvSDU9QnPP65AqEw+aA8c/pjR9QNslVmt4iroIpCMXiXxd3o4Uy
        Dspgq0tpQTjHC7ESHtjy8qZaoVAlkaS8EDOZcqqwbGBaGSB47JBQzqZXcKI/ylgR
        6rkSdBayZGHft7U8n5uvLW5j/qTsIk27K9ZxqlibhVCmQ15banMzpJwczg==
        -----END CERTIFICATE-----
    """.trimIndent()

    val privateKey: PrivateKey by lazy {
        val keyBytes = Base64.getDecoder().decode(PK8_BASE64)
        val keySpec = PKCS8EncodedKeySpec(keyBytes)
        KeyFactory.getInstance("RSA").generatePrivate(keySpec)
    }

    val certificate: X509Certificate by lazy {
        val cf = CertificateFactory.getInstance("X.509")
        cf.generateCertificate(ByteArrayInputStream(X509_PEM.toByteArray(Charsets.UTF_8))) as X509Certificate
    }
}
