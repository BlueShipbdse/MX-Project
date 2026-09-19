package kireiko.dev.anticheat.checks.clicks;


import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUseItem;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import kireiko.dev.anticheat.api.PacketCheckHandler;
import kireiko.dev.anticheat.api.data.ConfigLabel;
import kireiko.dev.anticheat.api.events.CPacketEvent;
import kireiko.dev.anticheat.api.player.PlayerProfile;
import kireiko.dev.anticheat.managers.CheckManager;
import kireiko.dev.anticheat.utils.version.VersionUtil;
import kireiko.dev.millennium.math.Statistics;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;

import static com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Client.*;

public final class AutoClickerCheck implements PacketCheckHandler {
    private static final int USE_MAIN = 0;
    private static final int USE_OFF = 1;
    private static final int MAIN_BLOCK = 2;
    private static final int OFF_BLOCK = 3;
    private static final int INTERACT_MAIN = 4;
    private static final int INTERACT_OFF = 5;
    private static final int INTERACT_AT_MAIN = 6;
    private static final int INTERACT_AT_OFF = 7;
    private static final int INTERACT_EMPTY_MARKED = 8;

    private final PlayerProfile profile;
    private volatile long leftOldTime = System.currentTimeMillis(), rightOldTime = System.currentTimeMillis(),
                                lastAttack = System.currentTimeMillis(), lastBlockPlacement = System.currentTimeMillis(), lastCompletedDig = System.currentTimeMillis(), lastCancelledDig = System.currentTimeMillis();
    private volatile boolean leftEnabled = true;
    private volatile @MagicConstant(intValues = {USE_MAIN, USE_OFF, MAIN_BLOCK, OFF_BLOCK, INTERACT_MAIN, INTERACT_OFF, INTERACT_AT_MAIN, INTERACT_AT_OFF, INTERACT_EMPTY_MARKED}) int useType = USE_OFF;
    private final DoubleList leftStack = new DoubleArrayList();
    private final DoubleList rightStack = new DoubleArrayList();
    private boolean entropyQuery = false;
    private Map<String, Object> localCfg = new TreeMap<>();

    @Override
    public ConfigLabel config() {
        localCfg.put("enabled", false);
        localCfg.put("addGlobalVl", 20);
        return new ConfigLabel("auto_clicker", localCfg);
    }
    @Override
    public void applyConfig(Map<String, Object> params) {
        localCfg = params;
    }

    @Override
    public Map<String, Object> getConfig() {
        return localCfg;
    }


    public AutoClickerCheck(PlayerProfile profile) {
        this.profile = profile;
        if (CheckManager.classCheck(this.getClass()))
            this.localCfg = CheckManager.getConfig(this.getClass());
    }

    @Override
    public void event(Object o) {
        if (!(boolean) localCfg.get("enabled") || VersionUtil.getVersion().isNewerThanOrEquals(ServerVersion.V_1_21_11)) return;
        if (o instanceof CPacketEvent) {
            var event = ((CPacketEvent) o).getPacketEvent();
            var type = event.getPacketType();
            switch (type) {
                case USE_ITEM -> {
                    WrapperPlayClientUseItem packet = new WrapperPlayClientUseItem(event);
                    if (packet.getHand() == InteractionHand.MAIN_HAND) {
                        if (useType == INTERACT_MAIN) {
                            return;
                        }
                        if (useType <= USE_OFF || useType == INTERACT_EMPTY_MARKED) {
                            markRight();
                        }
                    } else if (packet.getHand() == InteractionHand.OFF_HAND) {
                        if (useType == USE_OFF || useType == INTERACT_EMPTY_MARKED) {
                            markRight();
                        }
                    }
                    useType = packet.getHand() == InteractionHand.MAIN_HAND ? USE_MAIN : USE_OFF;
                }
                case PLAYER_DIGGING -> {
                    WrapperPlayClientPlayerDigging packet = new WrapperPlayClientPlayerDigging(event);
                    switch (packet.getAction()) {
                        case START_DIGGING -> {
                            long currMill = System.currentTimeMillis();
                            long lastCompletedDig = currMill - this.lastCompletedDig;
                            long lastCancelledDig = currMill - this.lastCancelledDig;
                            if (lastCompletedDig > 400 && lastCancelledDig >= 50) {
                                markLeft();
                            }
                            leftEnabled = false;
                        }
                        case FINISHED_DIGGING -> {
                            lastCompletedDig = System.currentTimeMillis();
                            leftEnabled = true;
                        }
                        case CANCELLED_DIGGING -> {
                            lastCancelledDig = System.currentTimeMillis();
                            leftEnabled = true;
                        }
                    }
                }
                case PLAYER_BLOCK_PLACEMENT -> {
                    WrapperPlayClientPlayerBlockPlacement packet = new WrapperPlayClientPlayerBlockPlacement(event);
                    if (packet.getHand() == InteractionHand.MAIN_HAND) {
                        markRight();
                    } else if (packet.getHand() == InteractionHand.OFF_HAND && useType == OFF_BLOCK) {
                        //TODO impossible scenario
                        return;
                    }
                    useType = packet.getHand() == InteractionHand.MAIN_HAND ? MAIN_BLOCK : OFF_BLOCK;
                    lastBlockPlacement = System.currentTimeMillis();
                }
                case ANIMATION -> {
                    if (leftEnabled) {
                        long currMill = System.currentTimeMillis();
                        long lastBlockPlacement = currMill - this.lastBlockPlacement;
                        long lastAttack = currMill - this.lastAttack;
                        long lastCompletedDig = currMill - this.lastCompletedDig;
                        long lastCancelledDig = currMill - this.lastCancelledDig;
                        if (lastBlockPlacement > 10 && lastAttack > 3 && lastCompletedDig > 300 && lastCancelledDig >= 50) {
                            markLeft();
                        }
                    }
                }
                default -> {
                    boolean attack = false;
                    if (VersionUtil.getVersion().isNewerThanOrEquals(ServerVersion.V_26_1)) {
                        attack = type == ATTACK;
                    } else {
                        if (type == INTERACT_ENTITY) {
                            WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
                            if (packet.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                                int oldType = useType;
                                useType = packet.getAction() == WrapperPlayClientInteractEntity.InteractAction.INTERACT
                                                        ? packet.getHand() == InteractionHand.MAIN_HAND ? INTERACT_MAIN
                                                            : INTERACT_OFF
                                                        : packet.getHand() == InteractionHand.MAIN_HAND ? INTERACT_AT_MAIN
                                                            : INTERACT_AT_OFF;
                                boolean valid = switch (useType) {
                                    case INTERACT_AT_MAIN -> oldType == INTERACT_OFF || oldType < INTERACT_MAIN;
                                    case INTERACT_MAIN -> oldType == INTERACT_AT_MAIN;
                                    case INTERACT_AT_OFF -> oldType == INTERACT_MAIN;
                                    case INTERACT_OFF -> {
                                        Player player = event.getPlayer();
                                        if (oldType == INTERACT_AT_OFF && player.getInventory().getItemInOffHand().isEmpty()) {
                                            useType = INTERACT_EMPTY_MARKED;
                                            yield true;
                                        }
                                        yield false;
                                    }
                                    default -> throw new IllegalStateException("Unexpected value: " + oldType);
                                };

                                if (!valid) {
                                    //TODO impossible scenario
                                }
                            } else {
                                attack = true;
                            }
                        }
                    }
                    if (attack) {
                        if (!leftEnabled) {
                            //TODO impossible scenario
                            return;
                        }
                        lastAttack = System.currentTimeMillis();
                        markLeft();
                    }

                }
            }
        } else if (o instanceof BlockBreakEvent event) {
            if (event.isCancelled()) return;
            if (!leftEnabled) {
                leftEnabled = true;
                lastCompletedDig = System.currentTimeMillis();
            }
        }
    }

    private void markLeft() {
        if (!leftEnabled) return;

        double delay = (System.currentTimeMillis() - leftOldTime) / 50d;
        if (delay < 25) {
            leftStack.add(delay);
            if (leftStack.size() >= 100) {
                checkLeft();
            }
        }
        leftOldTime = System.currentTimeMillis();
    }

    private void markRight() {
        double delay = (System.currentTimeMillis() - rightOldTime) / 50d;
        if (delay < 25) {
            rightStack.add(delay);
            if (rightStack.size() >= 100) {
                checkRight();
            }
        }
        rightOldTime = System.currentTimeMillis();
    }

    private void checkLeft() {
        check(leftStack);
    }

    private void checkRight() {
        check(rightStack);
    }

    private void check(@NotNull DoubleList stack) {
        { // analysis
            final DoubleList shannonStack = new DoubleArrayList();
            final DoubleList localDeltaStack = new DoubleArrayList();
            for (double delay : stack) {
                localDeltaStack.add(delay);
                if (localDeltaStack.size() >= 20) {
                    shannonStack.add(Statistics.getShannonEntropy(localDeltaStack));
                    localDeltaStack.clear();
                }
            }
            final float vl = ((Number) localCfg.get("addGlobalVl")).floatValue() / 10;
            double kurtosis = Statistics.getKurtosis(stack);
            profile.debug("&7Auto Clicker Kurtosis (" + kurtosis + ") => <stack>: " + kurtosis);
            if (kurtosis < 0 || Double.isNaN(kurtosis)) {
                profile.punish("AutoClicker", "Kurtosis", "Analysis <negative> [" + kurtosis + "]", vl);
            } else {
                final List<Float> jiff = Statistics.getJiffDelta(shannonStack, 2);
                double min = Statistics.getMin(jiff);
                double max = Statistics.getMax(jiff);
                profile.debug("&7Auto Clicker Entropy: (" + min + ", " + max + ")" + ", Entropy: (" + jiff + ") => Initial Entropy Size: " + shannonStack.size() + ". Query? " + entropyQuery);
                if (min < 0.1 && max < 0.15) {
                    if (!entropyQuery) {
                        entropyQuery = true;
                    } else {
                        profile.punish("AutoClicker", "Entropy", "Analysis <min> (" + jiff + ") => " + min, vl);
                    }
                } else {
                    entropyQuery = false;
                }
            }
        }
        stack.clear();
    }
}
