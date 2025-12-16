package fr.neralix.infectedtag;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class Main extends JavaPlugin implements Listener {

    private final Set<UUID> infected = new HashSet<>();
    private final Set<UUID> participants = new HashSet<>();
    private final Random random = new Random();


    private static class EventZone {
        Location min;
        Location max;

        boolean isComplete() {
            return min != null && max != null && min.getWorld().equals(max.getWorld());
        }
    }

    private final EventZone[] eventZones = new EventZone[6];

    private Location safeMin;
    private Location safeMax;


    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);

        for (int i = 0; i < eventZones.length; i++) {
            eventZones[i] = new EventZone();
        }

        startSafeZoneParticles();
        startEventZoneParticles();

        getLogger().info("[InfectedTag] Plugin activé !");
    }


    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        switch (cmd.getName().toLowerCase()) {

            case "joininfected" -> handleJoinEvent(player);
            case "leaveinfected" -> handleLeaveEvent(player);
            case "infecter" -> handleInfectCommand(player, args);
            case "desinfecter" -> handleDesinfectCommand(player, args);
            case "desinfectall" -> handleDesinfectAll(player);
            case "infectedlist" -> handleListCommand(player);

            case "setzone1" -> setEventZone(player, 0, true);
            case "setzone2" -> setEventZone(player, 0, false);
            case "setzone3" -> setEventZone(player, 1, true);
            case "setzone4" -> setEventZone(player, 1, false);
            case "setzone5" -> setEventZone(player, 2, true);
            case "setzone6" -> setEventZone(player, 2, false);
            case "setzone7" -> setEventZone(player, 3, true);
            case "setzone8" -> setEventZone(player, 3, false);
            case "setzone9" -> setEventZone(player, 4, true);
            case "setzone10" -> setEventZone(player, 4, false);
            case "setzone11" -> setEventZone(player, 5, true);
            case "setzone12" -> setEventZone(player, 5, false);

            case "setsafezone1" -> setSafeZonePoint(player, true);
            case "setsafezone2" -> setSafeZonePoint(player, false);

            default -> player.sendMessage(ChatColor.RED + "Commande inconnue !");
        }
        return true;
    }


    private void handleJoinEvent(Player player) {
        if (participants.contains(player.getUniqueId())) {
            player.sendMessage(ChatColor.YELLOW + "Tu es déjà dans l'événement !");
            return;
        }
        participants.add(player.getUniqueId());
        player.sendMessage(ChatColor.AQUA + "Tu viens de rejoindre l'événement Infecté !");
        Bukkit.broadcastMessage(ChatColor.GREEN + player.getName() + " vient de rejoindre l'événement !");
    }

    private void handleLeaveEvent(Player player) {
        if (!participants.contains(player.getUniqueId())) {
            player.sendMessage(ChatColor.YELLOW + "Tu n'étais pas dans l'événement !");
            return;
        }
        participants.remove(player.getUniqueId());
        infected.remove(player.getUniqueId());
        removeEffects(player);
        removeProtection(player);
        player.sendMessage(ChatColor.YELLOW + "Tu as quitté l'événement.");
    }

    private void handleInfectCommand(Player player, String[] args) {
        if (args.length != 1) {
            player.sendMessage(ChatColor.RED + "Usage: /infecter <joueur>");
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !participants.contains(target.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Joueur introuvable !");
            return;
        }
        if (infected.contains(target.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Le joueur est déjà infecté !");
            return;
        }
        infectPlayer(target, player);
    }

    private void handleDesinfectCommand(Player player, String[] args) {
        if (args.length != 1) {
            player.sendMessage(ChatColor.RED + "Usage: /desinfecter <joueur>");
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !infected.contains(target.getUniqueId())) {
            player.sendMessage(ChatColor.YELLOW + "Le joueur n'est pas infecté.");
            return;
        }

        infected.remove(target.getUniqueId());
        removeEffects(target);
        removeProtection(target);
        target.sendMessage(ChatColor.AQUA + "Tu viens de redevenir un humain !");
    }

    private void handleDesinfectAll(Player player) {
        for (UUID uuid : new HashSet<>(infected)) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                infected.remove(uuid);
                removeEffects(p);
                removeProtection(p);
                p.sendMessage(ChatColor.AQUA + "Tu as été désinfecté !");
            }
        }
        Bukkit.broadcastMessage(ChatColor.GREEN + "Tous les infectés ont été rendus humains !");
    }

    private void handleListCommand(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Participants InfectedTag ===");
        for (UUID uuid : participants) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                String status = infected.contains(uuid) ? ChatColor.RED + "INFECTÉ" : ChatColor.GREEN + "HUMAIN";
                player.sendMessage("- " + p.getName() + " : " + status);
            }
        }
    }


    @EventHandler
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof Player victim)) return;

        if (participants.contains(attacker.getUniqueId())
                && participants.contains(victim.getUniqueId())
                && infected.contains(attacker.getUniqueId())
                && !infected.contains(victim.getUniqueId())) {

            infectPlayer(victim, attacker);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!infected.contains(player.getUniqueId())) return;
        if (safeMin == null || safeMax == null) return;

        if (isInside(event.getTo(), safeMin, safeMax)) {
            player.setHealth(0);
            player.sendMessage(ChatColor.DARK_RED + "Tu viens de rentrer dans une SAFE ZONE !");
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!infected.contains(player.getUniqueId())) return;

        List<EventZone> validZones = new ArrayList<>();
        for (EventZone z : eventZones)
            if (z.isComplete()) validZones.add(z);

        if (validZones.isEmpty()) return;

        Location spawn = null;

        for (int i = 0; i < 20; i++) {
            EventZone zone = validZones.get(random.nextInt(validZones.size()));
            Location test = getRandomLocationInZone(zone.min, zone.max);
            if (!isInSafeZone(test)) {
                spawn = test;
                break;
            }
        }

        if (spawn != null) {
            event.setRespawnLocation(spawn);
        }

        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (player.isOnline() && infected.contains(player.getUniqueId())) {
                applyProtection(player);
                applyBlindness(player);
                applyConfusion(player);
            }
        }, 20L);
    }


    private void infectPlayer(Player player, Player source) {
        infected.add(player.getUniqueId());
        player.sendMessage(ChatColor.DARK_GREEN + "Tu es INFECTÉ... 🤢");

        if (source != null)
            source.sendMessage(ChatColor.GREEN + "Tu viens d'infecter " + player.getName());

        applyProtection(player);
        applyBlindness(player);
        applyConfusion(player);
    }

    /* ================= EFFETS ================= */

    private void applyProtection(Player player) {
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.DAMAGE_RESISTANCE,
                Integer.MAX_VALUE,
                2,
                false,
                false,
                false
        ));
    }

    private void removeProtection(Player player) {
        player.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
    }

    private void applyBlindness(Player player) {
        new BukkitRunnable() {
            int time = 0;
            public void run() {
                if (!infected.contains(player.getUniqueId()) || !player.isOnline()) {
                    cancel();
                    return;
                }
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.BLINDNESS,
                        20,
                        0,
                        false,
                        false
                ));
                time += 60;
                if (time >= 200) cancel();
            }
        }.runTaskTimer(this, 0L, 60L);
    }

    private void applyConfusion(Player player) {
        new BukkitRunnable() {
            public void run() {
                if (!infected.contains(player.getUniqueId()) || !player.isOnline()) {
                    cancel();
                    return;
                }
                if (random.nextInt(100) < 40) {
                    player.addPotionEffect(new PotionEffect(
                            PotionEffectType.CONFUSION,
                            100,
                            1
                    ));
                }
            }
        }.runTaskTimer(this, 100L, 100L);
    }

    private void removeEffects(Player player) {
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        player.removePotionEffect(PotionEffectType.CONFUSION);
    }

    /* ================= ZONES SET ================= */

    private void setEventZone(Player player, int index, boolean isMin) {
        if (isMin) {
            eventZones[index].min = player.getLocation();
            player.sendMessage(ChatColor.GREEN + "Zone EVENT " + (index + 1) + " - Point 1 défini !");
        } else {
            eventZones[index].max = player.getLocation();
            player.sendMessage(ChatColor.GREEN + "Zone EVENT " + (index + 1) + " - Point 2 défini !");
        }
    }

    private void setSafeZonePoint(Player player, boolean isMin) {
        if (isMin) {
            safeMin = player.getLocation();
            player.sendMessage(ChatColor.RED + "SafeZone - Point 1 défini !");
        } else {
            safeMax = player.getLocation();
            player.sendMessage(ChatColor.RED + "SafeZone - Point 2 défini !");
        }
    }


    private void startSafeZoneParticles() {
        new BukkitRunnable() {
            public void run() {
                if (safeMin == null || safeMax == null) return;
                spawnZoneParticles(safeMin, safeMax, Particle.FLAME);
            }
        }.runTaskTimer(this, 0L, 20L);
    }

    private void startEventZoneParticles() {
        new BukkitRunnable() {
            public void run() {
                for (EventZone z : eventZones)
                    if (z.isComplete())
                        spawnZoneParticles(z.min, z.max, Particle.VILLAGER_HAPPY);
            }
        }.runTaskTimer(this, 0L, 20L);
    }

    private void spawnZoneParticles(Location min, Location max, Particle particle) {
        World world = min.getWorld();

        double minX = Math.min(min.getX(), max.getX());
        double maxX = Math.max(min.getX(), max.getX());
        double minY = Math.min(min.getY(), max.getY());
        double maxY = Math.max(min.getY(), max.getY());
        double minZ = Math.min(min.getZ(), max.getZ());
        double maxZ = Math.max(min.getZ(), max.getZ());

        for (double x = minX; x <= maxX; x++)
            for (double y = minY; y <= maxY; y++) {
                world.spawnParticle(particle, x, y + 0.5, minZ, 1, 0, 0, 0);
                world.spawnParticle(particle, x, y + 0.5, maxZ, 1, 0, 0, 0);
            }

        for (double z = minZ; z <= maxZ; z++)
            for (double y = minY; y <= maxY; y++) {
                world.spawnParticle(particle, minX, y + 0.5, z, 1, 0, 0, 0);
                world.spawnParticle(particle, maxX, y + 0.5, z, 1, 0, 0, 0);
            }
    }


    private boolean isInSafeZone(Location loc) {
        if (safeMin == null || safeMax == null || loc == null) return false;
        return isInside(loc, safeMin, safeMax);
    }
}
