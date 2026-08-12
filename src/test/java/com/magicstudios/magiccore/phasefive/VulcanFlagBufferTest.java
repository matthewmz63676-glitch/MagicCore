package com.magicstudios.magiccore.phasefive;

import com.magicstudios.magiccore.integrations.vulcan.VulcanFlag;
import com.magicstudios.magiccore.integrations.vulcan.VulcanFlagBuffer;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class VulcanFlagBufferTest {
    @Test void flagsAreBoundedAndQueryableForEvidence(){UUID player=UUID.randomUUID();Instant now=Instant.now();var buffer=new VulcanFlagBuffer(Duration.ofHours(1),2);buffer.record(new VulcanFlag(player,"SPEED",1,"first",now.minusSeconds(3)));buffer.record(new VulcanFlag(player,"SPEED",2,"second",now.minusSeconds(2)));buffer.record(new VulcanFlag(player,"FLY",3,"third",now.minusSeconds(1)));
        assertThat(buffer.recent(player,now.minusSeconds(10))).extracting(VulcanFlag::detail).containsExactly("second","third");assertThat(buffer.recent(player,now.minusMillis(1500))).singleElement().extracting(VulcanFlag::check).isEqualTo("FLY");}
}
