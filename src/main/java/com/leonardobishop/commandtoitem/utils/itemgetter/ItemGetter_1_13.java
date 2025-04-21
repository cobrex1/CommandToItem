package com.leonardobishop.commandtoitem.utils.itemgetter;

import me.arcaniax.hdb.api.HeadDatabaseAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ItemGetter_1_13 implements ItemGetter {
    /*
     item reader for 1.13.X ONLY

     supporting:
      - name
      - material
      - lore
      - enchantments (NamespacedKey)
      - itemflags
      - unbreakable
      - attribute modifier
      */
    @Override
    public ItemStack getItem(String path, FileConfiguration config, JavaPlugin plugin) {
        String cName = config.getString(path + ".name", path + ".name");
        String cType = config.getString(path + ".item", path + ".item");
        if (cType != null && cType.toLowerCase().startsWith("hdb:")) {
            String headId = cType.substring(4);

            if (Bukkit.getPluginManager().getPlugin("HeadDatabase") != null) {
                HeadDatabaseAPI hdbApi = new HeadDatabaseAPI();
                ItemStack head = hdbApi.getItemHead(headId);

                if (head != null) {
                    return head;
                } else {
                    plugin.getLogger().warning("HeadsDatabase: Could not find head with ID: " + headId);
                    return new ItemStack(Material.STONE);
                }
            } else {
                plugin.getLogger().warning("HeadsDatabase plugin not found!");
                return new ItemStack(Material.BARRIER);
            }
        }
        boolean unbreakable = config.getBoolean(path + ".unbreakable", false);
        List<String> cLore = config.getStringList(path + ".lore");
        List<String> cItemFlags = config.getStringList(path + ".itemflags");
        boolean hasAttributeModifiers = config.contains(path + ".attributemodifiers");
        List<Map<?, ?>> cAttributeModifiers = config.getMapList(path + ".attributemodifiers");

        String name;
        Material type = null;
        int data = 0;

        // lore
        List<String> lore = new ArrayList<>();
        if (cLore != null) {
            for (String s : cLore) {
                lore.add(ChatColor.translateAlternateColorCodes('&', s));
            }
        }

        // name
        name = ChatColor.translateAlternateColorCodes('&', cName);

        // material
        try {
            type = Material.valueOf(cType);
        } catch (Exception e) {
            plugin.getLogger().warning("Unrecognised material 1.13: " + cType);
            type = Material.STONE;
        }

        ItemStack is = new ItemStack(type, 1, (short) data);
        ItemMeta ism = is.getItemMeta();
        ism.setLore(lore);
        ism.setDisplayName(name);

        // attribute modifiers
        if (hasAttributeModifiers) {
            for (Map<?, ?> attr : cAttributeModifiers) {
                String cAttribute = (String) attr.get("attribute");
                Attribute attribute = null;
                for (Attribute enumattr : Attribute.values()) {
                    if (enumattr.toString().equals(cAttribute)) {
                        attribute = enumattr;
                        break;
                    }
                }

                if (attribute == null) continue;

                Map<?, ?> configurationSection = (Map<?, ?>) attr.get("modifier");

                String cUUID = (String) configurationSection.get("uuid");
                String cModifierName = (String) configurationSection.get("name");
                String cModifierOperation = (String) configurationSection.get("operation");
                double cAmount;
                try {
                    Object cAmountObj = configurationSection.get("amount");
                    if (cAmountObj instanceof Integer) {
                        cAmount = ((Integer) cAmountObj).doubleValue();
                    } else {
                        cAmount = (Double) cAmountObj;
                    }
                } catch (Exception e) {
                    cAmount = 1;
                }
                String cEquipmentSlot = (String) configurationSection.get("equipmentslot");

                UUID uuid = null;
                if (cUUID != null) {
                    try {
                        uuid = UUID.fromString(cUUID);
                    } catch (Exception ignored) {
                        // ignored
                    }
                }
                EquipmentSlot equipmentSlot = null;
                if (cEquipmentSlot != null) {
                    try {
                        equipmentSlot = EquipmentSlot.valueOf(cEquipmentSlot);
                    } catch (Exception ignored) {
                        // ignored
                    }
                }
                AttributeModifier.Operation operation = AttributeModifier.Operation.ADD_NUMBER;
                try {
                    operation = AttributeModifier.Operation.valueOf(cModifierOperation);
                } catch (Exception ignored) {
                    // ignored
                }

                AttributeModifier modifier;
                if (uuid == null) {
                    modifier = new AttributeModifier(cModifierName, cAmount, operation);
                } else if (equipmentSlot == null) {
                    modifier = new AttributeModifier(uuid, cModifierName, cAmount, operation);
                } else {
                    modifier = new AttributeModifier(uuid, cModifierName, cAmount, operation, equipmentSlot);
                }

                ism.addAttributeModifier(attribute, modifier);
            }
        }

        // item flags
        if (config.isSet(path + ".itemflags")) {
            for (String flag : cItemFlags) {
                for (ItemFlag iflag : ItemFlag.values()) {
                    if (iflag.toString().equals(flag)) {
                        ism.addItemFlags(iflag);
                        break;
                    }
                }
            }
        }


        // unbreakable
        ism.setUnbreakable(unbreakable);

        // enchantments
        if (config.isSet(path + ".enchantments")) {
            for (String key : config.getStringList(path + ".enchantments")) {
                String[] split = key.split(":");
                if (split.length < 2) {
                    plugin.getLogger().warning("Enchantment does not follow format {namespace}:{name}:{level} : " + key);
                    continue;
                }
                String namespace = split[0];
                String ench = split[1];
                String levelName;
                if (split.length >= 3) {
                    levelName = split[2];
                } else {
                    levelName = "1";
                }

                NamespacedKey namespacedKey;
                try {
                    namespacedKey = new NamespacedKey(namespace, ench);
                } catch (Exception e) {
                    plugin.getLogger().warning("Unrecognised namespace: " + namespace);
                    continue;
                }
                Enchantment enchantment;
                if ((enchantment = Enchantment.getByKey(namespacedKey)) == null) {
                    plugin.getLogger().warning("Unrecognised enchantment: " + namespacedKey);
                    continue;
                }

                int level;
                try {
                    level = Integer.parseInt(levelName);
                } catch (NumberFormatException e) {
                    level = 1;
                }

                is.addUnsafeEnchantment(enchantment, level);
            }
        }

        is.setItemMeta(ism);
        return is;
    }
}
