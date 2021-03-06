package me.nickimpact.gts.reforged.entries;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.nickimpact.impactor.api.configuration.Config;
import com.nickimpact.impactor.sponge.ui.SpongeIcon;
import com.nickimpact.impactor.sponge.ui.SpongeLayout;
import com.nickimpact.impactor.sponge.ui.SpongeUI;
import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.config.PixelmonItems;
import com.pixelmonmod.pixelmon.entities.pixelmon.EnumSpecialTexture;
import com.pixelmonmod.pixelmon.entities.pixelmon.stats.Gender;
import com.pixelmonmod.pixelmon.enums.EnumSpecies;
import com.pixelmonmod.pixelmon.enums.forms.EnumGreninja;
import com.pixelmonmod.pixelmon.enums.forms.EnumNoForm;
import com.pixelmonmod.pixelmon.enums.forms.IEnumForm;
import com.pixelmonmod.pixelmon.items.ItemPixelmonSprite;
import com.pixelmonmod.pixelmon.storage.NbtKeys;
import com.pixelmonmod.pixelmon.storage.PlayerPartyStorage;
import me.nickimpact.gts.api.listings.entries.EntryUI;
import me.nickimpact.gts.api.plugin.PluginInstance;
import me.nickimpact.gts.config.ConfigKeys;
import me.nickimpact.gts.config.MsgConfigKeys;
import me.nickimpact.gts.reforged.ReforgedBridge;
import me.nickimpact.gts.reforged.config.PokemonConfigKeys;
import me.nickimpact.gts.reforged.config.PokemonMsgConfigKeys;
import me.nickimpact.gts.sponge.SpongeListing;
import me.nickimpact.gts.sponge.SpongePlugin;
import me.nickimpact.gts.sponge.TextParsingUtils;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.DyeColors;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.item.inventory.ClickInventoryEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.property.InventoryDimension;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.text.Text;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ReforgedUI implements EntryUI<Player> {

	private SpongeUI display;
	private Player viewer;

	private Pokemon selection;
	private PlayerPartyStorage party;

	private double min;
	private double amount;

	public ReforgedUI() {}

	private ReforgedUI(Player player) {
		this.viewer = player;
		this.display = SpongeUI.builder()
				.title(((SpongePlugin) PluginInstance.getInstance()).getTextParsingUtils().fetchAndParseMsg(
						player, ReforgedBridge.getInstance().getMsgConfig(), PokemonMsgConfigKeys.UI_TITLES_POKEMON, null, null
				))
				.dimension(InventoryDimension.of(9, 6))
				.build()
				.define(this.forgeLayout());
	}

	@Override
	public ReforgedUI createFor(Player player) {
		return new ReforgedUI(player);
	}

	@Override
	public SpongeUI getDisplay() {
		return this.display;
	}

	private SpongeLayout forgeLayout() {
		SpongeLayout.SpongeLayoutBuilder slb = SpongeLayout.builder();
		slb.rows(SpongeIcon.BORDER, 0, 2);
		slb.slots(SpongeIcon.BORDER, 9, 18, 16, 34, 43, 52);

		EconomyService economy = ((SpongePlugin) PluginInstance.getInstance()).getEconomy();
		Config config = PluginInstance.getInstance().getConfiguration();
		Config msgConfig = PluginInstance.getInstance().getMsgConfig();
		TextParsingUtils parser = ((SpongePlugin) PluginInstance.getInstance()).getTextParsingUtils();
		Map<String, Function<CommandSource, Optional<Text>>> tokens = Maps.newHashMap();
		tokens.put("gts_button_currency_left_click", src -> Optional.of(economy.getDefaultCurrency().format(new BigDecimal(config.get(ConfigKeys.PRICING_LEFTCLICK_BASE)))));
		tokens.put("gts_button_currency_right_click", src -> Optional.of(economy.getDefaultCurrency().format(new BigDecimal(config.get(ConfigKeys.PRICING_RIGHTCLICK_BASE)))));
		tokens.put("gts_button_currency_shift_left_click", src -> Optional.of(economy.getDefaultCurrency().format(new BigDecimal(config.get(ConfigKeys.PRICING_LEFTCLICK_SHIFT)))));
		tokens.put("gts_button_currency_shift_right_click", src -> Optional.of(economy.getDefaultCurrency().format(new BigDecimal(config.get(ConfigKeys.PRICING_RIGHTCLICK_SHIFT)))));
		tokens.put("gts_min_price", src -> Optional.of(economy.getDefaultCurrency().format(new BigDecimal(1))));
		tokens.put("gts_max_price", src -> Optional.of(economy.getDefaultCurrency().format(new BigDecimal(config.get(ConfigKeys.MAX_MONEY_PRICE)))));

		slb.slot(this.moneyDecIcon(tokens), 37);
		slb.slot(this.moneyIcon(tokens), 39);
		slb.slot(this.moneyIncIcon(tokens), 41);

		this.party = Pixelmon.storageManager.getParty(this.viewer.getUniqueId());
		int index = 0;
		for(Pokemon pokemon : party.getAll()) {
			if (pokemon == null) {
				index++;
				continue;
			}

			Map<String, Object> variables = Maps.newHashMap();
			variables.put("pokemon", pokemon);

			ItemStack display = this.pokemonDisplay(pokemon);
			display.offer(Keys.DISPLAY_NAME, parser.fetchAndParseMsg(
					this.viewer,
					ReforgedBridge.getInstance().getMsgConfig(),
					pokemon.isEgg() ? PokemonMsgConfigKeys.POKEMON_ENTRY_BASE_TITLE_EGG : PokemonMsgConfigKeys.POKEMON_ENTRY_BASE_TITLE,
					null,
					variables
			));
			this.addLore(pokemon, display, Lists.newArrayList(ReforgedBridge.getInstance().getMsgConfig().get(PokemonMsgConfigKeys.POKEMON_PREVIEW_LORE)), this.viewer, variables);
			SpongeIcon icon = new SpongeIcon(display);
			icon.addListener(clickable -> {
				this.selection = pokemon;
				this.display.setSlot(17, new SpongeIcon(display));
				this.min = this.calcMin(pokemon);
				this.amount = this.min;
				this.display.setSlot(39, this.moneyIcon(tokens));
			});
			slb.slot(icon, 10 + index++);
		}

		SpongeIcon confirm = new SpongeIcon(ItemStack.builder().itemType(ItemTypes.DYE)
				.add(Keys.DISPLAY_NAME, parser.fetchAndParseMsg(
						this.viewer,
						msgConfig,
						MsgConfigKeys.CONFIRM_SELECTION,
						null,
						null
				))
				.add(Keys.DYE_COLOR, DyeColors.LIME)
				.build()
		);
		confirm.addListener(clickable -> {
			if(this.selection != null) {
				this.display.close(clickable.getPlayer());
				if(this.party.countPokemon() == 1) {
					clickable.getPlayer().sendMessage(parser.fetchAndParseMsg(clickable.getPlayer(), ReforgedBridge.getInstance().getMsgConfig(), PokemonMsgConfigKeys.ERROR_LAST_MEMBER, null, null));
					return;
				}

				SpongeListing listing = SpongeListing.builder()
						.entry(new ReforgedEntry(this.selection))
						.price(this.amount)
						.id(UUID.randomUUID())
						.owner(clickable.getPlayer().getUniqueId())
						.expiration(LocalDateTime.now().plusSeconds(config.get(ConfigKeys.LISTING_TIME)))
						.build();

				listing.publish(PluginInstance.getInstance(), clickable.getPlayer().getUniqueId());
			}
		});
		slb.slot(confirm, 35);

		SpongeIcon cancel = new SpongeIcon(ItemStack.builder().itemType(ItemTypes.DYE)
				.add(Keys.DISPLAY_NAME, parser.fetchAndParseMsg(
						this.viewer,
						msgConfig,
						MsgConfigKeys.CANCEL,
						null,
						null
				))
				.add(Keys.DYE_COLOR, DyeColors.RED)
				.build()
		);
		cancel.addListener(clickable -> {
			this.display.close(clickable.getPlayer());
		});
		slb.slot(cancel, 53);


		return slb.build();
	}

	private void addLore(Pokemon pokemon, ItemStack icon, List<String> template, Player player, Map<String, Object> variables) {
		Map<String, Function<CommandSource, Optional<Text>>> tokens = Maps.newHashMap();
		for (EnumHidableDetail detail : EnumHidableDetail.values()) {
			if (detail.getCondition().test(pokemon)) {
				KeyDetailHolder holder = detail.getField().apply(pokemon);
				template.addAll(ReforgedBridge.getInstance().getMsgConfig().get(holder.getKey()));
				if(holder.getTokens() != null) {
					tokens.putAll(holder.getTokens());
				}
			}
		}

		List<Text> translated = template.stream().map(str -> ((SpongePlugin) PluginInstance.getInstance()).getTextParsingUtils().fetchAndParseMsg(player, str, tokens, variables)).collect(Collectors.toList());
		icon.offer(Keys.ITEM_LORE, translated);
	}

	private SpongeIcon moneyIncIcon(Map<String, Function<CommandSource, Optional<Text>>> tokens) {
		Config config = ReforgedBridge.getInstance().getConfig();
		Config msgConfig = PluginInstance.getInstance().getMsgConfig();
		TextParsingUtils parser = ((SpongePlugin) PluginInstance.getInstance()).getTextParsingUtils();
		ItemStack inc = ItemStack.builder()
				.itemType(ItemTypes.DYE)
				.add(Keys.DYE_COLOR, DyeColors.LIME)
				.add(Keys.DISPLAY_NAME, ((SpongePlugin) PluginInstance.getInstance()).getTextParsingUtils().fetchAndParseMsg(
						viewer, PluginInstance.getInstance().getMsgConfig(), MsgConfigKeys.BUTTONS_INCREASE_CURRENCY_TITLE, null, null
				))
				.add(Keys.ITEM_LORE,
					parser.fetchAndParseMsgs(this.viewer, msgConfig, MsgConfigKeys.BUTTONS_INCREASE_CURRENCY_LORE, tokens, null)
				)
				.build();
		SpongeIcon icon = new SpongeIcon(inc);
		icon.addListener(clickable -> {
			ClickInventoryEvent event = clickable.getEvent();
			if(event instanceof ClickInventoryEvent.Shift) {
				if(event instanceof ClickInventoryEvent.Shift.Secondary) {
					this.amount = Math.min(PluginInstance.getInstance().getConfiguration().get(ConfigKeys.MAX_MONEY_PRICE), this.amount + config.get(PokemonConfigKeys.PRICING_RIGHTCLICK_SHIFT));
				} else {
					this.amount = Math.min(PluginInstance.getInstance().getConfiguration().get(ConfigKeys.MAX_MONEY_PRICE), this.amount + config.get(PokemonConfigKeys.PRICING_LEFTCLICK_SHIFT));
				}
			} else {
				if(event instanceof ClickInventoryEvent.Secondary) {
					this.amount = Math.min(PluginInstance.getInstance().getConfiguration().get(ConfigKeys.MAX_MONEY_PRICE), this.amount + config.get(PokemonConfigKeys.PRICING_RIGHTCLICK_BASE));
				} else {
					this.amount = Math.min(PluginInstance.getInstance().getConfiguration().get(ConfigKeys.MAX_MONEY_PRICE), this.amount + config.get(PokemonConfigKeys.PRICING_LEFTCLICK_BASE));
				}
			}
			this.display.setSlot(39, this.moneyIcon(tokens));
		});

		return icon;
	}

	private SpongeIcon moneyIcon(Map<String, Function<CommandSource, Optional<Text>>> tokens) {
		EconomyService economy = ((SpongePlugin) PluginInstance.getInstance()).getEconomy();
		tokens.put("gts_price", src -> Optional.of(economy.getDefaultCurrency().format(new BigDecimal(this.amount))));
		Config msgConfig = PluginInstance.getInstance().getMsgConfig();
		TextParsingUtils parser = ((SpongePlugin) PluginInstance.getInstance()).getTextParsingUtils();
		ItemStack inc = ItemStack.builder()
				.itemType(ItemTypes.GOLD_INGOT)
				.add(Keys.DISPLAY_NAME, ((SpongePlugin) PluginInstance.getInstance()).getTextParsingUtils().fetchAndParseMsg(
						viewer, PluginInstance.getInstance().getMsgConfig(), MsgConfigKeys.PRICE_DISPLAY_TITLE, null, null
				))
				.add(Keys.ITEM_LORE,
						parser.fetchAndParseMsgs(this.viewer, msgConfig, MsgConfigKeys.PRICE_DISPLAY_LORE, tokens, null)
				)
				.build();
		return new SpongeIcon(inc);
	}

	private SpongeIcon moneyDecIcon(Map<String, Function<CommandSource, Optional<Text>>> tokens) {
		Config config = ReforgedBridge.getInstance().getConfig();
		Config msgConfig = PluginInstance.getInstance().getMsgConfig();
		TextParsingUtils parser = ((SpongePlugin) PluginInstance.getInstance()).getTextParsingUtils();
		ItemStack inc = ItemStack.builder()
				.itemType(ItemTypes.DYE)
				.add(Keys.DYE_COLOR, DyeColors.RED)
				.add(Keys.DISPLAY_NAME, ((SpongePlugin) PluginInstance.getInstance()).getTextParsingUtils().fetchAndParseMsg(
						viewer, PluginInstance.getInstance().getMsgConfig(), MsgConfigKeys.BUTTONS_DECREASE_CURRENCY_TITLE, null, null
				))
				.add(Keys.ITEM_LORE,
						parser.fetchAndParseMsgs(this.viewer, msgConfig, MsgConfigKeys.BUTTONS_DECREASE_CURRENCY_LORE, tokens, null)
				)
				.build();
		SpongeIcon icon = new SpongeIcon(inc);
		icon.addListener(clickable -> {
			ClickInventoryEvent event = clickable.getEvent();
			if(event instanceof ClickInventoryEvent.Shift) {
				if(event instanceof ClickInventoryEvent.Shift.Secondary) {
					this.amount = Math.max(min, this.amount - config.get(PokemonConfigKeys.PRICING_RIGHTCLICK_SHIFT));
				} else {
					this.amount = Math.max(min, this.amount - config.get(PokemonConfigKeys.PRICING_LEFTCLICK_SHIFT));
				}
			} else {
				if(event instanceof ClickInventoryEvent.Secondary) {
					this.amount = Math.max(min, this.amount - config.get(PokemonConfigKeys.PRICING_RIGHTCLICK_BASE));
				} else {
					this.amount = Math.max(min, this.amount - config.get(PokemonConfigKeys.PRICING_LEFTCLICK_BASE));
				}
			}
			this.display.setSlot(39, this.moneyIcon(tokens));
		});

		return icon;
	}

	private double calcMin(Pokemon pokemon) {
		if(!PluginInstance.getInstance().getConfiguration().get(ConfigKeys.MIN_PRICING_ENABLED)) {
			return 1.0;
		}

		double price = ReforgedBridge.getInstance().getConfig().get(PokemonConfigKeys.MIN_PRICING_POKEMON_BASE);
		boolean isLegend = EnumSpecies.legendaries.contains(pokemon.getSpecies().name());
		if (isLegend && pokemon.isShiny()) {
			price += ReforgedBridge.getInstance().getConfig().get(PokemonConfigKeys.MIN_PRICING_POKEMON_LEGEND) + ReforgedBridge.getInstance().getConfig().get(PokemonConfigKeys.MIN_PRICING_POKEMON_SHINY);
		} else if (isLegend) {
			price += ReforgedBridge.getInstance().getConfig().get(PokemonConfigKeys.MIN_PRICING_POKEMON_LEGEND);
		} else if (pokemon.isShiny()) {
			price += ReforgedBridge.getInstance().getConfig().get(PokemonConfigKeys.MIN_PRICING_POKEMON_SHINY);
		}

		for (int iv : pokemon.getStats().ivs.getArray()) {
			if (iv >= ReforgedBridge.getInstance().getConfig().get(PokemonConfigKeys.MIN_PRICING_POKEMON_IVS_MINVAL)) {
				price += ReforgedBridge.getInstance().getConfig().get(PokemonConfigKeys.MIN_PRICING_POKEMON_IVS_PRICE);
			}
		}

		if (pokemon.getAbilitySlot() == 2) {
			price += ReforgedBridge.getInstance().getConfig().get(PokemonConfigKeys.MIN_PRICING_POKEMON_HA);
		}

		return price;
	}

	private ItemStack pokemonDisplay(Pokemon pokemon) {
		Calendar calendar = Calendar.getInstance();
		boolean aprilFools = false;
		if(calendar.get(Calendar.MONTH) == Calendar.APRIL && calendar.get(Calendar.DAY_OF_MONTH) == 1) {
			aprilFools = true;
		}

		if(pokemon.isEgg()) {
			net.minecraft.item.ItemStack item = new net.minecraft.item.ItemStack(PixelmonItems.itemPixelmonSprite);
			NBTTagCompound nbt = new NBTTagCompound();
			switch (pokemon.getSpecies()) {
				case Manaphy:
				case Togepi:
					nbt.setString(NbtKeys.SPRITE_NAME,
							String.format("pixelmon:sprites/eggs/%s1", pokemon.getSpecies().name.toLowerCase()));
					break;
				default:
					nbt.setString(NbtKeys.SPRITE_NAME, "pixelmon:sprites/eggs/egg1");
					break;
			}
			item.setTagCompound(nbt);
			return (ItemStack) (Object) item;
		} else {
			return (ItemStack) (Object) (aprilFools ? ItemPixelmonSprite.getPhoto(Pixelmon.pokemonFactory.create(EnumSpecies.Bidoof)) : ItemPixelmonSprite.getPhoto(pokemon));
		}
	}
}
