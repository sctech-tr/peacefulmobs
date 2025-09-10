package dev.localplayer.sctech.peacefulmobs;

import dev.localplayer.sctech.peacefulmobs.mixins.MobEntityAccessor;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;

public class PeacefulMobsMod implements ModInitializer {

	@Override
	public void onInitialize() {


		// Handle mobs when they load
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
			if (!world.isClient && entity instanceof MobEntity) {
				handleMobLoad((MobEntity) entity);
			}
		});
	}


	private static void handleMobLoad(MobEntity mob) {
		try {
			mob.setPersistent();
		} catch (Throwable ignored) {}
		removePlayerTargetGoals(mob);
	}


	private static void removePlayerTargetGoals(MobEntity mob) {
		try {
			MobEntityAccessor accessor = (MobEntityAccessor) mob;
			// Clear ALL target goals → disables attacking
			accessor.getTargetSelector().clear(predicate -> true);
		} catch (Throwable t) {
			System.err.println("[peacefulmobs] Failed to clear goals for " + mob.getName().getString());
			t.printStackTrace();
		}
	}


	@SuppressWarnings("rawtypes")
	private static boolean isActiveTargetForPlayers(Goal g) {
		try {
			if (g == null) return false;
			if (g.getClass().getName().contains("ActiveTargetGoal") || g.getClass().getName().toLowerCase(Locale.ROOT).contains("target")) {
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
						if (PlayerEntity.class.isAssignableFrom(c)) return true;
					}
				}
			}
		} catch (Throwable ignored) {}
		String name = g.getClass().getSimpleName().toLowerCase(Locale.ROOT);
		return name.contains("player") || name.contains("target");
	}

}
