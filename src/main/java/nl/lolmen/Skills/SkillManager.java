package nl.lolmen.Skills;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import nl.lolmen.Skills.skills.*;
import nl.lolmen.Skillz.Configurator;


public class SkillManager {
	private File settings = new File("plugins" + File.separator
			+ "Skillz" + File.separator + "skills.yml");
	public HashMap<String, SkillBase> skills = new HashMap<String, SkillBase>();

	public boolean configed = true;
	public boolean beingConfigged = false;
	public Configurator configger;

	public void loadSkillsSettings() {
		if (!settings.exists()) {
			createSkillsSettings();
			this.configed = false;
			Bukkit.getLogger().info("Going to ask OP to Config settings!");
		} 
		YamlConfiguration c = YamlConfiguration.loadConfiguration(settings);
		try {
			if(!c.contains("moneyOnLevelup")){ 
				createSkillsSettings();
				c = YamlConfiguration.loadConfiguration(settings); //load it again to not have an empty file
			}
			SkillsSettings.setBroadcastOnLevelup(c.getBoolean("broadcastOnLevelup", true));
			SkillsSettings.setDebug(c.getBoolean("debug", false));
			SkillsSettings.setItemOnLevelup(c.getString("itemOnLevelup", "89,1"));
			SkillsSettings.setLightningOnLevelup(c.getBoolean("lightningOnLevelup", false));
			SkillsSettings.setMoneyOnLevelup(c.getInt("moneyOnLevelup", 20));
			SkillsSettings.setResetSkillsOnLevelup(c.getBoolean("resetAllSkillsOnDeath", false));
			SkillsSettings.setLevelsDownOnDeath(c.getInt("loseLevelsOnDeath", 0));
			SkillsSettings.setLevelsReset(c.getString("lostLevels", "You lost levels because you died."));
			SkillsSettings.setFalldmg(c.getString("fallDamageMessage"));
			SkillsSettings.setLvlup(c.getString("levelupMessage"));
			SkillsSettings.setUsePerSkillPerms(c.getBoolean("usePermissionsForEverySkill", false));
			// Now the actual skills
			for (String key : c.getConfigurationSection("skills").getKeys(false)) {
				String keys = key.toLowerCase();
				boolean enabled = c.getBoolean("skills." + key + ".enabled", true);
				if(!enabled){
					continue;
				}
				int multiplier = c.getInt("skills." + key + ".XP-gain-multiplier", 1);
				String item = null;
				int money = -1;
				if (c.contains("skills." + key + ".reward.item")) {
					item = c.getString("skills." + key + ".reward.item");
				}else{
					item = SkillsSettings.getItemOnLevelup();
				}
				if (c.contains("skills." + key + ".reward.money")) {
					money = c.getInt("skills." + key + ".reward.money");
				}else{
					money = SkillsSettings.getMoneyOnLevelup();
				}
				HashMap<Integer, String> optReward = new HashMap<Integer, String>();
				HashMap<Integer, String> optRewardFixed = new HashMap<Integer, String>();
				if(c.contains("skills." + key + ".reward.every_many_levels")){
					for(String s: c.getConfigurationSection("skills." + key + ".reward.every_many_levels").getKeys(false)){
						optReward.put(Integer.parseInt(s), c.getString("skills." + key + ".reward.every_many_levels." + s));
					}
				}
				if(c.contains("skills." + key + ".reward.fixed_levels")){
					for(String s: c.getConfigurationSection("skills." + key + ".reward.fixed_levels").getKeys(false)){
						optRewardFixed.put(Integer.parseInt(s), c.getString("skills." + key + ".reward.fixed_levels." + s));
					}
				}
				if (key.equalsIgnoreCase("archery")) {
					Archery a = new Archery();
					a.setBlocks_till_XP(c.getInt("skills." + key + ".blocks-till-1XP-add", 10));
					a.setItemOnLevelup(item);
					a.setMoneyOnLevelup(money);
					a.setSkillName(key);
					a.setEnabled(enabled);
					a.setMultiplier(multiplier);
					a.setCritCalc(c.getString("skills." + key + ".critChance", "$LEVEL*0.1"));
					if(!optReward.isEmpty()){
						for(int i : optReward.keySet()){
							a.add_to_every_many_levels(i, optReward.get(i));
						}
					}
					if(!optRewardFixed.isEmpty()){
						for(int i : optRewardFixed.keySet()){
							a.add_to_fixed_levels(i, optRewardFixed.get(i));
						}
					}
					skills.put(keys, a);
					continue;
				}
				if(keys.equalsIgnoreCase("acrobatics")){
					Acrobatics a = new Acrobatics();
					a.setItemOnLevelup(item);
					a.setMoneyOnLevelup(money);
					a.setSkillName(keys);
					a.setEnabled(enabled);
					a.setMultiplier(multiplier);
					a.setLevelsTillLessDMG(c.getInt("skills." + key + ".levels-per-reducted-damage", 5));
					if(!optReward.isEmpty()){
						for(int i : optReward.keySet()){
							a.add_to_every_many_levels(i, optReward.get(i));
						}
					}
					if(!optRewardFixed.isEmpty()){
						for(int i : optRewardFixed.keySet()){
							a.add_to_fixed_levels(i, optRewardFixed.get(i));
						}
					}
					skills.put(keys, a);
					continue;
				}
				if (key.equalsIgnoreCase("mining")) {
					Mining a = new Mining();
					a.setItemOnLevelup(item);
					a.setMoneyOnLevelup(money);
					a.setSkillName(key);
					a.setEnabled(enabled);
					a.setMultiplier(multiplier);
					a.setAllFromFirstLevel(c.getBoolean("skills." + key + ".MineAllBlocksFromFirstLevel"));
					if(!optReward.isEmpty()){
						for(int i : optReward.keySet()){
							a.add_to_every_many_levels(i, optReward.get(i));
						}
					}
					if(!optRewardFixed.isEmpty()){
						for(int i : optRewardFixed.keySet()){
							a.add_to_fixed_levels(i, optRewardFixed.get(i));
						}
					}
					for(String s: c.getConfigurationSection("skills." + key + ".block_level").getKeys(false)){
						a.addBlockLevels(Integer.parseInt(s), c.getInt("skills." + key + ".block_level." + s));
					}
					for(String s: c.getConfigurationSection("skills." + key + ".block_XP").getKeys(false)){
						a.addBlock(Integer.parseInt(s), c.getInt("skills." + key + ".block_XP." + s));
					}
					a.setSpeed(c.getInt("miningspeed", 1));
					a.setDoubleDropChange(c.getInt("change", 5000));
					skills.put(keys, a);
					continue;
				}
				if(key.equalsIgnoreCase("woodcutting")){
					Woodcutting a = new Woodcutting();
					a.setItemOnLevelup(item);
					a.setMoneyOnLevelup(money);
					a.setSkillName(key);
					a.setEnabled(enabled);
					a.setMultiplier(multiplier);
					a.setAllFromFirstLevel(c.getBoolean("skills." + key + ".MineAllBlocksFromFirstLevel"));
					for(String s: c.getConfigurationSection("skills." + key + ".block_level").getKeys(false)){
						a.addBlockLevels(Integer.parseInt(s), c.getInt("skills." + key + ".block_level." + s));
					}
					for(String s: c.getConfigurationSection("skills." + key + ".block_XP").getKeys(false)){
						a.addBlock(Integer.parseInt(s), c.getInt("skills." + key + ".block_XP." + s));
					}
					if(!optReward.isEmpty()){
						for(int i : optReward.keySet()){
							a.add_to_every_many_levels(i, optReward.get(i));
						}
					}
					if(!optRewardFixed.isEmpty()){
						for(int i : optRewardFixed.keySet()){
							a.add_to_fixed_levels(i, optRewardFixed.get(i));
						}
					}
					skills.put(keys, a);
					continue;
				}
				if(key.equalsIgnoreCase("digging")){
					Digging a = new Digging();
					a.setItemOnLevelup(item);
					a.setMoneyOnLevelup(money);
					a.setSkillName(key);
					a.setEnabled(enabled);
					a.setMultiplier(multiplier);
					a.setAllFromFirstLevel(c.getBoolean("skills." + key + ".MineAllBlocksFromFirstLevel"));
					for(String s: c.getConfigurationSection("skills." + key + ".block_level").getKeys(false)){
						a.addBlockLevels(Integer.parseInt(s), c.getInt("skills." + key + ".block_level." + s));
					}
					for(String s: c.getConfigurationSection("skills." + key + ".block_XP").getKeys(false)){
						a.addBlock(Integer.parseInt(s), c.getInt("skills." + key + ".block_XP." + s));
					}
					if(!optReward.isEmpty()){
						for(int i : optReward.keySet()){
							a.add_to_every_many_levels(i, optReward.get(i));
						}
					}
					if(!optRewardFixed.isEmpty()){
						for(int i : optRewardFixed.keySet()){
							a.add_to_fixed_levels(i, optRewardFixed.get(i));
						}
					}
					skills.put(keys, a);
					continue;
				}
				if(keys.startsWith("swords")){
					Swords a = new Swords();
					a.setItemOnLevelup(item);
					a.setMoneyOnLevelup(money);
					a.setSkillName("swords");
					a.setEnabled(enabled);
					a.setMultiplier(multiplier);
					a.setLevelsPerExtraDamage(c.getInt("skills." + key + ".levelsPerExtraDamage", 20));
					a.setCritCalc(c.getString("skills." + key + ".critChance", "$LEVEL*0.1"));
					if(!optReward.isEmpty()){
						for(int i : optReward.keySet()){
							a.add_to_every_many_levels(i, optReward.get(i));
						}
					}
					if(!optRewardFixed.isEmpty()){
						for(int i : optRewardFixed.keySet()){
							a.add_to_fixed_levels(i, optRewardFixed.get(i));
						}
					}
					skills.put("swords", a);
					continue;
				}
				if(keys.startsWith("axes")){
					Axes a = new Axes();
					a.setItemOnLevelup(item);
					a.setMoneyOnLevelup(money);
					a.setSkillName("axes");
					a.setEnabled(enabled);
					a.setMultiplier(multiplier);
					a.setLevelsPerExtraDamage(c.getInt("skills." + key + ".levelsPerExtraDamage", 20));
					a.setCritCalc(c.getString("skills." + key + ".critChance", "$LEVEL*0.1"));
					if(!optReward.isEmpty()){
						for(int i : optReward.keySet()){
							a.add_to_every_many_levels(i, optReward.get(i));
						}
					}
					if(!optRewardFixed.isEmpty()){
						for(int i : optRewardFixed.keySet()){
							a.add_to_fixed_levels(i, optRewardFixed.get(i));
						}
					}
					skills.put("axes", a);
					continue;
				}
				if(keys.startsWith("unarmed")){
					Unarmed a = new Unarmed();
					a.setItemOnLevelup(item);
					a.setMoneyOnLevelup(money);
					a.setSkillName("unarmed");
					a.setEnabled(enabled);
					a.setMultiplier(multiplier);
					a.setLevelsPerExtraDamage(c.getInt("skills." + key + ".levelsPerExtraDamage", 20));
					a.setCritCalc(c.getString("skills." + key + ".critChance", "$LEVEL*0.1"));
					if(!optReward.isEmpty()){
						for(int i : optReward.keySet()){
							a.add_to_every_many_levels(i, optReward.get(i));
						}
					}
					if(!optRewardFixed.isEmpty()){
						for(int i : optRewardFixed.keySet()){
							a.add_to_fixed_levels(i, optRewardFixed.get(i));
						}
					}
					skills.put("unarmed", a);
					continue;
				}
				SkillBase a = new SkillBase();
				a.setItemOnLevelup(item);
				a.setMoneyOnLevelup(money);
				a.setSkillName(key);
				a.setEnabled(enabled);
				a.setMultiplier(multiplier);
				if(!optReward.isEmpty()){
					for(int i : optReward.keySet()){
						a.add_to_every_many_levels(i, optReward.get(i));
					}
				}
				if(!optRewardFixed.isEmpty()){
					for(int i : optRewardFixed.keySet()){
						a.add_to_fixed_levels(i, optRewardFixed.get(i));
					}
				}
				skills.put(keys, a);
				continue;
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public HashSet<SkillBase> getSkills() {
		HashSet<SkillBase> set = new HashSet<SkillBase>();
		for (String s : skills.keySet()) {
			set.add(skills.get(s));
		}
		return set;
	}

	public void createSkillsSettings() {
		Bukkit.getLogger().info("Trying to create default skills...");
		try {
			new File("plugins/Skillz/").mkdir();
			File efile = new File("plugins" + File.separator + "Skillz" + File.separator + "skills.yml");
			InputStream in = this.getClass().getClassLoader().getResourceAsStream("skills.yml");
			OutputStream out = new BufferedOutputStream(new FileOutputStream(efile));
			int c;
			while ((c = in.read()) != -1) {
				out.write(c);
			}
			out.flush();
			out.close();
			in.close();
			Bukkit.getLogger().info("Default skills created succesfully!");
		} catch (Exception e) {
			e.printStackTrace();
			Bukkit.getLogger().info("Error creating Skills file! Using default Skills!");
		}
	}

}
