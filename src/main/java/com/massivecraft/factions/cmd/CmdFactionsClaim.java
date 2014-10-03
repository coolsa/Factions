package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Perm;
import com.massivecraft.factions.cmd.arg.ARFaction;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.factions.entity.MPerm;
import com.massivecraft.factions.task.SpiralTask;
import com.massivecraft.massivecore.cmd.arg.ARInteger;
import com.massivecraft.massivecore.cmd.req.ReqHasPerm;
import com.massivecraft.massivecore.cmd.req.ReqIsPlayer;
import com.massivecraft.massivecore.ps.PS;


public class CmdFactionsClaim extends FactionsCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdFactionsClaim()
	{
		// Aliases
		this.addAliases("claim");

		// Args
		this.addOptionalArg("radius", "1");
		this.addOptionalArg("faction", "you");

		// Requirements
		this.addRequirements(ReqIsPlayer.get());
		this.addRequirements(ReqHasPerm.get(Perm.CLAIM.node));
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform()
	{	
		// Args
		Integer radius = this.arg(0, ARInteger.get(), 1);
		if (radius == null) return;
		
		final Faction forFaction = this.arg(1, ARFaction.get(), msenderFaction);
		if (forFaction == null) return;
		
		// MPerm
		if (forFaction.isNormal() && !MPerm.getPermTerritory().has(msender, forFaction, true)) return;
		
		// Validate
		if (radius < 1)
		{
			msg("<b>If you specify a radius, it must be at least 1.");
			return;
		}
		
		if (radius > MConf.get().radiusClaimRadiusLimit && !msender.isUsingAdminMode())
		{
			msg("<b>The maximum radius allowed is <h>%s<b>.", MConf.get().radiusClaimRadiusLimit);
			return;
		}
		
		// Apply
		
		// single chunk
		if (radius < 2)
		{
			msender.tryClaim(forFaction, PS.valueOf(me), true, true);
			return;
		}
		
		// radius claim
		if (!Perm.CLAIM_RADIUS.has(sender, false))
		{
			msg("<b>You do not have permission to claim in a radius.");
			return;
		}

		// TODO: There must be a better way than using a spiral task.
		// TODO: Do some research to allow for claming sets of chunks in a batch with atomicity.
		// This will probably result in an alteration to the owner change event.
		// It would possibly contain a set of chunks instead of a single chunk.
		
		new SpiralTask(PS.valueOf(me), radius)
		{
			private int failCount = 0;
			private final int limit = MConf.get().radiusClaimFailureLimit - 1;

			@Override
			public boolean work()
			{
				boolean success = msender.tryClaim(forFaction, PS.valueOf(this.currentLocation()), true, true);
				if (success)
				{
					this.failCount = 0;
				}
				else if (this.failCount++ >= this.limit)
				{
					this.stop();
					return false;
				}
				return true;
			}
		};
		
	}
	
}
