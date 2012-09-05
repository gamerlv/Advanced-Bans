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
	
	/**
	 * Ban a active player
	 * @param player
	 * @param time Time in minutes
	 * @param reason
	 * @param banBy
	 * @return false on failure
	 */
	public boolean ban( Player ply, int time, String reason, String banBy );
	
	/**
	 * Remove the active bans for player ply
	 * @param player
	 * @param reason Optional reason for removing the ban
	 * @param unBanBy Who unbanned the player
	 * @return False on failure
	 */
	public boolean unban( Player ply, String reason, String unBanBy );
	
	/**
	 * Ban an player, based on IP
	 * @param IP
	 * @param time Time in minutes
	 * @param reason 
	 * @param banBy
	 * @return False on failure
	 */
	public boolean banIp( String IP, int time, String reason, String banBy );
	
	/**
	 * 
	 * @param IP
	 * @param reason
	 * @param unbanBy
	 * @return
	 */
	public boolean unbanIp( String IP, String reason, String unbanBy );
	
	/**
	 * Check if ply has any active bans
	 * @param ply
	 * @return
	 */
	public boolean isBanned( Player ply );
	
	/**
	 * Check if ply has any bans
	 * @param ply
	 * @return
	 */
	public boolean hasBans( Player ply );
	
	/**
	 * How many bans does ply have?
	 * @param ply
	 * @param activeOnly Do you want only active bans?
	 * @return
	 */
	public int ammountOfBans( Player ply , boolean activeOnly );

}
