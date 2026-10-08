package com.hbm_m.inventory.menu;

import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.item.weapon.sedna.mods.XWeaponModManager;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code com.hbm.inventory.container.ContainerWeaponTable}. Slots 0-6 Mods, 7 Waffe, danach Spielerinventar.
 * Der Konfigurationswechsel (Original {@code slotClick} mit mode 999_999) laeuft ueber {@link #clickMenuButton}
 * (Vanilla-Paket ServerboundContainerButtonClick).
 * <p>Abweichung fuer 1.20: Die Waffenslot-Logik (Index zuruecksetzen, Mods aus der Waffe laden) laeuft nur serverseitig,
 * der Index wird per {@link DataSlot} an den Client gespiegelt - sonst setzt jede Slot-Synchronisation den
 * Client-Index auf 0 zurueck.</p>
 */
public class WeaponTableMenu extends AbstractContainerMenu {

	public SimpleContainer mods = new SimpleContainer(7);
	public Container gun = new ResultContainer();
	public int index = 0;

	private final Player player;

	public WeaponTableMenu(int id, Inventory inventory) {
		super(ModMenuTypes.WEAPON_TABLE_MENU.get(), id);
		this.player = inventory.player;

		for (int i = 0; i < 7; i++) this.addSlot(new ModSlot(mods, i, 44 + 18 * i, 108));

		this.addSlot(new Slot(gun, 0, 8, 108) {

			@Override
			public boolean mayPlace(ItemStack stack) {
				return gun.getItem(0).isEmpty() && stack.getItem() instanceof ItemGunBaseNT;
			}

			@Override
			public void set(ItemStack stack) {

				if (!isClient()) {
					WeaponTableMenu.this.index = 0;

					if (!stack.isEmpty()) {
						ItemStack[] mods = XWeaponModManager.getUpgradeItems(stack, WeaponTableMenu.this.index);

						if (mods != null) for (int i = 0; i < Math.min(mods.length, 7); i++) {
							WeaponTableMenu.this.mods.setItem(i, mods[i] == null ? ItemStack.EMPTY : mods[i]);
						}
					}
				}

				super.set(stack);
			}

			@Override
			public void onTake(Player player, ItemStack stack) {
				super.onTake(player, stack);

				XWeaponModManager.install(
						stack, WeaponTableMenu.this.index,
						mods.getItem(0),
						mods.getItem(1),
						mods.getItem(2),
						mods.getItem(3),
						mods.getItem(4),
						mods.getItem(5),
						mods.getItem(6));

				for (int i = 0; i < 7; i++) {
					ItemStack mod = WeaponTableMenu.this.mods.getItem(i);
					if (XWeaponModManager.isApplicable(stack, mod, WeaponTableMenu.this.index, false)) WeaponTableMenu.this.mods.setItem(i, ItemStack.EMPTY);
				}

				WeaponTableMenu.this.index = 0;
			}
		});

		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 9; j++) {
				this.addSlot(new Slot(inventory, j + i * 9 + 9, 8 + j * 18, 158 + i * 18));
			}
		}

		for (int i = 0; i < 9; i++) {
			this.addSlot(new Slot(inventory, i, 8 + i * 18, 216));
		}

		this.addDataSlot(new DataSlot() {
			@Override public int get() { return WeaponTableMenu.this.index; }
			@Override public void set(int value) { WeaponTableMenu.this.index = value; }
		});

		this.slotsChanged(this.mods);
	}

	private boolean isClient() {
		return this.player.level().isClientSide;
	}

	/** Original {@code slotClick(index, button, 999_999, player)}: Konfiguration wechseln. */
	@Override
	public boolean clickMenuButton(Player player, int button) {

		if (player.level().isClientSide) return false;
		ItemStack stack = gun.getItem(0);
		if (!stack.isEmpty() && stack.getItem() instanceof ItemGunBaseNT) {
			int configs = ((ItemGunBaseNT) stack.getItem()).getConfigCount();
			if (configs < button) return false;

			XWeaponModManager.install(
					stack, this.index,
					mods.getItem(0),
					mods.getItem(1),
					mods.getItem(2),
					mods.getItem(3),
					mods.getItem(4),
					mods.getItem(5),
					mods.getItem(6));

			for (int i = 0; i < 7; i++) {
				ItemStack mod = WeaponTableMenu.this.mods.getItem(i);
				if (XWeaponModManager.isApplicable(stack, mod, this.index, false)) WeaponTableMenu.this.mods.setItem(i, ItemStack.EMPTY);
			}

			this.index = button;

			ItemStack[] mods = XWeaponModManager.getUpgradeItems(stack, this.index);

			if (mods != null) for (int i = 0; i < Math.min(mods.length, 7); i++) {
				WeaponTableMenu.this.mods.setItem(i, mods[i] == null ? ItemStack.EMPTY : mods[i]);
			}

			this.broadcastChanges();
			return true;
		}
		return false;
	}

	@Override
	public void removed(Player player) {
		super.removed(player);

		if (!player.level().isClientSide) {
			for (int i = 0; i < this.mods.getContainerSize(); ++i) {
				ItemStack itemstack = this.mods.removeItemNoUpdate(i);

				if (!itemstack.isEmpty()) {
					player.drop(itemstack, false);
				}
			}

			ItemStack itemstack = this.gun.removeItemNoUpdate(0);

			if (!itemstack.isEmpty()) {
				XWeaponModManager.uninstall(itemstack, index);
				player.drop(itemstack, false);
			}
		}
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		ItemStack copy = ItemStack.EMPTY;
		Slot slot = this.slots.get(index);

		if (slot != null && slot.hasItem()) {
			ItemStack stack = slot.getItem();
			copy = stack.copy();

			if (index < 8) {
				if (!this.moveItemStackTo(stack, 8, this.slots.size(), true)) return ItemStack.EMPTY;
				// Kopie statt des (jetzt leeren) Stapels: ein leerer 1.20-Stapel liefert getItem() == AIR
				slot.onTake(player, copy);
			} else {
				if (stack.getItem() instanceof ItemGunBaseNT) {
					if (!this.moveItemStackTo(stack, 7, 8, false)) return ItemStack.EMPTY;
				} else {
					if (!this.moveItemStackTo(stack, 0, 7, false)) return ItemStack.EMPTY;
				}
			}

			if (stack.isEmpty()) {
				slot.set(ItemStack.EMPTY);
			} else {
				slot.setChanged();
			}
		}

		return copy;
	}

	public class ModSlot extends Slot {

		public ModSlot(Container inventory, int index, int x, int y) {
			super(inventory, index, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return !gun.getItem(0).isEmpty() && XWeaponModManager.isApplicable(gun.getItem(0), stack, WeaponTableMenu.this.index, true);
		}

		@Override
		public void set(ItemStack stack) {
			super.set(stack);
			refreshInstalledMods();
		}

		@Override
		public void onTake(Player player, ItemStack stack) {
			super.onTake(player, stack);
			refreshInstalledMods();
		}

		public void refreshInstalledMods() {
			if (gun.getItem(0).isEmpty()) return;
			XWeaponModManager.install(
					gun.getItem(0), WeaponTableMenu.this.index,
					mods.getItem(0),
					mods.getItem(1),
					mods.getItem(2),
					mods.getItem(3),
					mods.getItem(4),
					mods.getItem(5),
					mods.getItem(6)); //miscalculated, slot array isn't visible - fuck!
		}
	}
}
