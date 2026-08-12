package com.magicstudios.magiccore.modules.store;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

public final class PurchaseSignatures {
    private PurchaseSignatures() { }
    public static String canonical(PurchaseRequest request){return String.join("\n",request.eventId(),request.productId(),request.playerId().toString(),request.playerName(),
            Long.toString(request.paidMinor()),request.occurredAt().toString(),request.nonce());}
    public static String sign(PurchaseRequest request,String secret){try{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));return HexFormat.of().formatHex(mac.doFinal(canonical(request).getBytes(StandardCharsets.UTF_8)));}
        catch(Exception failure){throw new IllegalStateException("HMAC unavailable",failure);}}
    public static boolean verify(PurchaseRequest request,String secret,String signature){try{return MessageDigest.isEqual(HexFormat.of().parseHex(sign(request,secret)),HexFormat.of().parseHex(signature));}catch(IllegalArgumentException failure){return false;}}
}
