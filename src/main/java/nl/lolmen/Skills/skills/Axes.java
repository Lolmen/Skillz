package nl.lolmen.Skills.skills;

import java.util.Random;

import org.bukkit.Bukkit;

import nl.lolmen.Skills.SkillBase;
import nl.lolmen.Skills.SkillsSettings;
import nl.lolmen.Skillz.MathProcessor;

public class Axes extends SkillBase{
	
	private int levelsPerExtraDamage;
	private String critCalc;

	public int getLevelsPerExtraDamage() {
		return levelsPerExtraDamage;
	}

	public void setLevelsPerExtraDamage(int levelsPerExtraDamage) {
		this.levelsPerExtraDamage = levelsPerExtraDamage;
	}

	public String getCritCalc() {
		return critCalc;
	}

	public void setCritCalc(String critCalc) {
		this.critCalc = critCalc;
	}
	
	public int getCritChance(int level){
		if(critCalc != null && critCalc != "" && critCalc.contains("$LEVEL")){
			String send = critCalc.replace("$LEVEL", Integer.toString(level));
			return (int)MathProcessor.processEquation(send);
		}else if(SkillsSettings.isDebug()){
			Bukkit.getLogger().info("Can't calculate crit chance, config is wrong: " + this.critCalc);
		}
		return 0;
	}
	
	public boolean willCrit(int level){
		int chance = this.getCritChance(level);
		Random rant = new Random();
		int result = rant.nextInt(100);
		if(result < chance){
			return true;
		}
		return false;
	}
	
	public int getExtraDamage(int level){
		return level / this.levelsPerExtraDamage;
	}

}