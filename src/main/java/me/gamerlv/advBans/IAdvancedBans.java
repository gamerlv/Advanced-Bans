/**
 * 
 */
package me.gamerlv.advBans;

import org.bukkit.entity.Player;

/**
 * @author levi
 * Inital layout of an API for advanced bans
 * @devnote This probably won't complete until AdvBans until 0.9
 */
public interface IAdvancedBans {
	
	public boolean ban( Player player, int time, String reason, String banBy );
	
	public boolean unban( Player player, String reason, String unBanBy );
	
	public boolean banIp( String IP, int time, String reason, String banBy );
	
	public boolean unbanIp( String IP, String reason, String unbanBy );
	
	public boolean isBanned( Player ply );
	
	public int ammountOfBans( Player ply , boolean activeOnly );

}
