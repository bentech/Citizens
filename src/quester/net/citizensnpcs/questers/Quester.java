package net.citizensnpcs.questers;

import java.util.List;
import java.util.Map;
import java.util.Set;

import net.citizensnpcs.npctypes.CitizensNPC;
import net.citizensnpcs.npctypes.CitizensNPCType;
import net.citizensnpcs.questers.data.PlayerProfile;
import net.citizensnpcs.questers.quests.Quest;
import net.citizensnpcs.resources.npclib.HumanNPC;
import net.citizensnpcs.utils.PageUtils;
import net.citizensnpcs.utils.PageUtils.PageInstance;
import net.citizensnpcs.utils.StringUtils;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class Quester extends CitizensNPC {
	private final Map<Player, PageInstance> displays = Maps.newHashMap();
	private final Map<Player, Integer> queue = Maps.newHashMap();
	private final Set<Player> pending = Sets.newHashSet();
	private final List<String> quests = Lists.newArrayList();

	public void addQuest(String quest) {
		quests.add(quest);
	}

	public void removeQuest(String quest) {
		quests.remove(quest);
	}

	public boolean hasQuest(String string) {
		return quests.contains(string);
	}

	public List<String> getQuests() {
		return quests;
	}

	@Override
	public void onLeftClick(Player player, HumanNPC npc) {
		cycle(player);
	}

	@Override
	public void onRightClick(Player player, HumanNPC npc) {
		if (QuestManager.hasQuest(player)) {
			checkCompletion(player, npc);
		} else {
			if (displays.get(player) == null) {
				cycle(player);
				return;
			}
			PageInstance display = displays.get(player);
			if (!pending.contains(player)) {
				display.displayNext();
				if (display.currentPage() == display.maxPages()) {
					player.sendMessage(ChatColor.GREEN
							+ "Right click again to accept.");
					pending.add(player);
				}
			} else {
				attemptAssign(player, npc);
			}
		}
	}

	private void cycle(Player player) {
		if (QuestManager.hasQuest(player)) {
			player.sendMessage(ChatColor.GRAY
					+ "Only one quest can be taken on at a time.");
			return;
		}
		if (quests == null || quests.size() == 0) {
			player.sendMessage(ChatColor.GRAY + "No quests are available.");
			return;
		}
		pending.remove(player);
		if (queue.get(player) == null || queue.get(player) + 1 >= quests.size()) {
			queue.put(player, 0);
			if (quests.size() == 1
					&& !QuestManager.canRepeat(player,
							getQuest(fetchFromList(player)))) {
				player.sendMessage(ChatColor.GRAY + "No quests are available.");
				return;
			}
		} else {
			int base = queue.get(player), orig = base;
			while (true) {
				base = base + 1 >= quests.size() ? 0 : base + 1;
				if (QuestManager.canRepeat(player, getQuest(fetch(base)))) {
					break;
				}
				if (base == orig) {
					player.sendMessage(ChatColor.GRAY
							+ "No quests are available.");
					return;
				}
			}
			queue.put(player, base);
		}
		updateDescription(player);
	}

	private void updateDescription(Player player) {
		Quest quest = getQuest(fetchFromList(player));
		if (quest == null)
			return;
		PageInstance display = PageUtils.newInstance(player);
		display.setSmoothTransition(true);
		display.header(ChatColor.GREEN
				+ StringUtils.listify("Quest %x/%y - "
						+ StringUtils.wrap(quest.getName())));
		for (String push : Splitter.on("<br>").omitEmptyStrings()
				.split(quest.getDescription())) {
			display.push(push);
			if ((display.elements() % 8 == 0 && display.maxPages() == 1)
					|| display.elements() % 9 == 0) {
				display.push(ChatColor.GOLD
						+ "Right click to continue description.");
			}
		}
		display.process(1);
		if (display.maxPages() == 1) {
			player.sendMessage(ChatColor.GOLD + "Right click to accept.");
			pending.add(player);
		}
		displays.put(player, display);
	}

	private void attemptAssign(Player player, HumanNPC npc) {
		if (QuestManager.assignQuest(player, npc.getUID(),
				fetchFromList(player))) {
			displays.remove(player);
			pending.remove(player);
		}
	}

	private void checkCompletion(Player player, HumanNPC npc) {
		PlayerProfile profile = PlayerProfile.getProfile(player.getName());
		if (profile.getProgress().getQuesterUID() == npc.getUID()) {
			if (profile.getProgress().isFullyCompleted()) {
				QuestManager.completeQuest(player);
			} else {
				player.sendMessage(ChatColor.GRAY
						+ "The quest isn't completed yet.");
			}
		} else {
			player.sendMessage(ChatColor.GRAY
					+ "You already have a quest from another NPC.");
		}
	}

	private Quest getQuest(String name) {
		return QuestManager.getQuest(name);
	}

	private String fetch(int index) {
		return quests.get(index);
	}

	private String fetchFromList(Player player) {
		return quests.size() > 0 ? fetch(queue.get(player)) : "";
	}

	@Override
	public CitizensNPCType getType() {
		return new QuesterType();
	}
}