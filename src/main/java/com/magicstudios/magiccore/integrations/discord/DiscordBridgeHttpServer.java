package com.magicstudios.magiccore.integrations.discord;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class DiscordBridgeHttpServer implements AutoCloseable {
    private static final ObjectMapper JSON=new ObjectMapper().findAndRegisterModules();private static final int MAX_REQUEST_BYTES=131_072;
    private final CustomDiscordBridge bridge;private final HttpServer server;
    public DiscordBridgeHttpServer(CustomDiscordBridge bridge,String host,int port)throws IOException{this.bridge=bridge;server=HttpServer.create(new InetSocketAddress(host,port),32);server.createContext("/bridge",this::handle);server.setExecutor(java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());}
    public void start(){server.start();}
    private void handle(HttpExchange exchange)throws IOException{try{if(!exchange.getRequestMethod().equals("POST")){respond(exchange,405,"method not allowed");return;}byte[]body=exchange.getRequestBody().readNBytes(MAX_REQUEST_BYTES+1);if(body.length>MAX_REQUEST_BYTES){respond(exchange,413,"request too large");return;}BridgeEnvelope envelope=JSON.readValue(body,BridgeEnvelope.class);String source=exchange.getRemoteAddress().getAddress().getHostAddress();bridge.accept(envelope,source).toCompletableFuture().join();Object result=switch(envelope.type()){
            case"LINK_REDEEM"->{LinkRedeem request=JSON.readValue(envelope.payload(),LinkRedeem.class);yield bridge.redeemLinkCode(request.code(),request.discordId(),"http:"+envelope.id()).toCompletableFuture().join();}
            case"OUTBOX_POLL"->{OutboxPoll request=JSON.readValue(envelope.payload(),OutboxPoll.class);yield bridge.pendingOutbox(request.limit()).toCompletableFuture().join();}
            case"OUTBOX_ACK"->{OutboxAck request=JSON.readValue(envelope.payload(),OutboxAck.class);yield bridge.acknowledge(request.messageId(),"http:"+envelope.id()).toCompletableFuture().join();}
            case"OUTBOX_FAIL"->{OutboxFail request=JSON.readValue(envelope.payload(),OutboxFail.class);yield bridge.markFailed(request.messageId(),request.error(),"http:"+envelope.id()).toCompletableFuture().join();}
            case"HEALTH"->bridge.bridgeHealth().toCompletableFuture().join();
            default->throw new IllegalArgumentException("Unsupported bridge message type");};String payload=JSON.writeValueAsString(result);BridgeEnvelope response=bridge.sign("RESPONSE",payload,UUID.randomUUID().toString());byte[]encoded=JSON.writeValueAsBytes(response);exchange.getResponseHeaders().set("Content-Type","application/json; charset=utf-8");exchange.getResponseHeaders().set("Cache-Control","no-store");exchange.sendResponseHeaders(200,encoded.length);exchange.getResponseBody().write(encoded);}catch(Throwable failure){Throwable root=root(failure);int status=root instanceof SecurityException?401:root instanceof IllegalArgumentException?400:409;respond(exchange,status,root.getClass().getSimpleName()+": "+String.valueOf(root.getMessage()));}finally{exchange.close();}}
    private static void respond(HttpExchange exchange,int status,String message)throws IOException{byte[]body=message.replaceAll("[\\r\\n]"," ").getBytes(StandardCharsets.UTF_8);exchange.getResponseHeaders().set("Content-Type","text/plain; charset=utf-8");exchange.getResponseHeaders().set("Cache-Control","no-store");exchange.sendResponseHeaders(status,body.length);exchange.getResponseBody().write(body);}
    private static Throwable root(Throwable failure){Throwable value=failure;while(value.getCause()!=null)value=value.getCause();return value;}
    @Override public void close(){server.stop(1);}
    private record LinkRedeem(String code,String discordId){}
    private record OutboxPoll(int limit){}
    private record OutboxAck(UUID messageId){}
    private record OutboxFail(UUID messageId,String error){}
}
