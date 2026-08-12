package com.magicstudios.magiccore.phaseseven;

import com.magicstudios.magiccore.gui.GuiPage;
import com.magicstudios.magiccore.gui.GuiSlotPolicy;
import com.magicstudios.magiccore.gui.GuiMarkup;
import com.magicstudios.magiccore.gui.SecureStorageItemPolicy;
import com.magicstudios.magiccore.text.MiniMessageRenderer;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GuiSecurityPolicyTest {
    @Test void cancelsEveryInteractionAgainstAMagicInventory() {
        assertThat(GuiSlotPolicy.cancelClick(true)).isTrue();
        assertThat(GuiSlotPolicy.cancelDrag(true)).isTrue();
        assertThat(GuiSlotPolicy.actionableTopSlot(true, 0, 54)).isTrue();
        assertThat(GuiSlotPolicy.actionableTopSlot(true, 53, 54)).isTrue();
        assertThat(GuiSlotPolicy.actionableTopSlot(true, 54, 54)).isFalse();
        assertThat(GuiSlotPolicy.actionableTopSlot(true, -999, 54)).isFalse();
        assertThat(GuiSlotPolicy.cancelClick(false)).isFalse();
    }

    @Test void reservesStableNavigationSlots() {
        assertThat(GuiSlotPolicy.previousOrBackSlot(54)).isEqualTo(45);
        assertThat(GuiSlotPolicy.closeSlot(54)).isEqualTo(49);
        assertThat(GuiSlotPolicy.nextSlot(54)).isEqualTo(53);
    }

    @Test void rejectsImpossiblePagesBeforeAnInventoryCanOpen() {
        assertThatThrownBy(() -> new GuiPage("x", 0, 0, 1, Map.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GuiPage("x", 6, 1, 1, Map.of())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void styleGuideShorthandIsCompletedIntoStrictMiniMessage() {
        String completed=GuiMarkup.complete("<aqua><b>Profile</b> <white>Current status");
        assertThat(completed).endsWith("</white></aqua>");
        new MiniMessageRenderer().validate(completed);
    }

    @Test void secureStorageInsertionPolicyFailsClosedForEveryRiskClass() {
        var policy=new SecureStorageItemPolicy(100,250,"DENY_NON_EMPTY","DENY");
        assertThat(policy.accepts(50,false,false,false,100,1)).isTrue();
        assertThat(policy.accepts(101,false,false,false,0,1)).isFalse();
        assertThat(policy.accepts(50,true,true,false,0,1)).isFalse();
        assertThat(policy.accepts(50,false,false,true,0,1)).isFalse();
        assertThat(policy.accepts(50,false,false,false,200,2)).isFalse();
    }
}
