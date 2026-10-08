package com.j41k.fridamanager.repack

import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Date
import javax.security.auth.x500.X500Principal

/**
 * Genera una identidad de firma efímera para el APK repaquetado.
 *
 * Clave RSA con certificado autofirmado, distinta en cada repaquetado.
 *
 * Nota sobre Android: NO registramos ni forzamos el proveedor "BC". El sistema ya
 * trae un proveedor "BC" recortado (sin CertificateFactory X.509), así que forzarlo
 * rompe la conversión del certificado. Las operaciones (RSA, SHA256withRSA, X.509)
 * las resuelven los proveedores por defecto; BouncyCastle solo aporta las clases
 * constructoras del certificado.
 */
object Keygen {

    data class Keys(val key: PrivateKey, val cert: X509Certificate)

    fun generate(): Keys {
        val kpg = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }
        val pair: KeyPair = kpg.generateKeyPair()

        val dn = X500Principal("CN=Android, OU=Android, O=Google Inc., L=Mountain View, ST=California, C=US")
        val now = System.currentTimeMillis()
        val notBefore = Date(now - 90L * 24 * 60 * 60 * 1000)        // 3 meses atrás
        val notAfter = Date(now + 30L * 365 * 24 * 60 * 60 * 1000)   // 30 años
        val serial = BigInteger(160, SecureRandom())

        val builder = JcaX509v3CertificateBuilder(dn, serial, notBefore, notAfter, dn, pair.public)
        val signer = JcaContentSignerBuilder("SHA256withRSA").build(pair.private)
        val cert = JcaX509CertificateConverter().getCertificate(builder.build(signer))

        return Keys(pair.private, cert)
    }
}
