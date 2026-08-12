package com.magicstudios.magiccore.modules.display;

import java.util.UUID;

/** TAB consumes MagicCore's canonical PAPI placeholders; it does not become rank truth. */
public final class TabDisplayService implements DisplayService {
    @Override public String provider() { return "TAB"; }
    @Override public void refresh(UUID playerId) { }
    @Override public void refreshAll() { }
}
