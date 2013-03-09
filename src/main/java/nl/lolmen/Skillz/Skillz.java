package nl.lolmen.Skillz;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Properties;
import nl.lolmen.API.SkillzAPI;
import nl.lolmen.Skills.*;
import nl.lolmen.Skillz.Metrics.Plotter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Skillz extends JavaPlugin {

    //File stuff
    public String maindir = "plugins" + File.separator + "Skillz" + File.separator;
    public File skillzFile = new File(maindir + "skills.yml");
    //public static Skillz p;
    //Classes
    private Convert converter = new Convert(this);
    public HighScore high = new HighScore();
    private SkillPlayerListener player = new SkillPlayerListener(this);
    public FastBreak fb = new FastBreak();
    public static SkillzAPI api;
    private Metrics metrics;
    private SkillManager skillManager;
    private CustomSkillManager customManager;
    private MySQL mysql = null;
    private UserManager userManager;
    //For faster block breaking
    public HashMap<Player, Block> FBlock = new HashMap<Player, Block>();
    public HashMap<Player, Integer> FCount = new HashMap<Player, Integer>();
    //Settings
    public boolean useMySQL = false;
    protected String dbHost;
    protected String dbPass;
    protected String dbUser;
    protected String dbName;
    public String dbTable;
    protected int dbPort;
    public String noPerm = ChatColor.RED + "You do not have Permissions to do this!";
    public double version;
    public boolean update;
    public boolean debug;
    public boolean updateAvailable;
    public boolean hasVault;
    public boolean broadcast;
    
    private String debugPlayer;
    protected double newVersion;

    @Override
    public void onDisable() {
        this.getUserManager().save(false);
        if ((this.useMySQL) && (this.mysql != null)) {
            this.mysql.close();
        }
        this.high.saveMaps();
        this.getServer().getScheduler().cancelTasks(this);
        this.getLogger().info("Disabled!");
    }

    @Override
    public void onEnable() {
        api = new SkillzAPI(this);
        double time = System.nanoTime();
        this.makeSettings();
        this.loadSkillz();
        if (this.useMySQL) {
            loadMySQL();
        }
        this.loadUserManager();
        this.checkPlayers();
        this.startUserSavingThread();
        try {
            this.metrics = new Metrics(this);
            this.metrics.addCustomData(new Plotter() {
                @Override
                public String getColumnName() {
                    return "Total Level-Ups";
                }
                @Override
                public int getValue() {
                    int amount = CPU.levelUps;
                    CPU.levelUps = 0;
                    return amount;
                }
            });
            this.metrics.addCustomData(new Plotter() {
                @Override
                public String getColumnName() {
                    return "Total XP-Gained";
                }
                @Override
                public int getValue() {
                    int amount = CPU.xpUps;
                    CPU.xpUps = 0;
                    return amount;
                }
            });
            this.metrics.start();
            this.getLogger().info("Metrics loaded! View them @ http://mcstats.org/plugin/Skillz");
        } catch (IOException e) {
            e.printStackTrace();
            this.getLogger().info("Failed to load Metrics!");
        }
        if (this.update) {
            new Updater(this, "skillz", this.getFile(), Updater.UpdateType.DEFAULT, true);
        }
        this.setupPlugins();
        this.high.loadMaps();
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new SkillBlockListener(this), this);
        pm.registerEvents(new SkillEntityListener(this), this);
        pm.registerEvents(this.player, this);
        double end = System.nanoTime();
        double taken = (end - time) / 1000000;
        this.getLogger().info("version " + this.version + " build " + this.getDescription().getVersion() + " enabled - took " + taken + "ms!");
    }

    private void loadUserManager() {
        if (this.userManager == null) {
            this.userManager = new UserManager(this);
        }
        if (this.getServer().getOnlinePlayers().length != 0) {
            //There are players in the server
            for (Player p : this.getServer().getOnlinePlayers()) {
                this.userManager.loadPlayer(p.getName());
            }
        }
    }

    private void loadSkillz() {
        this.skillManager = new SkillManager(this);
        this.skillManager.loadSkillsSettings();
        this.customManager = new CustomSkillManager(this);
        this.customManager.loadCustomSkills();
        YamlConfiguration c = new YamlConfiguration();
        try {
            c.load(this.skillzFile);
            this.version = c.getDouble("version", 5.6);
            this.update = c.getBoolean("update", true);
            this.dbUser = c.getString("MySQL-User", "root");
            this.dbPass = c.getString("MySQL-Pass", "root");
            this.dbHost = c.getString("MySQL-Host", "localhost");
            this.dbPort = c.getInt("MySQL-Port", 3306);
            this.dbName = c.getString("MySQL-Database", "minecraft");
            this.dbTable = c.getString("MySQL-Table", "Skillz");
            this.useMySQL = c.getBoolean("useMySQL", false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public SkillzAPI api() {
        return Skillz.api;
    }

    public SkillManager getSkillManager() {
        return this.skillManager;
    }

    public CustomSkillManager getCustomSkillManager() {
        return this.customManager;
    }

    public MySQL getMySQL() {
        return this.mysql;
    }
    
    public UserManager getUserManager(){
        return this.userManager;
    }

    protected String getDatabaseTable() {
        return this.dbTable;
    }

    private void setupPlugins() {
        Plugin test;
        test = getServer().getPluginManager().getPlugin("Citizens");
        if (test != null) {
            SkillsSettings.setHasCitizens(true);
        } else {
            SkillsSettings.setHasCitizens(false);
        }
        test = getServer().getPluginManager().getPlugin("Vault");
        if (test != null) {
            SkillsSettings.setHasVault(true);
            this.getLogger().info("Hooked into Vault, just in case :)");
            return;
        }
        test = getServer().getPluginManager().getPlugin("Worldguard");
        if(test != null){
            SkillsSettings.setWorldGuard((WorldGuardPlugin)test);
        }
        if (SkillsSettings.getMoneyOnLevelup() == 0) {
            return;
        }
        this.getLogger().warning("Vault not found. Money reward -> 0");
        SkillsSettings.setMoneyOnLevelup(0);
    }

    public void loadMySQL() {
        this.mysql = new MySQL(this.dbHost, this.dbPort, this.dbUser, this.dbPass, this.dbName, this.dbTable);
        if (this.mysql.isFault()) {
            this.useMySQL = false;
        }
    }

    private void makeSettings() {
        new File(this.maindir).mkdir();
        if (new File(this.maindir + "skills/").exists()) {
            new File(this.maindir + "skills/").delete();
        }
        if (new File(this.maindir + "users/").exists()) {
            new File(this.maindir + "users/").renameTo(new File(this.maindir + "players"));
        } else {
            new File(this.maindir + "players/").mkdir();
        }
    }

    /**
     * @param str Command name, not used, using cmd.getName()
     */
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String str, String[] args) {
        if (cmd.getName().equalsIgnoreCase("skills")) {
            try {
                if (args.length == 0) {
                    if (!sender.hasPermission("skillz.skills")) {
                        sender.sendMessage(this.noPerm);
                        return true;
                    }
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(ChatColor.RED + "You are not a player!");
                        return true;
                    }
                    Player p = (Player) sender;
                    sender.sendMessage(ChatColor.RED + "===Skillz===");
                    new SkillsCommand().sendSkills(p, this);
                    return true;
                }
                if (args[0].equalsIgnoreCase("check")) {
                    if (!sender.hasPermission("skillz.check")) {
                        sender.sendMessage(this.noPerm);
                        return true;
                    }
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(ChatColor.RED + "Only players can use this option!");
                        return true;
                    }
                    for (SkillBase s : this.getSkillManager().getSkills()) {
                        if (!s.isEnabled()) {
                            continue;
                        }
                        this.getNextLevel((Player) sender, s.getSkillName());
                    }
                    return true;
                }
                if (args[0].equalsIgnoreCase("top")) {
                    if (args.length == 1) {
                        sender.sendMessage(ChatColor.RED + "===HighScores===");
                        for (SkillBase s : this.getSkillManager().getSkills()) {
                            if (!s.isEnabled()) {
                                continue;
                            }
                            sender.sendMessage(this.high.gethighest(s));
                        }
                        return true;
                    } else if (args.length == 2) {
                        sender.sendMessage(ChatColor.RED + "===HighScores===");
                        if (this.getSkillManager().skills.containsKey(args[1])) {
                            sender.sendMessage(this.high.gethighest(this.skillManager.skills.get(args[1])));
                        } else {
                            sender.sendMessage("No such skill: " + args[1]);
                        }
                        return true;
                    } else {
                        sender.sendMessage(ChatColor.RED + "Too many arguments!");
                        return true;
                    }
                }
                if (args[0].equalsIgnoreCase("convert")) {
                    if (sender.isOp()) {
                        if (args.length == 3) {
                            String from = args[1];
                            String to = args[2];
                            if (from.contains("flat")) {
                                if (to.equalsIgnoreCase("mysql")) {
                                    if (this.converter.flatToMySQL()) {
                                        sender.sendMessage("Conversion succesful! Using MySQL now!");
                                        return true;
                                    }
                                    sender.sendMessage("Something went wrong! Check the log!");
                                    return true;
                                }
                                sender.sendMessage("Not sure what you ment by " + to);
                                return true;
                            }
                            if (from.equalsIgnoreCase("mysql")) {
                                if (to.contains("flat")) {
                                    if (this.converter.MySQLtoFlat()) {
                                        sender.sendMessage("Conversion succesful! Using Flatfile!");
                                        return true;
                                    }
                                    sender.sendMessage("Something went wrong! Check the log!");
                                    return true;
                                }
                                sender.sendMessage("Not sure what you ment by " + to);
                                return true;
                            }
                            sender.sendMessage("Not sure what you ment by " + from);
                            return true;
                        } else if (args.length == 2) {
                            sender.sendMessage(ChatColor.RED + "Too little arguments!");
                            return true;
                        } else {
                            sender.sendMessage(ChatColor.RED + "Too many arguments!");
                            return true;
                        }
                    }
                    sender.sendMessage(this.noPerm);
                    return true;
                }
                if (args[0].equalsIgnoreCase("reset")) {
                    if (args.length == 1) {
                        if (!(sender instanceof Player)) {
                            sender.sendMessage(ChatColor.RED + "You are not a player!");
                            return true;
                        }
                        Player p = (Player) sender;
                        if (!p.hasPermission("skillz.reset.self")) {
                            p.sendMessage(this.noPerm);
                            return true;
                        }
                        this.getUserManager().getPlayer(sender.getName()).reset();
                        return true;
                    }
                    if (!sender.hasPermission("skillz.reset.other")) {
                        sender.sendMessage(this.noPerm);
                    }
                    for (int i = 1; i < args.length; i++) {
                        Player target = getServer().getPlayer(args[i]);
                        if (target == null) {
                            sender.sendMessage("Can't find player " + args[i]);
                            continue;
                        }
                        if(this.getUserManager().hasPlayer(target.getName())){
                            this.getUserManager().getPlayer(target.getName()).reset();
                            sender.sendMessage("Resetted data for " + target.getName());
                            continue;
                        }else{
                            sender.sendMessage(target.getName() + " doesn't have a User Profile! Nothing to reset!");
                            continue;
                        }
                    }
                    return true;
                }
                if (args[0].equalsIgnoreCase("page")) {
                    if (args.length == 1) {
                        sender.sendMessage("Please specify the page you want to see!");
                        return true;
                    }
                    try {
                        int page = Integer.parseInt(args[1]);
                        new SkillsCommand().sendSkills((Player) sender, page, this);
                        return true;
                    } catch (Exception e) {
                        sender.sendMessage("Page must be an int!");
                        return true;
                    }
                }
                if (args[0].equalsIgnoreCase("config")) {
                    if (sender.isOp()) {
                        if (this.getSkillManager().beingConfigged) {
                            sender.sendMessage("Someone is already configging the plugin!");
                            return true;
                        }
                        if (this.getSkillManager().configed) {
                            sender.sendMessage("Skillz already is configed! Edit skills.yml to edit");
                            return true;
                        }
                        if (!(sender instanceof Player)) {
                            sender.sendMessage("You have to be player to use this command!");
                            return true;
                        }
                        this.getSkillManager().configger = new Configurator(this, (Player) sender);
                        this.getSkillManager().beingConfigged = true;
                    } else {
                        sender.sendMessage("You have to be OP to use this command!");
                    }
                    return true;
                }
                if(args[0].equalsIgnoreCase("debug")){
                    //Just some debug code, removing when it works
                    if(this.debugPlayer == null){
                        this.debugPlayer = sender.getName();
                        sender.sendMessage("Debug enabled on you");
                        return true;
                    }
                    if(this.debugPlayer.equals(sender.getName())){
                        sender.sendMessage("Debug disabled on you");
                        this.debugPlayer = null;
                        return true;
                    }
                    this.getServer().getPlayer(this.debugPlayer).sendMessage("Disabling debug on you...");
                    this.debugPlayer = sender.getName();
                    sender.sendMessage("Debug enabled on you");
                    return true;
                }
                if(args[0].equalsIgnoreCase("save")){
                    if(!sender.hasPermission("skillz.save")){
                        sender.sendMessage(this.noPerm);
                        return true;
                    }
                    this.getUserManager().save(false);
                    sender.sendMessage("Saving complete!");
                    return true;
                }
                if(args[0].equalsIgnoreCase("addxp")){
                	if(sender.hasPermission("skills.addxp")){
                	try{
                	if(args[1] != null && args[2] != null && args[3] != null){
                     SkillBase s;
                     s = this.getSkillManager().skills.get(args[2]);

                     if (s == null) {
                                 s = this.getCustomSkillManager().getSkill(args[2]);
                                 if(s == null)
                                 {
                                	 return false;
                                 }
                                 
                             }  
                             
                             Player p = Bukkit.getPlayer(args[1]);

                             s.addXP(p, Integer.parseInt(args[3]));
            
                             return true;
                	}
                	return false;
                	}
                	catch(Exception e)
                	{}
                	}
                }
                if(args[0].equalsIgnoreCase("clean")){
                    if(!sender.hasPermission("skillz.clean")){
                        sender.sendMessage(this.noPerm);
                        return true;
                    }
                    if(!this.useMySQL){
                        sender.sendMessage("You are not using MySQL, no need to clean.");
                        return true;
                    }
                    this.getMySQL().clean(this.getDatabaseTable());
                    sender.sendMessage("Database cleaned!");
                    return true;
                }
                if(args[0].equalsIgnoreCase("reload")){
                    if(!sender.hasPermission("skillz.reload")){
                        sender.sendMessage(this.noPerm);
                        return true;
                    }
                    this.getSkillManager().reload();
                    sender.sendMessage(ChatColor.GREEN + "Skillz Reloaded!");
                    return true;
                }
                //Get another players Skills
                if (!sender.hasPermission("skillz.skills.other")) {
                    sender.sendMessage(this.noPerm);
                    return true;
                }
                int page = 1;
                try {
                    page = args.length == 2 ? Integer.parseInt(args[1]) : 1;
                } catch (Exception e) {
                    page = 1;
                }
                sender.sendMessage(ChatColor.RED + "===Skillz===");
                Player p = getServer().getPlayer(args[0]);
                if (p == null) {
                    OfflinePlayer p2 = this.getServer().getOfflinePlayer(args[0]);
                    if (p2 == null) {
                        sender.sendMessage("No player available by that name: " + args[0]);
                        return true;
                    }
                    new SkillsCommand().sendSkills(sender, p2.getName(), page, this);
                    return true;
                }
                new SkillsCommand().sendSkills(sender, p, page, this);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    private void getNextLevel(Player player, final String strings) {
        final String name = player.getName();
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                Player p = Bukkit.getPlayer(name);
                String string = strings.toLowerCase();
                if (useMySQL) {
                    try {
                        String query = "SELECT * FROM " + dbTable + " WHERE player = '" + p.getName() + "' AND skill = '" + string + "';";
                        ResultSet set = mysql.executeQuery(query);
                        if (set == null) {
                            p.sendMessage(ChatColor.RED + "There is no such skill: " + string);
                            return;
                        }
                        while (set.next()) {
                            int xp = set.getInt("xp");
                            int lvl = set.getInt("level");
                            int remaining = ((lvl) * (lvl) * 10) - xp;
                            p.sendMessage("XP remaining for " + ChatColor.RED + string + ChatColor.WHITE + ": " + ChatColor.RED + Integer.toString(remaining));
                            return;
                        }
                        p.sendMessage(ChatColor.RED + "There is no such skill: " + string);
                        return;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return;
                }
                Properties prop = new Properties();
                try {
                    FileInputStream in = new FileInputStream(maindir + "players/" + p.getName().toLowerCase() + ".txt");
                    prop.load(in);
                    if (!prop.containsKey(string)) {
                        p.sendMessage(ChatColor.RED + "There is no such skill: " + string);
                        return;
                    }
                    String get = prop.getProperty(string);
                    String[] array = get.split(";");
                    int xp = Integer.parseInt(array[0]);
                    int lvl = Integer.parseInt(array[1]);
                    int remaining = ((lvl) * (lvl) * 10) - xp;
                    p.sendMessage("XP remaining for " + ChatColor.RED + string + ChatColor.WHITE + ": " + ChatColor.RED + Integer.toString(remaining));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();
    }

    private void startUserSavingThread() {
        this.getServer().getScheduler().runTaskTimer(this, new Runnable(){
            @Override
            public void run() {
                getUserManager().save(true);
            }
        }, 24000L, 24000L);
    }

    private void checkPlayers() {
        Player[] online = this.getServer().getOnlinePlayers();
        for(int i = 0; i < online.length; i++){
            String name = online[i].getName();
            if(SkillsSettings.isDebug()){
                this.getLogger().info("[Debug] Reloading player " + name);
            }
            this.getUserManager().loadPlayer(online[i].getName());
        }
    }
    
    public void debug(String message){
        this.debug(message, 2);
    }
    
    public void debug(String message, int level){
        // 1 = only if Debug is on, 2 is on Player, 3 is global
        if(level == 3){
            this.getLogger().info("[Debug] " + message);
            return;
        }
        if(level == 2){
            if(debugPlayer == null){
                this.debug(message, 1);
                // not debugging to player
                return;
            }
            this.getServer().getPlayer(this.debugPlayer).sendMessage("[Debug] " + message);
            return;
        }
        if(level == 1){
            if(SkillsSettings.isDebug()){
                this.getLogger().info("[Debug] " + message);
            }
        }
    }
}
