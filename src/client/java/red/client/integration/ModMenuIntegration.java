package red.client.integration;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import red.client.gui.RedClientScreen;

/**
 * ModMenu integration for RedClient
 * This allows users to access the RedClient GUI from the ModMenu mod list
 */
public class ModMenuIntegration implements ModMenuApi {
    
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new RedClientScreen();
    }
}
