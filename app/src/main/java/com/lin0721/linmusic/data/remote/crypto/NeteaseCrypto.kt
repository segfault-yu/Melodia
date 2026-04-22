package com.lin0721.linmusic.data.remote.crypto

import java.math.BigInteger
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

/**
 * 网易云音乐加密工具类
 * 包含 WeApi, LinuxApi, EApi 的实现
 */
object NeteaseCrypto {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private const val IV = "0102030405060708"
    private const val WEAPI_NONCE = "0CoJ66S234fd97Wy"
    private const val WEAPI_PUBLIC_KEY = "010001"
    private const val WEAPI_MODULUS =
        "00e0b50916409369105c245242a2e48e1fb573b31dd096b8686d8fab37e48b411851b033c8f12a20822617f63116900f340864388414983057398e9836371804d001614742a25df93a61c14041e17d5985698b64e52f15569424754859a72dfc4f826379c3f0c33604f76263ed49265f7c3275727659547d2f347a829f074d2077"

    private const val LINUX_API_KEY = "rU33S79X301668qc"
    private const val EAPI_KEY = "e82ee393015b2258"

    /**
     * WeApi 加密
     * 适用于 Web 端、小程序等
     */
    fun weapi(params: Map<String, Any?>): Map<String, String> {
        val text = json.encodeToString(params)
        val secretKey = generateRandomString(16)
        
        val paramsEncrypted = aesEncrypt(aesEncrypt(text, WEAPI_NONCE), secretKey)
        val encSecKey = rsaEncrypt(secretKey, WEAPI_PUBLIC_KEY, WEAPI_MODULUS)
        
        return mapOf(
            "params" to paramsEncrypted,
            "encSecKey" to encSecKey
        )
    }

    /**
     * LinuxApi 加密
     * 适用于部分特殊接口
     */
    fun linuxapi(params: Map<String, Any?>): Map<String, String> {
        val text = json.encodeToString(params)
        return mapOf(
            "eparams" to aesEncryptHex(text, LINUX_API_KEY, mode = "AES/ECB/PKCS5Padding").uppercase()
        )
    }

    /**
     * EApi 加密
     * 适用于移动端原生接口
     */
    fun eapi(url: String, params: Any): Map<String, String> {
        val text = if (params is String) params else json.encodeToString(params)
        val message = "nobody${url}use${text}md5foreapi"
        val digest = md5(message)
        val data = "${url}-36jh-${text}-36jh-${digest}"
        return mapOf(
            "params" to aesEncryptHex(data, EAPI_KEY, mode = "AES/ECB/PKCS5Padding").uppercase()
        )
    }

    /**
     * AES 加密 (Base64 输出)
     */
    private fun aesEncrypt(
        text: String,
        key: String,
        iv: String = IV,
        mode: String = "AES/CBC/PKCS5Padding"
    ): String {
        val cipher = Cipher.getInstance(mode)
        val keySpec = SecretKeySpec(key.toByteArray(), "AES")
        if (mode.contains("CBC")) {
            val ivSpec = IvParameterSpec(iv.toByteArray())
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
        } else {
            cipher.init(Cipher.ENCRYPT_MODE, keySpec)
        }
        val encrypted = cipher.doFinal(text.toByteArray())
        return Base64.getEncoder().encodeToString(encrypted)
    }

    /**
     * AES 加密 (Hex 输出)
     */
    private fun aesEncryptHex(
        text: String,
        key: String,
        iv: String = IV,
        mode: String = "AES/CBC/PKCS5Padding"
    ): String {
        val cipher = Cipher.getInstance(mode)
        val keySpec = SecretKeySpec(key.toByteArray(), "AES")
        if (mode.contains("CBC")) {
            val ivSpec = IvParameterSpec(iv.toByteArray())
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
        } else {
            cipher.init(Cipher.ENCRYPT_MODE, keySpec)
        }
        val encrypted = cipher.doFinal(text.toByteArray())
        return encrypted.joinToString("") { "%02x".format(it) }
    }

    /**
     * RSA 加密 (网易特化版)
     */
    private fun rsaEncrypt(text: String, pubKey: String, modulus: String): String {
        val reversedText = text.reversed()
        val biText = BigInteger(1, reversedText.toByteArray())
        val biPubKey = BigInteger(pubKey, 16)
        val biModulus = BigInteger(modulus, 16)
        val biRet = biText.modPow(biPubKey, biModulus)
        return biRet.toString(16).padStart(256, '0')
    }

    /**
     * 生成指定长度的随机字符串
     */
    private fun generateRandomString(length: Int): String {
        val charPool = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..length)
            .map { kotlin.random.Random.nextInt(0, charPool.length) }
            .map(charPool::get)
            .joinToString("")
    }

    /**
     * MD5 摘要
     */
    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        return BigInteger(1, md.digest(input.toByteArray()))
            .toString(16)
            .padStart(32, '0')
    }
}
