package com.magicstudios.magiccore.discordbot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class MagicCoreDiscordBot extends ListenerAdapter {
    private final BridgeClient bridge;private final String channelId;
    private MagicCoreDiscordBot(BridgeClient bridge,String channelId){this.bridge=bridge;this.channelId=channelId;}
    public static void main(String[]args)throws Exception{String token=require("DISCORD_BOT_TOKEN"),secret=require("MAGICCORE_DISCORD_BRIDGE_SECRET"),channel=require("DISCORD_NOTIFICATION_CHANNEL_ID");String url=System.getenv().getOrDefault("MAGICCORE_BRIDGE_URL","http://127.0.0.1:8765/bridge");BridgeClient bridge=new BridgeClient(url,secret);var listener=new MagicCoreDiscordBot(bridge,channel);JDA jda=JDABuilder.createDefault(token).addEventListeners(listener).build().awaitReady();jda.updateCommands().addCommands(Commands.slash("link","Link this Discord account to Minecraft").addOption(OptionType.STRING,"code","One-time code from /magic discord link",true),Commands.slash("magiccore-health","Show bridge queue health")).queue();Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().name("magiccore-discord-outbox").factory()).scheduleWithFixedDelay(()->listener.poll(jda),1,5,TimeUnit.SECONDS);}
    @Override public void onSlashCommandInteraction(SlashCommandInteractionEvent event){if(event.getName().equals("link")){event.deferReply(true).queue();try{String code=Objects.requireNonNull(event.getOption("code")).getAsString();var link=bridge.redeem(code,event.getUser().getId());event.getHook().sendMessage("Linked to Minecraft UUID `"+link.playerId()+"`.").queue();}catch(Exception failure){event.getHook().sendMessage("Link failed: "+safe(failure)).queue();}return;}if(event.getName().equals("magiccore-health")){event.deferReply(true).queue();try{var health=bridge.health();event.getHook().sendMessage("Bridge available: "+health.available()+", pending: "+health.pending()+", dead: "+health.dead()).queue();}catch(Exception failure){event.getHook().sendMessage("Health check failed: "+safe(failure)).queue();}}}
    private void poll(JDA jda){for(BridgeClient.OutboxMessage message:safePoll())try{var channel=jda.getTextChannelById(channelId);if(channel==null)throw new IllegalStateException("Notification channel unavailable");channel.sendMessage(message.envelope().payload()).complete();bridge.acknowledge(message.id());}catch(Exception failure){try{bridge.fail(message.id(),safe(failure));}catch(Exception ignored){}}}
    private java.util.List<BridgeClient.OutboxMessage>safePoll(){try{return bridge.poll(50);}catch(Exception ignored){return java.util.List.of();}}
    private static String require(String name){String value=System.getenv(name);if(value==null||value.isBlank())throw new IllegalStateException("Missing environment variable "+name);return value;}
    private static String safe(Throwable failure){Throwable root=failure;while(root.getCause()!=null)root=root.getCause();String message=String.valueOf(root.getMessage()).replaceAll("[\\r\\n]"," ");return message.substring(0,Math.min(300,message.length()));}
}
