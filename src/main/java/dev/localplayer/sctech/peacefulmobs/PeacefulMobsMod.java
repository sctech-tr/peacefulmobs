package dev.localplayer.sctech.peacefulmobs;

import dev.localplayer.sctech.peacefulmobs.mixin.MobEntityAccessor;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

import net.minecraft.world.entity.ai.goal.Goal;

import java.lang.reflect.Field;
import java.util.Locale;

public class PeacefulMobsMod implements ModInitializer {

	@Override
	public void onInitialize() {

		// Handle mobs when they load
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
			if (entity instanceof Mob mob) {
				handleMobLoad(mob);
			}
		});
	}

	private static void handleMobLoad(Mob mob) {
		removePlayerTargetGoals(mob);
	}

	private static void removePlayerTargetGoals(Mob mob) {
		try {
			MobEntityAccessor accessor = (MobEntityAccessor) mob;

			// Clear ALL target goals → disables attacking
			accessor.getTargetSelector().removeAllGoals(goal -> true);

		} catch (Throwable t) {
			System.err.println("[peacefulmobs] Failed to clear goals for " + mob.getName().getString());
			t.printStackTrace();
		}
	}

	@SuppressWarnings("rawtypes")
	private static boolean isActiveTargetForPlayers(Goal g) {
		try {
			if (g == null) return false;

			if (
					g.getClass().getName().contains("ActiveTargetGoal") ||
							g.getClass().getName().toLowerCase(Locale.ROOT).contains("target")
			) {

				Field targetClassField = null;
				Class<?> cls = g.getClass();

				while (cls != null) {
					try {
						targetClassField = cls.getDeclaredField("targetClass");
						break;

					} catch (NoSuchFieldException e) {

						try {
							targetClassField = cls.getDeclaredField("targetType");
							break;

						} catch (NoSuchFieldException ignored) {
							cls = cls.getSuperclass();
						}
					}
				}

				if (targetClassField != null) {
					targetClassField.setAccessible(true);

					Object val = targetClassField.get(g);

					if (val instanceof Class) {
						Class c = (Class) val;

						if (Player.class.isAssignableFrom(c)) {
							return true;
						}
					}
				}
			}

		} catch (Throwable ignored) {}

		String name = g.getClass().getSimpleName().toLowerCase(Locale.ROOT);

		return name.contains("player") || name.contains("target");
	}
}