package com.magicstudios.magiccore.discordbot;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

final class BridgeClient {
    private static final ObjectMapper JSON=new ObjectMapper().findAndRegisterModules();
    private final URI endpoint;private final byte[]secret;private final HttpClient http=HttpClient.newBuilder().build();
    BridgeClient(String endpoint,String secret){this.endpoint=URI.create(endpoint);this.secret=secret.getBytes(StandardCharsets.UTF_8);if(this.secret.length<32)throw new IllegalArgumentException("Bridge secret must be at least 32 bytes");}
    Link redeem(String code,String discordId)throws Exception{return request("LINK_REDEEM",JSON.writeValueAsString(new LinkRequest(code,discordId)),Link.class);}
    List<OutboxMessage>poll(int limit)throws Exception{return request("OUTBOX_POLL",JSON.writeValueAsString(new PollRequest(limit)),new TypeReference<>(){});}
    boolean acknowledge(UUID id)throws Exception{return request("OUTBOX_ACK",JSON.writeValueAsString(new AckRequest(id)),Boolean.class);}
    OutboxMessage fail(UUID id,String error)throws Exception{return request("OUTBOX_FAIL",JSON.writeValueAsString(new FailRequest(id,error)),OutboxMessage.class);}
    Health health()throws Exception{return request("HEALTH","{}",Health.class);}
    private<T>T request(String type,String payload,Class<T>result)throws Exception{return JSON.readValue(response(type,payload),result);}
    private<T>T request(String type,String payload,TypeReference<T>result)throws Exception{return JSON.readValue(response(type,payload),result);}
    private String response(String type,String payload)throws Exception{Envelope request=sign(type,payload);byte[]body=JSON.writeValueAsBytes(request);HttpResponse<byte[]>response=http.send(HttpRequest.newBuilder(endpoint).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofByteArray(body)).build(),HttpResponse.BodyHandlers.ofByteArray());if(response.statusCode()!=200)throw new IllegalStateException("Bridge HTTP "+response.statusCode()+": "+new String(response.body(),StandardCharsets.UTF_8));Envelope envelope=JSON.readValue(response.body(),Envelope.class);verify(envelope);return envelope.payload();}
    private Envelope sign(String type,String payload){Envelope unsigned=new Envelope(UUID.randomUUID(),UUID.randomUUID().toString(),type,payload,Instant.now(),"");return new Envelope(unsigned.id(),unsigned.nonce(),type,payload,unsigned.timestamp(),hmac(canonical(unsigned)));}
    private void verify(Envelope envelope){byte[]expected=hmac(canonical(envelope)).getBytes(StandardCharsets.US_ASCII);if(!MessageDigest.isEqual(expected,envelope.signature().getBytes(StandardCharsets.US_ASCII)))throw new SecurityException("Invalid bridge response signature");}
    private String hmac(String value){try{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(secret,"HmacSHA256"));return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception failure){throw new IllegalStateException(failure);}}
    private static String canonical(Envelope value){return value.id()+"\n"+value.nonce()+"\n"+value.type()+"\n"+value.payload()+"\n"+value.timestamp().toEpochMilli();}
    record Envelope(UUID id,String nonce,String type,String payload,Instant timestamp,String signature){}
    record LinkRequest(String code,String discordId){}record PollRequest(int limit){}record AckRequest(UUID messageId){}record FailRequest(UUID messageId,String error){}
    record Link(UUID playerId,String discordId,Instant linkedAt,Instant revokedAt){}
    record Health(boolean available,long pending,long dead,Instant checkedAt){}
    record OutboxMessage(UUID id,Envelope envelope,String status,int attempts,String lastError,Instant nextAttemptAt,Instant createdAt,Instant updatedAt){}
}
