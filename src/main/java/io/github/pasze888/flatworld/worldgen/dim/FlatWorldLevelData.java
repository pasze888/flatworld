package io.github.pasze888.flatworld.worldgen.dim;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.storage.WorldData;
import net.minecraft.world.level.timers.TimerCallbacks;
import net.minecraft.world.level.timers.TimerQueue;

/**
 * flat_world 维度专用的 {@link ServerLevelData}：把该维度的时间与天气从主世界解耦。
 *
 * <p>非主世界维度默认拿到的是 {@link DerivedLevelData}——它的 getter 委托主世界、setter 全是空实现，
 * 所以 {@code level.setDayTime(...)} 与 {@code level.setWeatherParameters(...)} 在该维度上根本写不进去，
 * 时间与天气完全跟随主世界（这也是移除 DimensionType 的 {@code fixed_time} 之后仍需要本类的原因）。
 * 换成持有独立副本之后，配置里的时间/天气模式才有落点。
 *
 * <p>两处刻意的细节：
 * <ul>
 *   <li><b>天气的 setter 一律忽略写入。</b>原版 {@code ServerLevel#advanceWeatherCycle()} 每 tick 都会读
 *       {@code levelData} 的天气、算完之后再写回 {@code serverLevelData}；若接受写入，它算出的随机天气
 *       就会盖掉配置设定的值。让写入全部落空，天气便只能由 {@link #applyWeather} 单方面设定。</li>
 *   <li><b>{@link #getScheduledEvents()} 返回自有队列。</b>{@link DerivedLevelData} 会把它委托给主世界的
 *       {@code TimerQueue}；而本维度自行推进时间、计划事件也随之由本维度 tick，委托会导致主世界的计划事件
 *       被 tick 两次。</li>
 * </ul>
 *
 * <p><b>来源说明：</b>「用一个自实现的 {@link ServerLevelData} 覆写时间与天气访问器，让非主世界维度脱离
 * 主世界时钟」这一思路参考自 Re-Avaritia 的
 * {@code committee.nova.mods.avaritia.common.dimension.PersonalLevelData}
 * （<a href="https://github.com/Nova-Committee/Re-Avaritia">Nova-Committee/Re-Avaritia</a>，
 * MIT License, Copyright (c) 2022 cnlimiter）。本类是独立实现，与之的差异：去掉了 FOLLOW 系列模式
 * （门控标志由 4 个减为 1 个 {@link #timeCycling}）、不覆写 gameTime、把计划事件换成维度自己的
 * {@link TimerQueue}。差异的完整论证见 {@code docs/design/time-weather-modes.md}，版权声明见 README。
 * 未使用 Re-Avaritia 的任何素材（其素材为 CC BY-NC-SA 4.0）。
 */
public class FlatWorldLevelData extends DerivedLevelData {

    /**
     * 固定天气模式下写入天气计时器的值。它只用来让 {@code advanceWeatherCycle()} 走稳定的分支：
     * CLEAR 时 clearWeatherTime &gt; 0 会一直清零天气；RAIN/THUNDER 时它大于 0 则不会触发天气翻转。
     */
    public static final int FIXED_WEATHER_TIME = 6000;

    /**
     * 时间模式。{@link #DAY} / {@link #NIGHT} 把时间钉死在固定时刻，{@link #CYCLE} 让时间自行流动。
     */
    public enum TimeMode {
        /** 正午，等价于原 DimensionType 的 {@code fixed_time: 6000}。 */
        DAY(6000L),
        /** 午夜。 */
        NIGHT(18000L),
        /** 自行流动（仍受 {@code doDaylightCycle} 游戏规则约束）。 */
        CYCLE(0L);

        private final long fixedDayTime;

        TimeMode(long fixedDayTime) {
            this.fixedDayTime = fixedDayTime;
        }

        /** 固定模式下要钉住的 {@code dayTime}；{@link #CYCLE} 无意义。 */
        public long fixedDayTime() {
            return this.fixedDayTime;
        }
    }

    /**
     * 天气模式。三档都是固定值——原版那套随机天气循环不适合一个各档都要"稳定可控"的维度。
     */
    public enum WeatherMode {
        CLEAR,
        RAIN,
        THUNDER
    }

    /**
     * 本维度自己的计划事件队列，避免 {@code getScheduledEvents()} 委托给主世界造成双重 tick。
     */
    private final TimerQueue<MinecraftServer> scheduledEvents = new TimerQueue<>(TimerCallbacks.SERVER_CALLBACKS);

    private long dayTime;
    private boolean raining;
    private boolean thundering;
    private int rainTime;
    private int thunderTime;
    private int clearWeatherTime;

    /**
     * {@link #CYCLE} 模式下为 true，此时 {@link #setDayTime(long)} 接受外部写入（{@code /time set}、睡觉跳夜）。
     * 固定模式下为 false，外部写入一律落空，时间只由 {@link #applyTime} 钉住。
     */
    private boolean timeCycling;

    public FlatWorldLevelData(WorldData worldData, ServerLevelData wrapped) {
        super(worldData, wrapped);
        this.dayTime = wrapped.getDayTime();
    }

    /**
     * 按模式设定时间。每 tick 调用，固定模式下写的是同一个值，因此是幂等的。
     *
     * @param cycleAdvance 仅在 {@link TimeMode#CYCLE} 下有意义：是否推进一 tick
     *                     （传入 {@code doDaylightCycle} 的结果，与原版语义保持一致）
     */
    public void applyTime(TimeMode mode, boolean cycleAdvance) {
        this.timeCycling = mode == TimeMode.CYCLE;
        switch (mode) {
            case DAY, NIGHT -> this.dayTime = mode.fixedDayTime();
            case CYCLE -> {
                if (cycleAdvance) {
                    this.dayTime++;
                }
            }
        }
    }

    /**
     * 按模式设定天气。每 tick 调用，写的是同一组值，因此是幂等的。
     *
     * <p>{@code clearWeatherTime} 只在 CLEAR 下非零：它让 {@code advanceWeatherCycle()} 每 tick 都走
     * "正在放晴"分支，从而无法翻转到下雨。
     */
    public void applyWeather(WeatherMode mode) {
        switch (mode) {
            case CLEAR -> {
                this.clearWeatherTime = FIXED_WEATHER_TIME;
                this.rainTime = 0;
                this.thunderTime = 0;
                this.raining = false;
                this.thundering = false;
            }
            case RAIN -> {
                this.clearWeatherTime = 0;
                this.rainTime = FIXED_WEATHER_TIME;
                this.thunderTime = 0;
                this.raining = true;
                this.thundering = false;
            }
            case THUNDER -> {
                this.clearWeatherTime = 0;
                this.rainTime = FIXED_WEATHER_TIME;
                this.thunderTime = FIXED_WEATHER_TIME;
                this.raining = true;
                this.thundering = true;
            }
        }
    }

    @Override
    public long getDayTime() {
        return this.dayTime;
    }

    @Override
    public void setDayTime(long time) {
        if (this.timeCycling) {
            this.dayTime = time;
        }
    }

    @Override
    public boolean isRaining() {
        return this.raining;
    }

    @Override
    public void setRaining(boolean raining) {
        // 忽略：天气只由 applyWeather 设定，否则 advanceWeatherCycle 每 tick 会把随机天气写回来。
    }

    @Override
    public int getRainTime() {
        return this.rainTime;
    }

    @Override
    public void setRainTime(int time) {
        // 忽略，见 setRaining。
    }

    @Override
    public boolean isThundering() {
        return this.thundering;
    }

    @Override
    public void setThundering(boolean thundering) {
        // 忽略，见 setRaining。
    }

    @Override
    public int getThunderTime() {
        return this.thunderTime;
    }

    @Override
    public void setThunderTime(int time) {
        // 忽略，见 setRaining。
    }

    @Override
    public int getClearWeatherTime() {
        return this.clearWeatherTime;
    }

    @Override
    public void setClearWeatherTime(int time) {
        // 忽略，见 setRaining。
    }

    @Override
    public TimerQueue<MinecraftServer> getScheduledEvents() {
        return this.scheduledEvents;
    }
}
