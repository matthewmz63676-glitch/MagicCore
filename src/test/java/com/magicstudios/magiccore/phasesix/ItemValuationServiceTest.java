package com.magicstudios.magiccore.phasesix;

import com.magicstudios.magiccore.config.model.ItemWorthFile;
import com.magicstudios.magiccore.modules.shop.ItemFingerprint;
import com.magicstudios.magiccore.modules.worth.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ItemValuationServiceTest {
    private static final ItemFingerprint FINGERPRINT=ItemFingerprint.of("DIAMOND",new byte[]{1,2,3});
    private final ConfiguredItemValuationService service=new ConfiguredItemValuationService(new ItemWorthFile(1,"COINS",
            new ItemWorthFile.Policies("ADDITIVE",250,"LINEAR",1000,"ALLOW","REJECT_NONEMPTY","USE_ENTRY",List.of("minecraft:knowledge_book"),List.of("magiccore:protected")),
            new ItemWorthFile.Presentation(true,true,true,"Worth: {amount} {currency}","Not sellable"),List.of(
                    new ItemWorthFile.WorthEntry("diamond","minecraft:diamond","minerals",100),
                    new ItemWorthFile.WorthEntry("ruby","itemsadder:ruby","custom",250))));

    @Test void valuesStacksCustomIdsEnchantmentsAndDurability(){assertThat(service.value(input("minecraft:diamond",2,0,0,0,0,Set.of())).totalWorthMinor()).isEqualTo(200);
        assertThat(service.value(input("minecraft:diamond",2,4,0,0,0,Set.of())).totalWorthMinor()).isEqualTo(220);
        assertThat(service.value(input("minecraft:diamond",1,0,50,100,0,Set.of())).unitWorthMinor()).isEqualTo(50);
        assertThat(service.value(input("itemsadder:ruby",3,0,0,0,0,Set.of())).totalWorthMinor()).isEqualTo(750);
        assertThat(service.categories()).containsExactly("custom","minerals");}
    @Test void protectedAndNonemptyContainersFailClosed(){assertThat(service.value(input("minecraft:diamond",1,0,0,0,0,Set.of("magiccore:protected"))).code()).isEqualTo("PROTECTED_ITEM");
        assertThat(service.value(input("minecraft:diamond",1,0,0,0,4,Set.of())).code()).isEqualTo("CONTAINER_POLICY");
        assertThat(service.render(service.value(input("minecraft:unknown",1,0,0,0,0,Set.of())))).isEqualTo("Not sellable");}
    private static ValuationInput input(String id,int amount,int enchants,int damage,int maximum,int contents,Set<String>keys){return new ValuationInput(id,"minecraft:diamond",FINGERPRINT,amount,enchants,damage,maximum,false,contents,"",keys);}
}
