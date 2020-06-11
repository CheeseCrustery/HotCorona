package me.noodian.corona;

import org.bukkit.scheduler.BukkitRunnable;
import java.util.ArrayList;

public class UpdateManager extends BukkitRunnable {

	private static UpdateManager m_Instance = null;

	public ArrayList<Ticking> Objects;

	private UpdateManager() {
		Objects = new ArrayList<Ticking>();
	}

	public static UpdateManager GetInstance() {
		if (m_Instance == null) m_Instance = new UpdateManager();
		return m_Instance;
	}

	@Override
	public void run() {
		for (Ticking object : Objects) {
			object.Tick();
		}
	}
}
