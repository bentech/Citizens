package net.citizensnpcs.questers.data;

import java.util.List;

import net.citizensnpcs.SettingsManager.SettingsType;
import net.citizensnpcs.properties.Node;
import net.citizensnpcs.properties.Properties;
import net.citizensnpcs.properties.PropertyManager;
import net.citizensnpcs.questers.Quester;
import net.citizensnpcs.resources.npclib.HumanNPC;

import com.google.common.collect.Lists;

public class QuesterProperties extends PropertyManager implements Properties {
	public static final QuesterProperties INSTANCE = new QuesterProperties();

	private QuesterProperties() {
	}

	private static final String isQuester = ".quester.toggle";
	private static final String quests = ".quester.quests";

	@Override
	public void saveState(HumanNPC npc) {
		if (exists(npc)) {
			setEnabled(npc, npc.isType("quester"));
			setQuests(npc);
		}
	}

	private void setQuests(HumanNPC npc) {
		StringBuilder write = new StringBuilder();
		Quester quester = npc.getType("quester");
		for (String quest : quester.getQuests()) {
			if (!write.toString().contains(quest))
				write.append(quest + ";");
		}
		profiles.setString(npc.getUID() + quests, write.toString());
	}

	private String getQuests(HumanNPC npc) {
		if (profiles.pathExists(npc.getUID() + quests)) {
			Quester quester = npc.getType("quester");
			for (String quest : profiles.getString(npc.getUID() + quests)
					.split(";")) {
				quester.addQuest(quest);
			}
			return profiles.getString(npc.getUID() + quests);
		}
		return "";
	}

	@Override
	public void loadState(HumanNPC npc) {
		if (getEnabled(npc)) {
			npc.registerType("quester");
			getQuests(npc);
		}
		saveState(npc);
	}

	@Override
	public void setEnabled(HumanNPC npc, boolean value) {
		profiles.setBoolean(npc.getUID() + isQuester, value);
	}

	@Override
	public boolean getEnabled(HumanNPC npc) {
		return profiles.getBoolean(npc.getUID() + isQuester);
	}

	@Override
	public void copy(int UID, int nextUID) {
		if (profiles.pathExists(UID + isQuester)) {
			profiles.setString(nextUID + isQuester,
					profiles.getString(UID + isQuester));
		}
		if (profiles.pathExists(UID + quests)) {
			profiles.setString(nextUID + quests,
					profiles.getString(UID + quests));
		}
	}

	@Override
	public List<Node> getNodes() {
		List<Node> nodes = Lists.newArrayList();
		nodes.add(new Node("QuestSaveDelay", SettingsType.GENERAL,
				"quests.save.command-delay-ms", 5000));
		nodes.add(new Node("ItemExploitCheckDelay", SettingsType.GENERAL,
				"quests.exploits.item-pickup.check-delay", 400));
		nodes.add(new Node("CombatExploitTimes", SettingsType.GENERAL,
				"quests.exploits.combat.check-times", 2));
		nodes.add(new Node("CombatExploitRadius", SettingsType.GENERAL,
				"quests.exploits.combat.check-radius", 20));
		nodes.add(new Node("BlockTrackingRemoveDelay", SettingsType.GENERAL,
				"quests.exploits.blocks.tracking-remove-delay", 6000));
		return nodes;
	}
}