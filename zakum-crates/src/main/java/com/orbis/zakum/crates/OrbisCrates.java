package com.orbis.zakum.crates;

import com.orbis.zakum.core.ZakumCore;
import com.orbis.zakum.crates.anim.CrateAnimator;
import com.orbis.zakum.crates.listener.CrateInteractionListener;
import org.bukkit.plugin.java.JavaPlugin;

public class OrbisCrates extends JavaPlugin {
    
    private ZakumCore core;
    private CrateAnimator animator;
    private CrateRewardExecutor executor;

    @Override
    public void onEnable() {
        if (getServer().getPluginManager().getPlugin("ZakumCore") == null) return;
        this.core = ZakumCore.getInstance();
        
        this.animator = new CrateAnimator();
        this.executor = new CrateRewardExecutor();
        
        // Start the Single-Ticker (Runs every 1 tick)
        getServer().getScheduler().runTaskTimer(this, animator, 1L, 1L);
        
        // Register Interaction Listener
        getServer().getPluginManager().registerEvents(new CrateInteractionListener(animator), this);
        
        getLogger().info("OrbisCrates Active. Animator Started.");
    }
    
    public CrateRewardExecutor getRewardExecutor() { return executor; }
    // public RenderService getRenderService() { ... } 
}
