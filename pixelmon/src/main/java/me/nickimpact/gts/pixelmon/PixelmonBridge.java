package me.nickimpact.gts.pixelmon;

import com.google.inject.Inject;
import com.nickimpact.impactor.api.logger.Logger;
import com.nickimpact.impactor.logging.ConsoleLogger;
import com.nickimpact.impactor.logging.SpongeLogger;
import me.nickimpact.gts.GTS;
import me.nickimpact.gts.api.GtsService;
import me.nickimpact.gts.api.text.Translator;
import me.nickimpact.gts.pixelmon.entries.PokemonEntry;
import me.nickimpact.gts.pixelmon.text.NucleusPokemonTokens;
import me.nickimpact.gts.pixelmon.ui.PixelmonUI;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.plugin.Dependency;
import org.spongepowered.api.plugin.Plugin;

import java.util.Map;

@Plugin(id = "gts_pixelmon", name = "GTS Pixelmon Bridge", version = "1.0.0", dependencies = @Dependency(id = "gts"))
public class PixelmonBridge {

	@Inject
	private org.slf4j.Logger fallback;

	@Listener
	public void onPreInit(GamePreInitializationEvent e) {
		Logger logger = new ConsoleLogger(GTS.getInstance(), new SpongeLogger(GTS.getInstance(), fallback));

		GtsService service = Sponge.getServiceManager().provideUnchecked(GtsService.class);
		service.registerEntry(PokemonEntry.class, new PixelmonUI(null), ItemStack.builder().build());
		for(Map.Entry<String, Translator> token : NucleusPokemonTokens.getTokens().entrySet()) {
			if(!service.getTokensService().register(token.getKey(), token.getValue())) {
				logger.warn("Unable to register token {{" + token.getKey() + "}} as it's already registered!");
			}
		}
	}
}
