package com.magicstudios.magiccore.integrations.apollo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApolloLunarClientServiceTest {
    @Test
    void waypointPacketUsesOfficialJsonChannelSchema() throws Exception {
        byte[] payload = ApolloLunarClientService.waypointPayload(
                new LunarWaypoint("Spawn", "world", 12, 64, -8, 0x12ABEF, false));
        JsonNode json = new ObjectMapper().readTree(payload);
        assertThat(ApolloLunarClientService.JSON_CHANNEL).isEqualTo("apollo:json");
        assertThat(json.path("@type").asText()).isEqualTo(
                "type.googleapis.com/lunarclient.apollo.waypoint.v1.DisplayWaypointMessage");
        assertThat(json.path("location").path("world").asText()).isEqualTo("world");
        assertThat(json.path("location").path("z").asInt()).isEqualTo(-8);
        assertThat(json.path("color").path("color").asInt()).isEqualTo(0x12ABEF);
    }
}
