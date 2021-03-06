package net.citizensnpcs.guards;

import java.util.ArrayList;
import java.util.List;

import net.citizensnpcs.SettingsManager;
import net.citizensnpcs.SettingsManager.SettingsType;
import net.citizensnpcs.guards.GuardManager.GuardState;
import net.citizensnpcs.guards.flags.FlagInfo;
import net.citizensnpcs.guards.flags.FlagList;
import net.citizensnpcs.guards.flags.FlagList.FlagType;
import net.citizensnpcs.properties.Node;
import net.citizensnpcs.properties.Properties;
import net.citizensnpcs.properties.PropertyManager;
import net.citizensnpcs.resources.npclib.HumanNPC;

public class GuardProperties extends PropertyManager implements Properties {
	private static final String isGuard = ".guard.toggle";
	private static final String type = ".guard.type";
	private static final String radius = ".guard.radius";
	private static final String flag = ".guard.flags";
	private static final String aggressive = ".guard.aggressive";

	public static final GuardProperties INSTANCE = new GuardProperties();

	private GuardProperties() {
	}

	private void saveProtectionRadius(int UID, double rad) {
		profiles.setDouble(UID + radius, rad);
	}

	private double getProtectionRadius(int UID) {
		return profiles.getDouble(UID + radius,
				SettingsManager.getDouble("DefaultBouncerProtectionRadius"));
	}

	private void saveAggressive(int UID, boolean aggro) {
		profiles.setBoolean(UID + aggressive, aggro);
	}

	private boolean isAggressive(int UID) {
		return profiles.getBoolean(UID + aggressive, true);
	}

	private GuardState getGuardState(int UID) {
		return GuardState.parse(profiles.getString(UID + type));
	}

	private void saveGuardState(int UID, GuardState state) {
		profiles.setString(UID + type, state.name());
	}

	private void loadFlags(Guard guard, int UID) {
		String root = UID + flag, path = root;
		if (!profiles.pathExists(root)) {
			return;
		}
		FlagList flags = guard.getFlags();
		for (String key : profiles.getKeys(root)) {
			path = root + "." + key;
			boolean isSafe = profiles.getBoolean(path + ".safe");
			int priority = profiles.getInt(path + ".priority");
			flags.addFlag(FlagType.parse(profiles.getString(path + ".type")),
					FlagInfo.newInstance(key, priority, isSafe));
		}
	}

	private void saveFlags(int UID, FlagList flags) {
		String root = UID + flag, path = root;
		for (FlagType type : FlagType.values()) {
			for (FlagInfo info : flags.getFlags(type).values()) {
				path = root + "." + info.getName();
				profiles.setString(path + ".type", type.name());
				profiles.setBoolean(path + ".safe", info.isSafe());
				profiles.setInt(path + ".priority", info.priority());
			}
		}
	}

	@Override
	public void saveState(HumanNPC npc) {
		if (exists(npc)) {
			boolean is = npc.isType("guard");
			setEnabled(npc, is);
			if (is) {
				Guard guard = npc.getType("guard");
				saveGuardState(npc.getUID(), guard.getGuardState());
				saveFlags(npc.getUID(), guard.getFlags());
				saveProtectionRadius(npc.getUID(), guard.getProtectionRadius());
				saveAggressive(npc.getUID(), guard.isAggressive());
			}
		}
	}

	@Override
	public void loadState(HumanNPC npc) {
		if (getEnabled(npc)) {
			npc.registerType("guard");
			Guard guard = npc.getType("guard");
			loadFlags(guard, npc.getUID());
			guard.setGuardState(getGuardState(npc.getUID()));
			guard.setProtectionRadius(getProtectionRadius(npc.getUID()));
			guard.setAggressive(isAggressive(npc.getUID()));
		}
		saveState(npc);
	}

	@Override
	public void setEnabled(HumanNPC npc, boolean value) {
		profiles.setBoolean(npc.getUID() + isGuard, value);
	}

	@Override
	public boolean getEnabled(HumanNPC npc) {
		return profiles.getBoolean(npc.getUID() + isGuard);
	}

	@Override
	public void copy(int UID, int nextUID) {
		if (profiles.pathExists(UID + isGuard)) {
			profiles.setString(nextUID + isGuard,
					profiles.getString(UID + isGuard));
		}
		if (profiles.pathExists(UID + type)) {
			profiles.setString(nextUID + type, profiles.getString(UID + type));
		}
		if (profiles.pathExists(UID + flag)) {
			profiles.setString(nextUID + flag, profiles.getString(UID + flag));
		}
		if (profiles.pathExists(UID + radius)) {
			profiles.setString(nextUID + radius,
					profiles.getString(UID + radius));
		}
		if (profiles.pathExists(UID + aggressive)) {
			profiles.setString(nextUID + aggressive,
					profiles.getString(UID + aggressive));
		}
	}

	@Override
	public List<Node> getNodes() {
		List<Node> nodes = new ArrayList<Node>();
		nodes.add(new Node("MaxStationaryReturnTicks", SettingsType.GENERAL,
				"guards.max.stationary-return-ticks", 25));
		nodes.add(new Node("GuardRespawnDelay", SettingsType.GENERAL,
				"guards.respawn-delay", 100));
		nodes.add(new Node("DefaultBouncerProtectionRadius",
				SettingsType.GENERAL,
				"guards.bouncers.default.protection-radius", 10));
		return nodes;
	}
}