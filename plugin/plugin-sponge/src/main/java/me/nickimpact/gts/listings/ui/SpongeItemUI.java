package me.nickimpact.gts.listings.ui;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.collect.Lists;
import com.nickimpact.impactor.api.Impactor;
import com.nickimpact.impactor.api.gui.signs.SignQuery;
import com.nickimpact.impactor.api.services.text.MessageService;
import com.nickimpact.impactor.sponge.ui.SpongeIcon;
import com.nickimpact.impactor.sponge.ui.SpongeLayout;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.nickimpact.gts.common.config.MsgConfigKeys;
import me.nickimpact.gts.common.ui.Historical;
import me.nickimpact.gts.sponge.listings.ui.AbstractSpongeEntryUI;
import me.nickimpact.gts.ui.SpongeMainMenu;
import me.nickimpact.gts.ui.components.TimeSelectMenu;
import me.nickimpact.gts.util.Utilities;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.DyeColors;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.InventoryTransformation;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.entity.Hotbar;
import org.spongepowered.api.item.inventory.entity.MainPlayerInventory;
import org.spongepowered.api.item.inventory.property.InventoryDimension;
import org.spongepowered.api.item.inventory.property.SlotIndex;
import org.spongepowered.api.item.inventory.query.QueryOperationTypes;
import org.spongepowered.api.item.inventory.type.GridInventory;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.Optional;
import java.util.function.Supplier;

import static me.nickimpact.gts.util.Utilities.readMessageConfigOption;

public class SpongeItemUI extends AbstractSpongeEntryUI<SpongeItemUI.Chosen> implements Historical<SpongeMainMenu> {

    private double price = 50;

    public SpongeItemUI(Player viewer) {
        super(viewer);
        this.getDisplay().attachListener((player, event) -> {
                    event.getTransactions().forEach(transaction -> {
                        transaction.getSlot().getProperty(SlotIndex.class, "slotindex").ifPresent(slot -> {
                            //noinspection ConstantConditions
                            if(slot.getValue() >= 45) {
                                if(!transaction.getOriginal().getType().equals(ItemTypes.AIR)) {
                                    ItemStackSnapshot clicked = transaction.getOriginal();
                                    if(this.chosen != null) {
                                        this.viewer.sendMessage(Text.of(TextColors.RED, "You've already selected an item..."));
                                        return;
                                    }

                                    final int s = slot.getValue() - 45;

                                    this.chosen = new Chosen(clicked, s);
                                    this.getDisplay().setSlot(13, this.createChosenIcon());
                                    this.getDisplay().setSlot(44, this.generateConfirmIcon());
                                    this.style(true);
                                    //this.display.setSlot(slot.getValue(), new SpongeIcon(ItemStackSnapshot.NONE.createStack()));
                                    this.viewer.getOpenInventory().ifPresent(inv -> {
                                        inv.query(QueryOperationTypes.INVENTORY_TYPE.of(MainPlayerInventory.class))
                                                .transform(InventoryTransformation.of(
                                                        QueryOperationTypes.INVENTORY_TYPE.of(GridInventory.class),
                                                        QueryOperationTypes.INVENTORY_TYPE.of(Hotbar.class)
                                                ))
                                                .query(QueryOperationTypes.INVENTORY_PROPERTY.of(SlotIndex.of(s)))
                                                .first()
                                                .set(ItemStackSnapshot.NONE.createStack());
                                    });
                                }
                            }
                        });
                    });
                });
        this.getDisplay().define(this.design());
    }

    @Override
    protected Text getTitle() {
        return Text.of("Listing Creator - Items");
    }

    @Override
    protected InventoryDimension getDimensions() {
        return InventoryDimension.of(9, 5);
    }

    @Override
    public Optional<Chosen> getChosenOption() {
        return Optional.ofNullable(this.chosen);
    }

    @Override
    public void open(Player user) {
        this.getDisplay().open(user);
    }

    private SpongeLayout design() {
        final MessageService<Text> PARSER = Utilities.PARSER;

        SpongeLayout.SpongeLayoutBuilder slb = SpongeLayout.builder();
        slb.dimension(9, 4).border().dimension(9, 5);
        slb.slots(border(DyeColors.RED), 3, 4, 5, 10, 11, 12, 14, 15, 16, 21, 22, 23);
        slb.slots(SpongeIcon.BORDER, 19, 20, 24, 25, 37, 43);

        slb.slot(this.createNoneChosenIcon(), 13);

        SpongeIcon back = new SpongeIcon(ItemStack.builder()
                .itemType(ItemTypes.BARRIER)
                .add(Keys.DISPLAY_NAME, PARSER.parse(readMessageConfigOption(MsgConfigKeys.UI_GENERAL_BACK), Lists.newArrayList(() -> this.viewer)))
                .build()
        );
        back.addListener(clickable -> {
            this.getParent().ifPresent(parent -> parent.get().open());
        });
        slb.slot(back, 36);

        SpongeIcon price = this.generatePriceIcon(50);
        slb.slot(price, 38);

        ItemStack duration = ItemStack.builder()
                .itemType(ItemTypes.CLOCK)
                .add(Keys.DISPLAY_NAME, Text.of(TextColors.YELLOW, "Duration: ", TextColors.GREEN, "2 Hours"))
                .add(Keys.ITEM_LORE, Lists.newArrayList(

                ))
                .build();
        SpongeIcon time = new SpongeIcon(duration);
        time.addListener(clickable -> {
            new TimeSelectMenu(this.viewer).open();
        });
        slb.slot(time, 42);

        SpongeIcon waiting = this.generateWaitingIcon(false);
        slb.slot(waiting, 44);

        return slb.build();
    }

    @Override
    public Optional<Supplier<SpongeMainMenu>> getParent() {
        return Optional.of(() -> new SpongeMainMenu(this.viewer));
    }

    private SpongeIcon generatePriceIcon(double price) {
        ItemStack rep = ItemStack.builder()
                .itemType(ItemTypes.GOLD_INGOT)
                .add(Keys.DISPLAY_NAME, Text.of(TextColors.YELLOW, "Price: $", price))
                .add(Keys.ITEM_LORE, Lists.newArrayList(
                        Text.of(TextColors.GRAY, "The price a player will pay"),
                        Text.of(TextColors.GRAY, "in order to purchase this listing."),
                        Text.EMPTY,
                        Text.of(TextColors.YELLOW, "Click to edit!")
                ))
                .build();
        SpongeIcon icon = new SpongeIcon(rep);
        icon.addListener(clickable -> {
            SignQuery<Text, Player> query = SignQuery.<Text, Player>builder()
                .position(new Vector3d(0, 1, 0))
                .text(Lists.newArrayList(
                        Text.of(""),
                        Text.of("----------------"),
                        Text.of("Enter a Price"),
                        Text.of("for this Listing")
                ))
                .response(submission -> {
                    try {
                        double value = Double.parseDouble(submission.get(0));
                        if(value > 0) {
                            this.price = value;
                            SpongeIcon updated = this.generatePriceIcon(value);
                            this.getDisplay().setSlot(38, updated);

                            Impactor.getInstance().getScheduler().executeSync(() -> {
                                this.open(this.viewer);
                            });
                            return true;
                        }
                        return false;
                    } catch (Exception e) {
                        return false;
                    }
                })
                .reopenOnFailure(true)
                .build();
            this.viewer.closeInventory();
            query.sendTo(this.viewer);
        });
        return icon;
    }

    @Override
    public SpongeIcon createChosenIcon() {
        SpongeIcon icon = new SpongeIcon(this.chosen.getSelection().createStack());
        icon.addListener(clickable -> {
            this.getDisplay().setSlot(13, this.createNoneChosenIcon());
            this.getDisplay().setSlot(44, this.generateWaitingIcon(false));

            this.viewer.getOpenInventory().ifPresent(inv -> {
                inv.query(QueryOperationTypes.INVENTORY_TYPE.of(MainPlayerInventory.class))
                        .transform(InventoryTransformation.of(
                                QueryOperationTypes.INVENTORY_TYPE.of(GridInventory.class),
                                QueryOperationTypes.INVENTORY_TYPE.of(Hotbar.class)
                        ))
                        .query(QueryOperationTypes.INVENTORY_PROPERTY.of(SlotIndex.of(this.chosen.getSlot())))
                        .first()
                        .set(this.chosen.getSelection().createStack());
            });
            this.style(false);

            this.chosen = null;
        });
        return icon;
    }

    @Getter
    @RequiredArgsConstructor
    protected static class Chosen {
        private final ItemStackSnapshot selection;
        private final int slot;
    }

}